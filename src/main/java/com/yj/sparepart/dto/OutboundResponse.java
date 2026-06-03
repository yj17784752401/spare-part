package com.yj.sparepart.dto;

import lombok.Data;

/**
 * 出库响应 DTO
 * 包含出库结果详情
 */
@Data
public class OutboundResponse {

    /**
     * 生成的出库记录ID
     */
    private Long outboundId;

    /**
     * 用户请求的出库备件ID
     */
    private Long requestPartId;

    /**
     * 实际扣减库存的备件ID（可能与请求不同）
     */
    private Long actualOutboundPartId;

    /**
     * 是否使用了替代备件
     */
    private Boolean substituted;

    /**
     * 原始请求的备件名称
     */
    private String substitutedFromName;

    /**
     * 实际出库的备件名称（若使用替代，会标注可替代）
     */
    private String substitutedToName;

    /**
     * 实际出库备件的最新库存（扣减后）
     */
    private Integer finalStock;
}