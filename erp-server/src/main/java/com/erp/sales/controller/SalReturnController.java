package com.erp.sales.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.sales.dto.SalReturnRequest;
import com.erp.sales.entity.SalReturn;
import com.erp.sales.service.SalReturnService;
import com.erp.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales-returns")
@RequiredArgsConstructor
public class SalReturnController {
    private final SalReturnService returnService;

    @GetMapping
    @PreAuthorize("hasAuthority('sales:return:list')")
    public Result<PageResult<SalReturn>> list(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size,
                                              @RequestParam(required = false) String returnNo,
                                              @RequestParam(required = false) Long customerId,
                                              @RequestParam(required = false) String status,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(returnService.pageReturns(
                loginUser.getEnterpriseId(), page, size, returnNo, customerId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:return:list')")
    public Result<SalReturn> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(returnService.detail(id, loginUser.getEnterpriseId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales:return:list')")
    public Result<SalReturn> create(@Valid @RequestBody SalReturnRequest request,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(returnService.create(request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('sales:return:list')")
    public Result<Void> approve(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        returnService.approve(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('sales:return:list')")
    public Result<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        returnService.cancel(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }
}
