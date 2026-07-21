package com.erp.inventory.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.inventory.dto.InvAdjustmentRequest;
import com.erp.inventory.entity.InvAdjustment;
import com.erp.inventory.service.InvAdjustmentService;
import com.erp.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory-adjustments")
@RequiredArgsConstructor
public class InvAdjustmentController {
    private final InvAdjustmentService adjustmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:adjustment:list')")
    public Result<PageResult<InvAdjustment>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String adjustmentNo,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String adjustmentType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(adjustmentService.pageAdjustments(loginUser.getEnterpriseId(), page, size,
                adjustmentNo, warehouseId, adjustmentType, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:adjustment:list')")
    public Result<InvAdjustment> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(adjustmentService.getDetail(id, loginUser.getEnterpriseId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inventory:adjustment:list')")
    public Result<InvAdjustment> create(@Valid @RequestBody InvAdjustmentRequest request,
                                         @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(adjustmentService.createAdjustment(
                request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('inventory:adjustment:list')")
    public Result<Void> approve(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        adjustmentService.approveAdjustment(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('inventory:adjustment:list')")
    public Result<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        adjustmentService.cancelAdjustment(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:adjustment:list')")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        adjustmentService.deleteAdjustment(id, loginUser.getEnterpriseId());
        return Result.success();
    }
}
