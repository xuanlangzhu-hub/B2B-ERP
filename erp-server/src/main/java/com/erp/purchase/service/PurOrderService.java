package com.erp.purchase.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.finance.service.FinPayableService;
import com.erp.masterdata.entity.MdSupplier;
import com.erp.masterdata.mapper.MdSupplierMapper;
import com.erp.purchase.dto.PurOrderItemRequest;
import com.erp.purchase.dto.PurOrderRequest;
import com.erp.purchase.entity.PurOrder;
import com.erp.purchase.entity.PurOrderItem;
import com.erp.purchase.mapper.PurOrderItemMapper;
import com.erp.purchase.mapper.PurOrderMapper;
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
public class PurOrderService extends ServiceImpl<PurOrderMapper, PurOrder> {

    private final PurOrderItemMapper purOrderItemMapper;
    private final MdSupplierMapper supplierMapper;
    private final OrgWarehouseMapper warehouseMapper;
    private final FinPayableService payableService;

    public PageResult<PurOrder> pageOrders(Integer page, Integer size, String orderNo,
                                            Long supplierId, String status,
                                            String startDate, String endDate) {
        LambdaQueryWrapper<PurOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(orderNo), PurOrder::getOrderNo, orderNo)
                .eq(supplierId != null, PurOrder::getSupplierId, supplierId)
                .eq(StrUtil.isNotBlank(status), PurOrder::getStatus, status)
                .ge(StrUtil.isNotBlank(startDate), PurOrder::getOrderDate, startDate)
                .le(StrUtil.isNotBlank(endDate), PurOrder::getOrderDate, endDate)
                .orderByDesc(PurOrder::getCreatedAt);
        Page<PurOrder> result = page(new Page<>(page, size), wrapper);
        enrichOrders(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public PurOrder getDetailWithItems(Long id) {
        PurOrder order = getById(id);
        if (order == null) {
            throw new BusinessException("采购订单不存在");
        }
        List<PurOrderItem> items = purOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurOrderItem>()
                        .eq(PurOrderItem::getOrderId, id)
                        .orderByAsc(PurOrderItem::getLineNo));
        order.setItems(items);
        enrichOrders(Collections.singletonList(order));
        return order;
    }

