package com.erp.finance.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.PageResult;
import com.erp.finance.entity.FinReceivable;
import com.erp.finance.mapper.FinReceivableMapper;
import com.erp.masterdata.entity.MdCustomer;
import com.erp.masterdata.mapper.MdCustomerMapper;
import com.erp.sales.entity.SalOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinReceivableService extends ServiceImpl<FinReceivableMapper, FinReceivable> {
    private final MdCustomerMapper customerMapper;

    public PageResult<FinReceivable> pageReceivables(Long enterpriseId, Integer page, Integer size,
                                                      Long customerId, String status, String sourceNo) {
        LambdaQueryWrapper<FinReceivable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinReceivable::getEnterpriseId, enterpriseId)
                .eq(customerId != null, FinReceivable::getCustomerId, customerId)
                .eq(StrUtil.isNotBlank(status), FinReceivable::getStatus, status)
                .like(StrUtil.isNotBlank(sourceNo), FinReceivable::getSourceNo, sourceNo)
                .orderByDesc(FinReceivable::getBusinessDate)
                .orderByDesc(FinReceivable::getId);
        Page<FinReceivable> result = page(new Page<>(page, size), wrapper);
        enrich(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public FinReceivable createFromSalesOrder(SalOrder order, Long operatorId) {
        FinReceivable existing = lambdaQuery()
                .eq(FinReceivable::getEnterpriseId, order.getEnterpriseId())
                .eq(FinReceivable::getSourceType, "SALES_ORDER")
                .eq(FinReceivable::getSourceId, order.getId()).one();
        if (existing != null) {
            return existing;
        }
        BigDecimal amount = order.getPayableAmount() == null ? BigDecimal.ZERO : order.getPayableAmount();
        FinReceivable receivable = new FinReceivable()
                .setEnterpriseId(order.getEnterpriseId())
                .setStoreId(order.getStoreId())
                .setReceivableNo("YS" + System.currentTimeMillis() + order.getId())
                .setCustomerId(order.getCustomerId())
                .setSourceType("SALES_ORDER")
                .setSourceId(order.getId())
                .setSourceNo(order.getOrderNo())
                .setBusinessDate(LocalDate.now())
                .setDueDate(LocalDate.now().plusDays(30))
                .setOriginalAmount(amount)
                .setReceivedAmount(BigDecimal.ZERO)
                .setWriteOffAmount(BigDecimal.ZERO)
                .setOutstandingAmount(amount)
                .setStatus(amount.compareTo(BigDecimal.ZERO) == 0 ? "SETTLED" : "UNSETTLED")
                .setRemark("销售出库完成自动生成应收")
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);
        save(receivable);
        return receivable;
    }

    private void enrich(List<FinReceivable> rows) {
        if (rows == null || rows.isEmpty()) return;
        List<Long> ids = rows.stream().map(FinReceivable::getCustomerId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, MdCustomer> customers = ids.isEmpty() ? Collections.emptyMap()
                : customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(MdCustomer::getId, Function.identity()));
        rows.forEach(row -> {
            MdCustomer customer = customers.get(row.getCustomerId());
            row.setCustomerName(customer == null ? null : customer.getCustomerName());
        });
    }
}
