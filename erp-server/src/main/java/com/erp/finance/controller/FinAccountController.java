package com.erp.finance.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.finance.dto.FinAccountRequest;
import com.erp.finance.entity.FinAccount;
import com.erp.finance.service.FinAccountService;
import com.erp.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class FinAccountController {
    private final FinAccountService accountService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:accounting:list')")
    public Result<PageResult<FinAccount>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(accountService.pageAccounts(
                loginUser.getEnterpriseId(), page, size, keyword, status));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('finance:accounting:list', 'finance:receipt:list', 'finance:payment:list', 'sales:order:list', 'purchase:order:list')")
    public Result<List<Map<String, Object>>> options(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(accountService.options(loginUser.getEnterpriseId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:accounting:list')")
    public Result<FinAccount> create(@Valid @RequestBody FinAccountRequest request,
                                     @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(accountService.create(request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:accounting:list')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody FinAccountRequest request,
                               @AuthenticationPrincipal LoginUser loginUser) {
        accountService.update(id, request, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:accounting:list')")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        accountService.delete(id, loginUser.getEnterpriseId());
        return Result.success();
    }
}
