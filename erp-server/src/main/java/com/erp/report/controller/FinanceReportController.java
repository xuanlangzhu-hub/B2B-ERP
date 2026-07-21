package com.erp.report.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.report.service.FinanceReportService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports/finance")
@RequiredArgsConstructor
public class FinanceReportController {
    private final FinanceReportService service;

    @GetMapping("/customers") @PreAuthorize("hasAuthority('report:finance:view')")
    public Result<PageResult<Map<String, Object>>> customers(@RequestParam(defaultValue="1") Integer page,
            @RequestParam(defaultValue="10") Integer size, @RequestParam(required=false) Long customerId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal LoginUser user) {
        return Result.success(service.customerStatements(user.getEnterpriseId(), page, size, customerId, startDate, endDate));
    }

    @GetMapping("/customers/summary") @PreAuthorize("hasAuthority('report:finance:view')")
    public Result<Map<String,Object>> customerSummary(@RequestParam(required=false) Long customerId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal LoginUser user) {
        return Result.success(service.customerSummary(user.getEnterpriseId(), customerId, startDate, endDate));
    }

    @GetMapping("/customers/ledger") @PreAuthorize("hasAuthority('report:finance:view')")
    public Result<PageResult<Map<String,Object>>> customerLedger(@RequestParam(defaultValue="1") Integer page,
            @RequestParam(defaultValue="10") Integer size, @RequestParam Long customerId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal LoginUser user) {
        return Result.success(service.customerLedger(user.getEnterpriseId(), page, size, customerId, startDate, endDate));
    }

    @GetMapping("/suppliers") @PreAuthorize("hasAuthority('report:finance:view')")
    public Result<PageResult<Map<String,Object>>> suppliers(@RequestParam(defaultValue="1") Integer page,
            @RequestParam(defaultValue="10") Integer size, @RequestParam(required=false) Long supplierId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal LoginUser user) {
        return Result.success(service.supplierStatements(user.getEnterpriseId(), page, size, supplierId, startDate, endDate));
    }

    @GetMapping("/suppliers/summary") @PreAuthorize("hasAuthority('report:finance:view')")
    public Result<Map<String,Object>> supplierSummary(@RequestParam(required=false) Long supplierId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal LoginUser user) {
        return Result.success(service.supplierSummary(user.getEnterpriseId(), supplierId, startDate, endDate));
    }

    @GetMapping("/suppliers/ledger") @PreAuthorize("hasAuthority('report:finance:view')")
    public Result<PageResult<Map<String,Object>>> supplierLedger(@RequestParam(defaultValue="1") Integer page,
            @RequestParam(defaultValue="10") Integer size, @RequestParam Long supplierId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal LoginUser user) {
        return Result.success(service.supplierLedger(user.getEnterpriseId(), page, size, supplierId, startDate, endDate));
    }
}
