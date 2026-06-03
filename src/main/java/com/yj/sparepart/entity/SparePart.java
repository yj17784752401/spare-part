package com.yj.sparepart.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 备件主数据实体
 * 对应数据库表 spare_part
 */
@Data
@TableName("spare_part")
public class SparePart {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 备件唯一编码
     */
    private String partCode;

    /**
     * 备件名称
     */
    private String name;

    /**
     * 库存预警阈值，当库存 <= 此值时触发预警
     */
    private Integer alertThreshold;

    /**
     * 启用状态：1-启用，0-禁用
     */
    private Integer enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}