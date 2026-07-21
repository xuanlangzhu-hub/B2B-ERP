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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvMovementService extends ServiceImpl<InvStockMovementMapper, InvStockMovement> {

    private final MdProductMapper productMapper;
    private final OrgWarehouseMapper warehouseMapper;

    public PageResult<InvStockMovement> pageMovements(Long enterpriseId, Integer page, Integer size, Long warehouseId,
                                                       Long productId, String movementType,
                                                       String sourceNo, String startDate, String endDate) {
        LambdaQueryWrapper<InvStockMovement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvStockMovement::getEnterpriseId, enterpriseId)
                .eq(warehouseId != null, InvStockMovement::getWarehouseId, warehouseId)
                .eq(productId != null, InvStockMovement::getProductId, productId)
                .eq(StrUtil.isNotBlank(movementType), InvStockMovement::getMovementType, movementType)
                .like(StrUtil.isNotBlank(sourceNo), InvStockMovement::getSourceNo, sourceNo)
                .ge(StrUtil.isNotBlank(startDate), InvStockMovement::getBusinessDate, startDate)
                .le(StrUtil.isNotBlank(endDate), InvStockMovement::getBusinessDate, endDate)
                .orderByDesc(InvStockMovement::getCreatedAt);
        Page<InvStockMovement> result = page(new Page<>(page, size), wrapper);

        List<Long> productIds = result.getRecords().stream()
                .map(InvStockMovement::getProductId).distinct().collect(Collectors.toList());
        List<Long> warehouseIds = result.getRecords().stream()
                .map(InvStockMovement::getWarehouseId).distinct().collect(Collectors.toList());

        Map<Long, MdProduct> productMap = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(MdProduct::getId, p -> p, (a, b) -> a));
        Map<Long, OrgWarehouse> warehouseMap = warehouseIds.isEmpty() ? Map.of()
                : warehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(OrgWarehouse::getId, w -> w, (a, b) -> a));

        result.getRecords().forEach(m -> {
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

        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }
}
