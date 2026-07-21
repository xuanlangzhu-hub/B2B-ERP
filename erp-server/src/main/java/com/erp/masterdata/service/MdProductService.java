package com.erp.masterdata.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.entity.MdProductCategory;
import com.erp.masterdata.entity.MdUnit;
import com.erp.masterdata.mapper.MdProductMapper;
import com.erp.masterdata.mapper.MdProductCategoryMapper;
import com.erp.masterdata.mapper.MdUnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MdProductService extends ServiceImpl<MdProductMapper, MdProduct> {

    private final MdProductCategoryMapper categoryMapper;
    private final MdUnitMapper unitMapper;
    private final MdMetadataService metadataService;

    public PageResult<MdProduct> pageQuery(Long enterpriseId, Integer page, Integer size, String productCode, String productName,
                                            String barcode, Long categoryId, String status) {
        LambdaQueryWrapper<MdProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdProduct::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(productCode), MdProduct::getProductCode, productCode)
                .like(StrUtil.isNotBlank(productName), MdProduct::getProductName, productName)
                .like(StrUtil.isNotBlank(barcode), MdProduct::getBarcode, barcode)
                .eq(categoryId != null, MdProduct::getCategoryId, categoryId)
                .eq(StrUtil.isNotBlank(status), MdProduct::getStatus, status)
                .orderByDesc(MdProduct::getCreatedAt);
        Page<MdProduct> result = page(new Page<>(page, size), wrapper);
        enrichProducts(result.getRecords());
        metadataService.enrichProducts(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public MdProduct getDetail(Long id, Long enterpriseId) {
        MdProduct product = lambdaQuery().eq(MdProduct::getId, id).eq(MdProduct::getEnterpriseId, enterpriseId).one();
        if (product == null) throw new BusinessException("商品不存在");
        enrichProducts(List.of(product));
        metadataService.enrichProducts(List.of(product));
        return product;
    }

    @Transactional
    public MdProduct create(MdProduct product, Long enterpriseId, Long operatorId) {
        validateReferences(product, enterpriseId);
        if (lambdaQuery().eq(MdProduct::getProductCode, product.getProductCode())
                .eq(MdProduct::getEnterpriseId, enterpriseId).count() > 0) {
            throw new BusinessException("商品编码已存在");
        }
        product.setEnterpriseId(enterpriseId);
        product.setCreatedBy(operatorId);
        save(product);
        metadataService.saveProductRelations(product.getId(), enterpriseId, product.getTagIds(), product.getAttributes());
        return getDetail(product.getId(), enterpriseId);
    }

    @Transactional
    public void update(MdProduct product, Long enterpriseId, Long operatorId) {
        getDetail(product.getId(), enterpriseId);
        validateReferences(product, enterpriseId);
        if (lambdaQuery().eq(MdProduct::getProductCode, product.getProductCode())
                .eq(MdProduct::getEnterpriseId, enterpriseId)
                .ne(MdProduct::getId, product.getId()).count() > 0) {
            throw new BusinessException("商品编码已存在");
        }
        product.setEnterpriseId(enterpriseId).setUpdatedBy(operatorId);
        updateById(product);
        metadataService.saveProductRelations(product.getId(), enterpriseId, product.getTagIds(), product.getAttributes());
    }

    @Transactional
    public void delete(Long id, Long enterpriseId) {
        getDetail(id, enterpriseId);
        metadataService.saveProductRelations(id, enterpriseId, List.of(), List.of());
        removeById(id);
    }

    public List<Map<String, Object>> options(Long enterpriseId) {
        List<MdProduct> list = lambdaQuery()
                .eq(MdProduct::getEnterpriseId, enterpriseId)
                .eq(MdProduct::getStatus, "ENABLED")
                .orderByAsc(MdProduct::getProductCode)
                .list();
        return list.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("value", p.getId());
            map.put("label", p.getProductCode() + " - " + p.getProductName());
            map.put("productCode", p.getProductCode());
            map.put("productName", p.getProductName());
            map.put("unitId", p.getUnitId());
            map.put("salePrice", p.getSalePrice());
            map.put("purchasePrice", p.getPurchasePrice());
            return map;
        }).collect(Collectors.toList());
    }

    private void enrichProducts(List<MdProduct> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<Long> categoryIds = products.stream().map(MdProduct::getCategoryId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> unitIds = products.stream().map(MdProduct::getUnitId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, MdProductCategory> categories = categoryIds.isEmpty() ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(MdProductCategory::getId, Function.identity()));
        Map<Long, MdUnit> units = unitIds.isEmpty() ? Collections.emptyMap()
                : unitMapper.selectBatchIds(unitIds).stream()
                .collect(Collectors.toMap(MdUnit::getId, Function.identity()));
        products.forEach(product -> {
            MdProductCategory category = categories.get(product.getCategoryId());
            MdUnit unit = units.get(product.getUnitId());
            product.setCategoryName(category != null ? category.getCategoryName() : null);
            product.setUnitName(unit != null ? unit.getUnitName() : null);
        });
    }

    private void validateReferences(MdProduct product, Long enterpriseId) {
        if (product.getCategoryId() == null || categoryMapper.selectCount(new LambdaQueryWrapper<MdProductCategory>()
                .eq(MdProductCategory::getId, product.getCategoryId()).eq(MdProductCategory::getEnterpriseId, enterpriseId)
                .eq(MdProductCategory::getStatus, "ENABLED")) == 0) throw new BusinessException("商品分类无效或已停用");
        if (product.getUnitId() == null || unitMapper.selectCount(new LambdaQueryWrapper<MdUnit>()
                .eq(MdUnit::getId, product.getUnitId()).eq(MdUnit::getEnterpriseId, enterpriseId)
                .eq(MdUnit::getStatus, "ENABLED")) == 0) throw new BusinessException("商品单位无效或已停用");
    }
}
