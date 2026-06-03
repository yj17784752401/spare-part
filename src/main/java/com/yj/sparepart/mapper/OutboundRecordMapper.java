package com.yj.sparepart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yj.sparepart.entity.OutboundRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出库记录 Mapper
 */
@Mapper
public interface OutboundRecordMapper extends BaseMapper<OutboundRecord> {
}