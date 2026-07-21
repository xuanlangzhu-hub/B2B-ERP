package com.erp.inventory.service;

import com.erp.common.BusinessException;
import com.erp.inventory.dto.InvBorrowReturnItemRequest;
import com.erp.inventory.dto.InvBorrowReturnRequest;
import com.erp.inventory.entity.InvBorrow;
import com.erp.inventory.entity.InvBorrowItem;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.entity.InvStockMovement;
import com.erp.inventory.mapper.*;
import com.erp.masterdata.mapper.MdProductMapper;
import com.erp.masterdata.mapper.MdUnitMapper;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.mapper.OrgWarehouseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvBorrowServiceTest {
    @Mock private InvBorrowMapper borrowMapper;
    @Mock private InvBorrowItemMapper borrowItemMapper;
    @Mock private InvStockBalanceMapper stockBalanceMapper;
    @Mock private InvStockMovementMapper stockMovementMapper;
    @Mock private OrgWarehouseMapper warehouseMapper;
    @Mock private MdProductMapper productMapper;
    @Mock private MdUnitMapper unitMapper;
    @InjectMocks private InvBorrowService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", borrowMapper);
    }

    @Test
    void approveBorrowOutDeductsStockAndWritesMovement() {
        InvBorrow borrow = borrow("BORROW_OUT", "DRAFT");
        InvBorrowItem item = item("4", "0");
        InvStockBalance stock = stock("10", "10", "5", "50");
        stubCommon(borrow, item);
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 101L)).thenReturn(stock);
        when(stockBalanceMapper.updateById(any(InvStockBalance.class))).thenReturn(1);

        service.approveBorrow(1L, 1L, 99L);

        assertEquals(0, new BigDecimal("6").compareTo(stock.getQuantity()));
        assertEquals(0, new BigDecimal("30.00").compareTo(stock.getStockAmount()));
        assertEquals("APPROVED", borrow.getStatus());
        ArgumentCaptor<InvStockMovement> captor = ArgumentCaptor.forClass(InvStockMovement.class);
        verify(stockMovementMapper).insert(captor.capture());
        assertEquals("BORROW_OUT", captor.getValue().getMovementType());
        assertEquals("OUT", captor.getValue().getDirection());
    }

    @Test
    void approveBorrowInCreatesStock() {
        InvBorrow borrow = borrow("BORROW_IN", "DRAFT");
        InvBorrowItem item = item("3", "0").setUnitCost(new BigDecimal("4"));
        stubCommon(borrow, item);
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 101L)).thenReturn(null);
        when(stockBalanceMapper.insert(any(InvStockBalance.class))).thenReturn(1);

        service.approveBorrow(1L, 1L, 99L);

        ArgumentCaptor<InvStockBalance> stockCaptor = ArgumentCaptor.forClass(InvStockBalance.class);
        verify(stockBalanceMapper).insert(stockCaptor.capture());
        assertEquals(0, new BigDecimal("3").compareTo(stockCaptor.getValue().getQuantity()));
        assertEquals(0, new BigDecimal("12.00").compareTo(stockCaptor.getValue().getStockAmount()));
    }

    @Test
    void partialReturnOfBorrowOutRestoresStockAndUpdatesStatus() {
        InvBorrow borrow = borrow("BORROW_OUT", "APPROVED");
        InvBorrowItem item = item("4", "0").setUnitCost(new BigDecimal("5"));
        InvStockBalance stock = stock("6", "6", "5", "30");
        stubCommon(borrow, item);
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 101L)).thenReturn(stock);
        when(stockBalanceMapper.updateById(any(InvStockBalance.class))).thenReturn(1);

        service.returnBorrow(1L, returnRequest("2"), 1L, 99L);

        assertEquals(0, new BigDecimal("8").compareTo(stock.getQuantity()));
        assertEquals(0, new BigDecimal("2").compareTo(item.getReturnedQuantity()));
        assertEquals("PARTIALLY_RETURNED", borrow.getStatus());
        ArgumentCaptor<InvStockMovement> captor = ArgumentCaptor.forClass(InvStockMovement.class);
        verify(stockMovementMapper).insert(captor.capture());
        assertEquals("BORROW_OUT_RETURN", captor.getValue().getMovementType());
        assertEquals("IN", captor.getValue().getDirection());
    }

    @Test
    void returnRejectsQuantityGreaterThanRemainingBeforeStockWrite() {
        InvBorrow borrow = borrow("BORROW_OUT", "PARTIALLY_RETURNED");
        InvBorrowItem item = item("4", "3");
        when(borrowMapper.selectForUpdate(1L, 1L)).thenReturn(borrow);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse());
        when(borrowItemMapper.selectList(any())).thenReturn(List.of(item));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.returnBorrow(1L, returnRequest("2"), 1L, 99L));

        assertTrue(exception.getMessage().contains("超过未归还数量"));
        verifyNoInteractions(stockBalanceMapper, stockMovementMapper);
    }

    private void stubCommon(InvBorrow borrow, InvBorrowItem item) {
        when(borrowMapper.selectForUpdate(1L, 1L)).thenReturn(borrow);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse());
        when(borrowItemMapper.selectList(any())).thenReturn(List.of(item));
        when(borrowItemMapper.updateById(any(InvBorrowItem.class))).thenReturn(1);
        when(stockMovementMapper.insert(any(InvStockMovement.class))).thenReturn(1);
        when(borrowMapper.updateById(any(InvBorrow.class))).thenReturn(1);
    }

    private InvBorrow borrow(String type, String status) {
        return new InvBorrow().setId(1L).setEnterpriseId(1L).setBorrowNo("JY001")
                .setBorrowType(type).setBorrowDate(LocalDate.of(2026, 7, 21))
                .setWarehouseId(10L).setStatus(status).setTotalQuantity(new BigDecimal("4"));
    }

    private InvBorrowItem item(String quantity, String returned) {
        return new InvBorrowItem().setId(11L).setBorrowId(1L).setLineNo(1)
                .setProductId(101L).setProductCode("P101").setProductName("测试商品")
                .setQuantity(new BigDecimal(quantity)).setReturnedQuantity(new BigDecimal(returned))
                .setUnitCost(new BigDecimal("5"));
    }

    private InvBorrowReturnRequest returnRequest(String quantity) {
        InvBorrowReturnItemRequest item = new InvBorrowReturnItemRequest();
        item.setItemId(11L);
        item.setQuantity(new BigDecimal(quantity));
        InvBorrowReturnRequest request = new InvBorrowReturnRequest();
        request.setReturnDate(LocalDate.of(2026, 7, 22));
        request.setItems(List.of(item));
        return request;
    }

    private InvStockBalance stock(String quantity, String available, String cost, String amount) {
        return new InvStockBalance().setEnterpriseId(1L).setWarehouseId(10L).setProductId(101L)
                .setQuantity(new BigDecimal(quantity)).setLockedQuantity(BigDecimal.ZERO)
                .setAvailableQuantity(new BigDecimal(available)).setAvgCostPrice(new BigDecimal(cost))
                .setStockAmount(new BigDecimal(amount));
    }

    private OrgWarehouse warehouse() {
        return new OrgWarehouse().setId(10L).setEnterpriseId(1L).setStoreId(100L)
                .setWarehouseName("测试仓").setStatus("ENABLED");
    }
}
