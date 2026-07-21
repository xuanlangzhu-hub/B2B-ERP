package com.erp.inventory.service;

import com.erp.common.BusinessException;
import com.erp.inventory.entity.InvAdjustment;
import com.erp.inventory.entity.InvAdjustmentItem;
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
class InvAdjustmentServiceTest {
    @Mock private InvAdjustmentMapper adjustmentMapper;
    @Mock private InvAdjustmentItemMapper adjustmentItemMapper;
    @Mock private InvStockBalanceMapper stockBalanceMapper;
    @Mock private InvStockMovementMapper stockMovementMapper;
    @Mock private OrgWarehouseMapper warehouseMapper;
    @Mock private MdProductMapper productMapper;
    @Mock private MdUnitMapper unitMapper;
    @InjectMocks private InvAdjustmentService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", adjustmentMapper);
    }

    @Test
    void approveDecreaseDeductsStockAndWritesLossMovement() {
        InvAdjustment adjustment = adjustment("DECREASE");
        InvAdjustmentItem item = item("2", "5");
        InvStockBalance stock = stock("10", "10", "5", "50");
        stubApproval(adjustment, item, stock);

        service.approveAdjustment(1L, 1L, 99L);

        assertEquals(0, new BigDecimal("8").compareTo(stock.getQuantity()));
        assertEquals(0, new BigDecimal("40.00").compareTo(stock.getStockAmount()));
        assertEquals("COMPLETED", adjustment.getStatus());
        ArgumentCaptor<InvStockMovement> captor = ArgumentCaptor.forClass(InvStockMovement.class);
        verify(stockMovementMapper).insert(captor.capture());
        assertEquals("OUT", captor.getValue().getDirection());
        assertEquals("ADJUST", captor.getValue().getMovementType());
    }

    @Test
    void approveIncreaseCreatesMissingStock() {
        InvAdjustment adjustment = adjustment("INCREASE");
        InvAdjustmentItem item = item("3", "4");
        when(adjustmentMapper.selectForUpdate(1L, 1L)).thenReturn(adjustment);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse());
        when(adjustmentItemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 101L)).thenReturn(null);
        when(stockBalanceMapper.insert(any(InvStockBalance.class))).thenReturn(1);
        when(adjustmentItemMapper.updateById(any(InvAdjustmentItem.class))).thenReturn(1);
        when(stockMovementMapper.insert(any(InvStockMovement.class))).thenReturn(1);
        when(adjustmentMapper.updateById(any(InvAdjustment.class))).thenReturn(1);

        service.approveAdjustment(1L, 1L, 99L);

        ArgumentCaptor<InvStockBalance> stockCaptor = ArgumentCaptor.forClass(InvStockBalance.class);
        verify(stockBalanceMapper).insert(stockCaptor.capture());
        assertEquals(0, new BigDecimal("3").compareTo(stockCaptor.getValue().getQuantity()));
        assertEquals(0, new BigDecimal("12.00").compareTo(stockCaptor.getValue().getStockAmount()));
        ArgumentCaptor<InvStockMovement> movementCaptor = ArgumentCaptor.forClass(InvStockMovement.class);
        verify(stockMovementMapper).insert(movementCaptor.capture());
        assertEquals("IN", movementCaptor.getValue().getDirection());
    }

    @Test
    void approveDecreaseRejectsInsufficientStockBeforeWriting() {
        InvAdjustment adjustment = adjustment("DECREASE");
        InvAdjustmentItem item = item("3", "5");
        when(adjustmentMapper.selectForUpdate(1L, 1L)).thenReturn(adjustment);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse());
        when(adjustmentItemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 101L)).thenReturn(stock("2", "2", "5", "10"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.approveAdjustment(1L, 1L, 99L));

        assertTrue(exception.getMessage().contains("库存不足"));
        verify(stockBalanceMapper, never()).updateById(any(InvStockBalance.class));
        verifyNoInteractions(stockMovementMapper);
    }

    private void stubApproval(InvAdjustment adjustment, InvAdjustmentItem item, InvStockBalance stock) {
        when(adjustmentMapper.selectForUpdate(1L, 1L)).thenReturn(adjustment);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse());
        when(adjustmentItemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 101L)).thenReturn(stock);
        when(stockBalanceMapper.updateById(any(InvStockBalance.class))).thenReturn(1);
        when(adjustmentItemMapper.updateById(any(InvAdjustmentItem.class))).thenReturn(1);
        when(stockMovementMapper.insert(any(InvStockMovement.class))).thenReturn(1);
        when(adjustmentMapper.updateById(any(InvAdjustment.class))).thenReturn(1);
    }

    private InvAdjustment adjustment(String type) {
        return new InvAdjustment().setId(1L).setEnterpriseId(1L).setAdjustmentNo("TZ001")
                .setAdjustmentDate(LocalDate.of(2026, 7, 21)).setWarehouseId(10L)
                .setAdjustmentType(type).setStatus("DRAFT").setReason("测试调整");
    }

    private InvAdjustmentItem item(String quantity, String cost) {
        return new InvAdjustmentItem().setId(11L).setAdjustmentId(1L).setLineNo(1)
                .setProductId(101L).setProductCode("P101").setProductName("测试商品")
                .setQuantity(new BigDecimal(quantity)).setUnitCost(new BigDecimal(cost));
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
