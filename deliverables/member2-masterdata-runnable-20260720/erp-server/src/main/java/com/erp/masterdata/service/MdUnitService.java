package com.erp.masterdata.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.masterdata.entity.MdUnit;
import com.erp.masterdata.mapper.MdUnitMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MdUnitService extends ServiceImpl<MdUnitMapper, MdUnit> {

    public PageResult<MdUnit> pageQuery(Integer page, Integer size, String unitCode, String unitName, String status) {
        LambdaQueryWrapper<MdUnit> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(unitCode), MdUnit::getUnitCode, unitCode)
                .like(StrUtil.isNotBlank(unitName), MdUnit::getUnitName, unitName)
                .eq(StrUtil.isNotBlank(status), MdUnit::getStatus, status)
                .orderByAsc(MdUnit::getSortNo)
                .orderByDesc(MdUnit::getCreatedAt);
        Page<MdUnit> result = page(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public MdUnit create(MdUnit unit, Long enterpriseId, Long operatorId) {
        if (lambdaQuery().eq(MdUnit::getUnitCode, unit.getUnitCode())
                .eq(MdUnit::getEnterpriseId, enterpriseId).count() > 0) {
            throw new BusinessException("单位编码已存在");
        }
        unit.setEnterpriseId(enterpriseId);
        unit.setCreatedBy(operatorId);
        save(unit);
        return unit;
    }

    @Transactional
    public void update(MdUnit unit) {
        MdUnit existing = getById(unit.getId());
        if (existing == null) {
            throw new BusinessException("单位不存在");
        }
        if (lambdaQuery().eq(MdUnit::getUnitCode, unit.getUnitCode())
                .eq(MdUnit::getEnterpriseId, existing.getEnterpriseId())
                .ne(MdUnit::getId, unit.getId()).count() > 0) {
            throw new BusinessException("单位编码已存在");
        }
        updateById(unit);
    }

    @Transactional
    public void delete(Long id) {
        MdUnit unit = getById(id);
        if (unit == null) {
            throw new BusinessException("单位不存在");
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
