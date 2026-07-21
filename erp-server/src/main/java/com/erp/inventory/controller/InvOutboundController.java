package com.erp.inventory.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.inventory.entity.InvOutbound;
import com.erp.inventory.service.InvOutboundService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/outbounds")
@RequiredArgsConstructor
public class InvOutboundController {

    private final InvOutboundService outboundService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:outbound:list')")
    public Result<PageResult<InvOutbound>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String outboundNo,
            @RequestParam(required = false) String sourceNo,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(outboundService.pageOutbounds(loginUser.getEnterpriseId(), page, size,
                outboundNo, sourceNo, warehouseId, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:outbound:list')")
    public Result<InvOutbound> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(outboundService.getDetailWithItems(id, loginUser.getEnterpriseId()));
    }

    @PostMapping("/from-sales/{salesOrderId}")
    @PreAuthorize("hasAuthority('inventory:outbound:list')")
    public Result<InvOutbound> createFromSales(@PathVariable Long salesOrderId,
                                                @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(outboundService.createFromSales(
                salesOrderId, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PostMapping("/from-purchase-return/{returnId}")
    @PreAuthorize("hasAnyAuthority('inventory:outbound:list', 'purchase:return:list')")
    public Result<InvOutbound> createFromPurchaseReturn(@PathVariable Long returnId,
                                                         @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(outboundService.createFromPurchaseReturn(
                returnId, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('inventory:outbound:list')")
    public Result<Void> confirm(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        outboundService.confirmOutbound(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('inventory:outbound:list')")
    public Result<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        outboundService.cancelOutbound(id, loginUser.getEnterpriseId());
        return Result.success();
    }
}
