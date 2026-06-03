package com.yj.sparepart.controller;


import com.yj.sparepart.common.Result;
import com.yj.sparepart.dto.OutboundRequest;
import com.yj.sparepart.dto.OutboundResponse;
import com.yj.sparepart.exception.BusinessException;
import com.yj.sparepart.service.OutboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 出库接口控制器
 * 对外提供 REST API，处理备件出库请求
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OutboundController {

    private final OutboundService outboundService;

    /**
     * 备件出库接口
     * POST /api/v1/outbound
     *
     * @param request 出库请求体（partId, quantity, reason）
     * @return 统一响应结果，包含出库详情
     */
    @PostMapping("/outbound")
    public Result<OutboundResponse> outbound(@RequestBody OutboundRequest request) {
        // ---------- 参数校验 ----------
        if (request.getPartId() == null || request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException(400, "备件ID和出库数量必须大于0");
        }
        OutboundResponse response = outboundService.outbound(request);
        return Result.success(response);
    }
}