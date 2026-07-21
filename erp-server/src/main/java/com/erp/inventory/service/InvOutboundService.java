package com.erp.inventory.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.inventory.entity.*;
import com.erp.inventory.mapper.*;
import com.erp.sales.entity.SalOrder;
import com.erp.sales.entity.SalOrderItem;
import com.erp.sales.mapper.SalOrderItemMapper;
import com.erp.sales.mapper.SalOrderMapper;
import com.erp.sales.service.SalOrderService;
import com.erp.purchase.entity.PurOrderItem;
import com.erp.purchase.entity.PurReturn;
import com.erp.purchase.entity.PurReturnItem;
import com.erp.purchase.mapper.PurOrderItemMapper;
import com.erp.purchase.mapper.PurReturnItemMapper;
import com.erp.purchase.mapper.PurReturnMapper;
import com.erp.purchase.service.PurReturnService;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.mapper.OrgWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvOutboundService extends ServiceImpl<InvOutboundMapper, InvOutbound> {

    private final InvOutboundItemMapper outboundItemMapper;
    private final InvStockBalanceMapper stockBalanceMapper;
    private final InvStockMovementMapper stockMovementMapper;
    private final SalOrderMapper salOrderMapper;
    private final SalOrderItemMapper salOrderItemMapper;
    private final SalOrderService salOrderService;
    private final PurReturnMapper purReturnMapper;
    private final PurReturnItemMapper purReturnItemMapper;
    private final PurOrderItemMapper purOrderItemMapper;
    private final PurReturnService purReturnService;
    private final OrgWarehouseMapper warehouseMapper;

    public PageResult<InvOutbound> pageOutbounds(Long enterpriseId, Integer page, Integer size, String outboundNo, String sourceNo,
                                                  Long warehouseId, String status,
                                                  String startDate, String endDate) {
        LambdaQueryWrapper<InvOutbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvOutbound::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(outboundNo), InvOutbound::getOutboundNo, outboundNo)
                .like(StrUtil.isNotBlank(sourceNo), InvOutbound::getSourceNo, sourceNo)
                .eq(warehouseId != null, InvOutbound::getWarehouseId, warehouseId)
                .eq(StrUtil.isNotBlank(status), InvOutbound::getStatus, status)
                .ge(StrUtil.isNotBlank(startDate), InvOutbound::getOutboundDate, startDate)
                .le(StrUtil.isNotBlank(endDate), InvOutbound::getOutboundDate, endDate)
                .orderByDesc(InvOutbound::getCreatedAt);
        Page<InvOutbound> result = page(new Page<>(page, size), wrapper);
        enrichWarehouses(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public InvOutbound getDetailWithItems(Long id, Long enterpriseId) {
        InvOutbound outbound = lambdaQuery().eq(InvOutbound::getId, id)
                .eq(InvOutbound::getEnterpriseId, enterpriseId).one();
        if (outbound == null) {
            throw new BusinessException("出库单不存在");
        }
        List<InvOutboundItem> items = outboundItemMapper.selectList(
                new LambdaQueryWrapper<InvOutboundItem>()
                        .eq(InvOutboundItem::getOutboundId, id)
                        .orderByAsc(InvOutboundItem::getLineNo));
        outbound.setItems(items);
        enrichWarehouses(Collections.singletonList(outbound));
        return outbound;
    }

    @Transactional
    public InvOutbound createFromSales(Long salesOrderId, Long enterpriseId, Long operatorId) {
        SalOrder so = salOrderMapper.selectById(salesOrderId);
        if (so == null || !Objects.equals(so.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException("销售订单不存在");
        }
        if (!"APPROVED".equals(so.getStatus()) && !"PARTIALLY_OUTBOUND".equals(so.getStatus())) {
            throw new BusinessException("销售订单状态不允许出库");
        }

        List<SalOrderItem> soItems = salOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalOrderItem>().eq(SalOrderItem::getOrderId, salesOrderId));

        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        InvOutbound outbound = new InvOutbound()
                .setEnterpriseId(enterpriseId)
                .setStoreId(so.getStoreId())
                .setOutboundNo("OR" + System.currentTimeMillis())
                .setOutboundType("SALES")
                .setOutboundDate(so.getOrderDate())
                .setWarehouseId(so.getWarehouseId())
                .setSourceType("SALES_ORDER")
                .setSourceId(so.getId())
                .setSourceNo(so.getOrderNo())
                .setCustomerId(so.getCustomerId())
                .setStatus("DRAFT")
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);

        save(outbound);

        int lineNo = 1;
        for (SalOrderItem soItem : soItems) {
            BigDecimal remaining = soItem.getQuantity().subtract(
                    soItem.getOutboundQuantity() != null ? soItem.getOutboundQuantity() : BigDecimal.ZERO);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal lineAmount = remaining.multiply(soItem.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            totalQuantity = totalQuantity.add(remaining);
            totalAmount = totalAmount.add(lineAmount);

            InvOutboundItem item = new InvOutboundItem()
                    .setOutboundId(outbound.getId())
                    .setLineNo(lineNo++)
                    .setSourceItemId(soItem.getId())
                    .setProductId(soItem.getProductId())
                    .setProductCode(soItem.getProductCode())
                    .setProductName(soItem.getProductName())
                    .setSpecification(soItem.getSpecification())
                    .setUnitId(soItem.getUnitId())
                    .setQuantity(remaining)
                    .setUnitCost(soItem.getUnitPrice())
                    .setAmount(lineAmount)
                    .setRemark(soItem.getRemark());
            outboundItemMapper.insert(item);
        }

        if (totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("销售订单所有商品已全部出库");
        }

        outbound.setTotalQuantity(totalQuantity);
        outbound.setTotalAmount(totalAmount);
        updateById(outbound);

        return getDetailWithItems(outbound.getId(), enterpriseId);
    }

    @Transactional
    public InvOutbound createFromPurchaseReturn(Long returnId, Long enterpriseId, Long operatorId) {
        PurReturn purReturn = purReturnMapper.selectById(returnId);
        if (purReturn == null || !Objects.equals(purReturn.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException("采购退货单不存在");
        }
        if (!"APPROVED".equals(purReturn.getStatus())) {
            throw new BusinessException("只有已审核采购退货单可以生成退货出库");
        }
        Long activeCount = lambdaQuery().eq(InvOutbound::getEnterpriseId, enterpriseId)
                .eq(InvOutbound::getSourceType, "PURCHASE_RETURN")
                .eq(InvOutbound::getSourceId, returnId)
                .ne(InvOutbound::getStatus, "CANCELLED").count();
        if (activeCount > 0) throw new BusinessException("该采购退货单已生成出库单");

        List<PurReturnItem> returnItems = purReturnItemMapper.selectList(
                new LambdaQueryWrapper<PurReturnItem>().eq(PurReturnItem::getReturnId, returnId));
        InvOutbound outbound = new InvOutbound()
                .setEnterpriseId(enterpriseId)
                .setStoreId(purReturn.getStoreId())
                .setOutboundNo("OR" + System.currentTimeMillis())
                .setOutboundType("PURCHASE_RETURN")
                .setOutboundDate(purReturn.getReturnDate())
                .setWarehouseId(purReturn.getWarehouseId())
                .setSourceType("PURCHASE_RETURN")
                .setSourceId(purReturn.getId())
                .setSourceNo(purReturn.getReturnNo())
                .setSupplierId(purReturn.getSupplierId())
                .setStatus("DRAFT")
                .setTotalQuantity(purReturn.getTotalQuantity())
                .setTotalAmount(purReturn.getTotalAmount())
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);
        save(outbound);
        int lineNo = 1;
        for (PurReturnItem source : returnItems) {
            BigDecimal remaining = source.getQuantity().subtract(
                    source.getOutboundQuantity() == null ? BigDecimal.ZERO : source.getOutboundQuantity());
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) continue;
            outboundItemMapper.insert(new InvOutboundItem()
                    .setOutboundId(outbound.getId())
                    .setLineNo(lineNo++)
                    .setSourceItemId(source.getId())
                    .setProductId(source.getProductId())
                    .setProductCode(source.getProductCode())
                    .setProductName(source.getProductName())
                    .setSpecification(source.getSpecification())
                    .setUnitId(source.getUnitId())
                    .setQuantity(remaining)
                    .setUnitCost(source.getUnitPrice())
                    .setAmount(remaining.multiply(source.getUnitPrice()).setScale(2, RoundingMode.HALF_UP))
                    .setRemark(source.getRemark()));
        }
        if (lineNo == 1) throw new BusinessException("采购退货商品已全部出库");
        return getDetailWithItems(outbound.getId(), enterpriseId);
    }

    @Transactional
    public void confirmOutbound(Long id, Long enterpriseId, Long operatorId) {
        InvOutbound outbound = baseMapper.selectForUpdate(id, enterpriseId);
        if (outbound == null) {
            throw new BusinessException("出库单不存在");
        }
        if (!"DRAFT".equals(outbound.getStatus())) {
            throw new BusinessException("只有草稿状态的出库单可以确认");
        }

        List<InvOutboundItem> items = outboundItemMapper.selectList(
                new LambdaQueryWrapper<InvOutboundItem>()
                        .eq(InvOutboundItem::getOutboundId, id));

        if (items.isEmpty()) {
            throw new BusinessException("出库单无明细项目");
        }

        items = new ArrayList<>(items);
        items.sort(Comparator.comparing(InvOutboundItem::getProductId));
        Map<Long, InvStockBalance> lockedStocks = new HashMap<>();
        for (InvOutboundItem item : items) {
            InvStockBalance stock = stockBalanceMapper.selectForUpdate(
                    outbound.getEnterpriseId(), outbound.getWarehouseId(), item.getProductId());

            if (stock == null || stock.getAvailableQuantity().compareTo(item.getQuantity()) < 0) {
                throw new BusinessException("库存不足，商品: " + item.getProductCode() + " - " + item.getProductName());
            }
            lockedStocks.put(item.getProductId(), stock);
        }

        for (InvOutboundItem item : items) {
            InvStockBalance stock = lockedStocks.get(item.getProductId());
            BigDecimal beforeQty = stock.getQuantity();
            BigDecimal newQty = stock.getQuantity().subtract(item.getQuantity());
            BigDecimal unitCost = stock.getAvgCostPrice() != null ? stock.getAvgCostPrice() : BigDecimal.ZERO;
            BigDecimal costAmount = item.getQuantity().multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal beforeAmount = stock.getStockAmount() != null ? stock.getStockAmount() : BigDecimal.ZERO;
            BigDecimal newAmount = beforeAmount.subtract(costAmount).max(BigDecimal.ZERO);

            stock.setQuantity(newQty)
                    .setAvailableQuantity(newQty.subtract(stock.getLockedQuantity()))
                    .setStockAmount(newAmount)
                    .setLastMovementAt(LocalDateTime.now());
            stockBalanceMapper.updateById(stock);

            InvStockMovement movement = new InvStockMovement()
                    .setEnterpriseId(outbound.getEnterpriseId())
                    .setStoreId(outbound.getStoreId())
                    .setWarehouseId(outbound.getWarehouseId())
                    .setProductId(item.getProductId())
                    .setMovementNo("SM" + System.currentTimeMillis() + "_" + item.getLineNo())
                    .setMovementType("PURCHASE_RETURN".equals(outbound.getSourceType()) ? "PURCHASE_RETURN_OUT" : "SALES_OUT")
                    .setDirection("OUT")
                    .setQuantity(item.getQuantity())
                    .setUnitCost(unitCost)
                    .setAmount(costAmount)
                    .setBeforeQuantity(beforeQty)
                    .setAfterQuantity(stock.getQuantity())
                    .setSourceType(outbound.getSourceType())
                    .setSourceId(outbound.getSourceId())
                    .setSourceNo(outbound.getSourceNo())
                    .setSourceItemId(item.getSourceItemId())
                    .setBusinessDate(outbound.getOutboundDate())
                    .setOperatorId(operatorId)
                    .setRemark(item.getRemark());
            stockMovementMapper.insert(movement);

            if (item.getSourceItemId() != null && "SALES_ORDER".equals(outbound.getSourceType())) {
                SalOrderItem soItem = salOrderItemMapper.selectById(item.getSourceItemId());
                if (soItem != null) {
                    BigDecimal curOutbound = soItem.getOutboundQuantity() != null
                            ? soItem.getOutboundQuantity() : BigDecimal.ZERO;
                    soItem.setOutboundQuantity(curOutbound.add(item.getQuantity()));
                    salOrderItemMapper.updateById(soItem);
                }
            }
            if (item.getSourceItemId() != null && "PURCHASE_RETURN".equals(outbound.getSourceType())) {
                PurReturnItem returnItem = purReturnItemMapper.selectById(item.getSourceItemId());
                if (returnItem != null) {
                    returnItem.setOutboundQuantity((returnItem.getOutboundQuantity() == null ? BigDecimal.ZERO : returnItem.getOutboundQuantity()).add(item.getQuantity()));
                    purReturnItemMapper.updateById(returnItem);
                    PurOrderItem orderItem = purOrderItemMapper.selectById(returnItem.getPurchaseOrderItemId());
                    if (orderItem != null) {
                        orderItem.setReturnedQuantity((orderItem.getReturnedQuantity() == null ? BigDecimal.ZERO : orderItem.getReturnedQuantity()).add(item.getQuantity()));
                        purOrderItemMapper.updateById(orderItem);
                    }
                }
            }
        }

        outbound.setStatus("CONFIRMED")
                .setConfirmedBy(operatorId)
                .setConfirmedAt(LocalDateTime.now());
        updateById(outbound);

        if ("SALES_ORDER".equals(outbound.getSourceType()) && outbound.getSourceId() != null) {
            SalOrder so = salOrderMapper.selectById(outbound.getSourceId());
            if (so != null) {
                List<SalOrderItem> soItems = salOrderItemMapper.selectList(
                        new LambdaQueryWrapper<SalOrderItem>().eq(SalOrderItem::getOrderId, so.getId()));
                boolean allFullyOutbound = soItems.stream().allMatch(
                        soi -> {
                            BigDecimal outboundQty = soi.getOutboundQuantity() != null
                                    ? soi.getOutboundQuantity() : BigDecimal.ZERO;
                            return outboundQty.compareTo(soi.getQuantity()) >= 0;
                        });
                if (allFullyOutbound) {
                    salOrderService.complete(so.getId(), outbound.getEnterpriseId(), operatorId);
                } else if ("APPROVED".equals(so.getStatus())) {
                    so.setStatus("PARTIALLY_OUTBOUND");
                    salOrderMapper.updateById(so);
                }
            }
        }
        if ("PURCHASE_RETURN".equals(outbound.getSourceType()) && outbound.getSourceId() != null) {
            List<PurReturnItem> returnItems = purReturnItemMapper.selectList(
                    new LambdaQueryWrapper<PurReturnItem>().eq(PurReturnItem::getReturnId, outbound.getSourceId()));
            boolean complete = returnItems.stream().allMatch(item ->
                    (item.getOutboundQuantity() == null ? BigDecimal.ZERO : item.getOutboundQuantity())
                            .compareTo(item.getQuantity()) >= 0);
            if (complete) purReturnService.completeAfterOutbound(outbound.getSourceId(), outbound.getEnterpriseId(), operatorId);
        }
    }

    @Transactional
    public void cancelOutbound(Long id, Long enterpriseId) {
        InvOutbound outbound = baseMapper.selectForUpdate(id, enterpriseId);
        if (outbound == null) {
            throw new BusinessException("出库单不存在");
        }
        if (!"DRAFT".equals(outbound.getStatus())) {
            throw new BusinessException("只有草稿状态的出库单可以取消");
        }
        outbound.setStatus("CANCELLED");
        updateById(outbound);
    }

    private void enrichWarehouses(List<InvOutbound> outbounds) {
        if (outbounds == null || outbounds.isEmpty()) {
            return;
        }
        List<Long> warehouseIds = outbounds.stream().map(InvOutbound::getWarehouseId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, OrgWarehouse> warehouses = warehouseIds.isEmpty() ? Collections.emptyMap()
                : warehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(OrgWarehouse::getId, Function.identity()));
        outbounds.forEach(outbound -> {
            OrgWarehouse warehouse = warehouses.get(outbound.getWarehouseId());
            outbound.setWarehouseName(warehouse != null ? warehouse.getWarehouseName() : null);
        });
    }
}
