package com.erp.sales.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.finance.service.FinReceivableService;
import com.erp.masterdata.entity.MdCustomer;
import com.erp.masterdata.mapper.MdCustomerMapper;
import com.erp.sales.dto.SalOrderItemRequest;
import com.erp.sales.dto.SalOrderRequest;
import com.erp.sales.entity.SalOrder;
import com.erp.sales.entity.SalOrderItem;
import com.erp.sales.mapper.SalOrderItemMapper;
import com.erp.sales.mapper.SalOrderMapper;
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
public class SalOrderService extends ServiceImpl<SalOrderMapper, SalOrder> {

    private final SalOrderItemMapper salOrderItemMapper;
    private final MdCustomerMapper customerMapper;
    private final OrgWarehouseMapper warehouseMapper;
    private final FinReceivableService receivableService;

    public PageResult<SalOrder> pageOrders(Integer page, Integer size, String orderNo,
                                            Long customerId, String status,
                                            String startDate, String endDate) {
        LambdaQueryWrapper<SalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(orderNo), SalOrder::getOrderNo, orderNo)
                .eq(customerId != null, SalOrder::getCustomerId, customerId)
                .eq(StrUtil.isNotBlank(status), SalOrder::getStatus, status)
                .ge(StrUtil.isNotBlank(startDate), SalOrder::getOrderDate, startDate)
                .le(StrUtil.isNotBlank(endDate), SalOrder::getOrderDate, endDate)
                .orderByDesc(SalOrder::getCreatedAt);
        Page<SalOrder> result = page(new Page<>(page, size), wrapper);
        enrichOrders(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public SalOrder getDetailWithItems(Long id) {
        SalOrder order = getById(id);
        if (order == null) {
            throw new BusinessException("销售订单不存在");
        }
        List<SalOrderItem> items = salOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalOrderItem>()
                        .eq(SalOrderItem::getOrderId, id)
                        .orderByAsc(SalOrderItem::getLineNo));
        order.setItems(items);
        enrichOrders(Collections.singletonList(order));
        return order;
    }

