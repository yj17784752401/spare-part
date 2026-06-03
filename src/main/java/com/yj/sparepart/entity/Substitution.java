package com.yj.sparepart.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 可替代关系实体
 * 对应数据库表 substitution
 * 替代关系为无向：source 可被 target 替代，target 也可被 source 替代
 * 查询时需双向查找
 */
@Data
@TableName("substitution")
public class Substitution {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被替代备件ID
     */
    private Long sourceId;

    /**
     * 替代备件ID
     */
    private Long targetId;

    /**
     * 替代优先级，数字越小优先级越高
     */
    private Integer priority;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}