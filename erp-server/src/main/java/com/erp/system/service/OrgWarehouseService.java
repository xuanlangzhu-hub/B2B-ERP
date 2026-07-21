package com.erp.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.entity.OrgStore;
import com.erp.system.mapper.OrgWarehouseMapper;
import com.erp.system.mapper.OrgStoreMapper;
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
public class OrgWarehouseService extends ServiceImpl<OrgWarehouseMapper, OrgWarehouse> {
    private final OrgStoreMapper storeMapper;

    public PageResult<OrgWarehouse> pageQuery(Long enterpriseId, Integer page, Integer size, String warehouseCode, String warehouseName,
                                               String status) {
        LambdaQueryWrapper<OrgWarehouse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgWarehouse::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(warehouseCode), OrgWarehouse::getWarehouseCode, warehouseCode)
                .like(StrUtil.isNotBlank(warehouseName), OrgWarehouse::getWarehouseName, warehouseName)
                .eq(StrUtil.isNotBlank(status), OrgWarehouse::getStatus, status)
                .orderByDesc(OrgWarehouse::getCreatedAt);
        Page<OrgWarehouse> result = page(new Page<>(page, size), wrapper);
        enrichStores(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public OrgWarehouse getDetail(Long id, Long enterpriseId) {
        OrgWarehouse warehouse = lambdaQuery().eq(OrgWarehouse::getId, id)
                .eq(OrgWarehouse::getEnterpriseId, enterpriseId).one();
        if (warehouse == null) throw new BusinessException("仓库不存在");
        enrichStores(List.of(warehouse));
        return warehouse;
    }

    @Transactional
    public OrgWarehouse create(OrgWarehouse warehouse, Long enterpriseId, Long operatorId) {
        validateStore(warehouse.getStoreId(), enterpriseId);
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
    public void update(OrgWarehouse warehouse, Long enterpriseId, Long operatorId) {
        getDetail(warehouse.getId(), enterpriseId);
        validateStore(warehouse.getStoreId(), enterpriseId);
        if (lambdaQuery().eq(OrgWarehouse::getWarehouseCode, warehouse.getWarehouseCode())
                .eq(OrgWarehouse::getEnterpriseId, enterpriseId)
                .ne(OrgWarehouse::getId, warehouse.getId()).count() > 0) {
            throw new BusinessException("仓库编码已存在");
        }
        warehouse.setEnterpriseId(enterpriseId).setUpdatedBy(operatorId);
        updateById(warehouse);
    }

    @Transactional
    public void delete(Long id, Long enterpriseId) {
        getDetail(id, enterpriseId);
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

    private void validateStore(Long storeId, Long enterpriseId) {
        if (storeId != null && storeMapper.selectCount(new LambdaQueryWrapper<OrgStore>()
                .eq(OrgStore::getId, storeId).eq(OrgStore::getEnterpriseId, enterpriseId)
                .eq(OrgStore::getStatus, "ENABLED")) == 0) throw new BusinessException("所属门店无效或已停用");
    }

    private void enrichStores(List<OrgWarehouse> rows) {
        List<Long> ids = rows.stream().map(OrgWarehouse::getStoreId).filter(Objects::nonNull).distinct().toList();
        Map<Long, OrgStore> stores = ids.isEmpty() ? Collections.emptyMap() : storeMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(OrgStore::getId, Function.identity()));
        rows.forEach(row -> {
            OrgStore store = stores.get(row.getStoreId());
            row.setStoreName(store == null ? null : store.getStoreName());
        });
    }
}
