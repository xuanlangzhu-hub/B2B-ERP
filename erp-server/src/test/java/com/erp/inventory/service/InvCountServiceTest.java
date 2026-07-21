package com.erp.inventory.service;

import com.erp.common.BusinessException;
import com.erp.inventory.entity.InvCount;
import com.erp.inventory.entity.InvCountItem;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.entity.InvStockMovement;
import com.erp.inventory.mapper.InvCountItemMapper;
import com.erp.inventory.mapper.InvCountMapper;
import com.erp.inventory.mapper.InvStockBalanceMapper;
import com.erp.inventory.mapper.InvStockMovementMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvCountServiceTest {

    @Mock private InvCountMapper countMapper;
    @Mock private InvCountItemMapper countItemMapper;
    @Mock private InvStockBalanceMapper stockBalanceMapper;
    @Mock private InvStockMovementMapper stockMovementMapper;
    @Mock private OrgWarehouseMapper warehouseMapper;
    @Mock private MdProductMapper productMapper;
    @Mock private MdUnitMapper unitMapper;

    @InjectMocks private InvCountService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", countMapper);
    }

    @Test
    void approveCountAppliesLossAndWritesMovement() {
        InvCount count = count("APPROVED");
        InvCountItem item = item("10", "8");
        InvStockBalance stock = stock("10", "10", "5", "50");
        when(countMapper.selectForUpdate(1L, 1L)).thenReturn(count);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse());
        when(countItemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 101L)).thenReturn(stock);
        when(stockBalanceMapper.updateById(any(InvStockBalance.class))).thenReturn(1);
        when(stockMovementMapper.insert(any(InvStockMovement.class))).thenReturn(1);
        when(countMapper.updateById(any(InvCount.class))).thenReturn(1);

        service.approveCount(1L, 1L, 99L);

        assertEquals(new BigDecimal("8"), stock.getQuantity());
        assertEquals(new BigDecimal("8"), stock.getAvailableQuantity());
        assertEquals(new BigDecimal("40.00"), stock.getStockAmount());
        assertEquals("COMPLETED", count.getStatus());
        assertEquals(99L, count.getApprovedBy());

        ArgumentCaptor<InvStockMovement> movementCaptor = ArgumentCaptor.forClass(InvStockMovement.class);
        verify(stockMovementMapper).insert(movementCaptor.capture());
        InvStockMovement movement = movementCaptor.getValue();
        assertEquals("COUNT", movement.getMovementType());
        assertEquals("OUT", movement.getDirection());
        assertEquals(0, new BigDecimal("2.0000").compareTo(movement.getQuantity()));
        assertEquals(new BigDecimal("10"), movement.getBeforeQuantity());
        assertEquals(new BigDecimal("8"), movement.getAfterQuantity());
        assertEquals(new BigDecimal("10.00"), movement.getAmount());
    }

    @Test
    void approveCountRejectsChangedBookStockBeforeWriting() {
        when(countMapper.selectForUpdate(1L, 1L)).thenReturn(count("APPROVED"));
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse());
        when(countItemMapper.selectList(any())).thenReturn(List.of(item("10", "8")));
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 101L))
                .thenReturn(stock("9", "9", "5", "45"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.approveCount(1L, 1L, 99L));

        assertTrue(exception.getMessage().contains("账面库存已变化"));
        verify(stockBalanceMapper, never()).updateById(any(InvStockBalance.class));
        verifyNoInteractions(stockMovementMapper);
        verify(countMapper, never()).updateById(any(InvCount.class));
    }

    @Test
    void approveCountRejectsRepeatedApprovalBeforeReadingItems() {
        when(countMapper.selectForUpdate(1L, 1L)).thenReturn(count("COMPLETED"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.approveCount(1L, 1L, 99L));

        assertTrue(exception.getMessage().contains("待审核"));
        verifyNoInteractions(countItemMapper, stockBalanceMapper, stockMovementMapper, warehouseMapper);
    }

    private InvCount count(String status) {
        return new InvCount()
                .setId(1L)
                .setEnterpriseId(1L)
                .setCountNo("PD202607210001")
                .setCountDate(LocalDate.of(2026, 7, 21))
                .setWarehouseId(10L)
                .setStatus(status);
    }

    private InvCountItem item(String bookQuantity, String actualQuantity) {
        BigDecimal book = new BigDecimal(bookQuantity);
        BigDecimal actual = new BigDecimal(actualQuantity);
        return new InvCountItem()
                .setId(11L)
                .setCountId(1L)
                .setLineNo(1)
                .setProductId(101L)
                .setProductCode("P101")
                .setBookQuantity(book)
                .setActualQuantity(actual)
                .setDiffQuantity(actual.subtract(book))
                .setUnitCost(new BigDecimal("5"));
    }

    private InvStockBalance stock(String quantity, String available, String avgCost, String amount) {
        return new InvStockBalance()
                .setEnterpriseId(1L)
                .setWarehouseId(10L)
                .setProductId(101L)
                .setQuantity(new BigDecimal(quantity))
                .setLockedQuantity(BigDecimal.ZERO)
                .setAvailableQuantity(new BigDecimal(available))
                .setAvgCostPrice(new BigDecimal(avgCost))
                .setStockAmount(new BigDecimal(amount));
    }

    private OrgWarehouse warehouse() {
        return new OrgWarehouse()
                .setId(10L)
                .setEnterpriseId(1L)
                .setStoreId(20L)
                .setWarehouseName("测试仓")
                .setStatus("ENABLED");
    }
}
