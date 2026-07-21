package com.erp.inventory.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.PageResult;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.mapper.InvStockBalanceMapper;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.entity.MdUnit;
import com.erp.masterdata.mapper.MdProductMapper;
import com.erp.masterdata.mapper.MdUnitMapper;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.mapper.OrgWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvStockService extends ServiceImpl<InvStockBalanceMapper, InvStockBalance> {

    private final MdProductMapper productMapper;
    private final MdUnitMapper unitMapper;
    private final OrgWarehouseMapper warehouseMapper;

    public PageResult<InvStockBalance> pageStocks(Long enterpriseId, Integer page, Integer size, Long warehouseId,
                                                   String productCode, String productName, Boolean lowStock) {
        List<InvStockBalance> filtered = queryStocks(
                enterpriseId, warehouseId, productCode, productName, lowStock);

        int start = (page - 1) * size;
        int end = Math.min(start + size, filtered.size());
        List<InvStockBalance> pageRecords = start < filtered.size()
                ? filtered.subList(start, end) : List.of();
        return PageResult.of(pageRecords, filtered.size(), page, size);
    }

    public Map<String, Object> summaryStocks(Long enterpriseId, Long warehouseId,
                                              String productCode, String productName) {
        List<InvStockBalance> stocks = queryStocks(
                enterpriseId, warehouseId, productCode, productName, false);
        BigDecimal totalQuantity = stocks.stream().map(InvStockBalance::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalStockAmount = stocks.stream()
                .map(s -> s.getStockAmount() == null ? BigDecimal.ZERO : s.getStockAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long lowStockCount = stocks.stream().filter(s -> Boolean.TRUE.equals(s.getLowStock())).count();
        long warehouseCount = stocks.stream().map(InvStockBalance::getWarehouseId).distinct().count();
        long productCount = stocks.stream().map(InvStockBalance::getProductId).distinct().count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalQuantity", totalQuantity);
        summary.put("totalStockAmount", totalStockAmount);
        summary.put("lowStockCount", lowStockCount);
        summary.put("warehouseCount", warehouseCount);
        summary.put("productCount", productCount);
        return summary;
    }

    private List<InvStockBalance> queryStocks(Long enterpriseId, Long warehouseId,
                                               String productCode, String productName, Boolean lowStock) {
        List<InvStockBalance> allStocks = lambdaQuery()
                .eq(InvStockBalance::getEnterpriseId, enterpriseId)
                .eq(warehouseId != null, InvStockBalance::getWarehouseId, warehouseId)
                .ge(InvStockBalance::getQuantity, BigDecimal.ZERO)
                .orderByDesc(InvStockBalance::getUpdatedAt)
                .list();

        Map<Long, MdProduct> productMap = productMapper.selectList(
                new LambdaQueryWrapper<MdProduct>()
                        .eq(MdProduct::getEnterpriseId, enterpriseId)
                        .like(StrUtil.isNotBlank(productCode), MdProduct::getProductCode, productCode)
                        .like(StrUtil.isNotBlank(productName), MdProduct::getProductName, productName))
                .stream().collect(Collectors.toMap(MdProduct::getId, p -> p, (a, b) -> a));

        Map<Long, OrgWarehouse> warehouseMap = warehouseMapper.selectList(new LambdaQueryWrapper<OrgWarehouse>()
                        .eq(OrgWarehouse::getEnterpriseId, enterpriseId))
                .stream().collect(Collectors.toMap(OrgWarehouse::getId, w -> w, (a, b) -> a));

        List<Long> unitIds = productMap.values().stream().map(MdProduct::getUnitId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, MdUnit> unitMap = unitIds.isEmpty() ? Map.of()
                : unitMapper.selectBatchIds(unitIds).stream()
                .collect(Collectors.toMap(MdUnit::getId, u -> u, (a, b) -> a));

        return allStocks.stream()
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
                        s.setSpecification(p.getSpecification());
                        s.setUnitId(p.getUnitId());
                        MdUnit unit = unitMap.get(p.getUnitId());
                        s.setUnitName(unit == null ? null : unit.getUnitName());
                        s.setMinStock(p.getMinStock());
                        s.setLowStock(p.getMinStock() != null
                                && s.getAvailableQuantity().compareTo(p.getMinStock()) < 0);
                    }
                    OrgWarehouse w = warehouseMap.get(s.getWarehouseId());
                    if (w != null) {
                        s.setWarehouseName(w.getWarehouseName());
                    }
                })
                .collect(Collectors.toList());
    }
}