    @Transactional
    public PurOrder createOrder(PurOrderRequest request, Long enterpriseId, Long operatorId) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("订单明细不能为空");
        }

        PurOrder order = new PurOrder()
                .setEnterpriseId(enterpriseId)
                .setStoreId(request.getStoreId())
                .setOrderNo("CG" + System.currentTimeMillis())
                .setOrderDate(request.getOrderDate())
                .setSupplierId(request.getSupplierId())
                .setWarehouseId(request.getWarehouseId())
                .setPurchaserId(request.getPurchaserId())
                .setStatus("DRAFT")
                .setFreightAmount(request.getFreightAmount() != null ? request.getFreightAmount() : BigDecimal.ZERO)
                .setExpectedArrivalDate(request.getExpectedArrivalDate())
                .setRemark(request.getRemark())
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);

        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        for (PurOrderItemRequest itemReq : request.getItems()) {
            BigDecimal quantity = itemReq.getQuantity();
            BigDecimal unitPrice = itemReq.getUnitPrice();
            BigDecimal lineAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            totalQuantity = totalQuantity.add(quantity);
            totalAmount = totalAmount.add(lineAmount);

            if (itemReq.getDiscountRate() != null && itemReq.getDiscountRate().compareTo(BigDecimal.ZERO) > 0) {
                discountAmount = discountAmount.add(
                        lineAmount.multiply(itemReq.getDiscountRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            }
        }

        BigDecimal freight = order.getFreightAmount();
        BigDecimal payableAmount = totalAmount.subtract(discountAmount).add(freight);

        order.setTotalQuantity(totalQuantity)
                .setTotalAmount(totalAmount)
                .setDiscountAmount(discountAmount)
                .setPayableAmount(payableAmount)
                .setPaidAmount(BigDecimal.ZERO);

        save(order);

        for (PurOrderItemRequest itemReq : request.getItems()) {
            BigDecimal quantity = itemReq.getQuantity();
            BigDecimal unitPrice = itemReq.getUnitPrice();
            BigDecimal lineAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            PurOrderItem item = new PurOrderItem()
                    .setOrderId(order.getId())
                    .setLineNo(itemReq.getLineNo())
                    .setProductId(itemReq.getProductId())
                    .setProductCode(itemReq.getProductCode())
                    .setProductName(itemReq.getProductName())
                    .setSpecification(itemReq.getSpecification())
                    .setUnitId(itemReq.getUnitId())
                    .setQuantity(quantity)
                    .setUnitPrice(unitPrice)
                    .setDiscountRate(itemReq.getDiscountRate() != null ? itemReq.getDiscountRate() : BigDecimal.ZERO)
                    .setTaxRate(itemReq.getTaxRate() != null ? itemReq.getTaxRate() : BigDecimal.ZERO)
                    .setAmount(lineAmount)
                    .setInboundQuantity(BigDecimal.ZERO)
                    .setReturnedQuantity(BigDecimal.ZERO)
                    .setRemark(itemReq.getRemark());
            purOrderItemMapper.insert(item);
        }

        return getDetailWithItems(order.getId());
    }

    @Transactional
    public PurOrder updateOrder(Long id, PurOrderRequest request) {
        PurOrder order = getById(id);
        if (order == null) {
            throw new BusinessException("采购订单不存在");
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException("只能修改草稿状态的订单");
        }

        order.setStoreId(request.getStoreId())
                .setOrderDate(request.getOrderDate())
                .setSupplierId(request.getSupplierId())
                .setWarehouseId(request.getWarehouseId())
                .setPurchaserId(request.getPurchaserId())
                .setFreightAmount(request.getFreightAmount() != null ? request.getFreightAmount() : BigDecimal.ZERO)
                .setExpectedArrivalDate(request.getExpectedArrivalDate())
                .setRemark(request.getRemark());

        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        for (PurOrderItemRequest itemReq : request.getItems()) {
            BigDecimal quantity = itemReq.getQuantity();
            BigDecimal unitPrice = itemReq.getUnitPrice();
            BigDecimal lineAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            totalQuantity = totalQuantity.add(quantity);
            totalAmount = totalAmount.add(lineAmount);

            if (itemReq.getDiscountRate() != null && itemReq.getDiscountRate().compareTo(BigDecimal.ZERO) > 0) {
                discountAmount = discountAmount.add(
                        lineAmount.multiply(itemReq.getDiscountRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            }
        }

        BigDecimal freight = order.getFreightAmount();
        BigDecimal payableAmount = totalAmount.subtract(discountAmount).add(freight);

        order.setTotalQuantity(totalQuantity)
                .setTotalAmount(totalAmount)
                .setDiscountAmount(discountAmount)
                .setPayableAmount(payableAmount);

        updateById(order);

        purOrderItemMapper.delete(new LambdaQueryWrapper<PurOrderItem>().eq(PurOrderItem::getOrderId, id));

        for (PurOrderItemRequest itemReq : request.getItems()) {
            BigDecimal quantity = itemReq.getQuantity();
            BigDecimal unitPrice = itemReq.getUnitPrice();
            BigDecimal lineAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            PurOrderItem item = new PurOrderItem()
                    .setOrderId(order.getId())
                    .setLineNo(itemReq.getLineNo())
                    .setProductId(itemReq.getProductId())
                    .setProductCode(itemReq.getProductCode())
                    .setProductName(itemReq.getProductName())
                    .setSpecification(itemReq.getSpecification())
                    .setUnitId(itemReq.getUnitId())
                    .setQuantity(quantity)
                    .setUnitPrice(unitPrice)
                    .setDiscountRate(itemReq.getDiscountRate() != null ? itemReq.getDiscountRate() : BigDecimal.ZERO)
                    .setTaxRate(itemReq.getTaxRate() != null ? itemReq.getTaxRate() : BigDecimal.ZERO)
                    .setAmount(lineAmount)
                    .setInboundQuantity(BigDecimal.ZERO)
                    .setReturnedQuantity(BigDecimal.ZERO)
                    .setRemark(itemReq.getRemark());
            purOrderItemMapper.insert(item);
        }

        return getDetailWithItems(order.getId());
    }

    @Transactional
    public void deleteOrder(Long id) {
        PurOrder order = getById(id);
        if (order == null) {
            throw new BusinessException("采购订单不存在");
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException("只能删除草稿状态的订单");
        }
        purOrderItemMapper.delete(new LambdaQueryWrapper<PurOrderItem>().eq(PurOrderItem::getOrderId, id));
        removeById(id);
    }

    @Transactional
    public void approve(Long id, Long operatorId) {
        PurOrder order = getById(id);
        if (order == null) {
            throw new BusinessException("采购订单不存在");
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException("只有草稿状态的订单可以审核");
        }
        order.setStatus("APPROVED")
                .setApprovedBy(operatorId)
                .setApprovedAt(LocalDateTime.now());
        updateById(order);
    }

    @Transactional
    public void cancel(Long id) {
        PurOrder order = getById(id);
        if (order == null) {
            throw new BusinessException("采购订单不存在");
        }
        if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("当前状态不允许取消");
        }
        order.setStatus("CANCELLED");
        updateById(order);
    }

    @Transactional
    public void complete(Long id, Long enterpriseId, Long operatorId) {
        PurOrder order = getById(id);
        if (order == null || !Objects.equals(order.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException("采购订单不存在");
        }
        if (!"APPROVED".equals(order.getStatus()) && !"PARTIALLY_INBOUND".equals(order.getStatus())) {
            throw new BusinessException("当前状态不允许完成");
        }
        order.setStatus("COMPLETED");
        updateById(order);
        payableService.createFromPurchaseOrder(order, operatorId);
    }

    public List<PurOrder> getInboundOptions() {
        LambdaQueryWrapper<PurOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PurOrder::getStatus, "APPROVED", "PARTIALLY_INBOUND")
                .orderByDesc(PurOrder::getCreatedAt);
        List<PurOrder> orders = list(wrapper);
        enrichOrders(orders);
        return orders;
    }

    private void enrichOrders(List<PurOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> supplierIds = orders.stream().map(PurOrder::getSupplierId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> warehouseIds = orders.stream().map(PurOrder::getWarehouseId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, MdSupplier> suppliers = supplierIds.isEmpty() ? Collections.emptyMap()
                : supplierMapper.selectBatchIds(supplierIds).stream()
                .collect(Collectors.toMap(MdSupplier::getId, Function.identity()));
        Map<Long, OrgWarehouse> warehouses = warehouseIds.isEmpty() ? Collections.emptyMap()
                : warehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(OrgWarehouse::getId, Function.identity()));

        orders.forEach(order -> {
            MdSupplier supplier = suppliers.get(order.getSupplierId());
            OrgWarehouse warehouse = warehouses.get(order.getWarehouseId());
            order.setSupplierName(supplier != null ? supplier.getSupplierName() : null);
            order.setWarehouseName(warehouse != null ? warehouse.getWarehouseName() : null);
        });
    }
}
