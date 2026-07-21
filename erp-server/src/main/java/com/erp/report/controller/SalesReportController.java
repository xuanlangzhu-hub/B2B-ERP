package com.erp.report.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.report.service.SalesReportService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports/sales")
@RequiredArgsConstructor
public class SalesReportController {
    private final SalesReportService salesReportService;

    @GetMapping
    @PreAuthorize("hasAuthority('report:sales:view')")
    public Result<PageResult<Map<String, Object>>> page(
            @RequestParam(defaultValue = "DETAIL") String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal LoginUser user) {
        return Result.success(salesReportService.page(user.getEnterpriseId(), type, page, size,
                startDate, endDate, customerId, productId, categoryId));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('report:sales:view')")
    public Result<Map<String, Object>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal LoginUser user) {
        return Result.success(salesReportService.summary(user.getEnterpriseId(), startDate, endDate,
                customerId, productId, categoryId));
    }
}
