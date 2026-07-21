package com.erp.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.system.entity.OrgStore;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.entity.SysUser;
import com.erp.system.mapper.OrgStoreMapper;
import com.erp.system.mapper.OrgWarehouseMapper;
import com.erp.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrgStoreService extends ServiceImpl<OrgStoreMapper, OrgStore> {
    private final OrgWarehouseMapper warehouseMapper;
    private final SysUserMapper userMapper;

    public PageResult<OrgStore> pageQuery(Long enterpriseId, Integer page, Integer size,
                                          String storeCode, String storeName, String status) {
        Page<OrgStore> result = page(new Page<>(page, size), new LambdaQueryWrapper<OrgStore>()
                .eq(OrgStore::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(storeCode), OrgStore::getStoreCode, storeCode)
                .like(StrUtil.isNotBlank(storeName), OrgStore::getStoreName, storeName)
                .eq(StrUtil.isNotBlank(status), OrgStore::getStatus, status)
                .orderByDesc(OrgStore::getCreatedAt));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public OrgStore getDetail(Long id, Long enterpriseId) {
        OrgStore store = lambdaQuery().eq(OrgStore::getId, id).eq(OrgStore::getEnterpriseId, enterpriseId).one();
        if (store == null) throw new BusinessException("门店不存在");
        return store;
    }

    @Transactional
    public OrgStore createStore(OrgStore store, Long enterpriseId, Long operatorId) {
        normalize(store);
        if (lambdaQuery().eq(OrgStore::getEnterpriseId, enterpriseId)
                .eq(OrgStore::getStoreCode, store.getStoreCode()).count() > 0) {
            throw new BusinessException("门店编码已存在");
        }
        store.setId(null).setEnterpriseId(enterpriseId).setCreatedBy(operatorId).setUpdatedBy(operatorId);
        save(store);
        return store;
    }

    @Transactional
    public void updateStore(Long id, OrgStore submitted, Long enterpriseId, Long operatorId) {
        OrgStore existing = getDetail(id, enterpriseId);
        normalize(submitted);
        if (lambdaQuery().eq(OrgStore::getEnterpriseId, enterpriseId)
                .eq(OrgStore::getStoreCode, submitted.getStoreCode()).ne(OrgStore::getId, id).count() > 0) {
            throw new BusinessException("门店编码已存在");
        }
        submitted.setId(id).setEnterpriseId(enterpriseId).setCreatedBy(existing.getCreatedBy())
                .setCreatedAt(existing.getCreatedAt()).setUpdatedBy(operatorId);
        updateById(submitted);
    }

    @Transactional
    public void deleteStore(Long id, Long enterpriseId) {
        getDetail(id, enterpriseId);
        if (warehouseMapper.selectCount(new LambdaQueryWrapper<OrgWarehouse>()
                .eq(OrgWarehouse::getEnterpriseId, enterpriseId).eq(OrgWarehouse::getStoreId, id)) > 0) {
            throw new BusinessException("门店已关联仓库，不能删除");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEnterpriseId, enterpriseId).eq(SysUser::getDefaultStoreId, id)) > 0) {
            throw new BusinessException("门店已被员工设为默认门店，不能删除");
        }
        removeById(id);
    }

    public List<Map<String, Object>> options(Long enterpriseId) {
        return lambdaQuery().eq(OrgStore::getEnterpriseId, enterpriseId).eq(OrgStore::getStatus, "ENABLED")
                .orderByAsc(OrgStore::getStoreCode).list().stream().map(store -> {
                    Map<String, Object> option = new LinkedHashMap<>();
                    option.put("value", store.getId());
                    option.put("label", store.getStoreCode() + " - " + store.getStoreName());
                    return option;
                }).toList();
    }

    private void normalize(OrgStore store) {
        if (StrUtil.isBlank(store.getStoreCode()) || StrUtil.isBlank(store.getStoreName())) {
            throw new BusinessException("门店编码和名称不能为空");
        }
        store.setStoreCode(store.getStoreCode().trim()).setStoreName(store.getStoreName().trim());
        if (StrUtil.isBlank(store.getStatus())) store.setStatus("ENABLED");
    }
}
