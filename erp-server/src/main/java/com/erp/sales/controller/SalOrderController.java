package com.erp.sales.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.sales.dto.SalOrderRequest;
import com.erp.sales.entity.SalOrder;
import com.erp.sales.service.SalOrderService;
import com.erp.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sales-orders")
@RequiredArgsConstructor
public class SalOrderController {

    private final SalOrderService salOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('sales:order:list')")
    public Result<PageResult<SalOrder>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(salOrderService.pageOrders(page, size, orderNo, customerId, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:order:list')")
    public Result<SalOrder> detail(@PathVariable Long id) {
        return Result.success(salOrderService.getDetailWithItems(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales:order:list')")
    public Result<SalOrder> create(@Valid @RequestBody SalOrderRequest request,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(salOrderService.createOrder(request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:order:list')")
    public Result<SalOrder> update(@PathVariable Long id, @Valid @RequestBody SalOrderRequest request) {
        return Result.success(salOrderService.updateOrder(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:order:list')")
    public Result<Void> delete(@PathVariable Long id) {
        salOrderService.deleteOrder(id);
        return Result.success();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('sales:order:list')")
    public Result<Void> approve(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        salOrderService.approve(id, loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('sales:order:list')")
    public Result<Void> cancel(@PathVariable Long id) {
        salOrderService.cancel(id);
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('sales:order:list')")
    public Result<Void> complete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        salOrderService.complete(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @GetMapping("/outbound-options")
    @PreAuthorize("hasAuthority('sales:order:list')")
    public Result<List<SalOrder>> outboundOptions() {
        return Result.success(salOrderService.getOutboundOptions());
    }
}
