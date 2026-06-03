package com.yj.sparepart.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存预警记录实体
 * 对应数据库表 inventory_alert
 */
@Data
@TableName("inventory_alert")
public class InventoryAlert {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 产生预警的备件ID
     */
    private Long partId;

    /**
     * 预警类型，如 "LOW_STOCK"
     */
    private String alertType;

    /**
     * 处理状态：0-未处理，1-已处理
     */
    private Integer resolved;

    /**
     * 预警创建时间
     */
    private LocalDateTime createdAt;
}