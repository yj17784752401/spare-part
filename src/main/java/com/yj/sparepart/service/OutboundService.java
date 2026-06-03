package com.yj.sparepart.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.yj.sparepart.dto.OutboundRequest;
import com.yj.sparepart.dto.OutboundResponse;
import com.yj.sparepart.entity.OutboundRecord;

/**
 * 出库服务接口
 */
public interface OutboundService extends IService<OutboundRecord> {

    /**
     * 执行出库操作，包含库存检查、替代逻辑、库存扣减、出库记录、预警检查
     * 整个流程在一个数据库事务中完成
     *
     * @param request 出库请求参数
     * @return 出库结果详情
     */
    OutboundResponse outbound(OutboundRequest request);
}