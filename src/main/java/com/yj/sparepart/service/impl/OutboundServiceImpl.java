package com.yj.sparepart.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yj.sparepart.dto.OutboundRequest;
import com.yj.sparepart.dto.OutboundResponse;
import com.yj.sparepart.entity.Inventory;
import com.yj.sparepart.entity.InventoryAlert;
import com.yj.sparepart.entity.OutboundRecord;
import com.yj.sparepart.entity.SparePart;
import com.yj.sparepart.exception.BusinessException;
import com.yj.sparepart.mapper.*;
import com.yj.sparepart.service.OutboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 出库服务实现类
 * 负责核心业务逻辑：验证 -> 库存检查 -> 直接扣减或替代扣减 -> 生成记录 -> 库存预警
 * 所有操作在同一个事务中，使用乐观锁保证并发安全
 */
@Service
@RequiredArgsConstructor
public class OutboundServiceImpl extends ServiceImpl<OutboundRecordMapper,OutboundRecord> implements OutboundService{

    private final SparePartMapper sparePartMapper;
    private final InventoryMapper inventoryMapper;
    private final SubstitutionMapper substitutionMapper;
    private final OutboundRecordMapper outboundRecordMapper;
    private final InventoryAlertMapper inventoryAlertMapper;

    /**
     * 执行出库操作，完整业务流程如下：
     * 1. 参数校验
     * 2. 备件存在性与启用状态检查
     * 3. 尝试从请求备件直接扣减库存（乐观锁）
     * 4. 若库存不足，查找可替代备件（双向、优先级排序）
     * 5. 遍历替代备件，扣减第一个库存足够的备件库存（乐观锁）
     * 6. 若仍无法满足，抛出库存不足异常
     * 7. 生成出库记录
     * 8. 检查实际出库备件是否需要触发库存预警（防重复）
     *
     * @param request 出库请求
     * @return 出库响应详情
     */
    @Transactional
    @Override
    public OutboundResponse outbound(OutboundRequest request) {

        // ---------- 步骤1：参数校验 ----------
        if (request.getPartId() == null || request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException(400, "备件ID和出库数量必须大于0");
        }

        // ---------- 步骤2：查询备件是否存在且启用 ----------
        SparePart requestPart = sparePartMapper.selectById(request.getPartId());
        if (requestPart == null || requestPart.getEnabled() == 0) {
            throw new BusinessException(404,
                    "备件不存在或已禁用: " + (requestPart == null ? request.getPartId() : requestPart.getName()));
        }

        // ---------- 步骤3：获取请求备件的库存记录 ----------
        Inventory reqInv = inventoryMapper.selectById(request.getPartId());
        if (reqInv == null) {
            throw new BusinessException(404, "备件库存记录不存在");
        }

        // 标记是否使用了替代备件
        boolean substituted = false;
        // 实际出库的备件ID，初始设为请求的备件ID
        Long actualOutboundPartId = request.getPartId();
        // 实际出库的备件实体
        SparePart actualPart = requestPart;

        // ---------- 步骤4：判断库存是否充足 ----------
        if (reqInv.getQuantity() >= request.getQuantity()) {
            // 库存充足，直接扣减请求备件库存（乐观锁）
            int rows = inventoryMapper.decreaseWithVersion(
                    request.getPartId(), request.getQuantity(), reqInv.getVersion());
            if (rows == 0) {
                // 乐观锁冲突，扣减失败
                throw new BusinessException(400, "库存扣减失败，可能并发冲突");
            }
        } else {
            // ---------- 步骤5：库存不足，开始查找可替代备件 ----------
            // 5.1 双向查找可替代备件，并按优先级升序返回
            List<SubstitutionMapper.SubstituteDTO> substitutes =
                    substitutionMapper.findSubstitutes(request.getPartId());

            if (substitutes.isEmpty()) {
                throw new BusinessException(40001,
                        String.format("备件[%s]库存不足且无可替代备件，出库数量%d",
                                requestPart.getName(), request.getQuantity()));
            }

            // 5.2 已按优先级排序（SQL中已排序，此处再次排序确保）
            substitutes.sort(Comparator.comparingInt(SubstitutionMapper.SubstituteDTO::getPriority));

            boolean found = false;
            // 5.3 遍历可替代备件，选择第一个库存足够的进行扣减
            for (SubstitutionMapper.SubstituteDTO sub : substitutes) {
                Long subPartId = sub.getTargetId();

                // 跳过原备件本身（替代关系有可能指向自己）
                if (subPartId.equals(request.getPartId())) {
                    continue;
                }

                // 检查替代备件是否启用
                SparePart subPart = sparePartMapper.selectById(subPartId);
                if (subPart == null || subPart.getEnabled() == 0) {
                    continue;
                }

                // 获取替代备件库存
                Inventory subInv = inventoryMapper.selectById(subPartId);
                if (subInv == null) {
                    continue;
                }

                // 判断替代备件库存是否足够
                if (subInv.getQuantity() >= request.getQuantity()) {
                    // 尝试使用乐观锁扣减替代备件库存
                    int rows = inventoryMapper.decreaseWithVersion(
                            subPartId, request.getQuantity(), subInv.getVersion());
                    if (rows > 0) {
                        // 扣减成功，记录实际出库备件信息
                        actualOutboundPartId = subPartId;
                        actualPart = subPart;
                        substituted = true;
                        found = true;
                        break;
                    }
                    // 若乐观锁冲突，继续尝试下一个替代备件
                }
            }

            // 5.4 所有替代方案都无法满足，抛出异常
            if (!found) {
                throw new BusinessException(40001,
                        String.format("备件[%s]及其可替代备件库存均不足，出库数量%d",
                                requestPart.getName(), request.getQuantity()));
            }
        }

        // ---------- 步骤6：生成出库记录 ----------
        OutboundRecord record = new OutboundRecord();
        record.setRequestPartId(request.getPartId());
        record.setActualOutboundPartId(actualOutboundPartId);
        record.setQuantity(request.getQuantity());
        record.setSubstituted(substituted ? 1 : 0);
        record.setOutboundTime(LocalDateTime.now());
        record.setReason(request.getReason());
        outboundRecordMapper.insert(record);

        // ---------- 步骤7：库存预警检查 ----------
        // 重新查询实际出库备件的最新库存（可能已扣减）
        Inventory updatedInv = inventoryMapper.selectById(actualOutboundPartId);
        if (updatedInv != null
                && actualPart.getAlertThreshold() != null
                && updatedInv.getQuantity() <= actualPart.getAlertThreshold()) {

            // 检查是否已存在未处理的低库存预警（防止重复插入）
            int existingAlerts = inventoryAlertMapper.countUnresolvedLowStockAlert(actualOutboundPartId);
            if (existingAlerts == 0) {
                // 插入预警记录
                InventoryAlert alert = new InventoryAlert();
                alert.setPartId(actualOutboundPartId);
                alert.setAlertType("LOW_STOCK");
                alert.setResolved(0);
                alert.setCreatedAt(LocalDateTime.now());
                inventoryAlertMapper.insert(alert);
            }
        }

        // ---------- 步骤8：构造响应 ----------
        OutboundResponse resp = new OutboundResponse();
        resp.setOutboundId(record.getId());
        resp.setRequestPartId(request.getPartId());
        resp.setActualOutboundPartId(actualOutboundPartId);
        resp.setSubstituted(substituted);
        resp.setSubstitutedFromName(requestPart.getName());
        resp.setSubstitutedToName(substituted ? actualPart.getName() + "(可替代)" : requestPart.getName());
        resp.setFinalStock(updatedInv != null ? updatedInv.getQuantity() : 0);
        return resp;
    }
}