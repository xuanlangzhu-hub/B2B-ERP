package com.erp.inventory.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.inventory.dto.InvCountCreateRequest;
import com.erp.inventory.dto.InvCountItemRequest;
import com.erp.inventory.dto.InvCountUpdateRequest;
import com.erp.inventory.entity.InvCount;
import com.erp.inventory.entity.InvCountItem;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.entity.InvStockMovement;
import com.erp.inventory.mapper.InvCountItemMapper;
import com.erp.inventory.mapper.InvCountMapper;
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
import java.util.Collections;
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
public class InvCountService extends ServiceImpl<InvCountMapper, InvCount> {

    private static final Set<String> ACTIVE_STATUSES = Set.of("DRAFT", "COUNTING", "APPROVED");

    private final InvCountItemMapper countItemMapper;
    private final InvStockBalanceMapper stockBalanceMapper;
    private final InvStockMovementMapper stockMovementMapper;
    private final OrgWarehouseMapper warehouseMapper;
    private final MdProductMapper productMapper;
    private final MdUnitMapper unitMapper;

    public PageResult<InvCount> pageCounts(Long enterpriseId, Integer page, Integer size,
                                            String countNo, Long warehouseId, String status,
                                            String startDate, String endDate) {
        LambdaQueryWrapper<InvCount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvCount::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(countNo), InvCount::getCountNo, countNo)
                .eq(warehouseId != null, InvCount::getWarehouseId, warehouseId)
                .eq(StrUtil.isNotBlank(status), InvCount::getStatus, status)
                .ge(StrUtil.isNotBlank(startDate), InvCount::getCountDate, startDate)
                .le(StrUtil.isNotBlank(endDate), InvCount::getCountDate, endDate)
                .orderByDesc(InvCount::getCreatedAt);
        Page<InvCount> result = page(new Page<>(page, size), wrapper);
        enrichWarehouses(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public InvCount getDetail(Long id, Long enterpriseId) {
        InvCount count = lambdaQuery().eq(InvCount::getId, id)
                .eq(InvCount::getEnterpriseId, enterpriseId).one();
        if (count == null) {
            throw new BusinessException("盘点单不存在");
        }
        List<InvCountItem> items = countItemMapper.selectList(
                new LambdaQueryWrapper<InvCountItem>()
                        .eq(InvCountItem::getCountId, id)
                        .orderByAsc(InvCountItem::getLineNo));
        enrichUnits(items);
        count.setItems(items);
        enrichWarehouses(Collections.singletonList(count));
        return count;
    }

    @Transactional
    public InvCount createCount(InvCountCreateRequest request, Long enterpriseId, Long operatorId) {
        OrgWarehouse warehouse = validateWarehouse(request.getWarehouseId(), enterpriseId);
        long activeCount = lambdaQuery()
                .eq(InvCount::getEnterpriseId, enterpriseId)
                .eq(InvCount::getWarehouseId, request.getWarehouseId())
                .in(InvCount::getStatus, ACTIVE_STATUSES)
                .count();
        if (activeCount > 0) {
            throw new BusinessException("该仓库已有进行中的盘点单");
        }

        List<InvStockBalance> stocks = stockBalanceMapper.selectList(
                new LambdaQueryWrapper<InvStockBalance>()
                        .eq(InvStockBalance::getEnterpriseId, enterpriseId)
                        .eq(InvStockBalance::getWarehouseId, request.getWarehouseId())
                        .orderByAsc(InvStockBalance::getProductId));
        if (stocks.isEmpty()) {
            throw new BusinessException("该仓库暂无库存，不能创建盘点单");
        }

        List<Long> productIds = stocks.stream().map(InvStockBalance::getProductId).distinct().toList();
        Map<Long, MdProduct> productMap = productMapper.selectList(
                        new LambdaQueryWrapper<MdProduct>()
                                .eq(MdProduct::getEnterpriseId, enterpriseId)
                                .in(MdProduct::getId, productIds))
                .stream().collect(Collectors.toMap(MdProduct::getId, Function.identity()));
        if (productMap.size() != productIds.size()) {
            throw new BusinessException("库存中存在已失效商品，请先修复商品资料");
        }

        BigDecimal totalBook = stocks.stream()
                .map(stock -> zeroIfNull(stock.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        InvCount count = new InvCount()
                .setEnterpriseId(enterpriseId)
                .setCountNo("PD" + System.currentTimeMillis())
                .setCountDate(request.getCountDate())
                .setWarehouseId(request.getWarehouseId())
                .setStatus("DRAFT")
                .setTotalBookQuantity(totalBook)
                .setTotalActualQuantity(totalBook)
                .setTotalDiffQuantity(BigDecimal.ZERO.setScale(4))
                .setRemark(request.getRemark())
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);
        save(count);

        int lineNo = 1;
        for (InvStockBalance stock : stocks) {
            MdProduct product = productMap.get(stock.getProductId());
            BigDecimal bookQuantity = zeroIfNull(stock.getQuantity()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal unitCost = zeroIfNull(stock.getAvgCostPrice()).setScale(4, RoundingMode.HALF_UP);
            countItemMapper.insert(new InvCountItem()
                    .setCountId(count.getId())
                    .setLineNo(lineNo++)
                    .setProductId(product.getId())
                    .setProductCode(product.getProductCode())
                    .setProductName(product.getProductName())
                    .setUnitId(product.getUnitId())
                    .setBookQuantity(bookQuantity)
                    .setActualQuantity(bookQuantity)
                    .setDiffQuantity(BigDecimal.ZERO.setScale(4))
                    .setUnitCost(unitCost)
                    .setDiffAmount(BigDecimal.ZERO.setScale(2)));
        }
        count.setWarehouseName(warehouse.getWarehouseName());
        return getDetail(count.getId(), enterpriseId);
    }

    @Transactional
    public void startCount(Long id, Long enterpriseId, Long operatorId) {
        InvCount count = lockCount(id, enterpriseId);
        if (!"DRAFT".equals(count.getStatus())) {
            throw new BusinessException("只有草稿状态的盘点单可以开始盘点");
        }
        count.setStatus("COUNTING").setUpdatedBy(operatorId);
        updateById(count);
    }

    @Transactional
    public InvCount updateActualQuantities(Long id, InvCountUpdateRequest request,
                                            Long enterpriseId, Long operatorId) {
        InvCount count = lockCount(id, enterpriseId);
        if (!"DRAFT".equals(count.getStatus()) && !"COUNTING".equals(count.getStatus())) {
            throw new BusinessException("当前状态不允许修改实盘数量");
        }
        List<InvCountItem> existingItems = countItemMapper.selectList(
                new LambdaQueryWrapper<InvCountItem>()
                        .eq(InvCountItem::getCountId, id)
                        .orderByAsc(InvCountItem::getLineNo));
        if (existingItems.isEmpty()) {
            throw new BusinessException("盘点单没有明细");
        }

        Set<Long> requestIds = new HashSet<>();
        Map<Long, InvCountItemRequest> requestMap = new HashMap<>();
        for (InvCountItemRequest item : request.getItems()) {
            if (!requestIds.add(item.getId())) {
                throw new BusinessException("盘点明细不能重复提交");
            }
            requestMap.put(item.getId(), item);
        }
        if (requestMap.size() != existingItems.size()
                || existingItems.stream().anyMatch(item -> !requestMap.containsKey(item.getId()))) {
            throw new BusinessException("必须提交该盘点单的全部明细");
        }

        BigDecimal totalBook = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        BigDecimal totalDiff = BigDecimal.ZERO;
        for (InvCountItem existing : existingItems) {
            InvCountItemRequest submitted = requestMap.get(existing.getId());
            BigDecimal actual = submitted.getActualQuantity().setScale(4, RoundingMode.HALF_UP);
            if (actual.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("实盘数量不能小于0");
            }
            BigDecimal book = zeroIfNull(existing.getBookQuantity()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal diff = actual.subtract(book).setScale(4, RoundingMode.HALF_UP);
            BigDecimal diffAmount = diff.multiply(zeroIfNull(existing.getUnitCost()))
                    .setScale(2, RoundingMode.HALF_UP);
            existing.setActualQuantity(actual)
                    .setDiffQuantity(diff)
                    .setDiffAmount(diffAmount)
                    .setReason(submitted.getReason());
            countItemMapper.updateById(existing);
            totalBook = totalBook.add(book);
            totalActual = totalActual.add(actual);
            totalDiff = totalDiff.add(diff);
        }

        count.setStatus("COUNTING")
                .setTotalBookQuantity(totalBook)
                .setTotalActualQuantity(totalActual)
                .setTotalDiffQuantity(totalDiff)
                .setRemark(request.getRemark())
                .setUpdatedBy(operatorId);
        updateById(count);
        return getDetail(id, enterpriseId);
    }

    @Transactional
    public void submitCount(Long id, Long enterpriseId, Long operatorId) {
        InvCount count = lockCount(id, enterpriseId);
        if (!"COUNTING".equals(count.getStatus())) {
            throw new BusinessException("只有盘点中状态的单据可以提交审核");
        }
        long itemCount = countItemMapper.selectCount(
                new LambdaQueryWrapper<InvCountItem>().eq(InvCountItem::getCountId, id));
        if (itemCount == 0) {
            throw new BusinessException("盘点单没有明细");
        }
        count.setStatus("APPROVED").setUpdatedBy(operatorId);
        updateById(count);
    }

    @Transactional
    public void approveCount(Long id, Long enterpriseId, Long operatorId) {
        InvCount count = lockCount(id, enterpriseId);
        if (!"APPROVED".equals(count.getStatus())) {
            throw new BusinessException("只有待审核状态的盘点单可以审核入账");
        }
        OrgWarehouse warehouse = validateWarehouse(count.getWarehouseId(), enterpriseId);
        List<InvCountItem> items = countItemMapper.selectList(
                new LambdaQueryWrapper<InvCountItem>()
                        .eq(InvCountItem::getCountId, id)
                        .orderByAsc(InvCountItem::getLineNo));
        if (items.isEmpty()) {
            throw new BusinessException("盘点单没有明细");
        }

        Map<Long, InvStockBalance> lockedStocks = new HashMap<>();
        for (InvCountItem item : items) {
            InvStockBalance stock = stockBalanceMapper.selectForUpdate(
                    enterpriseId, count.getWarehouseId(), item.getProductId());
            if (stock == null) {
                throw new BusinessException("商品 " + item.getProductCode() + " 的库存记录不存在，请重新盘点");
            }
            BigDecimal current = zeroIfNull(stock.getQuantity()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal snapshot = zeroIfNull(item.getBookQuantity()).setScale(4, RoundingMode.HALF_UP);
            if (current.compareTo(snapshot) != 0) {
                throw new BusinessException("商品 " + item.getProductCode() + " 的账面库存已变化，请取消后重新盘点");
            }
            BigDecimal locked = zeroIfNull(stock.getLockedQuantity());
            if (item.getActualQuantity().compareTo(locked) < 0) {
                throw new BusinessException("商品 " + item.getProductCode() + " 的实盘数量不能小于锁定数量");
            }
            lockedStocks.put(item.getProductId(), stock);
        }

        for (InvCountItem item : items) {
            BigDecimal diff = zeroIfNull(item.getDiffQuantity());
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            InvStockBalance stock = lockedStocks.get(item.getProductId());
            BigDecimal before = zeroIfNull(stock.getQuantity());
            BigDecimal after = item.getActualQuantity();
            BigDecimal unitCost = zeroIfNull(stock.getAvgCostPrice()).setScale(4, RoundingMode.HALF_UP);
            stock.setQuantity(after)
                    .setAvailableQuantity(after.subtract(zeroIfNull(stock.getLockedQuantity())))
                    .setStockAmount(after.multiply(unitCost).setScale(2, RoundingMode.HALF_UP))
                    .setLastMovementAt(LocalDateTime.now());
            if (stockBalanceMapper.updateById(stock) != 1) {
                throw new BusinessException("库存已被其他操作修改，请重试");
            }

            stockMovementMapper.insert(new InvStockMovement()
                    .setEnterpriseId(enterpriseId)
                    .setStoreId(warehouse.getStoreId())
                    .setWarehouseId(count.getWarehouseId())
                    .setProductId(item.getProductId())
                    .setMovementNo("SMPD" + System.currentTimeMillis() + "_" + item.getLineNo())
                    .setMovementType("COUNT")
                    .setDirection(diff.compareTo(BigDecimal.ZERO) > 0 ? "IN" : "OUT")
                    .setQuantity(diff.abs())
                    .setUnitCost(unitCost)
                    .setAmount(diff.abs().multiply(unitCost).setScale(2, RoundingMode.HALF_UP))
                    .setBeforeQuantity(before)
                    .setAfterQuantity(after)
                    .setSourceType("INVENTORY_COUNT")
                    .setSourceId(count.getId())
                    .setSourceNo(count.getCountNo())
                    .setSourceItemId(item.getId())
                    .setBusinessDate(count.getCountDate())
                    .setOperatorId(operatorId)
                    .setRemark(StrUtil.blankToDefault(item.getReason(), "库存盘点差异")));
        }

        count.setStatus("COMPLETED")
                .setApprovedBy(operatorId)
                .setApprovedAt(LocalDateTime.now())
                .setUpdatedBy(operatorId);
        updateById(count);
    }

    @Transactional
    public void cancelCount(Long id, Long enterpriseId, Long operatorId) {
        InvCount count = lockCount(id, enterpriseId);
        if ("CANCELLED".equals(count.getStatus())) {
            return;
        }
        if ("COMPLETED".equals(count.getStatus())) {
            throw new BusinessException("已完成的盘点单不能取消");
        }
        count.setStatus("CANCELLED").setUpdatedBy(operatorId);
        updateById(count);
    }

    @Transactional
    public void deleteCount(Long id, Long enterpriseId) {
        InvCount count = lockCount(id, enterpriseId);
        if (!"DRAFT".equals(count.getStatus()) && !"CANCELLED".equals(count.getStatus())) {
            throw new BusinessException("只有草稿或已取消的盘点单可以删除");
        }
        countItemMapper.delete(new LambdaQueryWrapper<InvCountItem>().eq(InvCountItem::getCountId, id));
        removeById(id);
    }

    private InvCount lockCount(Long id, Long enterpriseId) {
        InvCount count = baseMapper.selectForUpdate(id, enterpriseId);
        if (count == null) {
            throw new BusinessException("盘点单不存在");
        }
        return count;
    }

    private OrgWarehouse validateWarehouse(Long warehouseId, Long enterpriseId) {
        OrgWarehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null || !Objects.equals(warehouse.getEnterpriseId(), enterpriseId)
                || !"ENABLED".equals(warehouse.getStatus())) {
            throw new BusinessException("仓库不存在或已停用");
        }
        return warehouse;
    }

    private void enrichWarehouses(List<InvCount> counts) {
        List<Long> warehouseIds = counts.stream().map(InvCount::getWarehouseId)
                .filter(Objects::nonNull).distinct().toList();
        if (warehouseIds.isEmpty()) {
            return;
        }
        Map<Long, String> warehouseNames = warehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(OrgWarehouse::getId, OrgWarehouse::getWarehouseName));
        counts.forEach(count -> count.setWarehouseName(warehouseNames.get(count.getWarehouseId())));
    }

    private void enrichUnits(List<InvCountItem> items) {
        List<Long> unitIds = items.stream().map(InvCountItem::getUnitId)
                .filter(Objects::nonNull).distinct().toList();
        if (unitIds.isEmpty()) {
            return;
        }
        Map<Long, String> unitNames = unitMapper.selectBatchIds(unitIds).stream()
                .collect(Collectors.toMap(MdUnit::getId, MdUnit::getUnitName));
        items.forEach(item -> item.setUnitName(unitNames.get(item.getUnitId())));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
