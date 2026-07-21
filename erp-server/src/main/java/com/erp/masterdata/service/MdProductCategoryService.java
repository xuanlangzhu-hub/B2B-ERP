package com.erp.masterdata.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.masterdata.entity.MdProductCategory;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.mapper.MdProductCategoryMapper;
import com.erp.masterdata.mapper.MdProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MdProductCategoryService extends ServiceImpl<MdProductCategoryMapper, MdProductCategory> {
    private final MdProductMapper productMapper;

    public PageResult<MdProductCategory> pageQuery(Long enterpriseId, Integer page, Integer size, String categoryCode,
                                                    String categoryName, String status) {
        LambdaQueryWrapper<MdProductCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdProductCategory::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(categoryCode), MdProductCategory::getCategoryCode, categoryCode)
                .like(StrUtil.isNotBlank(categoryName), MdProductCategory::getCategoryName, categoryName)
                .eq(StrUtil.isNotBlank(status), MdProductCategory::getStatus, status)
                .orderByAsc(MdProductCategory::getSortNo)
                .orderByDesc(MdProductCategory::getCreatedAt);
        Page<MdProductCategory> result = page(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public MdProductCategory getDetail(Long id, Long enterpriseId) {
        MdProductCategory category = lambdaQuery().eq(MdProductCategory::getId, id)
                .eq(MdProductCategory::getEnterpriseId, enterpriseId).one();
        if (category == null) throw new BusinessException("分类不存在");
        return category;
    }

    @Transactional
    public MdProductCategory create(MdProductCategory category, Long enterpriseId, Long operatorId) {
        if (lambdaQuery().eq(MdProductCategory::getCategoryCode, category.getCategoryCode())
                .eq(MdProductCategory::getEnterpriseId, enterpriseId).count() > 0) {
            throw new BusinessException("分类编码已存在");
        }
        category.setEnterpriseId(enterpriseId);
        category.setCreatedBy(operatorId);
        category.setParentId(category.getParentId() == null ? 0L : category.getParentId());
        category.setSortNo(category.getSortNo() == null ? 0 : category.getSortNo());
        category.setStatus(StrUtil.blankToDefault(category.getStatus(), "ENABLED"));
        save(category);
        return category;
    }

    @Transactional
    public void update(MdProductCategory category, Long enterpriseId, Long operatorId) {
        MdProductCategory existing = getDetail(category.getId(), enterpriseId);
        if (lambdaQuery().eq(MdProductCategory::getCategoryCode, category.getCategoryCode())
                .eq(MdProductCategory::getEnterpriseId, existing.getEnterpriseId())
                .ne(MdProductCategory::getId, category.getId()).count() > 0) {
            throw new BusinessException("分类编码已存在");
        }
        category.setEnterpriseId(enterpriseId).setUpdatedBy(operatorId);
        updateById(category);
    }

    @Transactional
    public void delete(Long id, Long enterpriseId) {
        getDetail(id, enterpriseId);
        if (lambdaQuery().eq(MdProductCategory::getEnterpriseId, enterpriseId)
                .eq(MdProductCategory::getParentId, id).count() > 0) {
            throw new BusinessException("存在子分类，无法删除");
        }
        if (productMapper.selectCount(new LambdaQueryWrapper<MdProduct>()
                .eq(MdProduct::getEnterpriseId, enterpriseId).eq(MdProduct::getCategoryId, id)) > 0) {
            throw new BusinessException("分类已被商品使用，无法删除");
        }
        removeById(id);
    }

    public List<Map<String, Object>> options(Long enterpriseId) {
        List<MdProductCategory> list = lambdaQuery()
                .eq(MdProductCategory::getEnterpriseId, enterpriseId)
                .eq(MdProductCategory::getStatus, "ENABLED")
                .orderByAsc(MdProductCategory::getSortNo)
                .list();
        return list.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("value", c.getId());
            map.put("label", c.getCategoryName());
            map.put("parentId", c.getParentId());
            return map;
        }).collect(Collectors.toList());
    }
}
