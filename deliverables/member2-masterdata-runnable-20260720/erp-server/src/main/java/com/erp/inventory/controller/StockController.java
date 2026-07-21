package com.erp.inventory.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.service.InvStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final InvStockService stockService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:stock:list')")
    public Result<PageResult<InvStockBalance>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Boolean lowStock) {
        return Result.success(stockService.pageStocks(page, size, warehouseId, productCode, productName, lowStock));
    }
}
