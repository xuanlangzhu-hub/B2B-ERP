package com.erp.inventory.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.inventory.dto.InvAdjustmentItemRequest;
import com.erp.inventory.dto.InvAdjustmentRequest;
import com.erp.inventory.entity.InvAdjustment;
import com.erp.inventory.entity.InvAdjustmentItem;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.entity.InvStockMovement;
import com.erp.inventory.mapper.InvAdjustmentItemMapper;
import com.erp.inventory.mapper.InvAdjustmentMapper;
import com.erp.inventory.mapper.InvStockBalanceMapper;
import com.erp.inventory.mapper.InvStockMovementMapper;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.entity.MdUnit;
import com.erp.masterdata.mapper.MdProductMapper;
import com.erp.masterdata.mapper.MdUnitMapper;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.mapper.OrgWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvAdjustmentService extends ServiceImpl<InvAdjustmentMapper, InvAdjustment> {

    private final InvAdjustmentItemMapper adjustmentItemMapper;
    private final InvStockBalanceMapper stockBalanceMapper;
    private final InvStockMovementMapper stockMovementMapper;
    private final OrgWarehouseMapper warehouseMapper;
    private final MdProductMapper productMapper;
    private final MdUnitMapper unitMapper;

    public PageResult<InvAdjustment> pageAdjustments(Long enterpriseId, Integer page, Integer size,
                                                      String adjustmentNo, Long warehouseId,
                                                      String adjustmentType, String status,
                                                      String startDate, String endDate) {
        LambdaQueryWrapper<InvAdjustment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvAdjustment::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(adjustmentNo), InvAdjustment::getAdjustmentNo, adjustmentNo)
                .eq(warehouseId != null, InvAdjustment::getWarehouseId, warehouseId)
                .eq(StrUtil.isNotBlank(adjustmentType), InvAdjustment::getAdjustmentType, adjustmentType)
                .eq(StrUtil.isNotBlank(status), InvAdjustment::getStatus, status)
                .ge(StrUtil.isNotBlank(startDate), InvAdjustment::getAdjustmentDate, startDate)
                .le(StrUtil.isNotBlank(endDate), InvAdjustment::getAdjustmentDate, endDate)
                .orderByDesc(InvAdjustment::getCreatedAt);
        Page<InvAdjustment> result = page(new Page<>(page, size), wrapper);
        enrichWarehouses(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public InvAdjustment getDetail(Long id, Long enterpriseId) {
        InvAdjustment adjustment = lambdaQuery().eq(InvAdjustment::getId, id)
                .eq(InvAdjustment::getEnterpriseId, enterpriseId).one();
        if (adjustment == null) throw new BusinessException("库存调整单不存在");
        List<InvAdjustmentItem> items = adjustmentItemMapper.selectList(
                new LambdaQueryWrapper<InvAdjustmentItem>()
                        .eq(InvAdjustmentItem::getAdjustmentId, id)
                        .orderByAsc(InvAdjustmentItem::getLineNo));
        enrichUnits(items);
        adjustment.setItems(items);
        enrichWarehouses(Collections.singletonList(adjustment));
        return adjustment;
    }

    @Transactional
    public InvAdjustment createAdjustment(InvAdjustmentRequest request, Long enterpriseId, Long operatorId) {
        OrgWarehouse warehouse = validateWarehouse(request.getWarehouseId(), enterpriseId);
        Map<Long, MdProduct> productMap = validateAndLoadProducts(request.getItems(), enterpriseId);
        List<Long> productIds = request.getItems().stream().map(InvAdjustmentItemRequest::getProductId).toList();
        Map<Long, InvStockBalance> stockMap = stockBalanceMapper.selectList(
                        new LambdaQueryWrapper<InvStockBalance>()
                                .eq(InvStockBalance::getEnterpriseId, enterpriseId)
                                .eq(InvStockBalance::getWarehouseId, warehouse.getId())
                                .in(InvStockBalance::getProductId, productIds))
                .stream().collect(Collectors.toMap(InvStockBalance::getProductId, Function.identity()));

        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        InvAdjustment adjustment = new InvAdjustment()
                .setEnterpriseId(enterpriseId)
                .setAdjustmentNo("TZ" + System.currentTimeMillis())
                .setAdjustmentDate(request.getAdjustmentDate())
                .setWarehouseId(request.getWarehouseId())
                .setAdjustmentType(request.getAdjustmentType())
                .setSourceType("MANUAL")
                .setStatus("DRAFT")
                .setReason(request.getReason())
                .setRemark(request.getRemark())
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);
        save(adjustment);

        int lineNo = 1;
        for (InvAdjustmentItemRequest submitted : request.getItems()) {
            MdProduct product = productMap.get(submitted.getProductId());
            InvStockBalance stock = stockMap.get(submitted.getProductId());
            BigDecimal quantity = submitted.getQuantity().setScale(4, RoundingMode.HALF_UP);
            BigDecimal unitCost;
            if ("DECREASE".equals(request.getAdjustmentType()) && stock != null) {
                unitCost = zero(stock.getAvgCostPrice());
            } else if (submitted.getUnitCost() != null) {
                unitCost = submitted.getUnitCost();
            } else if (stock != null) {
                unitCost = zero(stock.getAvgCostPrice());
            } else {
                unitCost = zero(product.getCostPrice());
            }
            unitCost = unitCost.setScale(4, RoundingMode.HALF_UP);
            BigDecimal amount = quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            adjustmentItemMapper.insert(new InvAdjustmentItem()
                    .setAdjustmentId(adjustment.getId())
                    .setLineNo(lineNo++)
                    .setProductId(product.getId())
                    .setProductCode(product.getProductCode())
                    .setProductName(product.getProductName())
                    .setUnitId(product.getUnitId())
                    .setQuantity(quantity)
                    .setUnitCost(unitCost)
                    .setAmount(amount)
                    .setReason(submitted.getReason()));
            totalQuantity = totalQuantity.add(quantity);
            totalAmount = totalAmount.add(amount);
        }
        adjustment.setTotalQuantity(totalQuantity).setTotalAmount(totalAmount);
        updateById(adjustment);
        return getDetail(adjustment.getId(), enterpriseId);
    }

    @Transactional
    public void approveAdjustment(Long id, Long enterpriseId, Long operatorId) {
        InvAdjustment adjustment = lockAdjustment(id, enterpriseId);
        if (!"DRAFT".equals(adjustment.getStatus())) {
            throw new BusinessException("只有草稿状态的库存调整单可以审核");
        }
        OrgWarehouse warehouse = validateWarehouse(adjustment.getWarehouseId(), enterpriseId);
        List<InvAdjustmentItem> items = adjustmentItemMapper.selectList(
                new LambdaQueryWrapper<InvAdjustmentItem>()
                        .eq(InvAdjustmentItem::getAdjustmentId, id));
        if (items.isEmpty()) throw new BusinessException("库存调整单没有明细");
        items = new ArrayList<>(items);
        items.sort(Comparator.comparing(InvAdjustmentItem::getProductId));

        Map<Long, InvStockBalance> stocks = new HashMap<>();
        for (InvAdjustmentItem item : items) {
            InvStockBalance stock = stockBalanceMapper.selectForUpdate(
                    enterpriseId, adjustment.getWarehouseId(), item.getProductId());
            if ("DECREASE".equals(adjustment.getAdjustmentType())
                    && (stock == null || zero(stock.getAvailableQuantity()).compareTo(item.getQuantity()) < 0)) {
                throw new BusinessException("库存不足，商品: " + item.getProductCode() + " - " + item.getProductName());
            }
            stocks.put(item.getProductId(), stock);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (InvAdjustmentItem item : items) {
            InvStockBalance stock = stocks.get(item.getProductId());
            BigDecimal before = stock == null ? BigDecimal.ZERO : zero(stock.getQuantity());
            boolean increase = "INCREASE".equals(adjustment.getAdjustmentType());
            BigDecimal unitCost = increase ? zero(item.getUnitCost()) : zero(stock.getAvgCostPrice());
            unitCost = unitCost.setScale(4, RoundingMode.HALF_UP);
            BigDecimal amount = item.getQuantity().multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal after = increase ? before.add(item.getQuantity()) : before.subtract(item.getQuantity());

            if (stock == null) {
                stock = new InvStockBalance()
                        .setEnterpriseId(enterpriseId)
                        .setWarehouseId(adjustment.getWarehouseId())
                        .setProductId(item.getProductId())
                        .setQuantity(after)
                        .setLockedQuantity(BigDecimal.ZERO)
                        .setAvailableQuantity(after)
                        .setAvgCostPrice(unitCost)
                        .setStockAmount(amount)
                        .setLastMovementAt(LocalDateTime.now())
                        .setVersion(0);
                stockBalanceMapper.insert(stock);
            } else if (increase) {
                BigDecimal newAmount = zero(stock.getStockAmount()).add(amount);
                BigDecimal avgCost = after.compareTo(BigDecimal.ZERO) > 0
                        ? newAmount.divide(after, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                stock.setQuantity(after)
                        .setAvailableQuantity(after.subtract(zero(stock.getLockedQuantity())))
                        .setAvgCostPrice(avgCost)
                        .setStockAmount(newAmount)
                        .setLastMovementAt(LocalDateTime.now());
                if (stockBalanceMapper.updateById(stock) != 1) throw new BusinessException("库存已变化，请重试");
            } else {
                stock.setQuantity(after)
                        .setAvailableQuantity(after.subtract(zero(stock.getLockedQuantity())))
                        .setStockAmount(zero(stock.getStockAmount()).subtract(amount).max(BigDecimal.ZERO))
                        .setLastMovementAt(LocalDateTime.now());
                if (stockBalanceMapper.updateById(stock) != 1) throw new BusinessException("库存已变化，请重试");
            }

            item.setUnitCost(unitCost).setAmount(amount);
            adjustmentItemMapper.updateById(item);
            totalAmount = totalAmount.add(amount);
            stockMovementMapper.insert(new InvStockMovement()
                    .setEnterpriseId(enterpriseId)
                    .setStoreId(warehouse.getStoreId())
                    .setWarehouseId(adjustment.getWarehouseId())
                    .setProductId(item.getProductId())
                    .setMovementNo("SMTZ" + System.currentTimeMillis() + "_" + item.getLineNo())
                    .setMovementType("ADJUST")
                    .setDirection(increase ? "IN" : "OUT")
                    .setQuantity(item.getQuantity())
                    .setUnitCost(unitCost)
                    .setAmount(amount)
                    .setBeforeQuantity(before)
                    .setAfterQuantity(after)
                    .setSourceType("INVENTORY_ADJUSTMENT")
                    .setSourceId(adjustment.getId())
                    .setSourceNo(adjustment.getAdjustmentNo())
                    .setSourceItemId(item.getId())
                    .setBusinessDate(adjustment.getAdjustmentDate())
                    .setOperatorId(operatorId)
                    .setRemark(StrUtil.blankToDefault(item.getReason(), adjustment.getReason())));
        }
        adjustment.setStatus("COMPLETED")
                .setTotalAmount(totalAmount)
                .setApprovedBy(operatorId)
                .setApprovedAt(LocalDateTime.now())
                .setUpdatedBy(operatorId);
        updateById(adjustment);
    }

    @Transactional
    public void cancelAdjustment(Long id, Long enterpriseId, Long operatorId) {
        InvAdjustment adjustment = lockAdjustment(id, enterpriseId);
        if ("CANCELLED".equals(adjustment.getStatus())) return;
        if ("COMPLETED".equals(adjustment.getStatus())) {
            throw new BusinessException("已完成的库存调整单不能取消");
        }
        adjustment.setStatus("CANCELLED").setUpdatedBy(operatorId);
        updateById(adjustment);
    }

    @Transactional
    public void deleteAdjustment(Long id, Long enterpriseId) {
        InvAdjustment adjustment = lockAdjustment(id, enterpriseId);
        if (!"DRAFT".equals(adjustment.getStatus()) && !"CANCELLED".equals(adjustment.getStatus())) {
            throw new BusinessException("只有草稿或已取消的库存调整单可以删除");
        }
        adjustmentItemMapper.delete(new LambdaQueryWrapper<InvAdjustmentItem>()
                .eq(InvAdjustmentItem::getAdjustmentId, id));
        removeById(id);
    }

    private Map<Long, MdProduct> validateAndLoadProducts(List<InvAdjustmentItemRequest> items, Long enterpriseId) {
        Set<Long> ids = new HashSet<>();
        for (InvAdjustmentItemRequest item : items) {
            if (!ids.add(item.getProductId())) throw new BusinessException("同一商品不能重复添加");
        }
        Map<Long, MdProduct> products = productMapper.selectList(new LambdaQueryWrapper<MdProduct>()
                        .eq(MdProduct::getEnterpriseId, enterpriseId)
                        .eq(MdProduct::getStatus, "ENABLED")
                        .in(MdProduct::getId, ids))
                .stream().collect(Collectors.toMap(MdProduct::getId, Function.identity()));
        if (products.size() != ids.size()) throw new BusinessException("调整明细中存在无效或已停用商品");
        return products;
    }

    private InvAdjustment lockAdjustment(Long id, Long enterpriseId) {
        InvAdjustment adjustment = baseMapper.selectForUpdate(id, enterpriseId);
        if (adjustment == null) throw new BusinessException("库存调整单不存在");
        return adjustment;
    }

    private OrgWarehouse validateWarehouse(Long id, Long enterpriseId) {
        OrgWarehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null || !Objects.equals(warehouse.getEnterpriseId(), enterpriseId)
                || !"ENABLED".equals(warehouse.getStatus())) {
            throw new BusinessException("仓库不存在或已停用");
        }
        return warehouse;
    }

    private void enrichWarehouses(List<InvAdjustment> adjustments) {
        List<Long> ids = adjustments.stream().map(InvAdjustment::getWarehouseId)
                .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return;
        Map<Long, String> names = warehouseMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(OrgWarehouse::getId, OrgWarehouse::getWarehouseName));
        adjustments.forEach(adjustment -> adjustment.setWarehouseName(names.get(adjustment.getWarehouseId())));
    }

    private void enrichUnits(List<InvAdjustmentItem> items) {
        List<Long> ids = items.stream().map(InvAdjustmentItem::getUnitId)
                .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return;
        Map<Long, String> names = unitMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(MdUnit::getId, MdUnit::getUnitName));
        items.forEach(item -> item.setUnitName(names.get(item.getUnitId())));
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
