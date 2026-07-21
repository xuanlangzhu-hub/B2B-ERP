package com.erp.report.service;

import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.report.mapper.PurchaseReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PurchaseReportService {
    private static final Set<String> TYPES = Set.of("DETAIL", "PRODUCT", "SUPPLIER", "CATEGORY");
    private final PurchaseReportMapper purchaseReportMapper;

    public PageResult<Map<String, Object>> page(Long enterpriseId, String type, Integer page, Integer size,
                                                 LocalDate startDate, LocalDate endDate, Long supplierId,
                                                 Long productId, Long categoryId) {
        String reportType = normalizeType(type);
        validateDates(startDate, endDate);
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null ? 10 : Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> details = loadDetails(enterpriseId, startDate, endDate,
                supplierId, productId, categoryId);
        List<Map<String, Object>> rows = "DETAIL".equals(reportType) ? details : aggregate(details, reportType);
        int from = Math.min((safePage - 1) * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        return PageResult.of(rows.subList(from, to), rows.size(), safePage, safeSize);
    }

    public Map<String, Object> summary(Long enterpriseId, LocalDate startDate, LocalDate endDate,
                                       Long supplierId, Long productId, Long categoryId) {
        validateDates(startDate, endDate);
        List<Map<String, Object>> rows = loadDetails(enterpriseId, startDate, endDate,
                supplierId, productId, categoryId);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("orderCount", rows.stream().map(row -> row.get("orderId")).filter(Objects::nonNull).distinct().count());
        summary.put("supplierCount", rows.stream().map(row -> row.get("supplierId")).filter(Objects::nonNull).distinct().count());
        summary.put("productCount", rows.stream().map(row -> row.get("productId")).filter(Objects::nonNull).distinct().count());
        summary.put("totalQuantity", sum(rows, "netQuantity"));
        summary.put("grossPurchaseAmount", sum(rows, "purchaseAmount"));
        summary.put("returnAmount", sum(rows, "returnAmount"));
        summary.put("netPurchaseAmount", sum(rows, "netPurchaseAmount"));
        return summary;
    }

    private List<Map<String, Object>> loadDetails(Long enterpriseId, LocalDate startDate, LocalDate endDate,
                                                   Long supplierId, Long productId, Long categoryId) {
        return purchaseReportMapper.selectDetails(enterpriseId, startDate, endDate, supplierId, productId, categoryId);
    }

    private List<Map<String, Object>> aggregate(List<Map<String, Object>> details, String type) {
        Map<Object, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> source : details) {
            Object key = switch (type) {
                case "SUPPLIER" -> source.get("supplierId");
                case "CATEGORY" -> source.get("categoryId") == null ? "NONE" : source.get("categoryId");
                default -> source.get("productId");
            };
            Map<String, Object> row = grouped.computeIfAbsent(key, ignored -> newAggregateRow(source, type));
            add(row, "grossQuantity", source.get("grossQuantity"));
            add(row, "returnQuantity", source.get("returnQuantity"));
            add(row, "netQuantity", source.get("netQuantity"));
            add(row, "purchaseAmount", source.get("purchaseAmount"));
            add(row, "returnAmount", source.get("returnAmount"));
            add(row, "netPurchaseAmount", source.get("netPurchaseAmount"));
            @SuppressWarnings("unchecked")
            Set<Object> orderIds = (Set<Object>) row.get("_orderIds");
            orderIds.add(source.get("orderId"));
        }
        List<Map<String, Object>> result = new ArrayList<>(grouped.values());
        result.forEach(row -> {
            @SuppressWarnings("unchecked")
            Set<Object> orderIds = (Set<Object>) row.remove("_orderIds");
            row.put("orderCount", orderIds.size());
        });
        result.sort(Comparator.comparing((Map<String, Object> row) -> decimal(row.get("netPurchaseAmount"))).reversed());
        return result;
    }

    private Map<String, Object> newAggregateRow(Map<String, Object> source, String type) {
        Map<String, Object> row = new LinkedHashMap<>();
        if ("SUPPLIER".equals(type)) {
            copy(source, row, "supplierId", "supplierCode", "supplierName");
        } else if ("CATEGORY".equals(type)) {
            copy(source, row, "categoryId", "categoryName");
        } else {
            copy(source, row, "productId", "productCode", "productName", "specification", "unitName", "categoryName");
        }
        row.put("grossQuantity", BigDecimal.ZERO);
        row.put("returnQuantity", BigDecimal.ZERO);
        row.put("netQuantity", BigDecimal.ZERO);
        row.put("purchaseAmount", BigDecimal.ZERO);
        row.put("returnAmount", BigDecimal.ZERO);
        row.put("netPurchaseAmount", BigDecimal.ZERO);
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

    private String normalizeType(String type) {
        String normalized = type == null ? "DETAIL" : type.trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(normalized)) throw new BusinessException("采购报表类型不正确");
        return normalized;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
    }
}
