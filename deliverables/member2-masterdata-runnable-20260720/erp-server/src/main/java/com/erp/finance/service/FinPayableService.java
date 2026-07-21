package com.erp.finance.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.PageResult;
import com.erp.finance.entity.FinPayable;
import com.erp.finance.mapper.FinPayableMapper;
import com.erp.masterdata.entity.MdSupplier;
import com.erp.masterdata.mapper.MdSupplierMapper;
import com.erp.purchase.entity.PurOrder;
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
public class FinPayableService extends ServiceImpl<FinPayableMapper, FinPayable> {
    private final MdSupplierMapper supplierMapper;

    public PageResult<FinPayable> pagePayables(Long enterpriseId, Integer page, Integer size,
                                               Long supplierId, String status, String sourceNo) {
        LambdaQueryWrapper<FinPayable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinPayable::getEnterpriseId, enterpriseId)
                .eq(supplierId != null, FinPayable::getSupplierId, supplierId)
                .eq(StrUtil.isNotBlank(status), FinPayable::getStatus, status)
                .like(StrUtil.isNotBlank(sourceNo), FinPayable::getSourceNo, sourceNo)
                .orderByDesc(FinPayable::getBusinessDate)
                .orderByDesc(FinPayable::getId);
        Page<FinPayable> result = page(new Page<>(page, size), wrapper);
        enrich(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public FinPayable createFromPurchaseOrder(PurOrder order, Long operatorId) {
        FinPayable existing = lambdaQuery()
                .eq(FinPayable::getEnterpriseId, order.getEnterpriseId())
                .eq(FinPayable::getSourceType, "PURCHASE_ORDER")
                .eq(FinPayable::getSourceId, order.getId()).one();
        if (existing != null) {
            return existing;
        }
        BigDecimal amount = order.getPayableAmount() == null ? BigDecimal.ZERO : order.getPayableAmount();
        FinPayable payable = new FinPayable()
                .setEnterpriseId(order.getEnterpriseId())
                .setStoreId(order.getStoreId())
                .setPayableNo("YF" + System.currentTimeMillis() + order.getId())
                .setSupplierId(order.getSupplierId())
                .setSourceType("PURCHASE_ORDER")
                .setSourceId(order.getId())
                .setSourceNo(order.getOrderNo())
                .setBusinessDate(LocalDate.now())
                .setDueDate(LocalDate.now().plusDays(30))
                .setOriginalAmount(amount)
                .setPaidAmount(BigDecimal.ZERO)
                .setWriteOffAmount(BigDecimal.ZERO)
                .setOutstandingAmount(amount)
                .setStatus(amount.compareTo(BigDecimal.ZERO) == 0 ? "SETTLED" : "UNSETTLED")
                .setRemark("采购入库完成自动生成应付")
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);
        save(payable);
        return payable;
    }

    private void enrich(List<FinPayable> rows) {
        if (rows == null || rows.isEmpty()) return;
        List<Long> ids = rows.stream().map(FinPayable::getSupplierId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, MdSupplier> suppliers = ids.isEmpty() ? Collections.emptyMap()
                : supplierMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(MdSupplier::getId, Function.identity()));
        rows.forEach(row -> {
            MdSupplier supplier = suppliers.get(row.getSupplierId());
            row.setSupplierName(supplier == null ? null : supplier.getSupplierName());
        });
    }
}
