package com.erp.inventory.service;

import com.erp.common.BusinessException;
import com.erp.inventory.entity.InvInbound;
import com.erp.inventory.entity.InvInboundItem;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.entity.InvStockMovement;
import com.erp.inventory.mapper.InvInboundItemMapper;
import com.erp.inventory.mapper.InvInboundMapper;
import com.erp.inventory.mapper.InvStockBalanceMapper;
import com.erp.inventory.mapper.InvStockMovementMapper;
import com.erp.purchase.mapper.PurOrderItemMapper;
import com.erp.purchase.mapper.PurOrderMapper;
import com.erp.purchase.service.PurOrderService;
import com.erp.sales.mapper.SalOrderItemMapper;
import com.erp.sales.mapper.SalReturnItemMapper;
import com.erp.sales.mapper.SalReturnMapper;
import com.erp.sales.service.SalReturnService;
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
class InvInboundServiceTest {

    @Mock private InvInboundMapper inboundMapper;
    @Mock private InvInboundItemMapper inboundItemMapper;
    @Mock private InvStockBalanceMapper stockBalanceMapper;
    @Mock private InvStockMovementMapper stockMovementMapper;
    @Mock private PurOrderMapper purOrderMapper;
    @Mock private PurOrderItemMapper purOrderItemMapper;
    @Mock private PurOrderService purOrderService;
    @Mock private SalReturnMapper salReturnMapper;
    @Mock private SalReturnItemMapper salReturnItemMapper;
    @Mock private SalOrderItemMapper salOrderItemMapper;
    @Mock private SalReturnService salReturnService;
    @Mock private OrgWarehouseMapper warehouseMapper;

    @InjectMocks private InvInboundService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", inboundMapper);
    }

    @Test
    void confirmInboundUsesWeightedAverageCostAndWritesMovement() {
        InvInbound inbound = inbound(1L, "DRAFT");
        InvInboundItem item = new InvInboundItem()
                .setId(1L)
                .setLineNo(1)
                .setProductId(101L)
                .setProductCode("P101")
                .setProductName("测试商品")
                .setQuantity(new BigDecimal("5"))
                .setUnitCost(new BigDecimal("12"))
                .setAmount(new BigDecimal("60"));
        InvStockBalance stock = new InvStockBalance()
                .setEnterpriseId(1L)
                .setWarehouseId(10L)
                .setProductId(101L)
                .setQuantity(new BigDecimal("10"))
                .setLockedQuantity(BigDecimal.ZERO)
                .setAvailableQuantity(new BigDecimal("10"))
                .setAvgCostPrice(new BigDecimal("10"))
                .setStockAmount(new BigDecimal("100"));
        when(inboundMapper.selectForUpdate(1L, 1L)).thenReturn(inbound);
        when(inboundItemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 101L)).thenReturn(stock);
        when(stockBalanceMapper.updateById(any(InvStockBalance.class))).thenReturn(1);
        when(stockMovementMapper.insert(any(InvStockMovement.class))).thenReturn(1);
        when(inboundMapper.updateById(any(InvInbound.class))).thenReturn(1);

        service.confirmInbound(1L, 1L, 99L);

        assertEquals(new BigDecimal("15"), stock.getQuantity());
        assertEquals(new BigDecimal("15"), stock.getAvailableQuantity());
        assertEquals(new BigDecimal("160"), stock.getStockAmount());
        assertEquals(new BigDecimal("10.6667"), stock.getAvgCostPrice());
        assertEquals("CONFIRMED", inbound.getStatus());

        ArgumentCaptor<InvStockMovement> movementCaptor = ArgumentCaptor.forClass(InvStockMovement.class);
        verify(stockMovementMapper).insert(movementCaptor.capture());
        InvStockMovement movement = movementCaptor.getValue();
        assertEquals("PURCHASE_IN", movement.getMovementType());
        assertEquals("IN", movement.getDirection());
        assertEquals(new BigDecimal("10"), movement.getBeforeQuantity());
        assertEquals(new BigDecimal("15"), movement.getAfterQuantity());
        assertEquals(new BigDecimal("60"), movement.getAmount());
    }

    @Test
    void confirmInboundRejectsRepeatedConfirmationBeforeReadingItems() {
        when(inboundMapper.selectForUpdate(1L, 1L)).thenReturn(inbound(1L, "CONFIRMED"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.confirmInbound(1L, 1L, 99L));

        assertTrue(exception.getMessage().contains("草稿"));
        verifyNoInteractions(inboundItemMapper, stockBalanceMapper, stockMovementMapper);
    }

    private InvInbound inbound(Long id, String status) {
        return new InvInbound()
                .setId(id)
                .setEnterpriseId(1L)
                .setStoreId(1L)
                .setWarehouseId(10L)
                .setInboundDate(LocalDate.of(2026, 7, 20))
                .setSourceType("OTHER")
                .setSourceNo("TEST-IN")
                .setStatus(status);
    }
}
