package com.erp.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.mapper.OrgWarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrgWarehouseService extends ServiceImpl<OrgWarehouseMapper, OrgWarehouse> {

    public PageResult<OrgWarehouse> pageQuery(Integer page, Integer size, String warehouseCode, String warehouseName,
                                               String status) {
        LambdaQueryWrapper<OrgWarehouse> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(warehouseCode), OrgWarehouse::getWarehouseCode, warehouseCode)
                .like(StrUtil.isNotBlank(warehouseName), OrgWarehouse::getWarehouseName, warehouseName)
                .eq(StrUtil.isNotBlank(status), OrgWarehouse::getStatus, status)
                .orderByDesc(OrgWarehouse::getCreatedAt);
        Page<OrgWarehouse> result = page(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public OrgWarehouse create(OrgWarehouse warehouse, Long enterpriseId, Long operatorId) {
        if (lambdaQuery().eq(OrgWarehouse::getWarehouseCode, warehouse.getWarehouseCode())
                .eq(OrgWarehouse::getEnterpriseId, enterpriseId).count() > 0) {
            throw new BusinessException("仓库编码已存在");
        }
        warehouse.setEnterpriseId(enterpriseId);
        warehouse.setCreatedBy(operatorId);
        save(warehouse);
        return warehouse;
    }

    @Transactional
    public void update(OrgWarehouse warehouse) {
        OrgWarehouse existing = getById(warehouse.getId());
        if (existing == null) {
            throw new BusinessException("仓库不存在");
        }
        if (lambdaQuery().eq(OrgWarehouse::getWarehouseCode, warehouse.getWarehouseCode())
                .eq(OrgWarehouse::getEnterpriseId, existing.getEnterpriseId())
                .ne(OrgWarehouse::getId, warehouse.getId()).count() > 0) {
            throw new BusinessException("仓库编码已存在");
        }
        updateById(warehouse);
    }

    @Transactional
    public void delete(Long id) {
        OrgWarehouse warehouse = getById(id);
        if (warehouse == null) {
            throw new BusinessException("仓库不存在");
        }
        removeById(id);
    }

    public List<Map<String, Object>> options(Long enterpriseId) {
        List<OrgWarehouse> list = lambdaQuery()
                .eq(OrgWarehouse::getEnterpriseId, enterpriseId)
                .eq(OrgWarehouse::getStatus, "ENABLED")
                .list();
        return list.stream().map(w -> {
            Map<String, Object> map = new HashMap<>();
            map.put("value", w.getId());
            map.put("label", w.getWarehouseName());
            map.put("storeId", w.getStoreId());
            return map;
        }).collect(Collectors.toList());
    }
}
