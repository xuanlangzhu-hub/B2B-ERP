package com.erp.inventory.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.inventory.dto.InvTransferItemRequest;
import com.erp.inventory.dto.InvTransferRequest;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.entity.InvStockMovement;
import com.erp.inventory.entity.InvTransfer;
import com.erp.inventory.entity.InvTransferItem;
import com.erp.inventory.mapper.InvStockBalanceMapper;
import com.erp.inventory.mapper.InvStockMovementMapper;
import com.erp.inventory.mapper.InvTransferItemMapper;
import com.erp.inventory.mapper.InvTransferMapper;
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
public class InvTransferService extends ServiceImpl<InvTransferMapper, InvTransfer> {

    private final InvTransferItemMapper transferItemMapper;
    private final InvStockBalanceMapper stockBalanceMapper;
    private final InvStockMovementMapper stockMovementMapper;
    private final OrgWarehouseMapper warehouseMapper;
    private final MdProductMapper productMapper;
    private final MdUnitMapper unitMapper;

    public PageResult<InvTransfer> pageTransfers(Long enterpriseId, Integer page, Integer size,
                                                  String transferNo, Long warehouseId, String status,
                                                  String startDate, String endDate) {
        LambdaQueryWrapper<InvTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvTransfer::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(transferNo), InvTransfer::getTransferNo, transferNo)
                .and(warehouseId != null, w -> w.eq(InvTransfer::getFromWarehouseId, warehouseId)
                        .or().eq(InvTransfer::getToWarehouseId, warehouseId))
                .eq(StrUtil.isNotBlank(status), InvTransfer::getStatus, status)
                .ge(StrUtil.isNotBlank(startDate), InvTransfer::getTransferDate, startDate)
                .le(StrUtil.isNotBlank(endDate), InvTransfer::getTransferDate, endDate)
                .orderByDesc(InvTransfer::getCreatedAt);
        Page<InvTransfer> result = page(new Page<>(page, size), wrapper);
        enrichWarehouses(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public InvTransfer getDetail(Long id, Long enterpriseId) {
        InvTransfer transfer = lambdaQuery().eq(InvTransfer::getId, id)
                .eq(InvTransfer::getEnterpriseId, enterpriseId).one();
        if (transfer == null) {
            throw new BusinessException("调拨单不存在");
        }
        List<InvTransferItem> items = transferItemMapper.selectList(
                new LambdaQueryWrapper<InvTransferItem>()
                        .eq(InvTransferItem::getTransferId, id)
                        .orderByAsc(InvTransferItem::getLineNo));
        enrichUnits(items);
        transfer.setItems(items);
        enrichWarehouses(Collections.singletonList(transfer));
        return transfer;
    }

    @Transactional
    public InvTransfer createTransfer(InvTransferRequest request, Long enterpriseId, Long operatorId) {
        validateRequest(request, enterpriseId);
        Map<Long, MdProduct> productMap = loadProducts(request.getItems(), enterpriseId);
        BigDecimal totalQuantity = request.getItems().stream()
                .map(InvTransferItemRequest::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        InvTransfer transfer = new InvTransfer()
                .setEnterpriseId(enterpriseId)
                .setTransferNo("DB" + System.currentTimeMillis())
                .setTransferDate(request.getTransferDate())
                .setFromWarehouseId(request.getFromWarehouseId())
                .setToWarehouseId(request.getToWarehouseId())
                .setStatus("DRAFT")
                .setTotalQuantity(totalQuantity)
                .setRemark(request.getRemark())
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);
        save(transfer);

        int lineNo = 1;
        for (InvTransferItemRequest submitted : request.getItems()) {
            MdProduct product = productMap.get(submitted.getProductId());
            transferItemMapper.insert(new InvTransferItem()
                    .setTransferId(transfer.getId())
                    .setLineNo(lineNo++)
                    .setProductId(product.getId())
                    .setProductCode(product.getProductCode())
                    .setProductName(product.getProductName())
                    .setUnitId(product.getUnitId())
                    .setQuantity(submitted.getQuantity().setScale(4, RoundingMode.HALF_UP))
                    .setOutboundQuantity(BigDecimal.ZERO.setScale(4))
                    .setInboundQuantity(BigDecimal.ZERO.setScale(4))
                    .setRemark(submitted.getRemark()));
        }
        return getDetail(transfer.getId(), enterpriseId);
    }

    @Transactional
    public void approveTransfer(Long id, Long enterpriseId, Long operatorId) {
        InvTransfer transfer = lockTransfer(id, enterpriseId);
        if (!"DRAFT".equals(transfer.getStatus())) {
            throw new BusinessException("只有草稿状态的调拨单可以审核");
        }
        List<InvTransferItem> items = transferItemMapper.selectList(
                new LambdaQueryWrapper<InvTransferItem>().eq(InvTransferItem::getTransferId, id));
        if (items.isEmpty()) {
            throw new BusinessException("调拨单没有明细");
        }
        for (InvTransferItem item : items) {
            InvStockBalance stock = stockBalanceMapper.selectOne(new LambdaQueryWrapper<InvStockBalance>()
                    .eq(InvStockBalance::getEnterpriseId, enterpriseId)
                    .eq(InvStockBalance::getWarehouseId, transfer.getFromWarehouseId())
                    .eq(InvStockBalance::getProductId, item.getProductId()));
            if (stock == null || zero(stock.getAvailableQuantity()).compareTo(item.getQuantity()) < 0) {
                throw new BusinessException("调出仓库存不足，商品: " + item.getProductCode() + " - " + item.getProductName());
            }
        }
        transfer.setStatus("APPROVED")
                .setApprovedBy(operatorId)
                .setApprovedAt(LocalDateTime.now())
                .setUpdatedBy(operatorId);
        updateById(transfer);
    }

    @Transactional
    public void completeTransfer(Long id, Long enterpriseId, Long operatorId) {
        InvTransfer transfer = lockTransfer(id, enterpriseId);
        if (!"APPROVED".equals(transfer.getStatus())) {
            throw new BusinessException("只有已审核状态的调拨单可以执行调拨");
        }
        OrgWarehouse fromWarehouse = validateWarehouse(transfer.getFromWarehouseId(), enterpriseId);
        OrgWarehouse toWarehouse = validateWarehouse(transfer.getToWarehouseId(), enterpriseId);
        List<InvTransferItem> items = transferItemMapper.selectList(
                new LambdaQueryWrapper<InvTransferItem>()
                        .eq(InvTransferItem::getTransferId, id));
        if (items.isEmpty()) {
            throw new BusinessException("调拨单没有明细");
        }
        items = new ArrayList<>(items);
        items.sort(Comparator.comparing(InvTransferItem::getProductId));

        Map<Long, InvStockBalance> sourceStocks = new HashMap<>();
        Map<Long, InvStockBalance> targetStocks = new HashMap<>();
        long firstWarehouseId = Math.min(transfer.getFromWarehouseId(), transfer.getToWarehouseId());
        long secondWarehouseId = Math.max(transfer.getFromWarehouseId(), transfer.getToWarehouseId());
        for (InvTransferItem item : items) {
            InvStockBalance first = stockBalanceMapper.selectForUpdate(
                    enterpriseId, firstWarehouseId, item.getProductId());
            InvStockBalance second = stockBalanceMapper.selectForUpdate(
                    enterpriseId, secondWarehouseId, item.getProductId());
            InvStockBalance source = transfer.getFromWarehouseId().equals(firstWarehouseId) ? first : second;
            InvStockBalance target = transfer.getToWarehouseId().equals(firstWarehouseId) ? first : second;
            if (source == null || zero(source.getAvailableQuantity()).compareTo(item.getQuantity()) < 0) {
                throw new BusinessException("调出仓库存不足，商品: " + item.getProductCode() + " - " + item.getProductName());
            }
            sourceStocks.put(item.getProductId(), source);
            targetStocks.put(item.getProductId(), target);
        }

        for (InvTransferItem item : items) {
            InvStockBalance source = sourceStocks.get(item.getProductId());
            InvStockBalance target = targetStocks.get(item.getProductId());
            BigDecimal quantity = item.getQuantity();
            BigDecimal unitCost = zero(source.getAvgCostPrice()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal transferAmount = quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);

            BigDecimal sourceBefore = zero(source.getQuantity());
            BigDecimal sourceAfter = sourceBefore.subtract(quantity);
            source.setQuantity(sourceAfter)
                    .setAvailableQuantity(sourceAfter.subtract(zero(source.getLockedQuantity())))
                    .setStockAmount(zero(source.getStockAmount()).subtract(transferAmount).max(BigDecimal.ZERO))
                    .setLastMovementAt(LocalDateTime.now());
            if (stockBalanceMapper.updateById(source) != 1) {
                throw new BusinessException("调出仓库存已变化，请重试");
            }

            BigDecimal targetBefore = target == null ? BigDecimal.ZERO : zero(target.getQuantity());
            BigDecimal targetAfter = targetBefore.add(quantity);
            if (target == null) {
                target = new InvStockBalance()
                        .setEnterpriseId(enterpriseId)
                        .setWarehouseId(transfer.getToWarehouseId())
                        .setProductId(item.getProductId())
                        .setQuantity(quantity)
                        .setLockedQuantity(BigDecimal.ZERO)
                        .setAvailableQuantity(quantity)
                        .setAvgCostPrice(unitCost)
                        .setStockAmount(transferAmount)
                        .setLastMovementAt(LocalDateTime.now())
                        .setVersion(0);
                stockBalanceMapper.insert(target);
            } else {
                BigDecimal targetAmount = zero(target.getStockAmount()).add(transferAmount);
                BigDecimal targetAvgCost = targetAfter.compareTo(BigDecimal.ZERO) > 0
                        ? targetAmount.divide(targetAfter, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                target.setQuantity(targetAfter)
                        .setAvailableQuantity(targetAfter.subtract(zero(target.getLockedQuantity())))
                        .setAvgCostPrice(targetAvgCost)
                        .setStockAmount(targetAmount)
                        .setLastMovementAt(LocalDateTime.now());
                if (stockBalanceMapper.updateById(target) != 1) {
                    throw new BusinessException("调入仓库存已变化，请重试");
                }
            }

            String movementBase = "SMDB" + System.currentTimeMillis() + "_" + item.getLineNo();
            stockMovementMapper.insert(new InvStockMovement()
                    .setEnterpriseId(enterpriseId)
                    .setStoreId(fromWarehouse.getStoreId())
                    .setWarehouseId(transfer.getFromWarehouseId())
                    .setProductId(item.getProductId())
                    .setMovementNo(movementBase + "O")
                    .setMovementType("TRANSFER_OUT")
                    .setDirection("OUT")
                    .setQuantity(quantity)
                    .setUnitCost(unitCost)
                    .setAmount(transferAmount)
                    .setBeforeQuantity(sourceBefore)
                    .setAfterQuantity(sourceAfter)
                    .setSourceType("INVENTORY_TRANSFER")
                    .setSourceId(transfer.getId())
                    .setSourceNo(transfer.getTransferNo())
                    .setSourceItemId(item.getId())
                    .setBusinessDate(transfer.getTransferDate())
                    .setOperatorId(operatorId)
                    .setRemark(item.getRemark()));
            stockMovementMapper.insert(new InvStockMovement()
                    .setEnterpriseId(enterpriseId)
                    .setStoreId(toWarehouse.getStoreId())
                    .setWarehouseId(transfer.getToWarehouseId())
                    .setProductId(item.getProductId())
                    .setMovementNo(movementBase + "I")
                    .setMovementType("TRANSFER_IN")
                    .setDirection("IN")
                    .setQuantity(quantity)
                    .setUnitCost(unitCost)
                    .setAmount(transferAmount)
                    .setBeforeQuantity(targetBefore)
                    .setAfterQuantity(targetAfter)
                    .setSourceType("INVENTORY_TRANSFER")
                    .setSourceId(transfer.getId())
                    .setSourceNo(transfer.getTransferNo())
                    .setSourceItemId(item.getId())
                    .setBusinessDate(transfer.getTransferDate())
                    .setOperatorId(operatorId)
                    .setRemark(item.getRemark()));

            item.setOutboundQuantity(quantity).setInboundQuantity(quantity);
            transferItemMapper.updateById(item);
        }

        transfer.setStatus("COMPLETED")
                .setCompletedAt(LocalDateTime.now())
                .setUpdatedBy(operatorId);
        updateById(transfer);
    }

    @Transactional
    public void cancelTransfer(Long id, Long enterpriseId, Long operatorId) {
        InvTransfer transfer = lockTransfer(id, enterpriseId);
        if ("CANCELLED".equals(transfer.getStatus())) {
            return;
        }
        if ("COMPLETED".equals(transfer.getStatus())) {
            throw new BusinessException("已完成的调拨单不能取消");
        }
        transfer.setStatus("CANCELLED").setUpdatedBy(operatorId);
        updateById(transfer);
    }

    @Transactional
    public void deleteTransfer(Long id, Long enterpriseId) {
        InvTransfer transfer = lockTransfer(id, enterpriseId);
        if (!"DRAFT".equals(transfer.getStatus()) && !"CANCELLED".equals(transfer.getStatus())) {
            throw new BusinessException("只有草稿或已取消的调拨单可以删除");
        }
        transferItemMapper.delete(new LambdaQueryWrapper<InvTransferItem>()
                .eq(InvTransferItem::getTransferId, id));
        removeById(id);
    }

    private void validateRequest(InvTransferRequest request, Long enterpriseId) {
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new BusinessException("调出仓库和调入仓库不能相同");
        }
        validateWarehouse(request.getFromWarehouseId(), enterpriseId);
        validateWarehouse(request.getToWarehouseId(), enterpriseId);
        Set<Long> products = new HashSet<>();
        for (InvTransferItemRequest item : request.getItems()) {
            if (!products.add(item.getProductId())) {
                throw new BusinessException("同一商品不能重复添加");
            }
        }
    }

    private Map<Long, MdProduct> loadProducts(List<InvTransferItemRequest> items, Long enterpriseId) {
        List<Long> ids = items.stream().map(InvTransferItemRequest::getProductId).distinct().toList();
        Map<Long, MdProduct> products = productMapper.selectList(new LambdaQueryWrapper<MdProduct>()
                        .eq(MdProduct::getEnterpriseId, enterpriseId)
                        .eq(MdProduct::getStatus, "ENABLED")
                        .in(MdProduct::getId, ids))
                .stream().collect(Collectors.toMap(MdProduct::getId, Function.identity()));
        if (products.size() != ids.size()) {
            throw new BusinessException("调拨明细中存在无效或已停用商品");
        }
        return products;
    }

    private InvTransfer lockTransfer(Long id, Long enterpriseId) {
        InvTransfer transfer = baseMapper.selectForUpdate(id, enterpriseId);
        if (transfer == null) {
            throw new BusinessException("调拨单不存在");
        }
        return transfer;
    }

    private OrgWarehouse validateWarehouse(Long id, Long enterpriseId) {
        OrgWarehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null || !Objects.equals(warehouse.getEnterpriseId(), enterpriseId)
                || !"ENABLED".equals(warehouse.getStatus())) {
            throw new BusinessException("仓库不存在或已停用");
        }
        return warehouse;
    }

    private void enrichWarehouses(List<InvTransfer> transfers) {
        List<Long> ids = transfers.stream()
                .flatMap(transfer -> java.util.stream.Stream.of(
                        transfer.getFromWarehouseId(), transfer.getToWarehouseId()))
                .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return;
        Map<Long, String> names = warehouseMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(OrgWarehouse::getId, OrgWarehouse::getWarehouseName));
        transfers.forEach(transfer -> {
            transfer.setFromWarehouseName(names.get(transfer.getFromWarehouseId()));
            transfer.setToWarehouseName(names.get(transfer.getToWarehouseId()));
        });
    }

    private void enrichUnits(List<InvTransferItem> items) {
        List<Long> ids = items.stream().map(InvTransferItem::getUnitId)
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
