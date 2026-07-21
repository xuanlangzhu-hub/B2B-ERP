package com.erp.report.service;

import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.report.mapper.SalesReportMapper;
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

class SalesReportServiceTest {
    private SalesReportMapper mapper;
    private SalesReportService service;

    @BeforeEach
    void setUp() {
        mapper = mock(SalesReportMapper.class);
        service = new SalesReportService(mapper);
    }

    @Test
    void summaryUsesNetSalesAndRealCost() {
        when(mapper.selectDetails(eq(1L), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(row(1L, 10L, "P001", "商品A", "100", "10", "90", "55", "35"),
                        row(1L, 11L, "P002", "商品B", "50", "0", "50", "20", "30")));

        Map<String, Object> summary = service.summary(1L, null, null, null, null, null);

        assertThat(summary.get("orderCount")).isEqualTo(1L);
        assertThat(summary.get("grossSalesAmount")).isEqualTo(new BigDecimal("150"));
        assertThat(summary.get("returnAmount")).isEqualTo(new BigDecimal("10"));
        assertThat(summary.get("netSalesAmount")).isEqualTo(new BigDecimal("140"));
        assertThat(summary.get("costAmount")).isEqualTo(new BigDecimal("75"));
        assertThat(summary.get("profitAmount")).isEqualTo(new BigDecimal("65"));
        assertThat(summary.get("profitRate")).isEqualTo(new BigDecimal("46.43"));
    }

    @Test
    void productReportAggregatesRowsAndPaginates() {
        when(mapper.selectDetails(anyLong(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(row(1L, 10L, "P001", "商品A", "100", "10", "90", "55", "35"),
                        row(2L, 10L, "P001", "商品A", "40", "0", "40", "20", "20")));

        PageResult<Map<String, Object>> result = service.page(1L, "product", 1, 10,
                null, null, null, null, null);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().getFirst().get("orderCount")).isEqualTo(2);
        assertThat(result.getRecords().getFirst().get("netSalesAmount")).isEqualTo(new BigDecimal("130"));
        assertThat(result.getRecords().getFirst().get("profitAmount")).isEqualTo(new BigDecimal("55"));
    }

    @Test
    void rejectsUnknownTypeAndReversedDates() {
        assertThatThrownBy(() -> service.page(1L, "unknown", 1, 10,
                null, null, null, null, null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.summary(1L, LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 7, 1), null, null, null)).isInstanceOf(BusinessException.class);
    }

    private Map<String, Object> row(Long orderId, Long productId, String productCode, String productName,
                                    String sales, String returned, String net, String cost, String profit) {
        Map<String, Object> row = new HashMap<>();
        row.put("orderId", orderId);
        row.put("productId", productId);
        row.put("productCode", productCode);
        row.put("productName", productName);
        row.put("categoryId", 100L);
        row.put("categoryName", "默认分类");
        row.put("customerId", 200L);
        row.put("customerCode", "C001");
        row.put("customerName", "客户A");
        row.put("grossQuantity", new BigDecimal("2"));
        row.put("returnQuantity", new BigDecimal("0"));
        row.put("netQuantity", new BigDecimal("2"));
        row.put("salesAmount", new BigDecimal(sales));
        row.put("returnAmount", new BigDecimal(returned));
        row.put("netSalesAmount", new BigDecimal(net));
        row.put("costAmount", new BigDecimal(cost));
        row.put("profitAmount", new BigDecimal(profit));
        return row;
    }
}
