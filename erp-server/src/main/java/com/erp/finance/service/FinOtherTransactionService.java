package com.erp.finance.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.finance.dto.FinOtherTransactionRequest;
import com.erp.finance.entity.FinAccount;
import com.erp.finance.entity.FinCapitalFlow;
import com.erp.finance.entity.FinOtherTransaction;
import com.erp.finance.mapper.FinAccountMapper;
import com.erp.finance.mapper.FinCapitalFlowMapper;
import com.erp.finance.mapper.FinOtherTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinOtherTransactionService extends ServiceImpl<FinOtherTransactionMapper, FinOtherTransaction> {
    private final FinAccountMapper accountMapper;
    private final FinCapitalFlowMapper flowMapper;

    public PageResult<FinOtherTransaction> pageTransactions(Long enterpriseId, Integer page, Integer size,
                                                              String transactionType, String category,
                                                              String status, String startDate, String endDate) {
        Page<FinOtherTransaction> result = page(new Page<>(page, size), new LambdaQueryWrapper<FinOtherTransaction>()
                .eq(FinOtherTransaction::getEnterpriseId, enterpriseId)
                .eq(StrUtil.isNotBlank(transactionType), FinOtherTransaction::getTransactionType, transactionType)
                .like(StrUtil.isNotBlank(category), FinOtherTransaction::getCategory, category)
                .eq(StrUtil.isNotBlank(status), FinOtherTransaction::getStatus, status)
                .ge(StrUtil.isNotBlank(startDate), FinOtherTransaction::getTransactionDate, startDate)
                .le(StrUtil.isNotBlank(endDate), FinOtherTransaction::getTransactionDate, endDate)
                .orderByDesc(FinOtherTransaction::getTransactionDate).orderByDesc(FinOtherTransaction::getId));
        enrich(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public FinOtherTransaction create(FinOtherTransactionRequest request, Long enterpriseId, Long operatorId) {
        validateRequest(request, enterpriseId);
        FinOtherTransaction row = new FinOtherTransaction().setEnterpriseId(enterpriseId).setStoreId(request.getStoreId())
                .setTransactionNo("QTSZ" + System.currentTimeMillis()).setTransactionDate(request.getTransactionDate())
                .setTransactionType(request.getTransactionType()).setCategory(request.getCategory().trim())
                .setAccountId(request.getAccountId()).setAmount(request.getAmount()).setCounterparty(request.getCounterparty())
                .setStatus("DRAFT").setRemark(request.getRemark()).setCreatedBy(operatorId).setUpdatedBy(operatorId);
        save(row); enrich(List.of(row)); return row;
    }

    @Transactional
    public void update(Long id, FinOtherTransactionRequest request, Long enterpriseId, Long operatorId) {
        FinOtherTransaction row = require(id, enterpriseId);
        if (!"DRAFT".equals(row.getStatus())) throw new BusinessException("只有草稿状态可以修改");
        validateRequest(request, enterpriseId);
        row.setStoreId(request.getStoreId()).setTransactionDate(request.getTransactionDate())
                .setTransactionType(request.getTransactionType()).setCategory(request.getCategory().trim())
                .setAccountId(request.getAccountId()).setAmount(request.getAmount()).setCounterparty(request.getCounterparty())
                .setRemark(request.getRemark()).setUpdatedBy(operatorId);
        updateById(row);
    }

    @Transactional
    public void confirm(Long id, Long enterpriseId, Long operatorId) {
        FinOtherTransaction row = baseMapper.selectForUpdate(id, enterpriseId);
        if (row == null) throw new BusinessException("其他收支单不存在");
        if (!"DRAFT".equals(row.getStatus())) throw new BusinessException("只有草稿状态可以确认");
        FinAccount account = accountMapper.selectForUpdate(row.getAccountId(), enterpriseId);
        if (account == null || !"ENABLED".equals(account.getStatus())) throw new BusinessException("资金账户不存在或已停用");
        BigDecimal before = zero(account.getCurrentBalance());
        boolean income = "INCOME".equals(row.getTransactionType());
        BigDecimal after = income ? before.add(row.getAmount()) : before.subtract(row.getAmount());
        account.setCurrentBalance(after).setUpdatedBy(operatorId); accountMapper.updateById(account);
        insertFlow(row, operatorId, income ? "OTHER_INCOME" : "OTHER_EXPENSE", income ? "IN" : "OUT", before, after);
        row.setStatus("CONFIRMED").setConfirmedBy(operatorId).setConfirmedAt(LocalDateTime.now()).setUpdatedBy(operatorId);
        updateById(row);
    }

    @Transactional
    public void cancel(Long id, Long enterpriseId, Long operatorId) {
        FinOtherTransaction row = baseMapper.selectForUpdate(id, enterpriseId);
        if (row == null) throw new BusinessException("其他收支单不存在");
        if ("CANCELLED".equals(row.getStatus())) throw new BusinessException("单据已取消");
        if ("DRAFT".equals(row.getStatus())) { row.setStatus("CANCELLED").setUpdatedBy(operatorId); updateById(row); return; }
        if (!"CONFIRMED".equals(row.getStatus())) throw new BusinessException("当前状态不能取消");
        FinAccount account = accountMapper.selectForUpdate(row.getAccountId(), enterpriseId);
        if (account == null) throw new BusinessException("资金账户不存在");
        BigDecimal before = zero(account.getCurrentBalance());
        boolean income = "INCOME".equals(row.getTransactionType());
        BigDecimal after = income ? before.subtract(row.getAmount()) : before.add(row.getAmount());
        account.setCurrentBalance(after).setUpdatedBy(operatorId); accountMapper.updateById(account);
        insertFlow(row, operatorId, income ? "OTHER_INCOME_REVERSAL" : "OTHER_EXPENSE_REVERSAL",
                income ? "OUT" : "IN", before, after);
        row.setStatus("CANCELLED").setUpdatedBy(operatorId); updateById(row);
    }

    @Transactional
    public void delete(Long id, Long enterpriseId) {
        FinOtherTransaction row = require(id, enterpriseId);
        if (!"DRAFT".equals(row.getStatus()) && !"CANCELLED".equals(row.getStatus())) {
            throw new BusinessException("只有草稿或已取消单据可以删除");
        }
        removeById(id);
    }

    private void insertFlow(FinOtherTransaction row, Long operatorId, String type, String direction,
                            BigDecimal before, BigDecimal after) {
        if (flowMapper.selectCount(new LambdaQueryWrapper<FinCapitalFlow>()
                .eq(FinCapitalFlow::getEnterpriseId, row.getEnterpriseId()).eq(FinCapitalFlow::getSourceType, "OTHER")
                .eq(FinCapitalFlow::getSourceId, row.getId()).eq(FinCapitalFlow::getFlowType, type)) > 0) {
            throw new BusinessException("该单据已生成对应资金流水");
        }
        flowMapper.insert(new FinCapitalFlow().setEnterpriseId(row.getEnterpriseId()).setStoreId(row.getStoreId())
                .setAccountId(row.getAccountId()).setFlowNo("ZJQT" + System.currentTimeMillis() + row.getId())
                .setFlowDate(row.getTransactionDate()).setFlowType(type).setDirection(direction).setAmount(row.getAmount())
                .setBeforeBalance(before).setAfterBalance(after).setSourceType("OTHER").setSourceId(row.getId())
                .setSourceNo(row.getTransactionNo()).setCounterpartyType("OTHER").setCounterpartyName(row.getCounterparty())
                .setOperatorId(operatorId).setRemark(row.getCategory()));
    }

    private void validateRequest(FinOtherTransactionRequest request, Long enterpriseId) {
        if (!Set.of("INCOME", "EXPENSE").contains(request.getTransactionType())) throw new BusinessException("收支类型不正确");
        FinAccount account = accountMapper.selectById(request.getAccountId());
        if (account == null || !Objects.equals(account.getEnterpriseId(), enterpriseId) || !"ENABLED".equals(account.getStatus())) {
            throw new BusinessException("资金账户不存在或已停用");
        }
    }

    private FinOtherTransaction require(Long id, Long enterpriseId) {
        FinOtherTransaction row = lambdaQuery().eq(FinOtherTransaction::getEnterpriseId, enterpriseId)
                .eq(FinOtherTransaction::getId, id).one();
        if (row == null) throw new BusinessException("其他收支单不存在"); return row;
    }

    private void enrich(List<FinOtherTransaction> rows) {
        List<Long> ids = rows.stream().map(FinOtherTransaction::getAccountId).distinct().toList();
        Map<Long, FinAccount> accounts = ids.isEmpty() ? Map.of() : accountMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(FinAccount::getId, Function.identity()));
        rows.forEach(row -> row.setAccountName(Optional.ofNullable(accounts.get(row.getAccountId())).map(FinAccount::getAccountName).orElse(null)));
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
