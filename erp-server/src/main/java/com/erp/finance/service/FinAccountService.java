package com.erp.finance.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.finance.dto.FinAccountRequest;
import com.erp.finance.entity.FinAccount;
import com.erp.finance.entity.FinCapitalFlow;
import com.erp.finance.mapper.FinAccountMapper;
import com.erp.finance.mapper.FinCapitalFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinAccountService extends ServiceImpl<FinAccountMapper, FinAccount> {
    private final FinCapitalFlowMapper capitalFlowMapper;

    public PageResult<FinAccount> pageAccounts(Long enterpriseId, Integer page, Integer size,
                                                String keyword, String status) {
        LambdaQueryWrapper<FinAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinAccount::getEnterpriseId, enterpriseId)
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(FinAccount::getAccountCode, keyword)
                        .or().like(FinAccount::getAccountName, keyword))
                .eq(StrUtil.isNotBlank(status), FinAccount::getStatus, status)
                .orderByAsc(FinAccount::getAccountCode);
        Page<FinAccount> result = page(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public List<Map<String, Object>> options(Long enterpriseId) {
        return lambdaQuery().eq(FinAccount::getEnterpriseId, enterpriseId)
                .eq(FinAccount::getStatus, "ENABLED")
                .orderByAsc(FinAccount::getAccountCode)
                .list().stream()
                .map(a -> Map.<String, Object>of(
                        "value", a.getId(),
                        "label", a.getAccountName(),
                        "accountType", a.getAccountType(),
                        "balance", a.getCurrentBalance()))
                .toList();
    }

    @Transactional
    public FinAccount create(FinAccountRequest request, Long enterpriseId, Long operatorId) {
        if (lambdaQuery().eq(FinAccount::getEnterpriseId, enterpriseId)
                .eq(FinAccount::getAccountCode, request.getAccountCode()).count() > 0) {
            throw new BusinessException("账户编码已存在");
        }
        BigDecimal openingBalance = request.getOpeningBalance();
        FinAccount account = new FinAccount()
                .setEnterpriseId(enterpriseId)
                .setAccountCode(request.getAccountCode())
                .setAccountName(request.getAccountName())
                .setAccountType(request.getAccountType())
                .setBankName(request.getBankName())
                .setAccountNumber(request.getAccountNumber())
                .setOpeningBalance(openingBalance)
                .setCurrentBalance(openingBalance)
                .setStatus(StrUtil.isBlank(request.getStatus()) ? "ENABLED" : request.getStatus())
                .setRemark(request.getRemark())
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);
        save(account);
        return account;
    }

    @Transactional
    public void update(Long id, FinAccountRequest request, Long enterpriseId, Long operatorId) {
        FinAccount account = lambdaQuery().eq(FinAccount::getEnterpriseId, enterpriseId)
                .eq(FinAccount::getId, id).one();
        if (account == null) {
            throw new BusinessException("账户不存在");
        }
        if (lambdaQuery().eq(FinAccount::getEnterpriseId, enterpriseId)
                .eq(FinAccount::getAccountCode, request.getAccountCode())
                .ne(FinAccount::getId, id).count() > 0) {
            throw new BusinessException("账户编码已存在");
        }
        account.setAccountCode(request.getAccountCode())
                .setAccountName(request.getAccountName())
                .setAccountType(request.getAccountType())
                .setBankName(request.getBankName())
                .setAccountNumber(request.getAccountNumber())
                .setStatus(StrUtil.isBlank(request.getStatus()) ? "ENABLED" : request.getStatus())
                .setRemark(request.getRemark())
                .setUpdatedBy(operatorId);
        updateById(account);
    }

    @Transactional
    public void delete(Long id, Long enterpriseId) {
        FinAccount account = lambdaQuery().eq(FinAccount::getEnterpriseId, enterpriseId)
                .eq(FinAccount::getId, id).one();
        if (account == null) {
            throw new BusinessException("账户不存在");
        }
        Long flowCount = capitalFlowMapper.selectCount(new LambdaQueryWrapper<FinCapitalFlow>()
                .eq(FinCapitalFlow::getEnterpriseId, enterpriseId)
                .eq(FinCapitalFlow::getAccountId, id));
        if (flowCount > 0) {
            throw new BusinessException("账户已有资金流水，不能删除，可改为停用");
        }
        removeById(id);
    }
}
