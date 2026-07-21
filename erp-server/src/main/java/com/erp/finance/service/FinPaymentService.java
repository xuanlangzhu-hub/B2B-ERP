package com.erp.finance.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.finance.dto.FinPaymentRequest;
import com.erp.finance.entity.*;
import com.erp.finance.mapper.*;
import com.erp.masterdata.entity.MdSupplier;
import com.erp.masterdata.mapper.MdSupplierMapper;
import com.erp.purchase.entity.PurOrder;
import com.erp.purchase.mapper.PurOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinPaymentService extends ServiceImpl<FinPaymentMapper, FinPayment> {
    private final MdSupplierMapper supplierMapper;
    private final FinAccountMapper accountMapper;
    private final FinPayableMapper payableMapper;
    private final FinPaymentItemMapper paymentItemMapper;
    private final FinCapitalFlowMapper capitalFlowMapper;
    private final PurOrderMapper purOrderMapper;

    public PageResult<FinPayment> pagePayments(Long enterpriseId, Integer page, Integer size,
                                               String paymentNo, Long supplierId,
                                               String startDate, String endDate) {
        LambdaQueryWrapper<FinPayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinPayment::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(paymentNo), FinPayment::getPaymentNo, paymentNo)
                .eq(supplierId != null, FinPayment::getSupplierId, supplierId)
                .ge(StrUtil.isNotBlank(startDate), FinPayment::getPaymentDate, startDate)
                .le(StrUtil.isNotBlank(endDate), FinPayment::getPaymentDate, endDate)
                .orderByDesc(FinPayment::getCreatedAt);
        Page<FinPayment> result = page(new Page<>(page, size), wrapper);
        enrichPayments(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public FinPayment createPayment(FinPaymentRequest request, Long enterpriseId, Long operatorId) {
        if (request.getPaymentAmount() == null || request.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("付款金额必须大于0");
        }
        validateSupplier(request.getSupplierId(), enterpriseId);
        validateAccount(request.getAccountId(), enterpriseId);

        FinPayment payment = new FinPayment()
                .setEnterpriseId(enterpriseId)
                .setStoreId(request.getStoreId())
                .setPaymentNo("FK" + System.currentTimeMillis())
                .setPaymentDate(request.getPaymentDate())
                .setSupplierId(request.getSupplierId())
                .setAccountId(request.getAccountId())
                .setPaymentMethod(request.getPaymentMethod())
                .setPaymentAmount(request.getPaymentAmount())
                .setAllocatedAmount(BigDecimal.ZERO)
                .setUnallocatedAmount(request.getPaymentAmount())
                .setStatus("DRAFT")
                .setReferenceNo(request.getReferenceNo())
                .setRemark(request.getRemark())
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);
        save(payment);
        enrichPayments(Collections.singletonList(payment));
        return payment;
    }

    @Transactional
    public void confirmPayment(Long id, Long enterpriseId, Long operatorId) {
        FinPayment payment = baseMapper.selectForUpdate(id, enterpriseId);
        if (payment == null) throw new BusinessException("付款单不存在");
        if (!"DRAFT".equals(payment.getStatus())) {
            throw new BusinessException("只有草稿状态的付款单可以确认");
        }
        FinAccount account = accountMapper.selectForUpdate(payment.getAccountId(), enterpriseId);
        if (account == null || !"ENABLED".equals(account.getStatus())) {
            throw new BusinessException("付款账户不存在或已停用");
        }
        BigDecimal beforeBalance = zero(account.getCurrentBalance());
        if (beforeBalance.compareTo(payment.getPaymentAmount()) < 0) {
            throw new BusinessException("账户余额不足");
        }

        BigDecimal remaining = payment.getPaymentAmount();
        List<FinPayable> candidates = payableMapper.selectList(new LambdaQueryWrapper<FinPayable>()
                .eq(FinPayable::getEnterpriseId, enterpriseId)
                .eq(FinPayable::getSupplierId, payment.getSupplierId())
                .in(FinPayable::getStatus, "UNSETTLED", "PARTIALLY_SETTLED")
                .gt(FinPayable::getOutstandingAmount, BigDecimal.ZERO)
                .orderByAsc(FinPayable::getBusinessDate)
                .orderByAsc(FinPayable::getId));

        BigDecimal allocated = BigDecimal.ZERO;
        for (FinPayable candidate : candidates) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            FinPayable payable = payableMapper.selectForUpdate(candidate.getId(), enterpriseId);
            if (payable == null || payable.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal currentAllocation = remaining.min(payable.getOutstandingAmount());
            BigDecimal newOutstanding = payable.getOutstandingAmount().subtract(currentAllocation);
            paymentItemMapper.insert(new FinPaymentItem()
                    .setPaymentId(payment.getId())
                    .setPayableId(payable.getId())
                    .setSourceNo(payable.getSourceNo())
                    .setAllocatedAmount(currentAllocation));
            payable.setPaidAmount(zero(payable.getPaidAmount()).add(currentAllocation))
                    .setOutstandingAmount(newOutstanding)
                    .setStatus(newOutstanding.compareTo(BigDecimal.ZERO) == 0 ? "SETTLED" : "PARTIALLY_SETTLED")
                    .setUpdatedBy(operatorId);
            payableMapper.updateById(payable);
            syncPurchaseOrder(payable, operatorId);
            allocated = allocated.add(currentAllocation);
            remaining = remaining.subtract(currentAllocation);
        }

        BigDecimal afterBalance = beforeBalance.subtract(payment.getPaymentAmount());
        account.setCurrentBalance(afterBalance).setUpdatedBy(operatorId);
        accountMapper.updateById(account);
        insertFlow(payment, operatorId, "PAYMENT", "OUT", beforeBalance, afterBalance,
                "采购付款资金流水");

        payment.setAllocatedAmount(allocated)
                .setUnallocatedAmount(remaining)
                .setStatus("CONFIRMED")
                .setConfirmedBy(operatorId)
                .setConfirmedAt(LocalDateTime.now())
                .setUpdatedBy(operatorId);
        updateById(payment);
    }

    @Transactional
    public void cancelPayment(Long id, Long enterpriseId, Long operatorId) {
        FinPayment payment = baseMapper.selectForUpdate(id, enterpriseId);
        if (payment == null) throw new BusinessException("付款单不存在");
        if ("CANCELLED".equals(payment.getStatus())) throw new BusinessException("付款单已取消");
        if ("DRAFT".equals(payment.getStatus())) {
            payment.setStatus("CANCELLED").setUpdatedBy(operatorId);
            updateById(payment);
            return;
        }
        if (!"CONFIRMED".equals(payment.getStatus())) throw new BusinessException("当前状态不允许取消");

        List<FinPaymentItem> items = paymentItemMapper.selectList(new LambdaQueryWrapper<FinPaymentItem>()
                .eq(FinPaymentItem::getPaymentId, payment.getId()));
        for (FinPaymentItem item : items) {
            FinPayable payable = payableMapper.selectForUpdate(item.getPayableId(), enterpriseId);
            if (payable == null) throw new BusinessException("核销应付记录不存在，无法冲销");
            BigDecimal newPaid = zero(payable.getPaidAmount()).subtract(item.getAllocatedAmount());
            if (newPaid.compareTo(BigDecimal.ZERO) < 0) throw new BusinessException("应付已付金额异常，无法冲销");
            BigDecimal newOutstanding = payable.getOutstandingAmount().add(item.getAllocatedAmount());
            payable.setPaidAmount(newPaid)
                    .setOutstandingAmount(newOutstanding)
                    .setStatus(newPaid.compareTo(BigDecimal.ZERO) == 0 ? "UNSETTLED" : "PARTIALLY_SETTLED")
                    .setUpdatedBy(operatorId);
            payableMapper.updateById(payable);
            syncPurchaseOrder(payable, operatorId);
        }

        FinAccount account = accountMapper.selectForUpdate(payment.getAccountId(), enterpriseId);
        if (account == null) throw new BusinessException("付款账户不存在");
        BigDecimal beforeBalance = zero(account.getCurrentBalance());
        BigDecimal afterBalance = beforeBalance.add(payment.getPaymentAmount());
        account.setCurrentBalance(afterBalance).setUpdatedBy(operatorId);
        accountMapper.updateById(account);
        insertFlow(payment, operatorId, "PAYMENT_REVERSAL", "IN", beforeBalance, afterBalance,
                "取消付款冲销流水");

        payment.setStatus("CANCELLED").setUpdatedBy(operatorId);
        updateById(payment);
    }

    private void insertFlow(FinPayment payment, Long operatorId, String flowType, String direction,
                            BigDecimal beforeBalance, BigDecimal afterBalance, String remark) {
        Long existing = capitalFlowMapper.selectCount(new LambdaQueryWrapper<FinCapitalFlow>()
                .eq(FinCapitalFlow::getEnterpriseId, payment.getEnterpriseId())
                .eq(FinCapitalFlow::getSourceType, "PAYMENT")
                .eq(FinCapitalFlow::getSourceId, payment.getId())
                .eq(FinCapitalFlow::getFlowType, flowType));
        if (existing > 0) throw new BusinessException("该付款单已生成对应资金流水");
        MdSupplier supplier = supplierMapper.selectById(payment.getSupplierId());
        capitalFlowMapper.insert(new FinCapitalFlow()
                .setEnterpriseId(payment.getEnterpriseId())
                .setStoreId(payment.getStoreId())
                .setAccountId(payment.getAccountId())
                .setFlowNo("ZJFK" + System.currentTimeMillis() + payment.getId())
                .setFlowDate(payment.getPaymentDate())
                .setFlowType(flowType)
                .setDirection(direction)
                .setAmount(payment.getPaymentAmount())
                .setBeforeBalance(beforeBalance)
                .setAfterBalance(afterBalance)
                .setSourceType("PAYMENT")
                .setSourceId(payment.getId())
                .setSourceNo(payment.getPaymentNo())
                .setCounterpartyType("SUPPLIER")
                .setCounterpartyId(payment.getSupplierId())
                .setCounterpartyName(supplier == null ? null : supplier.getSupplierName())
                .setOperatorId(operatorId)
                .setRemark(remark));
    }

    private void syncPurchaseOrder(FinPayable payable, Long operatorId) {
        if (!"PURCHASE_ORDER".equals(payable.getSourceType())) return;
        PurOrder order = purOrderMapper.selectById(payable.getSourceId());
        if (order == null || !Objects.equals(order.getEnterpriseId(), payable.getEnterpriseId())) return;
        BigDecimal paid = zero(payable.getPaidAmount()).add(zero(payable.getWriteOffAmount()));
        String settlement = paid.compareTo(BigDecimal.ZERO) == 0 ? "UNPAID"
                : payable.getOutstandingAmount().compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PARTIALLY_PAID";
        order.setPaidAmount(paid).setSettlementStatus(settlement).setUpdatedBy(operatorId);
        purOrderMapper.updateById(order);
    }

    private void validateSupplier(Long supplierId, Long enterpriseId) {
        MdSupplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null || !Objects.equals(supplier.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException("供应商不存在");
        }
    }

    private void validateAccount(Long accountId, Long enterpriseId) {
        FinAccount account = accountMapper.selectById(accountId);
        if (account == null || !Objects.equals(account.getEnterpriseId(), enterpriseId)
                || !"ENABLED".equals(account.getStatus())) {
            throw new BusinessException("账户不存在或已停用");
        }
    }

    private void enrichPayments(List<FinPayment> payments) {
        if (payments == null || payments.isEmpty()) return;
        List<Long> supplierIds = payments.stream().map(FinPayment::getSupplierId)
                .filter(Objects::nonNull).distinct().toList();
        List<Long> accountIds = payments.stream().map(FinPayment::getAccountId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, MdSupplier> suppliers = supplierIds.isEmpty() ? Collections.emptyMap()
                : supplierMapper.selectBatchIds(supplierIds).stream()
                .collect(Collectors.toMap(MdSupplier::getId, Function.identity()));
        Map<Long, FinAccount> accounts = accountIds.isEmpty() ? Collections.emptyMap()
                : accountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(FinAccount::getId, Function.identity()));
        payments.forEach(payment -> {
            MdSupplier supplier = suppliers.get(payment.getSupplierId());
            FinAccount account = accounts.get(payment.getAccountId());
            payment.setSupplierName(supplier == null ? null : supplier.getSupplierName());
            payment.setAccountName(account == null ? null : account.getAccountName());
        });
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
