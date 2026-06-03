package com.yj.sparepart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yj.sparepart.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存 Mapper
 * 提供自定义乐观锁扣减库存方法
 */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 使用乐观锁机制扣减库存
     * 只有当前版本号匹配且剩余库存 >= 扣减数量时，才会执行更新
     *
     * @param partId     备件ID
     * @param quantity   扣减数量
     * @param oldVersion 当前查询到的版本号
     * @return 影响行数，若为0表示版本冲突或库存不足，扣减失败
     */
    @Update("UPDATE inventory SET quantity = quantity - #{quantity}, " +
            "version = version + 1 " +
            "WHERE part_id = #{partId} AND quantity >= #{quantity} AND version = #{oldVersion}")
    int decreaseWithVersion(@Param("partId") Long partId,
                            @Param("quantity") int quantity,
                            @Param("oldVersion") int oldVersion);
}