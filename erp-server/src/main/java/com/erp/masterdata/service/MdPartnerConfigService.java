package com.erp.masterdata.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.masterdata.entity.*;
import com.erp.masterdata.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class MdPartnerConfigService {
    private final MdCustomerCategoryMapper customerCategoryMapper;
    private final MdCustomerLevelMapper customerLevelMapper;
    private final MdSupplierCategoryMapper supplierCategoryMapper;
    private final MdCustomerMapper customerMapper;
    private final MdSupplierMapper supplierMapper;

    public PageResult<MdCustomerCategory> pageCustomerCategories(Long enterpriseId, Integer page, Integer size,
                                                                  String code, String name, String status) {
        Page<MdCustomerCategory> result = customerCategoryMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MdCustomerCategory>().eq(MdCustomerCategory::getEnterpriseId, enterpriseId)
                        .like(StrUtil.isNotBlank(code), MdCustomerCategory::getCategoryCode, code)
                        .like(StrUtil.isNotBlank(name), MdCustomerCategory::getCategoryName, name)
                        .eq(StrUtil.isNotBlank(status), MdCustomerCategory::getStatus, status)
                        .orderByAsc(MdCustomerCategory::getSortNo).orderByDesc(MdCustomerCategory::getCreatedAt));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public MdCustomerCategory getCustomerCategory(Long id, Long enterpriseId) {
        MdCustomerCategory row = customerCategoryMapper.selectOne(new LambdaQueryWrapper<MdCustomerCategory>()
                .eq(MdCustomerCategory::getId, id).eq(MdCustomerCategory::getEnterpriseId, enterpriseId));
        if (row == null) throw new BusinessException("客户分类不存在");
        return row;
    }

    @Transactional
    public MdCustomerCategory saveCustomerCategory(Long id, MdCustomerCategory row, Long enterpriseId) {
        requireCodeName(row.getCategoryCode(), row.getCategoryName(), "客户分类");
        if (customerCategoryMapper.selectCount(new LambdaQueryWrapper<MdCustomerCategory>()
                .eq(MdCustomerCategory::getEnterpriseId, enterpriseId)
                .eq(MdCustomerCategory::getCategoryCode, row.getCategoryCode().trim())
                .ne(id != null, MdCustomerCategory::getId, id)) > 0) throw new BusinessException("客户分类编码已存在");
        row.setId(id).setEnterpriseId(enterpriseId).setCategoryCode(row.getCategoryCode().trim())
                .setCategoryName(row.getCategoryName().trim())
                .setSortNo(row.getSortNo() == null ? 0 : row.getSortNo())
                .setStatus(StrUtil.blankToDefault(row.getStatus(), "ENABLED"));
        if (id == null) customerCategoryMapper.insert(row);
        else { getCustomerCategory(id, enterpriseId); customerCategoryMapper.updateById(row); }
        return row;
    }

    @Transactional
    public void deleteCustomerCategory(Long id, Long enterpriseId) {
        getCustomerCategory(id, enterpriseId);
        if (customerMapper.selectCount(new LambdaQueryWrapper<MdCustomer>()
                .eq(MdCustomer::getEnterpriseId, enterpriseId).eq(MdCustomer::getCategoryId, id)) > 0) {
            throw new BusinessException("客户分类已被客户使用，不能删除");
        }
        customerCategoryMapper.deleteById(id);
    }

    public PageResult<MdCustomerLevel> pageCustomerLevels(Long enterpriseId, Integer page, Integer size,
                                                            String code, String name, String status) {
        Page<MdCustomerLevel> result = customerLevelMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MdCustomerLevel>().eq(MdCustomerLevel::getEnterpriseId, enterpriseId)
                        .like(StrUtil.isNotBlank(code), MdCustomerLevel::getLevelCode, code)
                        .like(StrUtil.isNotBlank(name), MdCustomerLevel::getLevelName, name)
                        .eq(StrUtil.isNotBlank(status), MdCustomerLevel::getStatus, status)
                        .orderByAsc(MdCustomerLevel::getSortNo).orderByDesc(MdCustomerLevel::getCreatedAt));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public MdCustomerLevel getCustomerLevel(Long id, Long enterpriseId) {
        MdCustomerLevel row = customerLevelMapper.selectOne(new LambdaQueryWrapper<MdCustomerLevel>()
                .eq(MdCustomerLevel::getId, id).eq(MdCustomerLevel::getEnterpriseId, enterpriseId));
        if (row == null) throw new BusinessException("客户等级不存在");
        return row;
    }

    @Transactional
    public MdCustomerLevel saveCustomerLevel(Long id, MdCustomerLevel row, Long enterpriseId) {
        requireCodeName(row.getLevelCode(), row.getLevelName(), "客户等级");
        BigDecimal discount = row.getDiscountRate() == null ? BigDecimal.ONE : row.getDiscountRate();
        if (discount.compareTo(BigDecimal.ZERO) <= 0 || discount.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException("折扣率必须大于0且不超过1");
        }
        if (customerLevelMapper.selectCount(new LambdaQueryWrapper<MdCustomerLevel>()
                .eq(MdCustomerLevel::getEnterpriseId, enterpriseId)
                .eq(MdCustomerLevel::getLevelCode, row.getLevelCode().trim())
                .ne(id != null, MdCustomerLevel::getId, id)) > 0) throw new BusinessException("客户等级编码已存在");
        row.setId(id).setEnterpriseId(enterpriseId).setLevelCode(row.getLevelCode().trim())
                .setLevelName(row.getLevelName().trim()).setDiscountRate(discount)
                .setCreditLimit(row.getCreditLimit() == null ? BigDecimal.ZERO : row.getCreditLimit())
                .setSortNo(row.getSortNo() == null ? 0 : row.getSortNo())
                .setStatus(StrUtil.blankToDefault(row.getStatus(), "ENABLED"));
        if (row.getCreditLimit().compareTo(BigDecimal.ZERO) < 0) throw new BusinessException("信用额度不能小于0");
        if (id == null) customerLevelMapper.insert(row);
        else { getCustomerLevel(id, enterpriseId); customerLevelMapper.updateById(row); }
        return row;
    }

    @Transactional
    public void deleteCustomerLevel(Long id, Long enterpriseId) {
        getCustomerLevel(id, enterpriseId);
        if (customerMapper.selectCount(new LambdaQueryWrapper<MdCustomer>()
                .eq(MdCustomer::getEnterpriseId, enterpriseId).eq(MdCustomer::getLevelId, id)) > 0) {
            throw new BusinessException("客户等级已被客户使用，不能删除");
        }
        customerLevelMapper.deleteById(id);
    }

    public PageResult<MdSupplierCategory> pageSupplierCategories(Long enterpriseId, Integer page, Integer size,
                                                                  String code, String name, String status) {
        Page<MdSupplierCategory> result = supplierCategoryMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MdSupplierCategory>().eq(MdSupplierCategory::getEnterpriseId, enterpriseId)
                        .like(StrUtil.isNotBlank(code), MdSupplierCategory::getCategoryCode, code)
                        .like(StrUtil.isNotBlank(name), MdSupplierCategory::getCategoryName, name)
                        .eq(StrUtil.isNotBlank(status), MdSupplierCategory::getStatus, status)
                        .orderByAsc(MdSupplierCategory::getSortNo).orderByDesc(MdSupplierCategory::getCreatedAt));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public MdSupplierCategory getSupplierCategory(Long id, Long enterpriseId) {
        MdSupplierCategory row = supplierCategoryMapper.selectOne(new LambdaQueryWrapper<MdSupplierCategory>()
                .eq(MdSupplierCategory::getId, id).eq(MdSupplierCategory::getEnterpriseId, enterpriseId));
        if (row == null) throw new BusinessException("供应商分类不存在");
        return row;
    }

    @Transactional
    public MdSupplierCategory saveSupplierCategory(Long id, MdSupplierCategory row, Long enterpriseId) {
        requireCodeName(row.getCategoryCode(), row.getCategoryName(), "供应商分类");
        if (supplierCategoryMapper.selectCount(new LambdaQueryWrapper<MdSupplierCategory>()
                .eq(MdSupplierCategory::getEnterpriseId, enterpriseId)
                .eq(MdSupplierCategory::getCategoryCode, row.getCategoryCode().trim())
                .ne(id != null, MdSupplierCategory::getId, id)) > 0) throw new BusinessException("供应商分类编码已存在");
        row.setId(id).setEnterpriseId(enterpriseId).setCategoryCode(row.getCategoryCode().trim())
                .setCategoryName(row.getCategoryName().trim())
                .setSortNo(row.getSortNo() == null ? 0 : row.getSortNo())
                .setStatus(StrUtil.blankToDefault(row.getStatus(), "ENABLED"));
        if (id == null) supplierCategoryMapper.insert(row);
        else { getSupplierCategory(id, enterpriseId); supplierCategoryMapper.updateById(row); }
        return row;
    }

    @Transactional
    public void deleteSupplierCategory(Long id, Long enterpriseId) {
        getSupplierCategory(id, enterpriseId);
        if (supplierMapper.selectCount(new LambdaQueryWrapper<MdSupplier>()
                .eq(MdSupplier::getEnterpriseId, enterpriseId).eq(MdSupplier::getCategoryId, id)) > 0) {
            throw new BusinessException("供应商分类已被供应商使用，不能删除");
        }
        supplierCategoryMapper.deleteById(id);
    }

    public List<Map<String, Object>> customerCategoryOptions(Long enterpriseId) {
        return options(customerCategoryMapper.selectList(new LambdaQueryWrapper<MdCustomerCategory>()
                        .eq(MdCustomerCategory::getEnterpriseId, enterpriseId).eq(MdCustomerCategory::getStatus, "ENABLED")
                        .orderByAsc(MdCustomerCategory::getSortNo)), MdCustomerCategory::getId, MdCustomerCategory::getCategoryName);
    }

    public List<Map<String, Object>> customerLevelOptions(Long enterpriseId) {
        return options(customerLevelMapper.selectList(new LambdaQueryWrapper<MdCustomerLevel>()
                        .eq(MdCustomerLevel::getEnterpriseId, enterpriseId).eq(MdCustomerLevel::getStatus, "ENABLED")
                        .orderByAsc(MdCustomerLevel::getSortNo)), MdCustomerLevel::getId, MdCustomerLevel::getLevelName);
    }

    public List<Map<String, Object>> supplierCategoryOptions(Long enterpriseId) {
        return options(supplierCategoryMapper.selectList(new LambdaQueryWrapper<MdSupplierCategory>()
                        .eq(MdSupplierCategory::getEnterpriseId, enterpriseId).eq(MdSupplierCategory::getStatus, "ENABLED")
                        .orderByAsc(MdSupplierCategory::getSortNo)), MdSupplierCategory::getId, MdSupplierCategory::getCategoryName);
    }

    private <T> List<Map<String, Object>> options(List<T> rows, Function<T, Long> id, Function<T, String> name) {
        return rows.stream().map(row -> {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("value", id.apply(row));
            option.put("label", name.apply(row));
            return option;
        }).toList();
    }

    private void requireCodeName(String code, String name, String label) {
        if (StrUtil.isBlank(code) || StrUtil.isBlank(name)) throw new BusinessException(label + "编码和名称不能为空");
    }
}
