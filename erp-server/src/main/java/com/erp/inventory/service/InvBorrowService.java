package com.erp.inventory.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.inventory.dto.InvBorrowItemRequest;
import com.erp.inventory.dto.InvBorrowRequest;
import com.erp.inventory.dto.InvBorrowReturnItemRequest;
import com.erp.inventory.dto.InvBorrowReturnRequest;
import com.erp.inventory.entity.InvBorrow;
import com.erp.inventory.entity.InvBorrowItem;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.entity.InvStockMovement;
import com.erp.inventory.mapper.InvBorrowItemMapper;
import com.erp.inventory.mapper.InvBorrowMapper;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvBorrowService extends ServiceImpl<InvBorrowMapper, InvBorrow> {

    private final InvBorrowItemMapper borrowItemMapper;
    private final InvStockBalanceMapper stockBalanceMapper;
    private final InvStockMovementMapper stockMovementMapper;
    private final OrgWarehouseMapper warehouseMapper;
    private final MdProductMapper productMapper;
    private final MdUnitMapper unitMapper;

    public PageResult<InvBorrow> pageBorrows(Long enterpriseId, Integer page, Integer size,
                                               String borrowNo, String borrowType, Long warehouseId,
                                               String partnerName, String status, Boolean overdueOnly) {
        LambdaQueryWrapper<InvBorrow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvBorrow::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(borrowNo), InvBorrow::getBorrowNo, borrowNo)
                .eq(StrUtil.isNotBlank(borrowType), InvBorrow::getBorrowType, borrowType)
                .eq(warehouseId != null, InvBorrow::getWarehouseId, warehouseId)
                .like(StrUtil.isNotBlank(partnerName), InvBorrow::getPartnerName, partnerName)
                .eq(StrUtil.isNotBlank(status), InvBorrow::getStatus, status)
                .and(Boolean.TRUE.equals(overdueOnly), condition -> condition
                        .lt(InvBorrow::getExpectedReturnDate, LocalDate.now())
                        .in(InvBorrow::getStatus, "APPROVED", "PARTIALLY_RETURNED"))
                .orderByDesc(InvBorrow::getCreatedAt);
        Page<InvBorrow> result = page(new Page<>(page, size), wrapper);
        enrichBorrowHeaders(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public InvBorrow getDetail(Long id, Long enterpriseId) {
        InvBorrow borrow = lambdaQuery().eq(InvBorrow::getId, id)
                .eq(InvBorrow::getEnterpriseId, enterpriseId).one();
        if (borrow == null) throw new BusinessException("借用单不存在");
        List<InvBorrowItem> items = loadItems(id);
        enrichUnits(items);
        BigDecimal returned = BigDecimal.ZERO;
        for (InvBorrowItem item : items) {
            BigDecimal returnedQuantity = zero(item.getReturnedQuantity());
            item.setRemainingQuantity(item.getQuantity().subtract(returnedQuantity));
            returned = returned.add(returnedQuantity);
        }
        borrow.setItems(items)
                .setReturnedQuantity(returned)
                .setRemainingQuantity(zero(borrow.getTotalQuantity()).subtract(returned));
        enrichBorrowHeaders(Collections.singletonList(borrow));
        return borrow;
    }

    @Transactional
    public InvBorrow createBorrow(InvBorrowRequest request, Long enterpriseId, Long operatorId) {
        validateDates(request.getBorrowDate(), request.getExpectedReturnDate());
        validateWarehouse(request.getWarehouseId(), enterpriseId);
        Map<Long, MdProduct> products = validateAndLoadProducts(request.getItems(), enterpriseId);
        List<Long> productIds = request.getItems().stream().map(InvBorrowItemRequest::getProductId).toList();
        Map<Long, InvStockBalance> stocks = stockBalanceMapper.selectList(
                        new LambdaQueryWrapper<InvStockBalance>()
                                .eq(InvStockBalance::getEnterpriseId, enterpriseId)
                                .eq(InvStockBalance::getWarehouseId, request.getWarehouseId())
                                .in(InvStockBalance::getProductId, productIds))
                .stream().collect(Collectors.toMap(InvStockBalance::getProductId, Function.identity()));

        InvBorrow borrow = new InvBorrow()
                .setEnterpriseId(enterpriseId)
                .setBorrowNo(("BORROW_OUT".equals(request.getBorrowType()) ? "JC" : "JR") + System.currentTimeMillis())
                .setBorrowType(request.getBorrowType())
                .setBorrowDate(request.getBorrowDate())
                .setExpectedReturnDate(request.getExpectedReturnDate())
                .setWarehouseId(request.getWarehouseId())
                .setPartnerType(request.getPartnerType())
                .setPartnerId(request.getPartnerId())
                .setPartnerName(request.getPartnerName().trim())
                .setStatus("DRAFT")
                .setTotalQuantity(BigDecimal.ZERO)
                .setRemark(request.getRemark())
                .setCreatedBy(operatorId)
                .setUpdatedBy(operatorId);
        save(borrow);

        BigDecimal total = BigDecimal.ZERO;
        int lineNo = 1;
        for (InvBorrowItemRequest submitted : request.getItems()) {
            MdProduct product = products.get(submitted.getProductId());
            InvStockBalance stock = stocks.get(submitted.getProductId());
            BigDecimal quantity = submitted.getQuantity().setScale(4, RoundingMode.HALF_UP);
            BigDecimal unitCost;
            if ("BORROW_OUT".equals(request.getBorrowType()) && stock != null) {
                unitCost = zero(stock.getAvgCostPrice());
            } else if (submitted.getUnitCost() != null) {
                unitCost = submitted.getUnitCost();
            } else if (stock != null) {
                unitCost = zero(stock.getAvgCostPrice());
            } else {
                unitCost = zero(product.getCostPrice());
            }
            borrowItemMapper.insert(new InvBorrowItem()
                    .setBorrowId(borrow.getId())
                    .setLineNo(lineNo++)
                    .setProductId(product.getId())
                    .setProductCode(product.getProductCode())
                    .setProductName(product.getProductName())
                    .setUnitId(product.getUnitId())
                    .setQuantity(quantity)
                    .setReturnedQuantity(BigDecimal.ZERO)
                    .setUnitCost(unitCost.setScale(4, RoundingMode.HALF_UP))
                    .setRemark(submitted.getRemark()));
            total = total.add(quantity);
        }
        borrow.setTotalQuantity(total);
        updateById(borrow);
        return getDetail(borrow.getId(), enterpriseId);
    }

    @Transactional
    public void approveBorrow(Long id, Long enterpriseId, Long operatorId) {
        InvBorrow borrow = lockBorrow(id, enterpriseId);
        if (!"DRAFT".equals(borrow.getStatus())) {
            throw new BusinessException("只有草稿状态的借用单可以审核");
        }
        OrgWarehouse warehouse = validateWarehouse(borrow.getWarehouseId(), enterpriseId);
        List<InvBorrowItem> items = loadItems(id);
        if (items.isEmpty()) throw new BusinessException("借用单没有明细");
        items.sort(Comparator.comparing(InvBorrowItem::getProductId));

        boolean borrowOut = "BORROW_OUT".equals(borrow.getBorrowType());
        Map<Long, InvStockBalance> stocks = new HashMap<>();
        for (InvBorrowItem item : items) {
            InvStockBalance stock = stockBalanceMapper.selectForUpdate(
                    enterpriseId, borrow.getWarehouseId(), item.getProductId());
            if (borrowOut && (stock == null || zero(stock.getAvailableQuantity()).compareTo(item.getQuantity()) < 0)) {
                throw new BusinessException("库存不足，商品: " + item.getProductCode() + " - " + item.getProductName());
            }
            stocks.put(item.getProductId(), stock);
        }

        for (InvBorrowItem item : items) {
            InvStockBalance stock = stocks.get(item.getProductId());
            BigDecimal before = stock == null ? BigDecimal.ZERO : zero(stock.getQuantity());
            BigDecimal unitCost = borrowOut ? zero(stock.getAvgCostPrice()) : zero(item.getUnitCost());
            BigDecimal amount = item.getQuantity().multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal after = borrowOut ? before.subtract(item.getQuantity()) : before.add(item.getQuantity());

            if (stock == null) {
                stock = new InvStockBalance()
                        .setEnterpriseId(enterpriseId)
                        .setWarehouseId(borrow.getWarehouseId())
                        .setProductId(item.getProductId())
                        .setQuantity(after)
                        .setLockedQuantity(BigDecimal.ZERO)
                        .setAvailableQuantity(after)
                        .setAvgCostPrice(unitCost)
                        .setStockAmount(amount)
                        .setLastMovementAt(LocalDateTime.now())
                        .setVersion(0);
                stockBalanceMapper.insert(stock);
            } else if (borrowOut) {
                stock.setQuantity(after)
                        .setAvailableQuantity(after.subtract(zero(stock.getLockedQuantity())))
                        .setStockAmount(zero(stock.getStockAmount()).subtract(amount).max(BigDecimal.ZERO))
                        .setLastMovementAt(LocalDateTime.now());
                requireStockUpdate(stock);
            } else {
                BigDecimal newAmount = zero(stock.getStockAmount()).add(amount);
                stock.setQuantity(after)
                        .setAvailableQuantity(after.subtract(zero(stock.getLockedQuantity())))
                        .setAvgCostPrice(after.signum() == 0 ? BigDecimal.ZERO
                                : newAmount.divide(after, 4, RoundingMode.HALF_UP))
                        .setStockAmount(newAmount)
                        .setLastMovementAt(LocalDateTime.now());
                requireStockUpdate(stock);
            }

            item.setUnitCost(unitCost.setScale(4, RoundingMode.HALF_UP));
            borrowItemMapper.updateById(item);
            insertMovement(borrow, item, warehouse, operatorId, borrow.getBorrowDate(),
                    borrowOut ? "BORROW_OUT" : "BORROW_IN", borrowOut ? "OUT" : "IN",
                    item.getQuantity(), unitCost, before, after, "借用单审核");
        }
        borrow.setStatus("APPROVED")
                .setApprovedBy(operatorId)
                .setApprovedAt(LocalDateTime.now())
                .setUpdatedBy(operatorId);
        updateById(borrow);
    }

    @Transactional
    public void returnBorrow(Long id, InvBorrowReturnRequest request, Long enterpriseId, Long operatorId) {
        InvBorrow borrow = lockBorrow(id, enterpriseId);
        if (!List.of("APPROVED", "PARTIALLY_RETURNED").contains(borrow.getStatus())) {
            throw new BusinessException("只有借用中或部分归还的单据可以归还");
        }
        if (request.getReturnDate().isBefore(borrow.getBorrowDate())) {
            throw new BusinessException("归还日期不能早于借用日期");
        }
        OrgWarehouse warehouse = validateWarehouse(borrow.getWarehouseId(), enterpriseId);
        List<InvBorrowItem> allItems = loadItems(id);
        Map<Long, InvBorrowItem> itemMap = allItems.stream()
                .collect(Collectors.toMap(InvBorrowItem::getId, Function.identity()));
        Set<Long> submittedIds = new HashSet<>();
        List<ReturnLine> lines = new ArrayList<>();
        for (InvBorrowReturnItemRequest submitted : request.getItems()) {
            if (!submittedIds.add(submitted.getItemId())) throw new BusinessException("同一明细不能重复归还");
            InvBorrowItem item = itemMap.get(submitted.getItemId());
            if (item == null) throw new BusinessException("归还明细不属于当前借用单");
            BigDecimal quantity = submitted.getQuantity().setScale(4, RoundingMode.HALF_UP);
            BigDecimal remaining = item.getQuantity().subtract(zero(item.getReturnedQuantity()));
            if (quantity.compareTo(remaining) > 0) {
                throw new BusinessException("归还数量超过未归还数量，商品: " + item.getProductName());
            }
            lines.add(new ReturnLine(item, quantity));
        }
        lines.sort(Comparator.comparing(line -> line.item().getProductId()));

        boolean borrowOut = "BORROW_OUT".equals(borrow.getBorrowType());
        Map<Long, InvStockBalance> stocks = new HashMap<>();
        for (ReturnLine line : lines) {
            InvStockBalance stock = stockBalanceMapper.selectForUpdate(
                    enterpriseId, borrow.getWarehouseId(), line.item().getProductId());
            if (!borrowOut && (stock == null || zero(stock.getAvailableQuantity()).compareTo(line.quantity()) < 0)) {
                throw new BusinessException("库存不足，无法归还借入商品: " + line.item().getProductName());
            }
            stocks.put(line.item().getProductId(), stock);
        }

        for (ReturnLine line : lines) {
            InvBorrowItem item = line.item();
            BigDecimal quantity = line.quantity();
            InvStockBalance stock = stocks.get(item.getProductId());
            BigDecimal before = stock == null ? BigDecimal.ZERO : zero(stock.getQuantity());
            BigDecimal unitCost = borrowOut ? zero(item.getUnitCost()) : zero(stock.getAvgCostPrice());
            BigDecimal amount = quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal after = borrowOut ? before.add(quantity) : before.subtract(quantity);

            if (stock == null) {
                stock = new InvStockBalance()
                        .setEnterpriseId(enterpriseId)
                        .setWarehouseId(borrow.getWarehouseId())
                        .setProductId(item.getProductId())
                        .setQuantity(after)
                        .setLockedQuantity(BigDecimal.ZERO)
                        .setAvailableQuantity(after)
                        .setAvgCostPrice(unitCost)
                        .setStockAmount(amount)
                        .setLastMovementAt(LocalDateTime.now())
                        .setVersion(0);
                stockBalanceMapper.insert(stock);
            } else if (borrowOut) {
                BigDecimal newAmount = zero(stock.getStockAmount()).add(amount);
                stock.setQuantity(after)
                        .setAvailableQuantity(after.subtract(zero(stock.getLockedQuantity())))
                        .setAvgCostPrice(after.signum() == 0 ? BigDecimal.ZERO
                                : newAmount.divide(after, 4, RoundingMode.HALF_UP))
                        .setStockAmount(newAmount)
                        .setLastMovementAt(LocalDateTime.now());
                requireStockUpdate(stock);
            } else {
                stock.setQuantity(after)
                        .setAvailableQuantity(after.subtract(zero(stock.getLockedQuantity())))
                        .setStockAmount(zero(stock.getStockAmount()).subtract(amount).max(BigDecimal.ZERO))
                        .setLastMovementAt(LocalDateTime.now());
                requireStockUpdate(stock);
            }

            item.setReturnedQuantity(zero(item.getReturnedQuantity()).add(quantity));
            borrowItemMapper.updateById(item);
            insertMovement(borrow, item, warehouse, operatorId, request.getReturnDate(),
                    borrowOut ? "BORROW_OUT_RETURN" : "BORROW_IN_RETURN", borrowOut ? "IN" : "OUT",
                    quantity, unitCost, before, after, "借用归还");
        }

        boolean completed = allItems.stream()
                .allMatch(item -> zero(item.getReturnedQuantity()).compareTo(item.getQuantity()) >= 0);
        borrow.setStatus(completed ? "COMPLETED" : "PARTIALLY_RETURNED").setUpdatedBy(operatorId);
        updateById(borrow);
    }

    @Transactional
    public void cancelBorrow(Long id, Long enterpriseId, Long operatorId) {
        InvBorrow borrow = lockBorrow(id, enterpriseId);
        if ("CANCELLED".equals(borrow.getStatus())) return;
        if (!"DRAFT".equals(borrow.getStatus())) throw new BusinessException("只有草稿借用单可以取消");
        borrow.setStatus("CANCELLED").setUpdatedBy(operatorId);
        updateById(borrow);
    }

    @Transactional
    public void deleteBorrow(Long id, Long enterpriseId) {
        InvBorrow borrow = lockBorrow(id, enterpriseId);
        if (!List.of("DRAFT", "CANCELLED").contains(borrow.getStatus())) {
            throw new BusinessException("只有草稿或已取消的借用单可以删除");
        }
        borrowItemMapper.delete(new LambdaQueryWrapper<InvBorrowItem>().eq(InvBorrowItem::getBorrowId, id));
        removeById(id);
    }

    private Map<Long, MdProduct> validateAndLoadProducts(List<InvBorrowItemRequest> items, Long enterpriseId) {
        Set<Long> ids = new HashSet<>();
        for (InvBorrowItemRequest item : items) {
            if (!ids.add(item.getProductId())) throw new BusinessException("同一商品不能重复添加");
        }
        Map<Long, MdProduct> products = productMapper.selectList(new LambdaQueryWrapper<MdProduct>()
                        .eq(MdProduct::getEnterpriseId, enterpriseId)
                        .eq(MdProduct::getStatus, "ENABLED")
                        .in(MdProduct::getId, ids))
                .stream().collect(Collectors.toMap(MdProduct::getId, Function.identity()));
        if (products.size() != ids.size()) throw new BusinessException("借用明细中存在无效或已停用商品");
        return products;
    }

    private void validateDates(LocalDate borrowDate, LocalDate expectedReturnDate) {
        if (expectedReturnDate != null && expectedReturnDate.isBefore(borrowDate)) {
            throw new BusinessException("预计归还日期不能早于借用日期");
        }
    }

    private OrgWarehouse validateWarehouse(Long id, Long enterpriseId) {
        OrgWarehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null || !Objects.equals(warehouse.getEnterpriseId(), enterpriseId)
                || !"ENABLED".equals(warehouse.getStatus())) {
            throw new BusinessException("仓库不存在或已停用");
        }
        return warehouse;
    }

    private InvBorrow lockBorrow(Long id, Long enterpriseId) {
        InvBorrow borrow = baseMapper.selectForUpdate(id, enterpriseId);
        if (borrow == null) throw new BusinessException("借用单不存在");
        return borrow;
    }

    private List<InvBorrowItem> loadItems(Long borrowId) {
        return new ArrayList<>(borrowItemMapper.selectList(new LambdaQueryWrapper<InvBorrowItem>()
                .eq(InvBorrowItem::getBorrowId, borrowId).orderByAsc(InvBorrowItem::getLineNo)));
    }

    private void enrichBorrowHeaders(List<InvBorrow> borrows) {
        List<Long> ids = borrows.stream().map(InvBorrow::getWarehouseId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> names = ids.isEmpty() ? Collections.emptyMap() : warehouseMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(OrgWarehouse::getId, OrgWarehouse::getWarehouseName));
        LocalDate today = LocalDate.now();
        borrows.forEach(borrow -> {
            borrow.setWarehouseName(names.get(borrow.getWarehouseId()));
            borrow.setOverdue(borrow.getExpectedReturnDate() != null
                    && borrow.getExpectedReturnDate().isBefore(today)
                    && List.of("APPROVED", "PARTIALLY_RETURNED").contains(borrow.getStatus()));
        });
    }

    private void enrichUnits(List<InvBorrowItem> items) {
        List<Long> ids = items.stream().map(InvBorrowItem::getUnitId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return;
        Map<Long, String> names = unitMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(MdUnit::getId, MdUnit::getUnitName));
        items.forEach(item -> item.setUnitName(names.get(item.getUnitId())));
    }

    private void insertMovement(InvBorrow borrow, InvBorrowItem item, OrgWarehouse warehouse,
                                Long operatorId, LocalDate businessDate, String type, String direction,
                                BigDecimal quantity, BigDecimal unitCost, BigDecimal before, BigDecimal after,
                                String remark) {
        stockMovementMapper.insert(new InvStockMovement()
                .setEnterpriseId(borrow.getEnterpriseId())
                .setStoreId(warehouse.getStoreId())
                .setWarehouseId(borrow.getWarehouseId())
                .setProductId(item.getProductId())
                .setMovementNo("SMJY" + System.currentTimeMillis() + "_" + item.getLineNo())
                .setMovementType(type)
                .setDirection(direction)
                .setQuantity(quantity)
                .setUnitCost(unitCost.setScale(4, RoundingMode.HALF_UP))
                .setAmount(quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP))
                .setBeforeQuantity(before)
                .setAfterQuantity(after)
                .setSourceType("INVENTORY_BORROW")
                .setSourceId(borrow.getId())
                .setSourceNo(borrow.getBorrowNo())
                .setSourceItemId(item.getId())
                .setBusinessDate(businessDate)
                .setOperatorId(operatorId)
                .setRemark(remark));
    }

    private void requireStockUpdate(InvStockBalance stock) {
        if (stockBalanceMapper.updateById(stock) != 1) throw new BusinessException("库存已变化，请重试");
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record ReturnLine(InvBorrowItem item, BigDecimal quantity) {
    }
}
