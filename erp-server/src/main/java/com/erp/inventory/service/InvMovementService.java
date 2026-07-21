package com.erp.inventory.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.PageResult;
import com.erp.inventory.entity.InvStockMovement;
import com.erp.inventory.mapper.InvStockMovementMapper;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.mapper.MdProductMapper;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.mapper.OrgWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvMovementService extends ServiceImpl<InvStockMovementMapper, InvStockMovement> {

    private final MdProductMapper productMapper;
    private final OrgWarehouseMapper warehouseMapper;

    public PageResult<InvStockMovement> pageMovements(Long enterpriseId, Integer page, Integer size, Long warehouseId,
                                                       Long productId, String movementType,
                                                       String sourceNo, String startDate, String endDate) {
        return pageMovements(enterpriseId, page, size, warehouseId, productId, movementType,
                null, null, sourceNo, startDate, endDate);
    }

    public PageResult<InvStockMovement> pageMovements(Long enterpriseId, Integer page, Integer size, Long warehouseId,
                                                       Long productId, String movementType, String direction,
                                                       Long categoryId, String sourceNo, String startDate, String endDate) {
        LambdaQueryWrapper<InvStockMovement> wrapper = buildWrapper(enterpriseId, warehouseId, productId,
                movementType, direction, categoryId, sourceNo, startDate, endDate);
        if (wrapper == null) return PageResult.of(List.of(), 0, page, size);
        Page<InvStockMovement> result = page(new Page<>(page, size), wrapper);
        enrich(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public Map<String, Object> summaryMovements(Long enterpriseId, Long warehouseId, Long productId,
                                                 String movementType, String direction, Long categoryId,
                                                 String sourceNo, String startDate, String endDate) {
        LambdaQueryWrapper<InvStockMovement> wrapper = buildWrapper(enterpriseId, warehouseId, productId,
                movementType, direction, categoryId, sourceNo, startDate, endDate);
        List<InvStockMovement> rows = wrapper == null ? List.of() : list(wrapper);
        BigDecimal inboundQuantity = rows.stream().filter(row -> "IN".equals(row.getDirection()))
                .map(InvStockMovement::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outboundQuantity = rows.stream().filter(row -> "OUT".equals(row.getDirection()))
                .map(InvStockMovement::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal inboundAmount = rows.stream().filter(row -> "IN".equals(row.getDirection()))
                .map(row -> row.getAmount() == null ? BigDecimal.ZERO : row.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outboundAmount = rows.stream().filter(row -> "OUT".equals(row.getDirection()))
                .map(row -> row.getAmount() == null ? BigDecimal.ZERO : row.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("movementCount", rows.size());
        summary.put("productCount", rows.stream().map(InvStockMovement::getProductId).distinct().count());
        summary.put("inboundQuantity", inboundQuantity);
        summary.put("outboundQuantity", outboundQuantity);
        summary.put("inboundAmount", inboundAmount);
        summary.put("outboundAmount", outboundAmount);
        return summary;
    }

    private LambdaQueryWrapper<InvStockMovement> buildWrapper(Long enterpriseId, Long warehouseId, Long productId,
                                                               String movementType, String direction, Long categoryId,
                                                               String sourceNo, String startDate, String endDate) {
        List<Long> categoryProductIds = null;
        if (categoryId != null) {
            categoryProductIds = productMapper.selectList(new LambdaQueryWrapper<MdProduct>()
                            .eq(MdProduct::getEnterpriseId, enterpriseId).eq(MdProduct::getCategoryId, categoryId))
                    .stream().map(MdProduct::getId).toList();
            if (categoryProductIds.isEmpty()) return null;
        }
        LambdaQueryWrapper<InvStockMovement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvStockMovement::getEnterpriseId, enterpriseId)
                .eq(warehouseId != null, InvStockMovement::getWarehouseId, warehouseId)
                .eq(productId != null, InvStockMovement::getProductId, productId)
                .in(categoryProductIds != null, InvStockMovement::getProductId, categoryProductIds)
                .eq(StrUtil.isNotBlank(movementType), InvStockMovement::getMovementType, movementType)
                .eq(StrUtil.isNotBlank(direction), InvStockMovement::getDirection, direction)
                .like(StrUtil.isNotBlank(sourceNo), InvStockMovement::getSourceNo, sourceNo)
                .ge(StrUtil.isNotBlank(startDate), InvStockMovement::getBusinessDate, startDate)
                .le(StrUtil.isNotBlank(endDate), InvStockMovement::getBusinessDate, endDate)
                .orderByDesc(InvStockMovement::getCreatedAt);
        return wrapper;
    }

    private void enrich(List<InvStockMovement> rows) {
        List<Long> productIds = rows.stream()
                .map(InvStockMovement::getProductId).distinct().collect(Collectors.toList());
        List<Long> warehouseIds = rows.stream()
                .map(InvStockMovement::getWarehouseId).distinct().collect(Collectors.toList());

        Map<Long, MdProduct> productMap = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(MdProduct::getId, p -> p, (a, b) -> a));
        Map<Long, OrgWarehouse> warehouseMap = warehouseIds.isEmpty() ? Map.of()
                : warehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(OrgWarehouse::getId, w -> w, (a, b) -> a));

        rows.forEach(m -> {
            MdProduct p = productMap.get(m.getProductId());
            if (p != null) {
                m.setProductCode(p.getProductCode());
                m.setProductName(p.getProductName());
            }
            OrgWarehouse w = warehouseMap.get(m.getWarehouseId());
            if (w != null) {
                m.setWarehouseName(w.getWarehouseName());
            }
        });

    }
}
