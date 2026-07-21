package com.erp.inventory.service;

import com.erp.common.BusinessException;
import com.erp.inventory.entity.InvOutbound;
import com.erp.inventory.entity.InvOutboundItem;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.entity.InvStockMovement;
import com.erp.inventory.mapper.InvOutboundItemMapper;
import com.erp.inventory.mapper.InvOutboundMapper;
import com.erp.inventory.mapper.InvStockBalanceMapper;
import com.erp.inventory.mapper.InvStockMovementMapper;
import com.erp.purchase.mapper.PurOrderItemMapper;
import com.erp.purchase.mapper.PurReturnItemMapper;
import com.erp.purchase.mapper.PurReturnMapper;
import com.erp.purchase.service.PurReturnService;
import com.erp.sales.mapper.SalOrderItemMapper;
import com.erp.sales.mapper.SalOrderMapper;
import com.erp.sales.service.SalOrderService;
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
class InvOutboundServiceTest {

    @Mock private InvOutboundMapper outboundMapper;
    @Mock private InvOutboundItemMapper outboundItemMapper;
    @Mock private InvStockBalanceMapper stockBalanceMapper;
    @Mock private InvStockMovementMapper stockMovementMapper;
    @Mock private SalOrderMapper salOrderMapper;
    @Mock private SalOrderItemMapper salOrderItemMapper;
    @Mock private SalOrderService salOrderService;
    @Mock private PurReturnMapper purReturnMapper;
    @Mock private PurReturnItemMapper purReturnItemMapper;
    @Mock private PurOrderItemMapper purOrderItemMapper;
    @Mock private PurReturnService purReturnService;
    @Mock private OrgWarehouseMapper warehouseMapper;

    @InjectMocks private InvOutboundService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", outboundMapper);
    }

    @Test
    void confirmOutboundValidatesWholeOrderBeforeAnyStockWrite() {
        InvOutbound outbound = outbound(1L, "DRAFT");
        InvOutboundItem enough = item(1L, 101L, "P101", "5");
        InvOutboundItem insufficient = item(2L, 102L, "P102", "2");
        when(outboundMapper.selectForUpdate(1L, 1L)).thenReturn(outbound);
        when(outboundItemMapper.selectList(any())).thenReturn(List.of(enough, insufficient));
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 101L)).thenReturn(stock(101L, "10", "10", "8", "80"));
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 102L)).thenReturn(stock(102L, "1", "1", "8", "8"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.confirmOutbound(1L, 1L, 99L));

        assertTrue(exception.getMessage().contains("P102"));
        verify(stockBalanceMapper, never()).updateById(any(InvStockBalance.class));
        verify(stockMovementMapper, never()).insert(any(InvStockMovement.class));
        verify(outboundMapper, never()).updateById(any(InvOutbound.class));
    }

    @Test
    void confirmOutboundDeductsStockAndWritesAuditableMovement() {
        InvOutbound outbound = outbound(1L, "DRAFT");
        InvOutboundItem item = item(1L, 101L, "P101", "3");
        InvStockBalance stock = stock(101L, "10", "10", "8", "80");
        when(outboundMapper.selectForUpdate(1L, 1L)).thenReturn(outbound);
        when(outboundItemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 101L)).thenReturn(stock);
        when(stockBalanceMapper.updateById(any(InvStockBalance.class))).thenReturn(1);
        when(stockMovementMapper.insert(any(InvStockMovement.class))).thenReturn(1);
        when(outboundMapper.updateById(any(InvOutbound.class))).thenReturn(1);

        service.confirmOutbound(1L, 1L, 99L);

        assertEquals(new BigDecimal("7"), stock.getQuantity());
        assertEquals(new BigDecimal("7"), stock.getAvailableQuantity());
        assertEquals(new BigDecimal("56.00"), stock.getStockAmount());
        assertEquals("CONFIRMED", outbound.getStatus());
        assertEquals(99L, outbound.getConfirmedBy());

        ArgumentCaptor<InvStockMovement> movementCaptor = ArgumentCaptor.forClass(InvStockMovement.class);
        verify(stockMovementMapper).insert(movementCaptor.capture());
        InvStockMovement movement = movementCaptor.getValue();
        assertEquals("SALES_OUT", movement.getMovementType());
        assertEquals("OUT", movement.getDirection());
        assertEquals(new BigDecimal("10"), movement.getBeforeQuantity());
        assertEquals(new BigDecimal("7"), movement.getAfterQuantity());
        assertEquals(new BigDecimal("24.00"), movement.getAmount());
    }

    @Test
    void confirmOutboundRejectsRepeatedConfirmationBeforeReadingItems() {
        when(outboundMapper.selectForUpdate(1L, 1L)).thenReturn(outbound(1L, "CONFIRMED"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.confirmOutbound(1L, 1L, 99L));

        assertTrue(exception.getMessage().contains("草稿"));
        verifyNoInteractions(outboundItemMapper, stockBalanceMapper, stockMovementMapper);
    }

    private InvOutbound outbound(Long id, String status) {
        return new InvOutbound()
                .setId(id)
                .setEnterpriseId(1L)
                .setStoreId(1L)
                .setWarehouseId(10L)
                .setOutboundDate(LocalDate.of(2026, 7, 20))
                .setSourceType("OTHER")
                .setSourceNo("TEST-OUT")
                .setStatus(status);
    }

    private InvOutboundItem item(Long id, Long productId, String productCode, String quantity) {
        return new InvOutboundItem()
                .setId(id)
                .setLineNo(id.intValue())
                .setProductId(productId)
                .setProductCode(productCode)
                .setProductName("测试商品" + productCode)
                .setQuantity(new BigDecimal(quantity));
    }

    private InvStockBalance stock(Long productId, String quantity, String available, String avgCost, String amount) {
        return new InvStockBalance()
                .setEnterpriseId(1L)
                .setWarehouseId(10L)
                .setProductId(productId)
                .setQuantity(new BigDecimal(quantity))
                .setLockedQuantity(BigDecimal.ZERO)
                .setAvailableQuantity(new BigDecimal(available))
                .setAvgCostPrice(new BigDecimal(avgCost))
                .setStockAmount(new BigDecimal(amount));
    }
}
