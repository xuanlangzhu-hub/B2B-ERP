package com.erp.report.service;

import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.report.mapper.PurchaseReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PurchaseReportServiceTest {
    private PurchaseReportMapper mapper;
    private PurchaseReportService service;

    @BeforeEach
    void setUp() {
        mapper = mock(PurchaseReportMapper.class);
        service = new PurchaseReportService(mapper);
    }

    @Test
    void summaryDeductsCompletedReturns() {
        when(mapper.selectDetails(eq(1L), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(row(1L, 10L, "100", "10", "90"), row(1L, 11L, "50", "0", "50")));

        Map<String, Object> summary = service.summary(1L, null, null, null, null, null);

        assertThat(summary.get("orderCount")).isEqualTo(1L);
        assertThat(summary.get("supplierCount")).isEqualTo(1L);
        assertThat(summary.get("productCount")).isEqualTo(2L);
        assertThat(summary.get("grossPurchaseAmount")).isEqualTo(new BigDecimal("150"));
        assertThat(summary.get("returnAmount")).isEqualTo(new BigDecimal("10"));
        assertThat(summary.get("netPurchaseAmount")).isEqualTo(new BigDecimal("140"));
    }

    @Test
    void supplierReportAggregatesRowsAndCountsOrders() {
        when(mapper.selectDetails(anyLong(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(row(1L, 10L, "100", "10", "90"), row(2L, 11L, "50", "0", "50")));

        PageResult<Map<String, Object>> result = service.page(1L, "supplier", 1, 10,
                null, null, null, null, null);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().getFirst().get("orderCount")).isEqualTo(2);
        assertThat(result.getRecords().getFirst().get("netPurchaseAmount")).isEqualTo(new BigDecimal("140"));
    }

    @Test
    void rejectsUnknownTypeAndReversedDates() {
        assertThatThrownBy(() -> service.page(1L, "profit", 1, 10,
                null, null, null, null, null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.summary(1L, LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 7, 1), null, null, null)).isInstanceOf(BusinessException.class);
    }

    private Map<String, Object> row(Long orderId, Long productId, String purchase, String returned, String net) {
        Map<String, Object> row = new HashMap<>();
        row.put("orderId", orderId);
        row.put("supplierId", 200L);
        row.put("supplierCode", "S001");
        row.put("supplierName", "供应商A");
        row.put("productId", productId);
        row.put("productCode", "P" + productId);
        row.put("productName", "商品" + productId);
        row.put("categoryId", 100L);
        row.put("categoryName", "默认分类");
        row.put("grossQuantity", new BigDecimal("2"));
        row.put("returnQuantity", new BigDecimal("0"));
        row.put("netQuantity", new BigDecimal("2"));
        row.put("purchaseAmount", new BigDecimal(purchase));
        row.put("returnAmount", new BigDecimal(returned));
        row.put("netPurchaseAmount", new BigDecimal(net));
        return row;
    }
}
