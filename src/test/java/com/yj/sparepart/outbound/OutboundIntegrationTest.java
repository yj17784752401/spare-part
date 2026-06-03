package com.yj.sparepart.outbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yj.sparepart.dto.OutboundRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 出库功能集成测试
 * 覆盖以下测试用例：
 * TC01: 库存充足直接出库
 * TC02: 库存不足时使用替代备件出库
 * TC03: 多替代备件按优先级选择
 * TC04: 所有库存不足回滚
 * TC05: 出库后触发库存预警
 * TC06: 并发扣减超卖控制
 * TC07: 备件不存在或已禁用
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OutboundIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 每个测试方法前重建数据表及基础测试数据
     */
    @BeforeEach
    void setUp() {
        // 清空并重建所有表
        jdbcTemplate.execute("DROP TABLE IF EXISTS inventory_alert;");
        jdbcTemplate.execute("DROP TABLE IF EXISTS outbound_record;");
        jdbcTemplate.execute("DROP TABLE IF EXISTS substitution;");
        jdbcTemplate.execute("DROP TABLE IF EXISTS inventory;");
        jdbcTemplate.execute("DROP TABLE IF EXISTS spare_part;");

        // 创建表结构（与 src/main/resources/db/schema.sql 一致）
        jdbcTemplate.execute("CREATE TABLE spare_part (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, part_code VARCHAR(64) NOT NULL UNIQUE, " +
                "name VARCHAR(128) NOT NULL, alert_threshold INT, enabled TINYINT DEFAULT 1, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        jdbcTemplate.execute("CREATE TABLE inventory (" +
                "part_id BIGINT PRIMARY KEY, quantity INT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0)");
        jdbcTemplate.execute("CREATE TABLE substitution (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, source_id BIGINT NOT NULL, target_id BIGINT NOT NULL, " +
                "priority INT NOT NULL DEFAULT 99, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        jdbcTemplate.execute("CREATE TABLE outbound_record (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, request_part_id BIGINT NOT NULL, " +
                "actual_outbound_part_id BIGINT NOT NULL, quantity INT NOT NULL, substituted TINYINT NOT NULL DEFAULT 0, " +
                "outbound_time DATETIME NOT NULL, reason VARCHAR(256))");
        jdbcTemplate.execute("CREATE TABLE inventory_alert (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, part_id BIGINT NOT NULL, alert_type VARCHAR(32) NOT NULL, " +
                "resolved TINYINT NOT NULL DEFAULT 0, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        // 插入测试备件数据
        jdbcTemplate.update("INSERT INTO spare_part (id, part_code, name, alert_threshold, enabled) VALUES (1001,'A','轴承A',10,1)");
        jdbcTemplate.update("INSERT INTO spare_part (id, part_code, name, alert_threshold, enabled) VALUES (1002,'B','轴承B',5,1)");
        jdbcTemplate.update("INSERT INTO spare_part (id, part_code, name, alert_threshold, enabled) VALUES (1003,'C','轴承C',2,1)");
        jdbcTemplate.update("INSERT INTO spare_part (id, part_code, name, alert_threshold, enabled) VALUES (1004,'D','禁用件',10,0)");

        // 插入初始库存
        jdbcTemplate.update("INSERT INTO inventory (part_id, quantity, version) VALUES (1001,20,0)");
        jdbcTemplate.update("INSERT INTO inventory (part_id, quantity, version) VALUES (1002,5,0)");
        jdbcTemplate.update("INSERT INTO inventory (part_id, quantity, version) VALUES (1003,0,0)");
        jdbcTemplate.update("INSERT INTO inventory (part_id, quantity, version) VALUES (1004,10,0)");

        // 插入替代关系：A(1001) ↔ B(1002) priority=1； A ↔ C(1003) priority=2
        jdbcTemplate.update("INSERT INTO substitution (source_id, target_id, priority) VALUES (1001,1002,1)");
        jdbcTemplate.update("INSERT INTO substitution (source_id, target_id, priority) VALUES (1001,1003,2)");
    }

    /**
     * TC01: 请求出库数量 ≤ 当前备件库存
     * 预期：扣减原备件库存，substituted=false，库存减少正确，无预警（若新库存 > 阈值）
     */
    @Test
    @Order(1)
    void TC01_outboundSufficientStock() throws Exception {
        OutboundRequest req = new OutboundRequest();
        req.setPartId(1001L);
        req.setQuantity(5);
        req.setReason("正常出库");

        mockMvc.perform(post("/api/v1/outbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.substituted").value(false))
                .andExpect(jsonPath("$.data.actualOutboundPartId").value(1001))
                .andExpect(jsonPath("$.data.finalStock").value(15)); // 20-5=15

        // 验证库存扣减
        Map<String, Object> inv = jdbcTemplate.queryForMap("SELECT quantity,version FROM inventory WHERE part_id=1001");
        assertEquals(15, (int) inv.get("quantity"));

        // 检查无预警 (15 > 阈值10)
        int alertCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_alert WHERE part_id=1001", Integer.class);
        assertEquals(0, alertCount);
    }

    /**
     * TC02: 请求出库数量 > 当前备件库存，但存在一个优先级最高的可替代备件库存足够
     * 预期：扣减可替代备件库存，substituted=true，actual_outbound_id 为替代备件ID
     */
    @Test
    @Order(2)
    void TC02_outboundWithSubstitute() throws Exception {
        // 调整库存：A=10, B=15，使A不足但B充足
        jdbcTemplate.update("UPDATE inventory SET quantity=10 WHERE part_id=1001");
        jdbcTemplate.update("UPDATE inventory SET quantity=15 WHERE part_id=1002");

        OutboundRequest req = new OutboundRequest();
        req.setPartId(1001L);
        req.setQuantity(12); // A库存10不足，B有15足够
        req.setReason("使用替代");

        mockMvc.perform(post("/api/v1/outbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.substituted").value(true))
                .andExpect(jsonPath("$.data.actualOutboundPartId").value(1002))
                .andExpect(jsonPath("$.data.finalStock").value(3)); // B=15-12=3

        // 验证A库存未变，B库存减少
        assertEquals(10, jdbcTemplate.queryForObject("SELECT quantity FROM inventory WHERE part_id=1001", Integer.class));
        assertEquals(3, jdbcTemplate.queryForObject("SELECT quantity FROM inventory WHERE part_id=1002", Integer.class));
    }

    /**
     * TC03: 多个可替代备件，按优先级选择第一个库存足够的
     * 预期：正确选择 priority 数值小的且库存足够的备件
     */
    @Test
    @Order(3)
    void TC03_substitutePriority() throws Exception {
        // A=5, B=8 (priority 1), C=20 (priority 2) ，请求A出库10 -> B不足，应选C
        jdbcTemplate.update("UPDATE inventory SET quantity=5 WHERE part_id=1001");
        jdbcTemplate.update("UPDATE inventory SET quantity=8 WHERE part_id=1002");
        jdbcTemplate.update("UPDATE inventory SET quantity=20 WHERE part_id=1003");

        OutboundRequest req = new OutboundRequest();
        req.setPartId(1001L);
        req.setQuantity(10);
        req.setReason("优先级测试");

        mockMvc.perform(post("/api/v1/outbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.substituted").value(true))
                .andExpect(jsonPath("$.data.actualOutboundPartId").value(1003)) // 选择了C而不是B
                .andExpect(jsonPath("$.data.finalStock").value(10)); // C=20-10=10
    }

    /**
     * TC04: 请求备件及所有可替代备件库存均不足
     * 预期：返回400错误，事务回滚，库存不变
     */
    @Test
    @Order(4)
    void TC04_allStockInsufficient() throws Exception {
        // A=2, B=1, C=0 全部不够
        jdbcTemplate.update("UPDATE inventory SET quantity=2 WHERE part_id=1001");
        jdbcTemplate.update("UPDATE inventory SET quantity=1 WHERE part_id=1002");
        jdbcTemplate.update("UPDATE inventory SET quantity=0 WHERE part_id=1003");

        OutboundRequest req = new OutboundRequest();
        req.setPartId(1001L);
        req.setQuantity(5);

        mockMvc.perform(post("/api/v1/outbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message", containsString("库存均不足")));

        // 验证库存未被修改（事务回滚）
        assertEquals(2, jdbcTemplate.queryForObject("SELECT quantity FROM inventory WHERE part_id=1001", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT quantity FROM inventory WHERE part_id=1002", Integer.class));
    }

    /**
     * TC05: 出库后，实际备件库存 ≤ 预警阈值
     * 预期：在 inventory_alert 表中生成未处理预警记录，相同备件未处理预警不重复插入
     */
    @Test
    @Order(5)
    void TC05_lowStockAlert() throws Exception {
        // A库存=5，阈值10，出库2后库存=3 ≤ 10，应生成预警
        jdbcTemplate.update("UPDATE inventory SET quantity=5 WHERE part_id=1001");
        jdbcTemplate.update("UPDATE spare_part SET alert_threshold=10 WHERE id=1001");

        OutboundRequest req = new OutboundRequest();
        req.setPartId(1001L);
        req.setQuantity(2);

        mockMvc.perform(post("/api/v1/outbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // 检查是否生成预警
        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_alert WHERE part_id=1001 AND alert_type='LOW_STOCK' AND resolved=0", Integer.class);
        assertEquals(1, count);

        // 再次出库1个，库存进一步降低，但不应重复插入预警
        req.setQuantity(1);
        mockMvc.perform(post("/api/v1/outbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        int count2 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_alert WHERE part_id=1001 AND alert_type='LOW_STOCK' AND resolved=0", Integer.class);
        assertEquals(1, count2);
    }

    /**
     * TC06: 并发出库（同一备件，两个请求各扣减50，库存80）
     * 预期：最终库存正确，无脏数据（使用乐观锁防止超卖）
     * 注意：本测试使用两个线程模拟并发，由于 MockMvc 线程安全，乐观锁将生效
     */
    @Test
    @Order(6)
    void TC06_concurrentOutbound() throws Exception {
        // 重置库存 A=80
        jdbcTemplate.update("UPDATE inventory SET quantity=80, version=0 WHERE part_id=1001");

        // 定义出库任务
        Runnable task = () -> {
            try {
                OutboundRequest req = new OutboundRequest();
                req.setPartId(1001L);
                req.setQuantity(50);
                mockMvc.perform(post("/api/v1/outbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // 启动两个线程
        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // 验证最终库存
        int finalQty = jdbcTemplate.queryForObject("SELECT quantity FROM inventory WHERE part_id=1001", Integer.class);
        // 库存不应为负数，且出库总量应等于库存减少量
        assertTrue(finalQty >= 0);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT * FROM outbound_record WHERE request_part_id=1001");
        long totalOutbound = records.stream()
                .mapToLong(r -> ((Number) r.get("quantity")).longValue())
                .sum();
        // 出库总量 + 最终库存 = 初始库存 80
        assertEquals(80, finalQty + totalOutbound);
    }

    /**
     * TC07: 请求备件不存在或已禁用
     * 预期：返回404错误
     */
    @Test
    @Order(7)
    void TC07_partNotFoundOrDisabled() throws Exception {
        // 不存在的备件
        OutboundRequest req = new OutboundRequest();
        req.setPartId(9999L);
        req.setQuantity(1);
        mockMvc.perform(post("/api/v1/outbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());

        // 已禁用的备件 (1004)
        req.setPartId(1004L);
        mockMvc.perform(post("/api/v1/outbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}