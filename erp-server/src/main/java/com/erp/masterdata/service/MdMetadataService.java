package com.erp.masterdata.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.masterdata.dto.ProductAttributeSelection;
import com.erp.masterdata.entity.*;
import com.erp.masterdata.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MdMetadataService {
    private final MdProductAttributeMapper attributeMapper;
    private final MdProductAttributeValueMapper attributeValueMapper;
    private final MdProductAttributeRelationMapper attributeRelationMapper;
    private final MdProductTagMapper productTagMapper;
    private final MdProductTagRelationMapper productTagRelationMapper;
    private final MdCustomerTagMapper customerTagMapper;
    private final MdCustomerTagRelationMapper customerTagRelationMapper;

    public PageResult<MdProductAttribute> pageAttributes(Long enterpriseId, Integer page, Integer size,
                                                          String code, String name, String status) {
        Page<MdProductAttribute> result = attributeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MdProductAttribute>().eq(MdProductAttribute::getEnterpriseId, enterpriseId)
                        .like(StrUtil.isNotBlank(code), MdProductAttribute::getAttributeCode, code)
                        .like(StrUtil.isNotBlank(name), MdProductAttribute::getAttributeName, name)
                        .eq(StrUtil.isNotBlank(status), MdProductAttribute::getStatus, status)
                        .orderByAsc(MdProductAttribute::getSortNo).orderByDesc(MdProductAttribute::getCreatedAt));
        enrichAttributeValues(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public MdProductAttribute getAttribute(Long id, Long enterpriseId) {
        MdProductAttribute attribute = attributeMapper.selectOne(new LambdaQueryWrapper<MdProductAttribute>()
                .eq(MdProductAttribute::getId, id).eq(MdProductAttribute::getEnterpriseId, enterpriseId));
        if (attribute == null) throw new BusinessException("商品属性不存在");
        enrichAttributeValues(List.of(attribute));
        return attribute;
    }

    @Transactional
    public MdProductAttribute saveAttribute(Long id, MdProductAttribute submitted, Long enterpriseId) {
        if (StrUtil.isBlank(submitted.getAttributeCode()) || StrUtil.isBlank(submitted.getAttributeName())) {
            throw new BusinessException("属性编码和名称不能为空");
        }
        if (!Set.of("SELECT", "TEXT", "NUMBER").contains(submitted.getInputType())) {
            throw new BusinessException("属性输入类型不正确");
        }
        if (attributeMapper.selectCount(new LambdaQueryWrapper<MdProductAttribute>()
                .eq(MdProductAttribute::getEnterpriseId, enterpriseId)
                .eq(MdProductAttribute::getAttributeCode, submitted.getAttributeCode().trim())
                .ne(id != null, MdProductAttribute::getId, id)) > 0) throw new BusinessException("属性编码已存在");
        if (id != null) getAttribute(id, enterpriseId);
        submitted.setId(id).setEnterpriseId(enterpriseId)
                .setAttributeCode(submitted.getAttributeCode().trim())
                .setAttributeName(submitted.getAttributeName().trim())
                .setSortNo(submitted.getSortNo() == null ? 0 : submitted.getSortNo())
                .setStatus(StrUtil.blankToDefault(submitted.getStatus(), "ENABLED"));
        if (id == null) attributeMapper.insert(submitted); else attributeMapper.updateById(submitted);
        syncAttributeValues(submitted);
        return getAttribute(submitted.getId(), enterpriseId);
    }

    @Transactional
    public void deleteAttribute(Long id, Long enterpriseId) {
        getAttribute(id, enterpriseId);
        if (attributeRelationMapper.selectCount(new LambdaQueryWrapper<MdProductAttributeRelation>()
                .eq(MdProductAttributeRelation::getAttributeId, id)) > 0) {
            throw new BusinessException("属性已被商品使用，不能删除");
        }
        attributeValueMapper.delete(new LambdaQueryWrapper<MdProductAttributeValue>()
                .eq(MdProductAttributeValue::getAttributeId, id));
        attributeMapper.deleteById(id);
    }

    public PageResult<MdProductTag> pageProductTags(Long enterpriseId, Integer page, Integer size, String name) {
        Page<MdProductTag> result = productTagMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MdProductTag>().eq(MdProductTag::getEnterpriseId, enterpriseId)
                        .like(StrUtil.isNotBlank(name), MdProductTag::getTagName, name)
                        .orderByDesc(MdProductTag::getCreatedAt));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public MdProductTag saveProductTag(Long id, MdProductTag tag, Long enterpriseId) {
        requireTagName(tag.getTagName());
        if (id != null) requireProductTag(id, enterpriseId);
        if (productTagMapper.selectCount(new LambdaQueryWrapper<MdProductTag>()
                .eq(MdProductTag::getEnterpriseId, enterpriseId).eq(MdProductTag::getTagName, tag.getTagName().trim())
                .ne(id != null, MdProductTag::getId, id)) > 0) throw new BusinessException("商品标签名称已存在");
        tag.setId(id).setEnterpriseId(enterpriseId).setTagName(tag.getTagName().trim())
                .setTagColor(StrUtil.blankToDefault(tag.getTagColor(), "#409EFF"));
        if (id == null) productTagMapper.insert(tag); else productTagMapper.updateById(tag);
        return tag;
    }

    @Transactional
    public void deleteProductTag(Long id, Long enterpriseId) {
        requireProductTag(id, enterpriseId);
        if (productTagRelationMapper.selectCount(new LambdaQueryWrapper<MdProductTagRelation>()
                .eq(MdProductTagRelation::getTagId, id)) > 0) throw new BusinessException("商品标签已被使用，不能删除");
        productTagMapper.deleteById(id);
    }

    public PageResult<MdCustomerTag> pageCustomerTags(Long enterpriseId, Integer page, Integer size, String name) {
        Page<MdCustomerTag> result = customerTagMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MdCustomerTag>().eq(MdCustomerTag::getEnterpriseId, enterpriseId)
                        .like(StrUtil.isNotBlank(name), MdCustomerTag::getTagName, name)
                        .orderByDesc(MdCustomerTag::getCreatedAt));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public MdCustomerTag saveCustomerTag(Long id, MdCustomerTag tag, Long enterpriseId) {
        requireTagName(tag.getTagName());
        if (id != null) requireCustomerTag(id, enterpriseId);
        if (customerTagMapper.selectCount(new LambdaQueryWrapper<MdCustomerTag>()
                .eq(MdCustomerTag::getEnterpriseId, enterpriseId).eq(MdCustomerTag::getTagName, tag.getTagName().trim())
                .ne(id != null, MdCustomerTag::getId, id)) > 0) throw new BusinessException("客户标签名称已存在");
        tag.setId(id).setEnterpriseId(enterpriseId).setTagName(tag.getTagName().trim())
                .setTagColor(StrUtil.blankToDefault(tag.getTagColor(), "#67C23A"));
        if (id == null) customerTagMapper.insert(tag); else customerTagMapper.updateById(tag);
        return tag;
    }

    @Transactional
    public void deleteCustomerTag(Long id, Long enterpriseId) {
        requireCustomerTag(id, enterpriseId);
        if (customerTagRelationMapper.selectCount(new LambdaQueryWrapper<MdCustomerTagRelation>()
                .eq(MdCustomerTagRelation::getTagId, id)) > 0) throw new BusinessException("客户标签已被使用，不能删除");
        customerTagMapper.deleteById(id);
    }

    public Map<String, Object> productMetadataOptions(Long enterpriseId) {
        List<MdProductAttribute> attributes = attributeMapper.selectList(new LambdaQueryWrapper<MdProductAttribute>()
                .eq(MdProductAttribute::getEnterpriseId, enterpriseId).eq(MdProductAttribute::getStatus, "ENABLED")
                .orderByAsc(MdProductAttribute::getSortNo));
        enrichAttributeValues(attributes);
        List<MdProductTag> tags = productTagMapper.selectList(new LambdaQueryWrapper<MdProductTag>()
                .eq(MdProductTag::getEnterpriseId, enterpriseId).orderByAsc(MdProductTag::getTagName));
        return Map.of("attributes", attributes, "tags", tags);
    }

    public List<MdCustomerTag> customerTagOptions(Long enterpriseId) {
        return customerTagMapper.selectList(new LambdaQueryWrapper<MdCustomerTag>()
                .eq(MdCustomerTag::getEnterpriseId, enterpriseId).orderByAsc(MdCustomerTag::getTagName));
    }

    @Transactional
    public void saveProductRelations(Long productId, Long enterpriseId, List<Long> tagIds,
                                     List<ProductAttributeSelection> selections) {
        List<Long> distinctTags = tagIds == null ? List.of() : tagIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!distinctTags.isEmpty() && productTagMapper.selectCount(new LambdaQueryWrapper<MdProductTag>()
                .eq(MdProductTag::getEnterpriseId, enterpriseId).in(MdProductTag::getId, distinctTags)) != distinctTags.size()) {
            throw new BusinessException("商品标签中存在无效数据");
        }
        List<ProductAttributeSelection> normalized = validateSelections(enterpriseId, selections);
        productTagRelationMapper.delete(new LambdaQueryWrapper<MdProductTagRelation>()
                .eq(MdProductTagRelation::getProductId, productId));
        attributeRelationMapper.delete(new LambdaQueryWrapper<MdProductAttributeRelation>()
                .eq(MdProductAttributeRelation::getProductId, productId));
        distinctTags.forEach(tagId -> productTagRelationMapper.insert(new MdProductTagRelation()
                .setProductId(productId).setTagId(tagId)));
        normalized.forEach(selection -> attributeRelationMapper.insert(new MdProductAttributeRelation()
                .setProductId(productId).setAttributeId(selection.getAttributeId())
                .setAttributeValueId(selection.getAttributeValueId()).setCustomValue(selection.getCustomValue())));
    }

    public void enrichProducts(List<MdProduct> products) {
        if (products == null || products.isEmpty()) return;
        List<Long> ids = products.stream().map(MdProduct::getId).toList();
        List<MdProductTagRelation> tagRelations = productTagRelationMapper.selectList(
                new LambdaQueryWrapper<MdProductTagRelation>().in(MdProductTagRelation::getProductId, ids));
        Set<Long> tagIds = tagRelations.stream().map(MdProductTagRelation::getTagId).collect(Collectors.toSet());
        Map<Long, MdProductTag> tags = tagIds.isEmpty() ? Map.of() : productTagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(MdProductTag::getId, Function.identity()));
        Map<Long, List<MdProductTagRelation>> byProduct = tagRelations.stream()
                .collect(Collectors.groupingBy(MdProductTagRelation::getProductId));
        Map<Long, List<ProductAttributeSelection>> attributesByProduct = attributeRelationMapper.selectList(
                        new LambdaQueryWrapper<MdProductAttributeRelation>().in(MdProductAttributeRelation::getProductId, ids))
                .stream().collect(Collectors.groupingBy(MdProductAttributeRelation::getProductId,
                        Collectors.mapping(relation -> {
                            ProductAttributeSelection selection = new ProductAttributeSelection();
                            selection.setAttributeId(relation.getAttributeId());
                            selection.setAttributeValueId(relation.getAttributeValueId());
                            selection.setCustomValue(relation.getCustomValue());
                            return selection;
                        }, Collectors.toList())));
        products.forEach(product -> {
            List<MdProductTagRelation> relations = byProduct.getOrDefault(product.getId(), List.of());
            product.setTagIds(relations.stream().map(MdProductTagRelation::getTagId).toList());
            product.setTagNames(relations.stream().map(MdProductTagRelation::getTagId).map(tags::get)
                    .filter(Objects::nonNull).map(MdProductTag::getTagName).toList());
            product.setAttributes(attributesByProduct.getOrDefault(product.getId(), List.of()));
        });
    }

    @Transactional
    public void saveCustomerTags(Long customerId, Long enterpriseId, List<Long> tagIds) {
        List<Long> distinct = tagIds == null ? List.of() : tagIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!distinct.isEmpty() && customerTagMapper.selectCount(new LambdaQueryWrapper<MdCustomerTag>()
                .eq(MdCustomerTag::getEnterpriseId, enterpriseId).in(MdCustomerTag::getId, distinct)) != distinct.size()) {
            throw new BusinessException("客户标签中存在无效数据");
        }
        customerTagRelationMapper.delete(new LambdaQueryWrapper<MdCustomerTagRelation>()
                .eq(MdCustomerTagRelation::getCustomerId, customerId));
        distinct.forEach(tagId -> customerTagRelationMapper.insert(new MdCustomerTagRelation()
                .setCustomerId(customerId).setTagId(tagId)));
    }

    public void enrichCustomers(List<MdCustomer> customers) {
        if (customers == null || customers.isEmpty()) return;
        List<Long> ids = customers.stream().map(MdCustomer::getId).toList();
        List<MdCustomerTagRelation> relations = customerTagRelationMapper.selectList(
                new LambdaQueryWrapper<MdCustomerTagRelation>().in(MdCustomerTagRelation::getCustomerId, ids));
        Set<Long> tagIds = relations.stream().map(MdCustomerTagRelation::getTagId).collect(Collectors.toSet());
        Map<Long, MdCustomerTag> tags = tagIds.isEmpty() ? Map.of() : customerTagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(MdCustomerTag::getId, Function.identity()));
        Map<Long, List<MdCustomerTagRelation>> byCustomer = relations.stream()
                .collect(Collectors.groupingBy(MdCustomerTagRelation::getCustomerId));
        customers.forEach(customer -> {
            List<MdCustomerTagRelation> rows = byCustomer.getOrDefault(customer.getId(), List.of());
            customer.setTagIds(rows.stream().map(MdCustomerTagRelation::getTagId).toList());
            customer.setTagNames(rows.stream().map(MdCustomerTagRelation::getTagId).map(tags::get)
                    .filter(Objects::nonNull).map(MdCustomerTag::getTagName).toList());
        });
    }

    private void syncAttributeValues(MdProductAttribute attribute) {
        List<MdProductAttributeValue> submitted = "SELECT".equals(attribute.getInputType())
                ? Optional.ofNullable(attribute.getValues()).orElse(List.of()) : List.of();
        Set<String> codes = new HashSet<>();
        for (MdProductAttributeValue value : submitted) {
            if (StrUtil.isBlank(value.getValueCode()) || StrUtil.isBlank(value.getValueName())) {
                throw new BusinessException("属性值编码和名称不能为空");
            }
            if (!codes.add(value.getValueCode().trim())) throw new BusinessException("同一属性下的属性值编码不能重复");
        }
        List<MdProductAttributeValue> existing = attributeValueMapper.selectList(
                new LambdaQueryWrapper<MdProductAttributeValue>().eq(MdProductAttributeValue::getAttributeId, attribute.getId()));
        Set<Long> retained = submitted.stream().map(MdProductAttributeValue::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        for (MdProductAttributeValue old : existing) {
            if (!retained.contains(old.getId())) {
                if (attributeRelationMapper.selectCount(new LambdaQueryWrapper<MdProductAttributeRelation>()
                        .eq(MdProductAttributeRelation::getAttributeValueId, old.getId())) > 0) {
                    throw new BusinessException("属性值“" + old.getValueName() + "”已被商品使用，不能删除");
                }
                attributeValueMapper.deleteById(old.getId());
            }
        }
        Map<Long, MdProductAttributeValue> existingMap = existing.stream()
                .collect(Collectors.toMap(MdProductAttributeValue::getId, Function.identity()));
        for (MdProductAttributeValue value : submitted) {
            value.setAttributeId(attribute.getId()).setValueCode(value.getValueCode().trim())
                    .setValueName(value.getValueName().trim())
                    .setSortNo(value.getSortNo() == null ? 0 : value.getSortNo())
                    .setStatus(StrUtil.blankToDefault(value.getStatus(), "ENABLED"));
            if (value.getId() == null) attributeValueMapper.insert(value);
            else {
                if (!existingMap.containsKey(value.getId())) throw new BusinessException("属性值不属于当前属性");
                attributeValueMapper.updateById(value);
            }
        }
    }

    private List<ProductAttributeSelection> validateSelections(Long enterpriseId, List<ProductAttributeSelection> submitted) {
        if (submitted == null) return List.of();
        Set<Long> seen = new HashSet<>();
        List<ProductAttributeSelection> result = new ArrayList<>();
        for (ProductAttributeSelection selection : submitted) {
            if (selection.getAttributeId() == null || !seen.add(selection.getAttributeId())) {
                throw new BusinessException("商品属性不能重复");
            }
            MdProductAttribute attribute = attributeMapper.selectOne(new LambdaQueryWrapper<MdProductAttribute>()
                    .eq(MdProductAttribute::getId, selection.getAttributeId())
                    .eq(MdProductAttribute::getEnterpriseId, enterpriseId).eq(MdProductAttribute::getStatus, "ENABLED"));
            if (attribute == null) throw new BusinessException("商品属性无效或已停用");
            if ("SELECT".equals(attribute.getInputType())) {
                // 属性是可选项：用户未选择时不保存关系，选择后才校验选项归属。
                if (selection.getAttributeValueId() == null) continue;
                if (selection.getAttributeValueId() == null || attributeValueMapper.selectCount(
                        new LambdaQueryWrapper<MdProductAttributeValue>()
                                .eq(MdProductAttributeValue::getId, selection.getAttributeValueId())
                                .eq(MdProductAttributeValue::getAttributeId, attribute.getId())
                                .eq(MdProductAttributeValue::getStatus, "ENABLED")) == 0) {
                    throw new BusinessException("商品属性值无效或已停用");
                }
                selection.setCustomValue(null);
            } else {
                selection.setAttributeValueId(null);
                if (StrUtil.isBlank(selection.getCustomValue())) continue;
                if ("NUMBER".equals(attribute.getInputType())) {
                    try { new java.math.BigDecimal(selection.getCustomValue()); }
                    catch (NumberFormatException e) { throw new BusinessException("数值属性必须填写数字"); }
                }
            }
            result.add(selection);
        }
        return result;
    }

    private void enrichAttributeValues(List<MdProductAttribute> attributes) {
        if (attributes.isEmpty()) return;
        Map<Long, List<MdProductAttributeValue>> values = attributeValueMapper.selectList(
                        new LambdaQueryWrapper<MdProductAttributeValue>()
                                .in(MdProductAttributeValue::getAttributeId, attributes.stream().map(MdProductAttribute::getId).toList())
                                .orderByAsc(MdProductAttributeValue::getSortNo))
                .stream().collect(Collectors.groupingBy(MdProductAttributeValue::getAttributeId));
        attributes.forEach(attribute -> attribute.setValues(values.getOrDefault(attribute.getId(), List.of())));
    }

    private MdProductTag requireProductTag(Long id, Long enterpriseId) {
        MdProductTag tag = productTagMapper.selectOne(new LambdaQueryWrapper<MdProductTag>()
                .eq(MdProductTag::getId, id).eq(MdProductTag::getEnterpriseId, enterpriseId));
        if (tag == null) throw new BusinessException("商品标签不存在");
        return tag;
    }

    private MdCustomerTag requireCustomerTag(Long id, Long enterpriseId) {
        MdCustomerTag tag = customerTagMapper.selectOne(new LambdaQueryWrapper<MdCustomerTag>()
                .eq(MdCustomerTag::getId, id).eq(MdCustomerTag::getEnterpriseId, enterpriseId));
        if (tag == null) throw new BusinessException("客户标签不存在");
        return tag;
    }

    private void requireTagName(String name) {
        if (StrUtil.isBlank(name)) throw new BusinessException("标签名称不能为空");
    }
}
