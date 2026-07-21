package com.erp.finance.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.finance.dto.FinReceiptRequest;
import com.erp.finance.entity.FinReceipt;
import com.erp.finance.service.FinReceiptService;
import com.erp.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
public class FinReceiptController {

    private final FinReceiptService receiptService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('finance:receipt:list', 'sales:order:list')")
    public Result<PageResult<FinReceipt>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String receiptNo,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(receiptService.pageReceipts(
                loginUser.getEnterpriseId(), page, size, receiptNo, customerId, startDate, endDate));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('finance:receipt:list', 'sales:order:list')")
    public Result<FinReceipt> create(@Valid @RequestBody FinReceiptRequest request,
                                      @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(receiptService.createReceipt(request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('finance:receipt:list', 'sales:order:list')")
    public Result<Void> confirm(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        receiptService.confirmReceipt(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('finance:receipt:list', 'sales:order:list')")
    public Result<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        receiptService.cancelReceipt(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }
}
