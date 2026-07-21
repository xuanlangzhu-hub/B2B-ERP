package com.erp.masterdata.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.masterdata.entity.MdCustomer;
import com.erp.masterdata.entity.MdCustomerCategory;
import com.erp.masterdata.entity.MdCustomerLevel;
import com.erp.masterdata.mapper.MdCustomerMapper;
import com.erp.masterdata.mapper.MdCustomerCategoryMapper;
import com.erp.masterdata.mapper.MdCustomerLevelMapper;
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
public class MdCustomerService extends ServiceImpl<MdCustomerMapper, MdCustomer> {
    private final MdCustomerCategoryMapper categoryMapper;
    private final MdCustomerLevelMapper levelMapper;
    private final MdMetadataService metadataService;

    public PageResult<MdCustomer> pageQuery(Long enterpriseId, Integer page, Integer size, String customerCode, String customerName,
                                             String contactPhone, Long categoryId, Long levelId, String status) {
        LambdaQueryWrapper<MdCustomer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdCustomer::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(customerCode), MdCustomer::getCustomerCode, customerCode)
                .like(StrUtil.isNotBlank(customerName), MdCustomer::getCustomerName, customerName)
                .like(StrUtil.isNotBlank(contactPhone), MdCustomer::getContactPhone, contactPhone)
                .eq(categoryId != null, MdCustomer::getCategoryId, categoryId)
                .eq(levelId != null, MdCustomer::getLevelId, levelId)
                .eq(StrUtil.isNotBlank(status), MdCustomer::getStatus, status)
                .orderByDesc(MdCustomer::getCreatedAt);
        Page<MdCustomer> result = page(new Page<>(page, size), wrapper);
        enrich(result.getRecords());
        metadataService.enrichCustomers(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public MdCustomer getDetail(Long id, Long enterpriseId) {
        MdCustomer customer = lambdaQuery().eq(MdCustomer::getId, id).eq(MdCustomer::getEnterpriseId, enterpriseId).one();
        if (customer == null) throw new BusinessException("客户不存在");
        enrich(List.of(customer));
        metadataService.enrichCustomers(List.of(customer));
        return customer;
    }

    @Transactional
    public MdCustomer create(MdCustomer customer, Long enterpriseId, Long operatorId) {
        validateReferences(customer, enterpriseId);
        if (lambdaQuery().eq(MdCustomer::getCustomerCode, customer.getCustomerCode())
                .eq(MdCustomer::getEnterpriseId, enterpriseId).count() > 0) {
            throw new BusinessException("客户编码已存在");
        }
        customer.setEnterpriseId(enterpriseId);
        customer.setCreatedBy(operatorId);
        save(customer);
        metadataService.saveCustomerTags(customer.getId(), enterpriseId, customer.getTagIds());
        return getDetail(customer.getId(), enterpriseId);
    }

    @Transactional
    public void update(MdCustomer customer, Long enterpriseId, Long operatorId) {
        getDetail(customer.getId(), enterpriseId);
        validateReferences(customer, enterpriseId);
        if (lambdaQuery().eq(MdCustomer::getCustomerCode, customer.getCustomerCode())
                .eq(MdCustomer::getEnterpriseId, enterpriseId)
                .ne(MdCustomer::getId, customer.getId()).count() > 0) {
            throw new BusinessException("客户编码已存在");
        }
        customer.setEnterpriseId(enterpriseId).setUpdatedBy(operatorId);
        updateById(customer);
        metadataService.saveCustomerTags(customer.getId(), enterpriseId, customer.getTagIds());
    }

    @Transactional
    public void delete(Long id, Long enterpriseId) {
        getDetail(id, enterpriseId);
        metadataService.saveCustomerTags(id, enterpriseId, List.of());
        removeById(id);
    }

    public List<Map<String, Object>> options(Long enterpriseId) {
        List<MdCustomer> list = lambdaQuery()
                .eq(MdCustomer::getEnterpriseId, enterpriseId)
                .eq(MdCustomer::getStatus, "ENABLED")
                .orderByAsc(MdCustomer::getCustomerCode)
                .list();
        return list.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("value", c.getId());
            map.put("label", c.getCustomerCode() + " - " + c.getCustomerName());
            return map;
        }).collect(Collectors.toList());
    }

    private void validateReferences(MdCustomer customer, Long enterpriseId) {
        if (customer.getCategoryId() != null && categoryMapper.selectCount(new LambdaQueryWrapper<MdCustomerCategory>()
                .eq(MdCustomerCategory::getId, customer.getCategoryId()).eq(MdCustomerCategory::getEnterpriseId, enterpriseId)
                .eq(MdCustomerCategory::getStatus, "ENABLED")) == 0) throw new BusinessException("客户分类无效或已停用");
        if (customer.getLevelId() != null && levelMapper.selectCount(new LambdaQueryWrapper<MdCustomerLevel>()
                .eq(MdCustomerLevel::getId, customer.getLevelId()).eq(MdCustomerLevel::getEnterpriseId, enterpriseId)
                .eq(MdCustomerLevel::getStatus, "ENABLED")) == 0) throw new BusinessException("客户等级无效或已停用");
    }

    private void enrich(List<MdCustomer> rows) {
        List<Long> categoryIds = rows.stream().map(MdCustomer::getCategoryId).filter(Objects::nonNull).distinct().toList();
        List<Long> levelIds = rows.stream().map(MdCustomer::getLevelId).filter(Objects::nonNull).distinct().toList();
        Map<Long, MdCustomerCategory> categories = categoryIds.isEmpty() ? Collections.emptyMap() : categoryMapper.selectBatchIds(categoryIds)
                .stream().collect(Collectors.toMap(MdCustomerCategory::getId, Function.identity()));
        Map<Long, MdCustomerLevel> levels = levelIds.isEmpty() ? Collections.emptyMap() : levelMapper.selectBatchIds(levelIds)
                .stream().collect(Collectors.toMap(MdCustomerLevel::getId, Function.identity()));
        rows.forEach(row -> {
            MdCustomerCategory category = categories.get(row.getCategoryId());
            MdCustomerLevel level = levels.get(row.getLevelId());
            row.setCategoryName(category == null ? null : category.getCategoryName());
            row.setLevelName(level == null ? null : level.getLevelName());
        });
    }
}
