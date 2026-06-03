package com.yj.sparepart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yj.sparepart.entity.InventoryAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 库存预警记录 Mapper
 */
@Mapper
public interface InventoryAlertMapper extends BaseMapper<InventoryAlert> {

    /**
     * 查询指定备件未处理（resolved = 0）的低库存预警数量
     *
     * @param partId 备件ID
     * @return 未处理预警记录数
     */
    @Select("SELECT COUNT(*) FROM inventory_alert " +
            "WHERE part_id = #{partId} AND alert_type = 'LOW_STOCK' AND resolved = 0")
    int countUnresolvedLowStockAlert(@Param("partId") Long partId);
}