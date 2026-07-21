package com.erp.finance.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.finance.dto.FinReceiptRequest;
import com.erp.finance.entity.*;
import com.erp.finance.mapper.*;
import com.erp.masterdata.entity.MdCustomer;
import com.erp.masterdata.mapper.MdCustomerMapper;
import com.erp.sales.entity.SalOrder;
import com.erp.sales.mapper.SalOrderMapper;
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
public class FinReceiptService extends ServiceImpl<FinReceiptMapper, FinReceipt> {
    private final MdCustomerMapper customerMapper;
    private final FinAccountMapper accountMapper;
    private final FinReceivableMapper receivableMapper;
    private final FinReceiptItemMapper receiptItemMapper;
    private final FinCapitalFlowMapper capitalFlowMapper;
    private final SalOrderMapper salOrderMapper;

    public PageResult<FinReceipt> pageReceipts(Long enterpriseId, Integer page, Integer size,
                                               String receiptNo, Long customerId,
                                               String startDate, String endDate) {
        LambdaQueryWrapper<FinReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinReceipt::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(receiptNo), FinReceipt::getReceiptNo, receiptNo)
                .eq(customerId != null, FinReceipt::getCustomerId, customerId)
                .ge(StrUtil.isNotBlank(startDate), FinReceipt::getReceiptDate, startDate)
                .le(StrUtil.isNotBlank(endDate), FinReceipt::getReceiptDate, endDate)
                .orderByDesc(FinReceipt::getCreatedAt);
        Page<FinReceipt> result = page(new Page<>(page, size), wrapper);
        enrichReceipts(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public FinReceipt createReceipt(FinReceiptRequest request, Long enterpriseId, Long operatorId) {
        if (request.getReceiptAmount() == null || request.getReceiptAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("收款金额必须大于0");
        }
        validateCustomer(request.getCustomerId(), enterpriseId);
        validateAccount(request.getAccountId(), enterpriseId);

        FinReceipt receipt = new FinReceipt()
                .setEnterpriseId(enterpriseId)
                .setStoreId(request.getStoreId())
                .setReceiptNo("SK" + System.currentTimeMillis())
                .setReceiptDate(request.getReceiptDate())
                .setCustomerId(request.getCustomerId())
                .setAccountId(request.getAccountId())
                .setPaymentMethod(request.getPaymentMethod())
                .setReceiptAmount(request.getReceiptAmount())
                .setAllocatedAmount(BigDecimal.ZERO)
                .setUnallocatedAmount(request.getReceiptAmount())
                .setStatus("DRAFT")
                .setReferenceNo(request.getReferenceNo())
                .setRemark(request.getRemark())
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);
        save(receipt);
        enrichReceipts(Collections.singletonList(receipt));
        return receipt;
    }

