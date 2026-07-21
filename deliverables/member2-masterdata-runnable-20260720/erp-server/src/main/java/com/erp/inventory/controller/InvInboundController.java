package com.erp.inventory.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.inventory.entity.InvInbound;
import com.erp.inventory.service.InvInboundService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inbounds")
@RequiredArgsConstructor
public class InvInboundController {

    private final InvInboundService inboundService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:inbound:list')")
    public Result<PageResult<InvInbound>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String inboundNo,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(inboundService.pageInbounds(loginUser.getEnterpriseId(), page, size, inboundNo, warehouseId, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:inbound:list')")
    public Result<InvInbound> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(inboundService.getDetailWithItems(id, loginUser.getEnterpriseId()));
    }

    @PostMapping("/from-purchase/{purchaseOrderId}")
    @PreAuthorize("hasAuthority('inventory:inbound:list')")
    public Result<InvInbound> createFromPurchase(@PathVariable Long purchaseOrderId,
                                                  @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(inboundService.createFromPurchase(
                purchaseOrderId, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PostMapping("/from-sales-return/{returnId}")
    @PreAuthorize("hasAnyAuthority('inventory:inbound:list', 'sales:return:list')")
    public Result<InvInbound> createFromSalesReturn(@PathVariable Long returnId,
                                                     @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(inboundService.createFromSalesReturn(
                returnId, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('inventory:inbound:list')")
    public Result<Void> confirm(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        inboundService.confirmInbound(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('inventory:inbound:list')")
    public Result<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        inboundService.cancelInbound(id, loginUser.getEnterpriseId());
        return Result.success();
    }
}
