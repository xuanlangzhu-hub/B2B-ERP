package com.erp.masterdata.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.masterdata.entity.MdSupplier;
import com.erp.masterdata.entity.MdSupplierCategory;
import com.erp.masterdata.mapper.MdSupplierMapper;
import com.erp.masterdata.mapper.MdSupplierCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Objects;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class MdSupplierService extends ServiceImpl<MdSupplierMapper, MdSupplier> {
    private final MdSupplierCategoryMapper categoryMapper;

    public PageResult<MdSupplier> pageQuery(Long enterpriseId, Integer page, Integer size, String supplierCode, String supplierName,
                                             String contactPhone, Long categoryId, String status) {
        LambdaQueryWrapper<MdSupplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdSupplier::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(supplierCode), MdSupplier::getSupplierCode, supplierCode)
                .like(StrUtil.isNotBlank(supplierName), MdSupplier::getSupplierName, supplierName)
                .like(StrUtil.isNotBlank(contactPhone), MdSupplier::getContactPhone, contactPhone)
                .eq(categoryId != null, MdSupplier::getCategoryId, categoryId)
                .eq(StrUtil.isNotBlank(status), MdSupplier::getStatus, status)
                .orderByDesc(MdSupplier::getCreatedAt);
        Page<MdSupplier> result = page(new Page<>(page, size), wrapper);
        enrich(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public MdSupplier getDetail(Long id, Long enterpriseId) {
        MdSupplier supplier = lambdaQuery().eq(MdSupplier::getId, id).eq(MdSupplier::getEnterpriseId, enterpriseId).one();
        if (supplier == null) throw new BusinessException("供应商不存在");
        enrich(List.of(supplier));
        return supplier;
    }

    @Transactional
    public MdSupplier create(MdSupplier supplier, Long enterpriseId, Long operatorId) {
        validateCategory(supplier.getCategoryId(), enterpriseId);
        if (lambdaQuery().eq(MdSupplier::getSupplierCode, supplier.getSupplierCode())
                .eq(MdSupplier::getEnterpriseId, enterpriseId).count() > 0) {
            throw new BusinessException("供应商编码已存在");
        }
        supplier.setEnterpriseId(enterpriseId);
        supplier.setCreatedBy(operatorId);
        save(supplier);
        return supplier;
    }

    @Transactional
    public void update(MdSupplier supplier, Long enterpriseId, Long operatorId) {
        getDetail(supplier.getId(), enterpriseId);
        validateCategory(supplier.getCategoryId(), enterpriseId);
        if (lambdaQuery().eq(MdSupplier::getSupplierCode, supplier.getSupplierCode())
                .eq(MdSupplier::getEnterpriseId, enterpriseId)
                .ne(MdSupplier::getId, supplier.getId()).count() > 0) {
            throw new BusinessException("供应商编码已存在");
        }
        supplier.setEnterpriseId(enterpriseId).setUpdatedBy(operatorId);
        updateById(supplier);
    }

    @Transactional
    public void delete(Long id, Long enterpriseId) {
        getDetail(id, enterpriseId);
        removeById(id);
    }

    public List<Map<String, Object>> options(Long enterpriseId) {
        List<MdSupplier> list = lambdaQuery()
                .eq(MdSupplier::getEnterpriseId, enterpriseId)
                .eq(MdSupplier::getStatus, "ENABLED")
                .orderByAsc(MdSupplier::getSupplierCode)
                .list();
        return list.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("value", s.getId());
            map.put("label", s.getSupplierCode() + " - " + s.getSupplierName());
            return map;
        }).collect(Collectors.toList());
    }

    private void validateCategory(Long categoryId, Long enterpriseId) {
        if (categoryId != null && categoryMapper.selectCount(new LambdaQueryWrapper<MdSupplierCategory>()
                .eq(MdSupplierCategory::getId, categoryId).eq(MdSupplierCategory::getEnterpriseId, enterpriseId)
                .eq(MdSupplierCategory::getStatus, "ENABLED")) == 0) throw new BusinessException("供应商分类无效或已停用");
    }

    private void enrich(List<MdSupplier> rows) {
        List<Long> ids = rows.stream().map(MdSupplier::getCategoryId).filter(Objects::nonNull).distinct().toList();
        Map<Long, MdSupplierCategory> categories = ids.isEmpty() ? Collections.emptyMap() : categoryMapper.selectBatchIds(ids)
                .stream().collect(Collectors.toMap(MdSupplierCategory::getId, Function.identity()));
        rows.forEach(row -> {
            MdSupplierCategory category = categories.get(row.getCategoryId());
            row.setCategoryName(category == null ? null : category.getCategoryName());
        });
    }
}