    @Transactional
    public SalOrder createOrder(SalOrderRequest request, Long enterpriseId, Long operatorId) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("订单明细不能为空");
        }

        SalOrder order = new SalOrder()
                .setEnterpriseId(enterpriseId)
                .setStoreId(request.getStoreId())
                .setOrderNo("XS" + System.currentTimeMillis())
                .setOrderDate(request.getOrderDate())
                .setCustomerId(request.getCustomerId())
                .setWarehouseId(request.getWarehouseId())
                .setSalespersonId(request.getSalespersonId())
                .setStatus("DRAFT")
                .setFreightAmount(request.getFreightAmount() != null ? request.getFreightAmount() : BigDecimal.ZERO)
                .setDeliveryAddress(request.getDeliveryAddress())
                .setExpectedDeliveryDate(request.getExpectedDeliveryDate())
                .setRemark(request.getRemark())
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);

        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        for (SalOrderItemRequest itemReq : request.getItems()) {
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
                .setReceivedAmount(BigDecimal.ZERO);

        save(order);

        for (SalOrderItemRequest itemReq : request.getItems()) {
            BigDecimal quantity = itemReq.getQuantity();
            BigDecimal unitPrice = itemReq.getUnitPrice();
            BigDecimal lineAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            SalOrderItem item = new SalOrderItem()
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
                    .setOutboundQuantity(BigDecimal.ZERO)
                    .setReturnedQuantity(BigDecimal.ZERO)
                    .setRemark(itemReq.getRemark());
            salOrderItemMapper.insert(item);
        }

        return getDetailWithItems(order.getId());
    }

    @Transactional
    public SalOrder updateOrder(Long id, SalOrderRequest request) {
        SalOrder order = getById(id);
        if (order == null) {
            throw new BusinessException("销售订单不存在");
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException("只能修改草稿状态的订单");
        }

        order.setStoreId(request.getStoreId())
                .setOrderDate(request.getOrderDate())
                .setCustomerId(request.getCustomerId())
                .setWarehouseId(request.getWarehouseId())
                .setSalespersonId(request.getSalespersonId())
                .setFreightAmount(request.getFreightAmount() != null ? request.getFreightAmount() : BigDecimal.ZERO)
                .setDeliveryAddress(request.getDeliveryAddress())
                .setExpectedDeliveryDate(request.getExpectedDeliveryDate())
                .setRemark(request.getRemark());

        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        for (SalOrderItemRequest itemReq : request.getItems()) {
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

        salOrderItemMapper.delete(new LambdaQueryWrapper<SalOrderItem>().eq(SalOrderItem::getOrderId, id));

        for (SalOrderItemRequest itemReq : request.getItems()) {
            BigDecimal quantity = itemReq.getQuantity();
            BigDecimal unitPrice = itemReq.getUnitPrice();
            BigDecimal lineAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            SalOrderItem item = new SalOrderItem()
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
                    .setOutboundQuantity(BigDecimal.ZERO)
                    .setReturnedQuantity(BigDecimal.ZERO)
                    .setRemark(itemReq.getRemark());
            salOrderItemMapper.insert(item);
        }

        return getDetailWithItems(order.getId());
    }

    @Transactional
    public void deleteOrder(Long id) {
        SalOrder order = getById(id);
        if (order == null) {
            throw new BusinessException("销售订单不存在");
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException("只能删除草稿状态的订单");
        }
        salOrderItemMapper.delete(new LambdaQueryWrapper<SalOrderItem>().eq(SalOrderItem::getOrderId, id));
        removeById(id);
    }

    @Transactional
    public void approve(Long id, Long operatorId) {
        SalOrder order = getById(id);
        if (order == null) {
            throw new BusinessException("销售订单不存在");
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
        SalOrder order = getById(id);
        if (order == null) {
            throw new BusinessException("销售订单不存在");
        }
        if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("当前状态不允许取消");
        }
        order.setStatus("CANCELLED");
        updateById(order);
    }

    @Transactional
    public void complete(Long id, Long enterpriseId, Long operatorId) {
        SalOrder order = getById(id);
        if (order == null || !Objects.equals(order.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException("销售订单不存在");
        }
        if (!"APPROVED".equals(order.getStatus()) && !"PARTIALLY_OUTBOUND".equals(order.getStatus())) {
            throw new BusinessException("当前状态不允许完成");
        }
        order.setStatus("COMPLETED");
        updateById(order);
        receivableService.createFromSalesOrder(order, operatorId);
    }

    public List<SalOrder> getOutboundOptions() {
        LambdaQueryWrapper<SalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SalOrder::getStatus, "APPROVED", "PARTIALLY_OUTBOUND")
                .orderByDesc(SalOrder::getCreatedAt);
        List<SalOrder> orders = list(wrapper);
        enrichOrders(orders);
        return orders;
    }

    private void enrichOrders(List<SalOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> customerIds = orders.stream().map(SalOrder::getCustomerId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> warehouseIds = orders.stream().map(SalOrder::getWarehouseId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, MdCustomer> customers = customerIds.isEmpty() ? Collections.emptyMap()
                : customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(MdCustomer::getId, Function.identity()));
        Map<Long, OrgWarehouse> warehouses = warehouseIds.isEmpty() ? Collections.emptyMap()
                : warehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(OrgWarehouse::getId, Function.identity()));

        orders.forEach(order -> {
            MdCustomer customer = customers.get(order.getCustomerId());
            OrgWarehouse warehouse = warehouses.get(order.getWarehouseId());
            order.setCustomerName(customer != null ? customer.getCustomerName() : null);
            order.setWarehouseName(warehouse != null ? warehouse.getWarehouseName() : null);
        });
    }
}
