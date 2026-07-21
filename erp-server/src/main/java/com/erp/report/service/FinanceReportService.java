package com.erp.report.service;

import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.report.mapper.FinanceReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FinanceReportService {
    private final FinanceReportMapper financeReportMapper;

    public PageResult<Map<String, Object>> customerStatements(Long enterpriseId, Integer page, Integer size,
                                                               Long customerId, LocalDate startDate, LocalDate endDate) {
        return statements(loadCustomer(enterpriseId, customerId, endDate), "SALE", "RECEIPT",
                page, size, startDate, endDate);
    }

    public PageResult<Map<String, Object>> supplierStatements(Long enterpriseId, Integer page, Integer size,
                                                               Long supplierId, LocalDate startDate, LocalDate endDate) {
        return statements(loadSupplier(enterpriseId, supplierId, endDate), "PURCHASE", "PAYMENT",
                page, size, startDate, endDate);
    }

    public Map<String, Object> customerSummary(Long enterpriseId, Long customerId,
                                                LocalDate startDate, LocalDate endDate) {
        return summary(statementRows(loadCustomer(enterpriseId, customerId, endDate), "SALE", "RECEIPT", startDate));
    }

    public Map<String, Object> supplierSummary(Long enterpriseId, Long supplierId,
                                                LocalDate startDate, LocalDate endDate) {
        return summary(statementRows(loadSupplier(enterpriseId, supplierId, endDate), "PURCHASE", "PAYMENT", startDate));
    }

    public PageResult<Map<String, Object>> customerLedger(Long enterpriseId, Integer page, Integer size,
                                                           Long customerId, LocalDate startDate, LocalDate endDate) {
        if (customerId == null) throw new BusinessException("请选择客户");
        return ledger(loadCustomer(enterpriseId, customerId, endDate), "SALE", page, size, startDate, endDate);
    }

    public PageResult<Map<String, Object>> supplierLedger(Long enterpriseId, Integer page, Integer size,
                                                           Long supplierId, LocalDate startDate, LocalDate endDate) {
        if (supplierId == null) throw new BusinessException("请选择供应商");
        return ledger(loadSupplier(enterpriseId, supplierId, endDate), "PURCHASE", page, size, startDate, endDate);
    }

    private List<Map<String, Object>> loadCustomer(Long enterpriseId, Long id, LocalDate endDate) {
        return financeReportMapper.customerEvents(enterpriseId, id, endDate);
    }

    private List<Map<String, Object>> loadSupplier(Long enterpriseId, Long id, LocalDate endDate) {
        return financeReportMapper.supplierEvents(enterpriseId, id, endDate);
    }

    private PageResult<Map<String, Object>> statements(List<Map<String, Object>> events, String increaseType,
                                                        String paymentType, Integer page, Integer size,
                                                        LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);
        List<Map<String, Object>> rows = statementRows(events, increaseType, paymentType, startDate);
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null ? 10 : Math.min(Math.max(size, 1), 100);
        int from = Math.min((safePage - 1) * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        return PageResult.of(rows.subList(from, to), rows.size(), safePage, safeSize);
    }

    private List<Map<String, Object>> statementRows(List<Map<String, Object>> events, String increaseType,
                                                     String paymentType, LocalDate startDate) {
        Map<Object, Map<String, Object>> partners = new LinkedHashMap<>();
        for (Map<String, Object> event : events) {
            Object id = event.get("partnerId");
            Map<String, Object> row = partners.computeIfAbsent(id, ignored -> newStatement(event));
            BigDecimal amount = decimal(event.get("amount"));
            LocalDate date = toDate(event.get("businessDate"));
            String type = String.valueOf(event.get("eventType"));
            boolean beforePeriod = startDate != null && date.isBefore(startDate);
            if (beforePeriod) {
                add(row, "openingBalance", increaseType.equals(type) ? amount : amount.negate());
            } else if (increaseType.equals(type)) {
                add(row, "increaseAmount", amount);
            } else if ("RETURN".equals(type)) {
                add(row, "returnAmount", amount);
            } else if (paymentType.equals(type)) {
                add(row, "paymentAmount", amount);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>(partners.values());
        rows.forEach(row -> row.put("closingBalance", decimal(row.get("openingBalance"))
                .add(decimal(row.get("increaseAmount"))).subtract(decimal(row.get("returnAmount")))
                .subtract(decimal(row.get("paymentAmount")))));
        rows.sort(Comparator.comparing((Map<String, Object> row) -> decimal(row.get("closingBalance"))).reversed());
        return rows;
    }

    private Map<String, Object> newStatement(Map<String, Object> event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("partnerId", event.get("partnerId"));
        row.put("partnerCode", event.get("partnerCode"));
        row.put("partnerName", event.get("partnerName"));
        row.put("openingBalance", BigDecimal.ZERO);
        row.put("increaseAmount", BigDecimal.ZERO);
        row.put("returnAmount", BigDecimal.ZERO);
        row.put("paymentAmount", BigDecimal.ZERO);
        return row;
    }

    private Map<String, Object> summary(List<Map<String, Object>> rows) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("partnerCount", rows.size());
        result.put("openingBalance", sum(rows, "openingBalance"));
        result.put("increaseAmount", sum(rows, "increaseAmount"));
        result.put("returnAmount", sum(rows, "returnAmount"));
        result.put("paymentAmount", sum(rows, "paymentAmount"));
        result.put("closingBalance", sum(rows, "closingBalance"));
        return result;
    }

    private PageResult<Map<String, Object>> ledger(List<Map<String, Object>> events, String increaseType,
                                                    Integer page, Integer size, LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);
        BigDecimal balance = BigDecimal.ZERO;
        List<Map<String, Object>> periodRows = new ArrayList<>();
        for (Map<String, Object> source : events) {
            LocalDate date = toDate(source.get("businessDate"));
            BigDecimal amount = decimal(source.get("amount"));
            boolean increase = increaseType.equals(String.valueOf(source.get("eventType")));
            balance = increase ? balance.add(amount) : balance.subtract(amount);
            if (startDate != null && date.isBefore(startDate)) continue;
            Map<String, Object> row = new LinkedHashMap<>(source);
            row.put("increaseAmount", increase ? amount : BigDecimal.ZERO);
            row.put("decreaseAmount", increase ? BigDecimal.ZERO : amount);
            row.put("balance", balance);
            periodRows.add(row);
        }
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null ? 10 : Math.min(Math.max(size, 1), 100);
        int from = Math.min((safePage - 1) * safeSize, periodRows.size());
        int to = Math.min(from + safeSize, periodRows.size());
        return PageResult.of(periodRows.subList(from, to), periodRows.size(), safePage, safeSize);
    }

    private void add(Map<String, Object> row, String field, BigDecimal amount) {
        row.put(field, decimal(row.get(field)).add(amount));
    }

    private BigDecimal sum(List<Map<String, Object>> rows, String field) {
        return rows.stream().map(row -> decimal(row.get(field))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return value instanceof BigDecimal number ? number : new BigDecimal(value.toString());
    }

    private LocalDate toDate(Object value) {
        return value instanceof LocalDate date ? date : LocalDate.parse(value.toString());
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
    }
}
