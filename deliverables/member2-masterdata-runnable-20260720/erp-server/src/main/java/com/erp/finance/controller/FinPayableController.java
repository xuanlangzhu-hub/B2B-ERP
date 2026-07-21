package com.erp.finance.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.finance.entity.FinPayable;
import com.erp.finance.service.FinPayableService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payables")
@RequiredArgsConstructor
public class FinPayableController {
    private final FinPayableService payableService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('finance:accounting:list', 'finance:payment:list', 'purchase:order:list')")
    public Result<PageResult<FinPayable>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sourceNo,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(payableService.pagePayables(
                loginUser.getEnterpriseId(), page, size, supplierId, status, sourceNo));
    }
}
