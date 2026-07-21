package com.erp.finance.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.finance.dto.FinOtherTransactionRequest;
import com.erp.finance.entity.FinOtherTransaction;
import com.erp.finance.service.FinOtherTransactionService;
import com.erp.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/other-transactions") @RequiredArgsConstructor
public class FinOtherTransactionController {
    private final FinOtherTransactionService service;
    @GetMapping @PreAuthorize("hasAuthority('finance:other:list')")
    public Result<PageResult<FinOtherTransaction>> list(@RequestParam(defaultValue="1") Integer page,
            @RequestParam(defaultValue="10") Integer size, @RequestParam(required=false) String transactionType,
            @RequestParam(required=false) String category, @RequestParam(required=false) String status,
            @RequestParam(required=false) String startDate, @RequestParam(required=false) String endDate,
            @AuthenticationPrincipal LoginUser user) {
        return Result.success(service.pageTransactions(user.getEnterpriseId(),page,size,transactionType,category,status,startDate,endDate));
    }
    @PostMapping @PreAuthorize("hasAuthority('finance:other:list')")
    public Result<FinOtherTransaction> create(@Valid @RequestBody FinOtherTransactionRequest request,@AuthenticationPrincipal LoginUser user){return Result.success(service.create(request,user.getEnterpriseId(),user.getUserId()));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('finance:other:list')")
    public Result<Void> update(@PathVariable Long id,@Valid @RequestBody FinOtherTransactionRequest request,@AuthenticationPrincipal LoginUser user){service.update(id,request,user.getEnterpriseId(),user.getUserId());return Result.success();}
    @PostMapping("/{id}/confirm") @PreAuthorize("hasAuthority('finance:other:list')")
    public Result<Void> confirm(@PathVariable Long id,@AuthenticationPrincipal LoginUser user){service.confirm(id,user.getEnterpriseId(),user.getUserId());return Result.success();}
    @PostMapping("/{id}/cancel") @PreAuthorize("hasAuthority('finance:other:list')")
    public Result<Void> cancel(@PathVariable Long id,@AuthenticationPrincipal LoginUser user){service.cancel(id,user.getEnterpriseId(),user.getUserId());return Result.success();}
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('finance:other:list')")
    public Result<Void> delete(@PathVariable Long id,@AuthenticationPrincipal LoginUser user){service.delete(id,user.getEnterpriseId());return Result.success();}
}