    @Transactional
    public void confirmReceipt(Long id, Long enterpriseId, Long operatorId) {
        FinReceipt receipt = baseMapper.selectForUpdate(id, enterpriseId);
        if (receipt == null) throw new BusinessException("收款单不存在");
        if (!"DRAFT".equals(receipt.getStatus())) {
            throw new BusinessException("只有草稿状态的收款单可以确认");
        }
        FinAccount account = accountMapper.selectForUpdate(receipt.getAccountId(), enterpriseId);
        if (account == null || !"ENABLED".equals(account.getStatus())) {
            throw new BusinessException("收款账户不存在或已停用");
        }

        BigDecimal remaining = receipt.getReceiptAmount();
        List<FinReceivable> candidates = receivableMapper.selectList(new LambdaQueryWrapper<FinReceivable>()
                .eq(FinReceivable::getEnterpriseId, enterpriseId)
                .eq(FinReceivable::getCustomerId, receipt.getCustomerId())
                .in(FinReceivable::getStatus, "UNSETTLED", "PARTIALLY_SETTLED")
                .gt(FinReceivable::getOutstandingAmount, BigDecimal.ZERO)
                .orderByAsc(FinReceivable::getBusinessDate)
                .orderByAsc(FinReceivable::getId));

        BigDecimal allocated = BigDecimal.ZERO;
        for (FinReceivable candidate : candidates) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            FinReceivable receivable = receivableMapper.selectForUpdate(candidate.getId(), enterpriseId);
            if (receivable == null || receivable.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal currentAllocation = remaining.min(receivable.getOutstandingAmount());
            BigDecimal newOutstanding = receivable.getOutstandingAmount().subtract(currentAllocation);
            receiptItemMapper.insert(new FinReceiptItem()
                    .setReceiptId(receipt.getId())
                    .setReceivableId(receivable.getId())
                    .setSourceNo(receivable.getSourceNo())
                    .setAllocatedAmount(currentAllocation));
            receivable.setReceivedAmount(zero(receivable.getReceivedAmount()).add(currentAllocation))
                    .setOutstandingAmount(newOutstanding)
                    .setStatus(newOutstanding.compareTo(BigDecimal.ZERO) == 0 ? "SETTLED" : "PARTIALLY_SETTLED")
                    .setUpdatedBy(operatorId);
            receivableMapper.updateById(receivable);
            syncSalesOrder(receivable, operatorId);
            allocated = allocated.add(currentAllocation);
            remaining = remaining.subtract(currentAllocation);
        }

        BigDecimal beforeBalance = zero(account.getCurrentBalance());
        BigDecimal afterBalance = beforeBalance.add(receipt.getReceiptAmount());
        account.setCurrentBalance(afterBalance).setUpdatedBy(operatorId);
        accountMapper.updateById(account);

        insertFlow(receipt, operatorId, "RECEIPT", "IN", beforeBalance, afterBalance,
                "销售收款资金流水");
        receipt.setAllocatedAmount(allocated)
                .setUnallocatedAmount(remaining)
                .setStatus("CONFIRMED")
                .setConfirmedBy(operatorId)
                .setConfirmedAt(LocalDateTime.now())
                .setUpdatedBy(operatorId);
        updateById(receipt);
    }

    @Transactional
    public void cancelReceipt(Long id, Long enterpriseId, Long operatorId) {
        FinReceipt receipt = baseMapper.selectForUpdate(id, enterpriseId);
        if (receipt == null) throw new BusinessException("收款单不存在");
        if ("CANCELLED".equals(receipt.getStatus())) throw new BusinessException("收款单已取消");
        if ("DRAFT".equals(receipt.getStatus())) {
            receipt.setStatus("CANCELLED").setUpdatedBy(operatorId);
            updateById(receipt);
            return;
        }
        if (!"CONFIRMED".equals(receipt.getStatus())) throw new BusinessException("当前状态不允许取消");

        List<FinReceiptItem> items = receiptItemMapper.selectList(new LambdaQueryWrapper<FinReceiptItem>()
                .eq(FinReceiptItem::getReceiptId, receipt.getId()));
        for (FinReceiptItem item : items) {
            FinReceivable receivable = receivableMapper.selectForUpdate(item.getReceivableId(), enterpriseId);
            if (receivable == null) throw new BusinessException("核销应收记录不存在，无法冲销");
            BigDecimal newReceived = zero(receivable.getReceivedAmount()).subtract(item.getAllocatedAmount());
            if (newReceived.compareTo(BigDecimal.ZERO) < 0) throw new BusinessException("应收已收金额异常，无法冲销");
            BigDecimal newOutstanding = receivable.getOutstandingAmount().add(item.getAllocatedAmount());
            receivable.setReceivedAmount(newReceived)
                    .setOutstandingAmount(newOutstanding)
                    .setStatus(newReceived.compareTo(BigDecimal.ZERO) == 0 ? "UNSETTLED" : "PARTIALLY_SETTLED")
                    .setUpdatedBy(operatorId);
            receivableMapper.updateById(receivable);
            syncSalesOrder(receivable, operatorId);
        }

        FinAccount account = accountMapper.selectForUpdate(receipt.getAccountId(), enterpriseId);
        if (account == null) throw new BusinessException("收款账户不存在");
        BigDecimal beforeBalance = zero(account.getCurrentBalance());
        BigDecimal afterBalance = beforeBalance.subtract(receipt.getReceiptAmount());
        account.setCurrentBalance(afterBalance).setUpdatedBy(operatorId);
        accountMapper.updateById(account);
        insertFlow(receipt, operatorId, "RECEIPT_REVERSAL", "OUT", beforeBalance, afterBalance,
                "取消收款冲销流水");

        receipt.setStatus("CANCELLED").setUpdatedBy(operatorId);
        updateById(receipt);
    }

