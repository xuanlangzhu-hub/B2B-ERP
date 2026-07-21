package com.erp.inventory.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.service.InvStockService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports/inventory")
@RequiredArgsConstructor
public class InventoryReportController {

    private final InvStockService stockService;

    @GetMapping
    @PreAuthorize("hasAuthority('report:inventory:view')")
    public Result<PageResult<InvStockBalance>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productName,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(stockService.pageStocks(loginUser.getEnterpriseId(), page, size,
                warehouseId, productCode, productName, false));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('report:inventory:view')")
    public Result<Map<String, Object>> summary(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productName,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(stockService.summaryStocks(loginUser.getEnterpriseId(),
                warehouseId, productCode, productName));
    }
}
