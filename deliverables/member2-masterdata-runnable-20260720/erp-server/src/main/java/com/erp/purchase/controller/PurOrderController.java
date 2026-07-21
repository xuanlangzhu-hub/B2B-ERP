package com.erp.purchase.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.purchase.dto.PurOrderRequest;
import com.erp.purchase.entity.PurOrder;
import com.erp.purchase.service.PurOrderService;
import com.erp.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurOrderController {

    private final PurOrderService purOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('purchase:order:list')")
    public Result<PageResult<PurOrder>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(purOrderService.pageOrders(page, size, orderNo, supplierId, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:order:list')")
    public Result<PurOrder> detail(@PathVariable Long id) {
        return Result.success(purOrderService.getDetailWithItems(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('purchase:order:list')")
    public Result<PurOrder> create(@Valid @RequestBody PurOrderRequest request,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(purOrderService.createOrder(request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:order:list')")
    public Result<PurOrder> update(@PathVariable Long id, @Valid @RequestBody PurOrderRequest request) {
        return Result.success(purOrderService.updateOrder(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:order:list')")
    public Result<Void> delete(@PathVariable Long id) {
        purOrderService.deleteOrder(id);
        return Result.success();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('purchase:order:list')")
    public Result<Void> approve(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        purOrderService.approve(id, loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('purchase:order:list')")
    public Result<Void> cancel(@PathVariable Long id) {
        purOrderService.cancel(id);
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('purchase:order:list')")
    public Result<Void> complete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        purOrderService.complete(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @GetMapping("/inbound-options")
    @PreAuthorize("hasAuthority('purchase:order:list')")
    public Result<List<PurOrder>> inboundOptions() {
        return Result.success(purOrderService.getInboundOptions());
    }
}
