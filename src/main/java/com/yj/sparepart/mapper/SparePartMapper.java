package com.yj.sparepart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yj.sparepart.entity.SparePart;
import org.apache.ibatis.annotations.Mapper;

/**
 * 备件主数据 Mapper
 * 继承 MyBatis-Plus BaseMapper，自动具备 CRUD 方法
 */
@Mapper
public interface SparePartMapper extends BaseMapper<SparePart> {
}