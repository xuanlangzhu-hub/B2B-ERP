package com.erp.inventory.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.entity.InvStockMovement;
import com.erp.inventory.service.InvMovementService;
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
    private final InvMovementService movementService;

    @GetMapping
    @PreAuthorize("hasAuthority('report:inventory:view')")
    public Result<PageResult<InvStockBalance>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean lowStock,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(stockService.pageStocks(loginUser.getEnterpriseId(), page, size,
                warehouseId, productCode, productName, lowStock, categoryId));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('report:inventory:view')")
    public Result<Map<String, Object>> summary(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean lowStock,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(stockService.summaryStocks(loginUser.getEnterpriseId(),
                warehouseId, productCode, productName, lowStock, categoryId));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('report:inventory:view')")
    public Result<PageResult<InvStockMovement>> movements(
            @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long warehouseId, @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String movementType, @RequestParam(required = false) String direction,
            @RequestParam(required = false) Long categoryId, @RequestParam(required = false) String sourceNo,
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal LoginUser user) {
        return Result.success(movementService.pageMovements(user.getEnterpriseId(), page, size, warehouseId,
                productId, movementType, direction, categoryId, sourceNo, startDate, endDate));
    }

    @GetMapping("/movements/summary")
    @PreAuthorize("hasAuthority('report:inventory:view')")
    public Result<Map<String, Object>> movementSummary(
            @RequestParam(required = false) Long warehouseId, @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String movementType, @RequestParam(required = false) String direction,
            @RequestParam(required = false) Long categoryId, @RequestParam(required = false) String sourceNo,
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal LoginUser user) {
        return Result.success(movementService.summaryMovements(user.getEnterpriseId(), warehouseId, productId,
                movementType, direction, categoryId, sourceNo, startDate, endDate));
    }
}
