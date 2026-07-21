package com.erp.masterdata.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.masterdata.entity.MdUnit;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.mapper.MdUnitMapper;
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
public class MdUnitService extends ServiceImpl<MdUnitMapper, MdUnit> {
    private final MdProductMapper productMapper;

    public PageResult<MdUnit> pageQuery(Long enterpriseId, Integer page, Integer size, String unitCode, String unitName, String status) {
        LambdaQueryWrapper<MdUnit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdUnit::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(unitCode), MdUnit::getUnitCode, unitCode)
                .like(StrUtil.isNotBlank(unitName), MdUnit::getUnitName, unitName)
                .eq(StrUtil.isNotBlank(status), MdUnit::getStatus, status)
                .orderByAsc(MdUnit::getSortNo)
                .orderByDesc(MdUnit::getCreatedAt);
        Page<MdUnit> result = page(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public MdUnit getDetail(Long id, Long enterpriseId) {
        MdUnit unit = lambdaQuery().eq(MdUnit::getId, id).eq(MdUnit::getEnterpriseId, enterpriseId).one();
        if (unit == null) throw new BusinessException("单位不存在");
        return unit;
    }

    @Transactional
    public MdUnit create(MdUnit unit, Long enterpriseId, Long operatorId) {
        if (lambdaQuery().eq(MdUnit::getUnitCode, unit.getUnitCode())
                .eq(MdUnit::getEnterpriseId, enterpriseId).count() > 0) {
            throw new BusinessException("单位编码已存在");
        }
        unit.setEnterpriseId(enterpriseId);
        unit.setCreatedBy(operatorId);
        unit.setPrecisionScale(unit.getPrecisionScale() == null ? 0 : unit.getPrecisionScale());
        unit.setSortNo(unit.getSortNo() == null ? 0 : unit.getSortNo());
        unit.setStatus(StrUtil.blankToDefault(unit.getStatus(), "ENABLED"));
        save(unit);
        return unit;
    }

    @Transactional
    public void update(MdUnit unit, Long enterpriseId, Long operatorId) {
        MdUnit existing = getDetail(unit.getId(), enterpriseId);
        if (lambdaQuery().eq(MdUnit::getUnitCode, unit.getUnitCode())
                .eq(MdUnit::getEnterpriseId, existing.getEnterpriseId())
                .ne(MdUnit::getId, unit.getId()).count() > 0) {
            throw new BusinessException("单位编码已存在");
        }
        unit.setEnterpriseId(enterpriseId).setUpdatedBy(operatorId);
        updateById(unit);
    }

    @Transactional
    public void delete(Long id, Long enterpriseId) {
        getDetail(id, enterpriseId);
        if (productMapper.selectCount(new LambdaQueryWrapper<MdProduct>()
                .eq(MdProduct::getEnterpriseId, enterpriseId).eq(MdProduct::getUnitId, id)) > 0) {
            throw new BusinessException("单位已被商品使用，无法删除");
        }
        removeById(id);
    }

    public List<Map<String, Object>> options(Long enterpriseId) {
        List<MdUnit> list = lambdaQuery()
                .eq(MdUnit::getEnterpriseId, enterpriseId)
                .eq(MdUnit::getStatus, "ENABLED")
                .orderByAsc(MdUnit::getSortNo)
                .list();
        return list.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("value", u.getId());
            map.put("label", u.getUnitName());
            map.put("precisionScale", u.getPrecisionScale());
            return map;
        }).collect(Collectors.toList());
    }
}
