package com.erp.inventory.service;

import com.erp.common.BusinessException;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.entity.InvStockMovement;
import com.erp.inventory.entity.InvTransfer;
import com.erp.inventory.entity.InvTransferItem;
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
class InvTransferServiceTest {
    @Mock private InvTransferMapper transferMapper;
    @Mock private InvTransferItemMapper transferItemMapper;
    @Mock private InvStockBalanceMapper stockBalanceMapper;
    @Mock private InvStockMovementMapper stockMovementMapper;
    @Mock private OrgWarehouseMapper warehouseMapper;
    @Mock private MdProductMapper productMapper;
    @Mock private MdUnitMapper unitMapper;
    @InjectMocks private InvTransferService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", transferMapper);
    }

    @Test
    void completeTransferMovesQuantityAndWritesPairedMovements() {
        InvTransfer transfer = transfer("APPROVED");
        InvTransferItem item = item("3");
        InvStockBalance source = stock(10L, "10", "10", "5", "50");
        InvStockBalance target = stock(20L, "2", "2", "4", "8");
        when(transferMapper.selectForUpdate(1L, 1L)).thenReturn(transfer);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse(10L, 100L));
        when(warehouseMapper.selectById(20L)).thenReturn(warehouse(20L, 200L));
        when(transferItemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 101L)).thenReturn(source);
        when(stockBalanceMapper.selectForUpdate(1L, 20L, 101L)).thenReturn(target);
        when(stockBalanceMapper.updateById(any(InvStockBalance.class))).thenReturn(1);
        when(stockMovementMapper.insert(any(InvStockMovement.class))).thenReturn(1);
        when(transferItemMapper.updateById(any(InvTransferItem.class))).thenReturn(1);
        when(transferMapper.updateById(any(InvTransfer.class))).thenReturn(1);

        service.completeTransfer(1L, 1L, 99L);

        assertEquals(0, new BigDecimal("7").compareTo(source.getQuantity()));
        assertEquals(0, new BigDecimal("35.00").compareTo(source.getStockAmount()));
        assertEquals(0, new BigDecimal("5").compareTo(target.getQuantity()));
        assertEquals(0, new BigDecimal("23.00").compareTo(target.getStockAmount()));
        assertEquals(0, new BigDecimal("4.6000").compareTo(target.getAvgCostPrice()));
        assertEquals("COMPLETED", transfer.getStatus());

        ArgumentCaptor<InvStockMovement> captor = ArgumentCaptor.forClass(InvStockMovement.class);
        verify(stockMovementMapper, times(2)).insert(captor.capture());
        List<InvStockMovement> movements = captor.getAllValues();
        assertEquals("TRANSFER_OUT", movements.get(0).getMovementType());
        assertEquals("OUT", movements.get(0).getDirection());
        assertEquals("TRANSFER_IN", movements.get(1).getMovementType());
        assertEquals("IN", movements.get(1).getDirection());
        assertEquals(0, movements.get(0).getAmount().compareTo(movements.get(1).getAmount()));
    }

    @Test
    void completeTransferChecksAllStockBeforeWriting() {
        InvTransferItem first = item("3");
        InvTransferItem second = item("2").setId(12L).setLineNo(2).setProductId(102L).setProductCode("P102");
        when(transferMapper.selectForUpdate(1L, 1L)).thenReturn(transfer("APPROVED"));
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse(10L, 100L));
        when(warehouseMapper.selectById(20L)).thenReturn(warehouse(20L, 200L));
        when(transferItemMapper.selectList(any())).thenReturn(List.of(first, second));
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 101L)).thenReturn(stock(10L, "10", "10", "5", "50"));
        when(stockBalanceMapper.selectForUpdate(1L, 20L, 101L)).thenReturn(null);
        when(stockBalanceMapper.selectForUpdate(1L, 10L, 102L)).thenReturn(stock(10L, "1", "1", "5", "5"));
        when(stockBalanceMapper.selectForUpdate(1L, 20L, 102L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.completeTransfer(1L, 1L, 99L));

        assertTrue(exception.getMessage().contains("P102"));
        verify(stockBalanceMapper, never()).updateById(any(InvStockBalance.class));
        verify(stockBalanceMapper, never()).insert(any(InvStockBalance.class));
        verifyNoInteractions(stockMovementMapper);
    }

    private InvTransfer transfer(String status) {
        return new InvTransfer().setId(1L).setEnterpriseId(1L).setTransferNo("DB001")
                .setTransferDate(LocalDate.of(2026, 7, 21)).setFromWarehouseId(10L)
                .setToWarehouseId(20L).setStatus(status);
    }

    private InvTransferItem item(String quantity) {
        return new InvTransferItem().setId(11L).setTransferId(1L).setLineNo(1)
                .setProductId(101L).setProductCode("P101").setProductName("测试商品")
                .setQuantity(new BigDecimal(quantity));
    }

    private InvStockBalance stock(Long warehouseId, String quantity, String available, String cost, String amount) {
        return new InvStockBalance().setEnterpriseId(1L).setWarehouseId(warehouseId).setProductId(101L)
                .setQuantity(new BigDecimal(quantity)).setLockedQuantity(BigDecimal.ZERO)
                .setAvailableQuantity(new BigDecimal(available)).setAvgCostPrice(new BigDecimal(cost))
                .setStockAmount(new BigDecimal(amount));
    }

    private OrgWarehouse warehouse(Long id, Long storeId) {
        return new OrgWarehouse().setId(id).setEnterpriseId(1L).setStoreId(storeId)
                .setWarehouseName("测试仓" + id).setStatus("ENABLED");
    }
}
