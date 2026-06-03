package com.yj.sparepart.dto;

import lombok.Data;

/**
 * 出库请求 DTO
 * 对应 POST /api/v1/outbound 的请求体
 */
@Data
public class OutboundRequest {

    /**
     * 用户请求出库的备件ID
     */
    private Long partId;

    /**
     * 出库数量，必须 > 0
     */
    private Integer quantity;

    /**
     * 出库原因（可选）
     */
    private String reason;
}