package com.erp.report.service;

import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.report.mapper.SalesReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SalesReportService {
    private static final Set<String> TYPES = Set.of("DETAIL", "PRODUCT", "CUSTOMER", "CATEGORY", "PROFIT");
    private final SalesReportMapper salesReportMapper;

    public PageResult<Map<String, Object>> page(Long enterpriseId, String type, Integer page, Integer size,
                                                 LocalDate startDate, LocalDate endDate, Long customerId,
                                                 Long productId, Long categoryId) {
        String reportType = normalizeType(type);
        validateDates(startDate, endDate);
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null ? 10 : Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> details = loadDetails(enterpriseId, startDate, endDate,
                customerId, productId, categoryId);
        List<Map<String, Object>> rows = "DETAIL".equals(reportType)
                ? details : aggregate(details, reportType);
        int from = Math.min((safePage - 1) * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        return PageResult.of(rows.subList(from, to), rows.size(), safePage, safeSize);
    }

    public Map<String, Object> summary(Long enterpriseId, LocalDate startDate, LocalDate endDate,
                                       Long customerId, Long productId, Long categoryId) {
        validateDates(startDate, endDate);
        List<Map<String, Object>> rows = loadDetails(enterpriseId, startDate, endDate,
                customerId, productId, categoryId);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("orderCount", rows.stream().map(row -> row.get("orderId")).filter(Objects::nonNull).distinct().count());
        summary.put("totalQuantity", sum(rows, "netQuantity"));
        summary.put("grossSalesAmount", sum(rows, "salesAmount"));
        summary.put("returnAmount", sum(rows, "returnAmount"));
        BigDecimal netSales = sum(rows, "netSalesAmount");
        BigDecimal cost = sum(rows, "costAmount");
        BigDecimal profit = sum(rows, "profitAmount");
        summary.put("netSalesAmount", netSales);
        summary.put("costAmount", cost);
        summary.put("profitAmount", profit);
        summary.put("profitRate", rate(profit, netSales));
        return summary;
    }

    private List<Map<String, Object>> loadDetails(Long enterpriseId, LocalDate startDate, LocalDate endDate,
                                                   Long customerId, Long productId, Long categoryId) {
        List<Map<String, Object>> rows = salesReportMapper.selectDetails(enterpriseId, startDate, endDate,
                customerId, productId, categoryId);
        rows.forEach(row -> row.put("profitRate", rate(decimal(row.get("profitAmount")),
                decimal(row.get("netSalesAmount")))));
        return rows;
    }

    private List<Map<String, Object>> aggregate(List<Map<String, Object>> details, String type) {
        Map<Object, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : details) {
            Object key = switch (type) {
                case "CUSTOMER" -> row.get("customerId");
                case "CATEGORY" -> row.get("categoryId") == null ? "NONE" : row.get("categoryId");
                default -> row.get("productId");
            };
            Map<String, Object> target = grouped.computeIfAbsent(key, ignored -> newAggregateRow(row, type));
            add(target, "grossQuantity", row.get("grossQuantity"));
            add(target, "returnQuantity", row.get("returnQuantity"));
            add(target, "netQuantity", row.get("netQuantity"));
            add(target, "salesAmount", row.get("salesAmount"));
            add(target, "returnAmount", row.get("returnAmount"));
            add(target, "netSalesAmount", row.get("netSalesAmount"));
            add(target, "costAmount", row.get("costAmount"));
            add(target, "profitAmount", row.get("profitAmount"));
            @SuppressWarnings("unchecked")
            Set<Object> orderIds = (Set<Object>) target.get("_orderIds");
            orderIds.add(row.get("orderId"));
        }
        List<Map<String, Object>> result = new ArrayList<>(grouped.values());
        result.forEach(row -> {
            @SuppressWarnings("unchecked")
            Set<Object> orderIds = (Set<Object>) row.remove("_orderIds");
            row.put("orderCount", orderIds.size());
            row.put("profitRate", rate(decimal(row.get("profitAmount")), decimal(row.get("netSalesAmount"))));
        });
        String sortField = "PROFIT".equals(type) ? "profitAmount" : "netSalesAmount";
        result.sort(Comparator.comparing((Map<String, Object> row) -> decimal(row.get(sortField))).reversed());
        return result;
    }

    private Map<String, Object> newAggregateRow(Map<String, Object> source, String type) {
        Map<String, Object> row = new LinkedHashMap<>();
        if ("CUSTOMER".equals(type)) {
            copy(source, row, "customerId", "customerCode", "customerName");
        } else if ("CATEGORY".equals(type)) {
            copy(source, row, "categoryId", "categoryName");
        } else {
            copy(source, row, "productId", "productCode", "productName", "specification", "unitName", "categoryName");
        }
        row.put("grossQuantity", BigDecimal.ZERO);
        row.put("returnQuantity", BigDecimal.ZERO);
        row.put("netQuantity", BigDecimal.ZERO);
        row.put("salesAmount", BigDecimal.ZERO);
        row.put("returnAmount", BigDecimal.ZERO);
        row.put("netSalesAmount", BigDecimal.ZERO);
        row.put("costAmount", BigDecimal.ZERO);
        row.put("profitAmount", BigDecimal.ZERO);
        row.put("_orderIds", new HashSet<>());
        return row;
    }

    private void add(Map<String, Object> row, String field, Object value) {
        row.put(field, decimal(row.get(field)).add(decimal(value)));
    }

    private void copy(Map<String, Object> source, Map<String, Object> target, String... fields) {
        for (String field : fields) target.put(field, source.get(field));
    }

    private BigDecimal sum(List<Map<String, Object>> rows, String field) {
        return rows.stream().map(row -> decimal(row.get(field))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal number) return number;
        return new BigDecimal(value.toString());
    }

    private BigDecimal rate(BigDecimal profit, BigDecimal sales) {
        return sales.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : profit.multiply(BigDecimal.valueOf(100)).divide(sales, 2, RoundingMode.HALF_UP);
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "DETAIL" : type.trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(normalized)) throw new BusinessException("销售报表类型不正确");
        return normalized;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
    }
}
