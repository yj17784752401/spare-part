
CREATE TABLE IF NOT EXISTS spare_part (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          part_code VARCHAR(64) NOT NULL UNIQUE,
                                          name VARCHAR(128) NOT NULL,
                                          alert_threshold INT,
                                          enabled TINYINT DEFAULT 1,
                                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory (
                                         part_id BIGINT PRIMARY KEY,
                                         quantity INT NOT NULL DEFAULT 0,
                                         version INT NOT NULL DEFAULT 0,
                                         FOREIGN KEY (part_id) REFERENCES spare_part(id)
);

CREATE TABLE IF NOT EXISTS substitution (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            source_id BIGINT NOT NULL,
                                            target_id BIGINT NOT NULL,
                                            priority INT NOT NULL DEFAULT 99,
                                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                            FOREIGN KEY (source_id) REFERENCES spare_part(id),
                                            FOREIGN KEY (target_id) REFERENCES spare_part(id)
);

CREATE TABLE IF NOT EXISTS outbound_record (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               request_part_id BIGINT NOT NULL,
                                               actual_outbound_part_id BIGINT NOT NULL,
                                               quantity INT NOT NULL,
                                               substituted TINYINT NOT NULL DEFAULT 0,
                                               outbound_time DATETIME NOT NULL,
                                               reason VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS inventory_alert (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               part_id BIGINT NOT NULL,
                                               alert_type VARCHAR(32) NOT NULL,
                                               resolved TINYINT NOT NULL DEFAULT 0,
                                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                               FOREIGN KEY (part_id) REFERENCES spare_part(id)
);


-- =============================================
-- MySQL 全场景测试数据（手动测试用）
-- 涵盖：库存充足、替代逻辑、优先级、全部不足、预警、禁用、并发
-- =============================================

-- 1. 备件主数据
INSERT INTO spare_part (id, part_code, name, alert_threshold, enabled, created_at) VALUES
-- 【TC01】库存充足直接出库 (partId=2001, 库存50, 阈值20)
(2001, 'P001', '标准备件A', 20, 1, NOW()),
-- 【TC02】库存不足，单一替代 (partId=2002, 库存10, 阈值5, 替代者2003库存30)
(2002, 'P002', '待替代件B', 5, 1, NOW()),
(2003, 'P003', '替代件B1', 10, 1, NOW()),
-- 【TC03】多替代按优先级 (partId=2004, 库存5, 替代者: 2005优先级1库存8, 2006优先级2库存20)
(2004, 'P004', '多替代件C', 5, 1, NOW()),
(2005, 'P005', '替代件C1(优先级高)', 8, 1, NOW()),
(2006, 'P006', '替代件C2(优先级低)', 8, 1, NOW()),
-- 【TC04】所有库存不足 (partId=2007, 库存2, 替代者2008库存1, 2009库存0)
(2007, 'P007', '全不足件D', 5, 1, NOW()),
(2008, 'P008', '替代件D1(不足)', 3, 1, NOW()),
(2009, 'P009', '替代件D2(无库存)', 3, 1, NOW()),
-- 【TC05】触发预警 (partId=2010, 库存15, 阈值20, 出库10后库存5 ≤ 20)
(2010, 'P010', '预警件E', 20, 1, NOW()),
-- 【TC06】并发测试 (partId=2011, 库存100, 阈值5)
(2011, 'P011', '并发测试件F', 5, 1, NOW()),
-- 【TC07】禁用备件 (partId=2012, 启用=0)
(2012, 'P012', '禁用件G', 10, 0, NOW()),
-- 额外：用于验证双向替代 (2013 ↔ 2014)
(2013, 'P013', '双向件H', 10, 1, NOW()),
(2014, 'P014', '双向件I', 10, 1, NOW());

-- 2. 库存数据
INSERT INTO inventory (part_id, quantity, version) VALUES
                                                       (2001, 50, 0),
                                                       (2002, 10, 0),
                                                       (2003, 30, 0),
                                                       (2004, 5,  0),
                                                       (2005, 8,  0),
                                                       (2006, 20, 0),
                                                       (2007, 2,  0),
                                                       (2008, 1,  0),
                                                       (2009, 0,  0),
                                                       (2010, 15, 0),
                                                       (2011, 100,0),
                                                       (2012, 50, 0),
                                                       (2013, 40, 0),
                                                       (2014, 60, 0);

-- 3. 替代关系（无向，每个关系只需插入一条）
INSERT INTO substitution (source_id, target_id, priority, created_at) VALUES
-- TC02: 2002 可被 2003 替代（优先级1）
(2002, 2003, 1, NOW()),
-- TC03: 2004 有两个替代 (2005 优先级1, 2006 优先级2)
(2004, 2005, 1, NOW()),
(2004, 2006, 2, NOW()),
-- TC04: 2007 有两个替代 (2008 优先级1, 2009 优先级2)
(2007, 2008, 1, NOW()),
(2007, 2009, 2, NOW()),
-- 双向替代验证：2013 ↔ 2014 (插入一条即可，系统应能双向查找)
(2013, 2014, 1, NOW());