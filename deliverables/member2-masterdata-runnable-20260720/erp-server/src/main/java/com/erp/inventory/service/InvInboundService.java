package com.erp.inventory.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.inventory.entity.*;
import com.erp.inventory.mapper.*;
import com.erp.purchase.entity.PurOrder;
import com.erp.purchase.entity.PurOrderItem;
import com.erp.purchase.mapper.PurOrderItemMapper;
import com.erp.purchase.mapper.PurOrderMapper;
import com.erp.purchase.service.PurOrderService;
import com.erp.sales.entity.SalOrderItem;
import com.erp.sales.entity.SalReturn;
import com.erp.sales.entity.SalReturnItem;
import com.erp.sales.mapper.SalOrderItemMapper;
import com.erp.sales.mapper.SalReturnItemMapper;
import com.erp.sales.mapper.SalReturnMapper;
import com.erp.sales.service.SalReturnService;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.mapper.OrgWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvInboundService extends ServiceImpl<InvInboundMapper, InvInbound> {

    private final InvInboundItemMapper inboundItemMapper;
    private final InvStockBalanceMapper stockBalanceMapper;
    private final InvStockMovementMapper stockMovementMapper;
    private final PurOrderMapper purOrderMapper;
    private final PurOrderItemMapper purOrderItemMapper;
    private final PurOrderService purOrderService;
    private final SalReturnMapper salReturnMapper;
    private final SalReturnItemMapper salReturnItemMapper;
    private final SalOrderItemMapper salOrderItemMapper;
    private final SalReturnService salReturnService;
    private final OrgWarehouseMapper warehouseMapper;

    public PageResult<InvInbound> pageInbounds(Long enterpriseId, Integer page, Integer size, String inboundNo,
                                                Long warehouseId, String status,
                                                String startDate, String endDate) {
        LambdaQueryWrapper<InvInbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvInbound::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(inboundNo), InvInbound::getInboundNo, inboundNo)
                .eq(warehouseId != null, InvInbound::getWarehouseId, warehouseId)
                .eq(StrUtil.isNotBlank(status), InvInbound::getStatus, status)
                .ge(StrUtil.isNotBlank(startDate), InvInbound::getInboundDate, startDate)
                .le(StrUtil.isNotBlank(endDate), InvInbound::getInboundDate, endDate)
                .orderByDesc(InvInbound::getCreatedAt);
        Page<InvInbound> result = page(new Page<>(page, size), wrapper);
        enrichWarehouses(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public InvInbound getDetailWithItems(Long id, Long enterpriseId) {
        InvInbound inbound = lambdaQuery().eq(InvInbound::getId, id)
                .eq(InvInbound::getEnterpriseId, enterpriseId).one();
        if (inbound == null) {
            throw new BusinessException("入库单不存在");
        }
        List<InvInboundItem> items = inboundItemMapper.selectList(
                new LambdaQueryWrapper<InvInboundItem>()
                        .eq(InvInboundItem::getInboundId, id)
                        .orderByAsc(InvInboundItem::getLineNo));
        inbound.setItems(items);
        enrichWarehouses(Collections.singletonList(inbound));
        return inbound;
    }

    @Transactional
    public InvInbound createFromPurchase(Long purchaseOrderId, Long enterpriseId, Long operatorId) {
        PurOrder po = purOrderMapper.selectById(purchaseOrderId);
        if (po == null || !Objects.equals(po.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException("采购订单不存在");
        }
        if (!"APPROVED".equals(po.getStatus()) && !"PARTIALLY_INBOUND".equals(po.getStatus())) {
            throw new BusinessException("采购订单状态不允许入库");
        }

        List<PurOrderItem> poItems = purOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurOrderItem>().eq(PurOrderItem::getOrderId, purchaseOrderId));

        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        InvInbound inbound = new InvInbound()
                .setEnterpriseId(enterpriseId)
                .setStoreId(po.getStoreId())
                .setInboundNo("IR" + System.currentTimeMillis())
                .setInboundType("PURCHASE")
                .setInboundDate(po.getOrderDate())
                .setWarehouseId(po.getWarehouseId())
                .setSourceType("PURCHASE_ORDER")
                .setSourceId(po.getId())
                .setSourceNo(po.getOrderNo())
                .setSupplierId(po.getSupplierId())
                .setStatus("DRAFT")
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);

        save(inbound);

        int lineNo = 1;
        for (PurOrderItem poItem : poItems) {
            BigDecimal remaining = poItem.getQuantity().subtract(
                    poItem.getInboundQuantity() != null ? poItem.getInboundQuantity() : BigDecimal.ZERO);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal lineAmount = remaining.multiply(poItem.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            totalQuantity = totalQuantity.add(remaining);
            totalAmount = totalAmount.add(lineAmount);

            InvInboundItem item = new InvInboundItem()
                    .setInboundId(inbound.getId())
                    .setLineNo(lineNo++)
                    .setSourceItemId(poItem.getId())
                    .setProductId(poItem.getProductId())
                    .setProductCode(poItem.getProductCode())
                    .setProductName(poItem.getProductName())
                    .setSpecification(poItem.getSpecification())
                    .setUnitId(poItem.getUnitId())
                    .setQuantity(remaining)
                    .setUnitCost(poItem.getUnitPrice())
                    .setAmount(lineAmount)
                    .setRemark(poItem.getRemark());
            inboundItemMapper.insert(item);
        }

        if (totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("采购订单所有商品已全部入库");
        }

        inbound.setTotalQuantity(totalQuantity);
        inbound.setTotalAmount(totalAmount);
        updateById(inbound);

        return getDetailWithItems(inbound.getId(), enterpriseId);
    }

    @Transactional
    public InvInbound createFromSalesReturn(Long returnId, Long enterpriseId, Long operatorId) {
        SalReturn salReturn = salReturnMapper.selectById(returnId);
        if (salReturn == null || !Objects.equals(salReturn.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException("销售退货单不存在");
        }
        if (!"APPROVED".equals(salReturn.getStatus())) {
            throw new BusinessException("只有已审核销售退货单可以生成退货入库");
        }
        Long activeCount = lambdaQuery().eq(InvInbound::getEnterpriseId, enterpriseId)
                .eq(InvInbound::getSourceType, "SALES_RETURN")
                .eq(InvInbound::getSourceId, returnId)
                .ne(InvInbound::getStatus, "CANCELLED").count();
        if (activeCount > 0) throw new BusinessException("该销售退货单已生成入库单");

        List<SalReturnItem> returnItems = salReturnItemMapper.selectList(
                new LambdaQueryWrapper<SalReturnItem>().eq(SalReturnItem::getReturnId, returnId));
        InvInbound inbound = new InvInbound()
                .setEnterpriseId(enterpriseId)
                .setStoreId(salReturn.getStoreId())
                .setInboundNo("IR" + System.currentTimeMillis())
                .setInboundType("SALES_RETURN")
                .setInboundDate(salReturn.getReturnDate())
                .setWarehouseId(salReturn.getWarehouseId())
                .setSourceType("SALES_RETURN")
                .setSourceId(salReturn.getId())
                .setSourceNo(salReturn.getReturnNo())
                .setCustomerId(salReturn.getCustomerId())
                .setStatus("DRAFT")
                .setTotalQuantity(salReturn.getTotalQuantity())
                .setTotalAmount(salReturn.getTotalAmount())
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);
        save(inbound);
        int lineNo = 1;
        BigDecimal totalCostAmount = BigDecimal.ZERO;
        for (SalReturnItem source : returnItems) {
            BigDecimal remaining = source.getQuantity().subtract(
                    source.getInboundQuantity() == null ? BigDecimal.ZERO : source.getInboundQuantity());
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) continue;
            InvStockMovement originalOutbound = stockMovementMapper.selectOne(
                    new LambdaQueryWrapper<InvStockMovement>()
                            .eq(InvStockMovement::getEnterpriseId, enterpriseId)
                            .eq(InvStockMovement::getSourceType, "SALES_ORDER")
                            .eq(InvStockMovement::getSourceId, salReturn.getSalesOrderId())
                            .eq(InvStockMovement::getSourceItemId, source.getSalesOrderItemId())
                            .eq(InvStockMovement::getDirection, "OUT")
                            .orderByDesc(InvStockMovement::getId)
                            .last("LIMIT 1"));
            InvStockBalance currentStock = stockBalanceMapper.selectOne(new LambdaQueryWrapper<InvStockBalance>()
                    .eq(InvStockBalance::getEnterpriseId, enterpriseId)
                    .eq(InvStockBalance::getWarehouseId, salReturn.getWarehouseId())
                    .eq(InvStockBalance::getProductId, source.getProductId()));
            BigDecimal unitCost = originalOutbound != null && originalOutbound.getUnitCost() != null
                    ? originalOutbound.getUnitCost()
                    : currentStock != null && currentStock.getAvgCostPrice() != null
                    ? currentStock.getAvgCostPrice() : BigDecimal.ZERO;
            BigDecimal costAmount = remaining.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            totalCostAmount = totalCostAmount.add(costAmount);
            inboundItemMapper.insert(new InvInboundItem()
                    .setInboundId(inbound.getId())
                    .setLineNo(lineNo++)
                    .setSourceItemId(source.getId())
                    .setProductId(source.getProductId())
                    .setProductCode(source.getProductCode())
                    .setProductName(source.getProductName())
                    .setSpecification(source.getSpecification())
                    .setUnitId(source.getUnitId())
                    .setQuantity(remaining)
                    .setUnitCost(unitCost)
                    .setAmount(costAmount)
                    .setRemark(source.getRemark()));
        }
        if (lineNo == 1) throw new BusinessException("销售退货商品已全部入库");
        inbound.setTotalAmount(totalCostAmount);
        updateById(inbound);
        return getDetailWithItems(inbound.getId(), enterpriseId);
    }

    @Transactional
    public void confirmInbound(Long id, Long enterpriseId, Long operatorId) {
        InvInbound inbound = lambdaQuery().eq(InvInbound::getId, id)
                .eq(InvInbound::getEnterpriseId, enterpriseId).one();
        if (inbound == null) {
            throw new BusinessException("入库单不存在");
        }
        if (!"DRAFT".equals(inbound.getStatus())) {
            throw new BusinessException("只有草稿状态的入库单可以确认");
        }

        List<InvInboundItem> items = inboundItemMapper.selectList(
                new LambdaQueryWrapper<InvInboundItem>()
                        .eq(InvInboundItem::getInboundId, id));

        if (items.isEmpty()) {
            throw new BusinessException("入库单无明细项目");
        }

        for (InvInboundItem item : items) {
            InvStockBalance stock = stockBalanceMapper.selectForUpdate(
                    inbound.getEnterpriseId(), inbound.getWarehouseId(), item.getProductId());

            BigDecimal beforeQty = BigDecimal.ZERO;

            if (stock == null) {
                stock = new InvStockBalance()
                        .setEnterpriseId(inbound.getEnterpriseId())
                        .setWarehouseId(inbound.getWarehouseId())
                        .setProductId(item.getProductId())
                        .setQuantity(item.getQuantity())
                        .setLockedQuantity(BigDecimal.ZERO)
                        .setAvailableQuantity(item.getQuantity())
                        .setAvgCostPrice(item.getUnitCost())
                        .setStockAmount(item.getAmount())
                        .setLastMovementAt(LocalDateTime.now())
                        .setVersion(0);
                stockBalanceMapper.insert(stock);
            } else {
                beforeQty = stock.getQuantity();
                BigDecimal newQty = stock.getQuantity().add(item.getQuantity());
                BigDecimal newAmount = stock.getStockAmount().add(item.getAmount());
                BigDecimal newAvgCost = newQty.compareTo(BigDecimal.ZERO) > 0
                        ? newAmount.divide(newQty, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                stock.setQuantity(newQty)
                        .setAvailableQuantity(newQty.subtract(stock.getLockedQuantity()))
                        .setAvgCostPrice(newAvgCost)
                        .setStockAmount(newAmount)
                        .setLastMovementAt(LocalDateTime.now());
                stockBalanceMapper.updateById(stock);
            }

            InvStockMovement movement = new InvStockMovement()
                    .setEnterpriseId(inbound.getEnterpriseId())
                    .setStoreId(inbound.getStoreId())
                    .setWarehouseId(inbound.getWarehouseId())
                    .setProductId(item.getProductId())
                    .setMovementNo("SM" + System.currentTimeMillis() + "_" + item.getLineNo())
                    .setMovementType("SALES_RETURN".equals(inbound.getSourceType()) ? "SALES_RETURN_IN" : "PURCHASE_IN")
                    .setDirection("IN")
                    .setQuantity(item.getQuantity())
                    .setUnitCost(item.getUnitCost())
                    .setAmount(item.getAmount())
                    .setBeforeQuantity(beforeQty)
                    .setAfterQuantity(stock.getQuantity())
                    .setSourceType(inbound.getSourceType())
                    .setSourceId(inbound.getSourceId())
                    .setSourceNo(inbound.getSourceNo())
                    .setSourceItemId(item.getSourceItemId())
                    .setBusinessDate(inbound.getInboundDate())
                    .setOperatorId(operatorId)
                    .setRemark(item.getRemark());
            stockMovementMapper.insert(movement);

            if (item.getSourceItemId() != null && "PURCHASE_ORDER".equals(inbound.getSourceType())) {
                PurOrderItem poItem = purOrderItemMapper.selectById(item.getSourceItemId());
                if (poItem != null) {
                    BigDecimal curInbound = poItem.getInboundQuantity() != null
                            ? poItem.getInboundQuantity() : BigDecimal.ZERO;
                    poItem.setInboundQuantity(curInbound.add(item.getQuantity()));
                    purOrderItemMapper.updateById(poItem);
                }
            }
            if (item.getSourceItemId() != null && "SALES_RETURN".equals(inbound.getSourceType())) {
                SalReturnItem returnItem = salReturnItemMapper.selectById(item.getSourceItemId());
                if (returnItem != null) {
                    returnItem.setInboundQuantity((returnItem.getInboundQuantity() == null ? BigDecimal.ZERO : returnItem.getInboundQuantity()).add(item.getQuantity()));
                    salReturnItemMapper.updateById(returnItem);
                    SalOrderItem orderItem = salOrderItemMapper.selectById(returnItem.getSalesOrderItemId());
                    if (orderItem != null) {
                        orderItem.setReturnedQuantity((orderItem.getReturnedQuantity() == null ? BigDecimal.ZERO : orderItem.getReturnedQuantity()).add(item.getQuantity()));
                        salOrderItemMapper.updateById(orderItem);
                    }
                }
            }
        }

        inbound.setStatus("CONFIRMED")
                .setConfirmedBy(operatorId)
                .setConfirmedAt(LocalDateTime.now());
        updateById(inbound);

        if ("PURCHASE_ORDER".equals(inbound.getSourceType()) && inbound.getSourceId() != null) {
            PurOrder po = purOrderMapper.selectById(inbound.getSourceId());
            if (po != null) {
                List<PurOrderItem> poItems = purOrderItemMapper.selectList(
                        new LambdaQueryWrapper<PurOrderItem>().eq(PurOrderItem::getOrderId, po.getId()));
                boolean allFullyInbound = poItems.stream().allMatch(
                        poi -> {
                            BigDecimal inboundQty = poi.getInboundQuantity() != null
                                    ? poi.getInboundQuantity() : BigDecimal.ZERO;
                            return inboundQty.compareTo(poi.getQuantity()) >= 0;
                        });
                if (allFullyInbound) {
                    purOrderService.complete(po.getId(), inbound.getEnterpriseId(), operatorId);
                } else if ("APPROVED".equals(po.getStatus())) {
                    po.setStatus("PARTIALLY_INBOUND");
                    purOrderMapper.updateById(po);
                }
            }
        }
        if ("SALES_RETURN".equals(inbound.getSourceType()) && inbound.getSourceId() != null) {
            List<SalReturnItem> returnItems = salReturnItemMapper.selectList(
                    new LambdaQueryWrapper<SalReturnItem>().eq(SalReturnItem::getReturnId, inbound.getSourceId()));
            boolean complete = returnItems.stream().allMatch(item ->
                    (item.getInboundQuantity() == null ? BigDecimal.ZERO : item.getInboundQuantity())
                            .compareTo(item.getQuantity()) >= 0);
            if (complete) salReturnService.completeAfterInbound(inbound.getSourceId(), inbound.getEnterpriseId(), operatorId);
        }
    }

    @Transactional
    public void cancelInbound(Long id, Long enterpriseId) {
        InvInbound inbound = lambdaQuery().eq(InvInbound::getId, id)
                .eq(InvInbound::getEnterpriseId, enterpriseId).one();
        if (inbound == null) {
            throw new BusinessException("入库单不存在");
        }
        if (!"DRAFT".equals(inbound.getStatus())) {
            throw new BusinessException("只有草稿状态的入库单可以取消");
        }
        inbound.setStatus("CANCELLED");
        updateById(inbound);
    }

    private void enrichWarehouses(List<InvInbound> inbounds) {
        if (inbounds == null || inbounds.isEmpty()) {
            return;
        }
        List<Long> warehouseIds = inbounds.stream().map(InvInbound::getWarehouseId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, OrgWarehouse> warehouses = warehouseIds.isEmpty() ? Collections.emptyMap()
                : warehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(OrgWarehouse::getId, Function.identity()));
        inbounds.forEach(inbound -> {
            OrgWarehouse warehouse = warehouses.get(inbound.getWarehouseId());
            inbound.setWarehouseName(warehouse != null ? warehouse.getWarehouseName() : null);
        });
    }
}
