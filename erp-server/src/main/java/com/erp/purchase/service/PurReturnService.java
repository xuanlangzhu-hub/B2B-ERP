package com.erp.purchase.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.finance.entity.FinPayable;
import com.erp.finance.mapper.FinPayableMapper;
import com.erp.masterdata.entity.MdSupplier;
import com.erp.masterdata.mapper.MdSupplierMapper;
import com.erp.purchase.dto.PurReturnItemRequest;
import com.erp.purchase.dto.PurReturnRequest;
import com.erp.purchase.entity.*;
import com.erp.purchase.mapper.*;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.mapper.OrgWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurReturnService extends ServiceImpl<PurReturnMapper, PurReturn> {
    private final PurReturnItemMapper returnItemMapper;
    private final PurOrderMapper orderMapper;
    private final PurOrderItemMapper orderItemMapper;
    private final FinPayableMapper payableMapper;
    private final MdSupplierMapper supplierMapper;
    private final OrgWarehouseMapper warehouseMapper;

    public PageResult<PurReturn> pageReturns(Long enterpriseId, Integer page, Integer size,
                                             String returnNo, Long supplierId, String status) {
        LambdaQueryWrapper<PurReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurReturn::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(returnNo), PurReturn::getReturnNo, returnNo)
                .eq(supplierId != null, PurReturn::getSupplierId, supplierId)
                .eq(StrUtil.isNotBlank(status), PurReturn::getStatus, status)
                .orderByDesc(PurReturn::getCreatedAt);
        Page<PurReturn> result = page(new Page<>(page, size), wrapper);
        enrich(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public PurReturn detail(Long id, Long enterpriseId) {
        PurReturn row = lambdaQuery().eq(PurReturn::getEnterpriseId, enterpriseId)
                .eq(PurReturn::getId, id).one();
        if (row == null) throw new BusinessException("采购退货单不存在");
        row.setItems(returnItemMapper.selectList(new LambdaQueryWrapper<PurReturnItem>()
                .eq(PurReturnItem::getReturnId, id).orderByAsc(PurReturnItem::getLineNo)));
        enrich(Collections.singletonList(row));
        return row;
    }

    @Transactional
    public PurReturn create(PurReturnRequest request, Long enterpriseId, Long operatorId) {
        PurOrder order = orderMapper.selectById(request.getPurchaseOrderId());
        if (order == null || !Objects.equals(order.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException("原采购单不存在");
        }
        if (!"COMPLETED".equals(order.getStatus())) throw new BusinessException("只能对已完成采购单申请退货");
        if (lambdaQuery().eq(PurReturn::getEnterpriseId, enterpriseId)
                .eq(PurReturn::getPurchaseOrderId, order.getId())
                .ne(PurReturn::getStatus, "CANCELLED").count() > 0) {
            throw new BusinessException("该采购单已存在退货申请");
        }
        OrgWarehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null || !Objects.equals(warehouse.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException("退货仓库不存在");
        }
        FinPayable payable = payableMapper.selectOne(new LambdaQueryWrapper<FinPayable>()
                .eq(FinPayable::getEnterpriseId, enterpriseId)
                .eq(FinPayable::getSourceType, "PURCHASE_ORDER")
                .eq(FinPayable::getSourceId, order.getId()));
        if (payable == null || payable.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("原采购单没有可冲减的未付款，暂不支持退款型退货");
        }

        Map<Long, PurOrderItem> sourceItems = orderItemMapper.selectList(
                        new LambdaQueryWrapper<PurOrderItem>().eq(PurOrderItem::getOrderId, order.getId()))
                .stream().collect(Collectors.toMap(PurOrderItem::getId, Function.identity()));
        Set<Long> usedItems = new HashSet<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PurReturnItem> items = new ArrayList<>();
        int lineNo = 1;
        for (PurReturnItemRequest itemRequest : request.getItems()) {
            if (!usedItems.add(itemRequest.getPurchaseOrderItemId())) throw new BusinessException("退货明细商品重复");
            PurOrderItem source = sourceItems.get(itemRequest.getPurchaseOrderItemId());
            if (source == null) throw new BusinessException("采购单明细不存在");
            BigDecimal received = zero(source.getInboundQuantity());
            BigDecimal available = received.subtract(zero(source.getReturnedQuantity()));
            if (itemRequest.getQuantity().compareTo(available) > 0) {
                throw new BusinessException("商品 " + source.getProductName() + " 的退货数量超过可退数量 " + available);
            }
            BigDecimal amount = itemRequest.getQuantity().multiply(source.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            totalQuantity = totalQuantity.add(itemRequest.getQuantity());
            totalAmount = totalAmount.add(amount);
            items.add(new PurReturnItem()
                    .setLineNo(lineNo++)
                    .setPurchaseOrderItemId(source.getId())
                    .setProductId(source.getProductId())
                    .setProductCode(source.getProductCode())
                    .setProductName(source.getProductName())
                    .setSpecification(source.getSpecification())
                    .setUnitId(source.getUnitId())
                    .setQuantity(itemRequest.getQuantity())
                    .setUnitPrice(source.getUnitPrice())
                    .setAmount(amount)
                    .setOutboundQuantity(BigDecimal.ZERO)
                    .setRemark(itemRequest.getRemark()));
        }
        if (totalAmount.compareTo(payable.getOutstandingAmount()) > 0) {
            throw new BusinessException("退货金额超过未付金额，当前版本暂不支持自动退款");
        }

        PurReturn purReturn = new PurReturn()
                .setEnterpriseId(enterpriseId)
                .setStoreId(order.getStoreId())
                .setReturnNo("CT" + System.currentTimeMillis())
                .setReturnDate(request.getReturnDate())
                .setPurchaseOrderId(order.getId())
                .setSupplierId(order.getSupplierId())
                .setWarehouseId(request.getWarehouseId())
                .setStatus("DRAFT")
                .setTotalQuantity(totalQuantity)
                .setTotalAmount(totalAmount)
                .setRefundAmount(BigDecimal.ZERO)
                .setReturnReason(request.getReturnReason())
                .setRemark(request.getRemark())
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);
        save(purReturn);
        items.forEach(item -> {
            item.setReturnId(purReturn.getId());
            returnItemMapper.insert(item);
        });
        return detail(purReturn.getId(), enterpriseId);
    }

    @Transactional
    public void approve(Long id, Long enterpriseId, Long operatorId) {
        PurReturn row = baseMapper.selectForUpdate(id, enterpriseId);
        if (row == null) throw new BusinessException("采购退货单不存在");
        if (!"DRAFT".equals(row.getStatus())) throw new BusinessException("只有草稿退货单可以审核");
        row.setStatus("APPROVED").setApprovedBy(operatorId).setApprovedAt(LocalDateTime.now()).setUpdatedBy(operatorId);
        updateById(row);
    }

    @Transactional
    public void cancel(Long id, Long enterpriseId, Long operatorId) {
        PurReturn row = baseMapper.selectForUpdate(id, enterpriseId);
        if (row == null) throw new BusinessException("采购退货单不存在");
        if (!"DRAFT".equals(row.getStatus())) throw new BusinessException("只有草稿退货单可以取消");
        row.setStatus("CANCELLED").setUpdatedBy(operatorId);
        updateById(row);
    }

    @Transactional
    public void completeAfterOutbound(Long id, Long enterpriseId, Long operatorId) {
        PurReturn row = baseMapper.selectForUpdate(id, enterpriseId);
        if (row == null || !"APPROVED".equals(row.getStatus())) throw new BusinessException("采购退货单状态不允许完成");
        FinPayable candidate = payableMapper.selectOne(new LambdaQueryWrapper<FinPayable>()
                .eq(FinPayable::getEnterpriseId, enterpriseId)
                .eq(FinPayable::getSourceType, "PURCHASE_ORDER")
                .eq(FinPayable::getSourceId, row.getPurchaseOrderId()));
        if (candidate == null) throw new BusinessException("原采购单应付不存在");
        FinPayable payable = payableMapper.selectForUpdate(candidate.getId(), enterpriseId);
        if (payable.getOutstandingAmount().compareTo(row.getTotalAmount()) < 0) {
            throw new BusinessException("未付金额不足以冲减本次退货，暂不支持自动退款");
        }
        BigDecimal newOutstanding = payable.getOutstandingAmount().subtract(row.getTotalAmount());
        BigDecimal newOriginal = payable.getOriginalAmount().subtract(row.getTotalAmount());
        payable.setOriginalAmount(newOriginal)
                .setOutstandingAmount(newOutstanding)
                .setStatus(newOutstanding.compareTo(BigDecimal.ZERO) == 0 ? "SETTLED"
                        : zero(payable.getPaidAmount()).compareTo(BigDecimal.ZERO) > 0 ? "PARTIALLY_SETTLED" : "UNSETTLED")
                .setUpdatedBy(operatorId);
        payableMapper.updateById(payable);

        PurOrder order = orderMapper.selectById(row.getPurchaseOrderId());
        order.setPayableAmount(order.getPayableAmount().subtract(row.getTotalAmount()))
                .setSettlementStatus(newOutstanding.compareTo(BigDecimal.ZERO) == 0 ? "PAID"
                        : zero(payable.getPaidAmount()).compareTo(BigDecimal.ZERO) > 0 ? "PARTIALLY_PAID" : "UNPAID")
                .setUpdatedBy(operatorId);
        orderMapper.updateById(order);
        row.setStatus("COMPLETED").setUpdatedBy(operatorId);
        updateById(row);
    }

    private void enrich(List<PurReturn> rows) {
        if (rows == null || rows.isEmpty()) return;
        Map<Long, PurOrder> orders = orderMapper.selectBatchIds(rows.stream().map(PurReturn::getPurchaseOrderId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(PurOrder::getId, Function.identity()));
        Map<Long, MdSupplier> suppliers = supplierMapper.selectBatchIds(rows.stream().map(PurReturn::getSupplierId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(MdSupplier::getId, Function.identity()));
        Map<Long, OrgWarehouse> warehouses = warehouseMapper.selectBatchIds(rows.stream().map(PurReturn::getWarehouseId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(OrgWarehouse::getId, Function.identity()));
        rows.forEach(row -> {
            PurOrder order = orders.get(row.getPurchaseOrderId());
            MdSupplier supplier = suppliers.get(row.getSupplierId());
            OrgWarehouse warehouse = warehouses.get(row.getWarehouseId());
            row.setPurchaseOrderNo(order == null ? null : order.getOrderNo());
            row.setSupplierName(supplier == null ? null : supplier.getSupplierName());
            row.setWarehouseName(warehouse == null ? null : warehouse.getWarehouseName());
        });
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
