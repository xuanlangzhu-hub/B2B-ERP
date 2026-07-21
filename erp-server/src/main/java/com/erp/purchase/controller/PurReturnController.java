package com.erp.purchase.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.purchase.dto.PurReturnRequest;
import com.erp.purchase.entity.PurReturn;
import com.erp.purchase.service.PurReturnService;
import com.erp.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchase-returns")
@RequiredArgsConstructor
public class PurReturnController {
    private final PurReturnService returnService;

    @GetMapping
    @PreAuthorize("hasAuthority('purchase:return:list')")
    public Result<PageResult<PurReturn>> list(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size,
                                              @RequestParam(required = false) String returnNo,
                                              @RequestParam(required = false) Long supplierId,
                                              @RequestParam(required = false) String status,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(returnService.pageReturns(
                loginUser.getEnterpriseId(), page, size, returnNo, supplierId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:return:list')")
    public Result<PurReturn> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(returnService.detail(id, loginUser.getEnterpriseId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('purchase:return:list')")
    public Result<PurReturn> create(@Valid @RequestBody PurReturnRequest request,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(returnService.create(request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('purchase:return:list')")
    public Result<Void> approve(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        returnService.approve(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('purchase:return:list')")
    public Result<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        returnService.cancel(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }
}
