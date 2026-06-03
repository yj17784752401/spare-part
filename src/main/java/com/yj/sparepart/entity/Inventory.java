package com.yj.sparepart.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 库存实体
 * 对应数据库表 inventory
 */
@Data
@TableName("inventory")
public class Inventory {

    /**
     * 备件ID，与 spare_part.id 对应，同时也是主键
     */
    @TableId
    private Long partId;

    /**
     * 当前库存数量，非负
     */
    private Integer quantity;

    /**
     * 乐观锁版本号，用于并发控制
     * 使用 @Version 注解可与 MyBatis-Plus 乐观锁插件配合，但本项目中手动处理乐观锁
     */
    @Version
    private Integer version;
}