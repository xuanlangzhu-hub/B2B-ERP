package com.erp.masterdata.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.masterdata.entity.MdSupplier;
import com.erp.masterdata.mapper.MdSupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MdSupplierService extends ServiceImpl<MdSupplierMapper, MdSupplier> {

    public PageResult<MdSupplier> pageQuery(Integer page, Integer size, String supplierCode, String supplierName,
                                             String contactPhone, String status) {
        LambdaQueryWrapper<MdSupplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(supplierCode), MdSupplier::getSupplierCode, supplierCode)
                .like(StrUtil.isNotBlank(supplierName), MdSupplier::getSupplierName, supplierName)
                .like(StrUtil.isNotBlank(contactPhone), MdSupplier::getContactPhone, contactPhone)
                .eq(StrUtil.isNotBlank(status), MdSupplier::getStatus, status)
                .orderByDesc(MdSupplier::getCreatedAt);
        Page<MdSupplier> result = page(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public MdSupplier create(MdSupplier supplier, Long enterpriseId, Long operatorId) {
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
    public void update(MdSupplier supplier) {
        MdSupplier existing = getById(supplier.getId());
        if (existing == null) {
            throw new BusinessException("供应商不存在");
        }
        if (lambdaQuery().eq(MdSupplier::getSupplierCode, supplier.getSupplierCode())
                .eq(MdSupplier::getEnterpriseId, existing.getEnterpriseId())
                .ne(MdSupplier::getId, supplier.getId()).count() > 0) {
            throw new BusinessException("供应商编码已存在");
        }
        updateById(supplier);
    }

    @Transactional
    public void delete(Long id) {
        MdSupplier supplier = getById(id);
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }
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
}
