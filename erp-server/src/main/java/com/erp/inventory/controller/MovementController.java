package com.erp.inventory.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.inventory.entity.InvStockMovement;
import com.erp.inventory.service.InvMovementService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stock-movements")
@RequiredArgsConstructor
public class MovementController {

    private final InvMovementService movementService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:movement:list')")
    public Result<PageResult<InvStockMovement>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) String sourceNo,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(movementService.pageMovements(
                loginUser.getEnterpriseId(), page, size, warehouseId, productId,
                movementType, sourceNo, startDate, endDate));
    }
}
