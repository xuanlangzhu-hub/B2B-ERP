package com.erp.finance.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.finance.entity.FinReceivable;
import com.erp.finance.service.FinReceivableService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/receivables")
@RequiredArgsConstructor
public class FinReceivableController {
    private final FinReceivableService receivableService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('finance:accounting:list', 'finance:receipt:list', 'sales:order:list')")
    public Result<PageResult<FinReceivable>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sourceNo,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(receivableService.pageReceivables(
                loginUser.getEnterpriseId(), page, size, customerId, status, sourceNo));
    }
}
