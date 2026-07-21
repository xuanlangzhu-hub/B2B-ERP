package com.erp.inventory.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.PageResult;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.mapper.InvStockBalanceMapper;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.mapper.MdProductMapper;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.mapper.OrgWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvStockService extends ServiceImpl<InvStockBalanceMapper, InvStockBalance> {

    private final MdProductMapper productMapper;
    private final OrgWarehouseMapper warehouseMapper;

    public PageResult<InvStockBalance> pageStocks(Integer page, Integer size, Long warehouseId,
                                                   String productCode, String productName, Boolean lowStock) {
        List<InvStockBalance> allStocks = lambdaQuery()
                .eq(warehouseId != null, InvStockBalance::getWarehouseId, warehouseId)
                .gt(InvStockBalance::getQuantity, BigDecimal.ZERO)
                .list();

        Map<Long, MdProduct> productMap = productMapper.selectList(
                new LambdaQueryWrapper<MdProduct>()
                        .like(StrUtil.isNotBlank(productCode), MdProduct::getProductCode, productCode)
                        .like(StrUtil.isNotBlank(productName), MdProduct::getProductName, productName))
                .stream().collect(Collectors.toMap(MdProduct::getId, p -> p, (a, b) -> a));

        Map<Long, OrgWarehouse> warehouseMap = warehouseMapper.selectList(null)
                .stream().collect(Collectors.toMap(OrgWarehouse::getId, w -> w, (a, b) -> a));

        List<InvStockBalance> filtered = allStocks.stream()
                .filter(s -> {
                    if (!productMap.containsKey(s.getProductId())) {
                        return false;
                    }
                    if (Boolean.TRUE.equals(lowStock)) {
                        MdProduct p = productMap.get(s.getProductId());
                        if (p == null || p.getMinStock() == null) {
                            return false;
                        }
                        return s.getAvailableQuantity().compareTo(p.getMinStock()) < 0;
                    }
                    return true;
                })
                .peek(s -> {
                    MdProduct p = productMap.get(s.getProductId());
                    if (p != null) {
                        s.setProductCode(p.getProductCode());
                        s.setProductName(p.getProductName());
                    }
                    OrgWarehouse w = warehouseMap.get(s.getWarehouseId());
                    if (w != null) {
                        s.setWarehouseName(w.getWarehouseName());
                    }
                })
                .collect(Collectors.toList());

        int start = (page - 1) * size;
        int end = Math.min(start + size, filtered.size());
        List<InvStockBalance> pageRecords = start < filtered.size()
                ? filtered.subList(start, end) : filtered;
        return PageResult.of(pageRecords, filtered.size(), page, size);
    }
}
