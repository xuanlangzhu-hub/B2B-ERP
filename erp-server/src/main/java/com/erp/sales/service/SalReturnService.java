package com.erp.sales.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.finance.entity.FinReceivable;
import com.erp.finance.mapper.FinReceivableMapper;
import com.erp.masterdata.entity.MdCustomer;
import com.erp.masterdata.mapper.MdCustomerMapper;
import com.erp.sales.dto.SalReturnItemRequest;
import com.erp.sales.dto.SalReturnRequest;
import com.erp.sales.entity.*;
import com.erp.sales.mapper.*;
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
public class SalReturnService extends ServiceImpl<SalReturnMapper, SalReturn> {
    private final SalReturnItemMapper returnItemMapper;
    private final SalOrderMapper orderMapper;
    private final SalOrderItemMapper orderItemMapper;
    private final FinReceivableMapper receivableMapper;
    private final MdCustomerMapper customerMapper;
    private final OrgWarehouseMapper warehouseMapper;

    public PageResult<SalReturn> pageReturns(Long enterpriseId, Integer page, Integer size,
                                             String returnNo, Long customerId, String status) {
        LambdaQueryWrapper<SalReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalReturn::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(returnNo), SalReturn::getReturnNo, returnNo)
                .eq(customerId != null, SalReturn::getCustomerId, customerId)
                .eq(StrUtil.isNotBlank(status), SalReturn::getStatus, status)
                .orderByDesc(SalReturn::getCreatedAt);
        Page<SalReturn> result = page(new Page<>(page, size), wrapper);
        enrich(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public SalReturn detail(Long id, Long enterpriseId) {
        SalReturn row = lambdaQuery().eq(SalReturn::getEnterpriseId, enterpriseId)
                .eq(SalReturn::getId, id).one();
        if (row == null) throw new BusinessException("销售退货单不存在");
        row.setItems(returnItemMapper.selectList(new LambdaQueryWrapper<SalReturnItem>()
                .eq(SalReturnItem::getReturnId, id).orderByAsc(SalReturnItem::getLineNo)));
        enrich(Collections.singletonList(row));
        return row;
    }

    @Transactional
    public SalReturn create(SalReturnRequest request, Long enterpriseId, Long operatorId) {
        SalOrder order = orderMapper.selectById(request.getSalesOrderId());
        if (order == null || !Objects.equals(order.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException("原销售单不存在");
        }
        if (!"COMPLETED".equals(order.getStatus())) throw new BusinessException("只能对已完成销售单申请退货");
        if (lambdaQuery().eq(SalReturn::getEnterpriseId, enterpriseId)
                .eq(SalReturn::getSalesOrderId, order.getId())
                .ne(SalReturn::getStatus, "CANCELLED").count() > 0) {
            throw new BusinessException("该销售单已存在退货申请");
        }
        OrgWarehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null || !Objects.equals(warehouse.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException("退货仓库不存在");
        }
        FinReceivable receivable = receivableMapper.selectOne(new LambdaQueryWrapper<FinReceivable>()
                .eq(FinReceivable::getEnterpriseId, enterpriseId)
                .eq(FinReceivable::getSourceType, "SALES_ORDER")
                .eq(FinReceivable::getSourceId, order.getId()));
        if (receivable == null || receivable.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("原销售单没有可冲减的未收款，暂不支持退款型退货");
        }

        Map<Long, SalOrderItem> sourceItems = orderItemMapper.selectList(
                        new LambdaQueryWrapper<SalOrderItem>().eq(SalOrderItem::getOrderId, order.getId()))
                .stream().collect(Collectors.toMap(SalOrderItem::getId, Function.identity()));
        Set<Long> usedItems = new HashSet<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<SalReturnItem> items = new ArrayList<>();
        int lineNo = 1;
        for (SalReturnItemRequest itemRequest : request.getItems()) {
            if (!usedItems.add(itemRequest.getSalesOrderItemId())) throw new BusinessException("退货明细商品重复");
            SalOrderItem source = sourceItems.get(itemRequest.getSalesOrderItemId());
            if (source == null) throw new BusinessException("销售单明细不存在");
            BigDecimal sold = zero(source.getOutboundQuantity());
            BigDecimal available = sold.subtract(zero(source.getReturnedQuantity()));
            if (itemRequest.getQuantity().compareTo(available) > 0) {
                throw new BusinessException("商品 " + source.getProductName() + " 的退货数量超过可退数量 " + available);
            }
            BigDecimal amount = itemRequest.getQuantity().multiply(source.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            totalQuantity = totalQuantity.add(itemRequest.getQuantity());
            totalAmount = totalAmount.add(amount);
            items.add(new SalReturnItem()
                    .setLineNo(lineNo++)
                    .setSalesOrderItemId(source.getId())
                    .setProductId(source.getProductId())
                    .setProductCode(source.getProductCode())
                    .setProductName(source.getProductName())
                    .setSpecification(source.getSpecification())
                    .setUnitId(source.getUnitId())
                    .setQuantity(itemRequest.getQuantity())
                    .setUnitPrice(source.getUnitPrice())
                    .setAmount(amount)
                    .setInboundQuantity(BigDecimal.ZERO)
                    .setRemark(itemRequest.getRemark()));
        }
        if (totalAmount.compareTo(receivable.getOutstandingAmount()) > 0) {
            throw new BusinessException("退货金额超过未收金额，当前版本暂不支持自动退款");
        }

        SalReturn salReturn = new SalReturn()
                .setEnterpriseId(enterpriseId)
                .setStoreId(order.getStoreId())
                .setReturnNo("XT" + System.currentTimeMillis())
                .setReturnDate(request.getReturnDate())
                .setSalesOrderId(order.getId())
                .setCustomerId(order.getCustomerId())
                .setWarehouseId(request.getWarehouseId())
                .setStatus("DRAFT")
                .setTotalQuantity(totalQuantity)
                .setTotalAmount(totalAmount)
                .setRefundAmount(BigDecimal.ZERO)
                .setReturnReason(request.getReturnReason())
                .setRemark(request.getRemark())
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);
        save(salReturn);
        items.forEach(item -> {
            item.setReturnId(salReturn.getId());
            returnItemMapper.insert(item);
        });
        return detail(salReturn.getId(), enterpriseId);
    }

    @Transactional
    public void approve(Long id, Long enterpriseId, Long operatorId) {
        SalReturn row = baseMapper.selectForUpdate(id, enterpriseId);
        if (row == null) throw new BusinessException("销售退货单不存在");
        if (!"DRAFT".equals(row.getStatus())) throw new BusinessException("只有草稿退货单可以审核");
        row.setStatus("APPROVED").setApprovedBy(operatorId).setApprovedAt(LocalDateTime.now()).setUpdatedBy(operatorId);
        updateById(row);
    }

    @Transactional
    public void cancel(Long id, Long enterpriseId, Long operatorId) {
        SalReturn row = baseMapper.selectForUpdate(id, enterpriseId);
        if (row == null) throw new BusinessException("销售退货单不存在");
        if (!"DRAFT".equals(row.getStatus())) throw new BusinessException("只有草稿退货单可以取消");
        row.setStatus("CANCELLED").setUpdatedBy(operatorId);
        updateById(row);
    }

    @Transactional
    public void completeAfterInbound(Long id, Long enterpriseId, Long operatorId) {
        SalReturn row = baseMapper.selectForUpdate(id, enterpriseId);
        if (row == null || !"APPROVED".equals(row.getStatus())) throw new BusinessException("销售退货单状态不允许完成");
        FinReceivable candidate = receivableMapper.selectOne(new LambdaQueryWrapper<FinReceivable>()
                .eq(FinReceivable::getEnterpriseId, enterpriseId)
                .eq(FinReceivable::getSourceType, "SALES_ORDER")
                .eq(FinReceivable::getSourceId, row.getSalesOrderId()));
        if (candidate == null) throw new BusinessException("原销售单应收不存在");
        FinReceivable receivable = receivableMapper.selectForUpdate(candidate.getId(), enterpriseId);
        if (receivable.getOutstandingAmount().compareTo(row.getTotalAmount()) < 0) {
            throw new BusinessException("未收金额不足以冲减本次退货，暂不支持自动退款");
        }
        BigDecimal newOutstanding = receivable.getOutstandingAmount().subtract(row.getTotalAmount());
        BigDecimal newOriginal = receivable.getOriginalAmount().subtract(row.getTotalAmount());
        receivable.setOriginalAmount(newOriginal)
                .setOutstandingAmount(newOutstanding)
                .setStatus(newOutstanding.compareTo(BigDecimal.ZERO) == 0 ? "SETTLED"
                        : zero(receivable.getReceivedAmount()).compareTo(BigDecimal.ZERO) > 0 ? "PARTIALLY_SETTLED" : "UNSETTLED")
                .setUpdatedBy(operatorId);
        receivableMapper.updateById(receivable);

        SalOrder order = orderMapper.selectById(row.getSalesOrderId());
        order.setPayableAmount(order.getPayableAmount().subtract(row.getTotalAmount()))
                .setSettlementStatus(newOutstanding.compareTo(BigDecimal.ZERO) == 0 ? "PAID"
                        : zero(receivable.getReceivedAmount()).compareTo(BigDecimal.ZERO) > 0 ? "PARTIALLY_PAID" : "UNPAID")
                .setUpdatedBy(operatorId);
        orderMapper.updateById(order);
        row.setStatus("COMPLETED").setUpdatedBy(operatorId);
        updateById(row);
    }

    private void enrich(List<SalReturn> rows) {
        if (rows == null || rows.isEmpty()) return;
        Map<Long, SalOrder> orders = orderMapper.selectBatchIds(rows.stream().map(SalReturn::getSalesOrderId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(SalOrder::getId, Function.identity()));
        Map<Long, MdCustomer> customers = customerMapper.selectBatchIds(rows.stream().map(SalReturn::getCustomerId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(MdCustomer::getId, Function.identity()));
        Map<Long, OrgWarehouse> warehouses = warehouseMapper.selectBatchIds(rows.stream().map(SalReturn::getWarehouseId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(OrgWarehouse::getId, Function.identity()));
        rows.forEach(row -> {
            SalOrder order = orders.get(row.getSalesOrderId());
            MdCustomer customer = customers.get(row.getCustomerId());
            OrgWarehouse warehouse = warehouses.get(row.getWarehouseId());
            row.setSalesOrderNo(order == null ? null : order.getOrderNo());
            row.setCustomerName(customer == null ? null : customer.getCustomerName());
            row.setWarehouseName(warehouse == null ? null : warehouse.getWarehouseName());
        });
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
