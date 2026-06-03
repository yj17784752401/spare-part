package com.yj.sparepart.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 出库记录实体
 * 对应数据库表 outbound_record
 */
@Data
@TableName("outbound_record")
public class OutboundRecord {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户请求出库的备件ID
     */
    private Long requestPartId;

    /**
     * 实际扣减库存的备件ID（可能与请求的不同，使用替代时不同）
     */
    private Long actualOutboundPartId;

    /**
     * 出库数量
     */
    private Integer quantity;

    /**
     * 是否使用替代：0-未使用，1-使用了替代
     */
    private Integer substituted;

    /**
     * 出库时间
     */
    private LocalDateTime outboundTime;

    /**
     * 出库原因（可选）
     */
    private String reason;
}