    private void insertFlow(FinReceipt receipt, Long operatorId, String flowType, String direction,
                            BigDecimal beforeBalance, BigDecimal afterBalance, String remark) {
        Long existing = capitalFlowMapper.selectCount(new LambdaQueryWrapper<FinCapitalFlow>()
                .eq(FinCapitalFlow::getEnterpriseId, receipt.getEnterpriseId())
                .eq(FinCapitalFlow::getSourceType, "RECEIPT")
                .eq(FinCapitalFlow::getSourceId, receipt.getId())
                .eq(FinCapitalFlow::getFlowType, flowType));
        if (existing > 0) throw new BusinessException("该收款单已生成对应资金流水");
        MdCustomer customer = customerMapper.selectById(receipt.getCustomerId());
        capitalFlowMapper.insert(new FinCapitalFlow()
                .setEnterpriseId(receipt.getEnterpriseId())
                .setStoreId(receipt.getStoreId())
                .setAccountId(receipt.getAccountId())
                .setFlowNo("ZJSK" + System.currentTimeMillis() + receipt.getId())
                .setFlowDate(receipt.getReceiptDate())
                .setFlowType(flowType)
                .setDirection(direction)
                .setAmount(receipt.getReceiptAmount())
                .setBeforeBalance(beforeBalance)
                .setAfterBalance(afterBalance)
                .setSourceType("RECEIPT")
                .setSourceId(receipt.getId())
                .setSourceNo(receipt.getReceiptNo())
                .setCounterpartyType("CUSTOMER")
                .setCounterpartyId(receipt.getCustomerId())
                .setCounterpartyName(customer == null ? null : customer.getCustomerName())
                .setOperatorId(operatorId)
                .setRemark(remark));
    }

    private void syncSalesOrder(FinReceivable receivable, Long operatorId) {
        if (!"SALES_ORDER".equals(receivable.getSourceType())) return;
        SalOrder order = salOrderMapper.selectById(receivable.getSourceId());
        if (order == null || !Objects.equals(order.getEnterpriseId(), receivable.getEnterpriseId())) return;
        BigDecimal received = zero(receivable.getReceivedAmount()).add(zero(receivable.getWriteOffAmount()));
        String settlement = received.compareTo(BigDecimal.ZERO) == 0 ? "UNPAID"
                : receivable.getOutstandingAmount().compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PARTIALLY_PAID";
        order.setReceivedAmount(received).setSettlementStatus(settlement).setUpdatedBy(operatorId);
        salOrderMapper.updateById(order);
    }

    private void validateCustomer(Long customerId, Long enterpriseId) {
        MdCustomer customer = customerMapper.selectById(customerId);
        if (customer == null || !Objects.equals(customer.getEnterpriseId(), enterpriseId)) {
            throw new BusinessException("客户不存在");
        }
    }

    private void validateAccount(Long accountId, Long enterpriseId) {
        FinAccount account = accountMapper.selectById(accountId);
        if (account == null || !Objects.equals(account.getEnterpriseId(), enterpriseId)
                || !"ENABLED".equals(account.getStatus())) {
            throw new BusinessException("账户不存在或已停用");
        }
    }

    private void enrichReceipts(List<FinReceipt> receipts) {
        if (receipts == null || receipts.isEmpty()) return;
        List<Long> customerIds = receipts.stream().map(FinReceipt::getCustomerId)
                .filter(Objects::nonNull).distinct().toList();
        List<Long> accountIds = receipts.stream().map(FinReceipt::getAccountId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, MdCustomer> customers = customerIds.isEmpty() ? Collections.emptyMap()
                : customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(MdCustomer::getId, Function.identity()));
        Map<Long, FinAccount> accounts = accountIds.isEmpty() ? Collections.emptyMap()
                : accountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(FinAccount::getId, Function.identity()));
        receipts.forEach(receipt -> {
            MdCustomer customer = customers.get(receipt.getCustomerId());
            FinAccount account = accounts.get(receipt.getAccountId());
            receipt.setCustomerName(customer == null ? null : customer.getCustomerName());
            receipt.setAccountName(account == null ? null : account.getAccountName());
        });
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
