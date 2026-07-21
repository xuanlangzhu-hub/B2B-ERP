package com.erp.finance.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.finance.dto.FinPaymentRequest;
import com.erp.finance.entity.FinPayment;
import com.erp.finance.service.FinPaymentService;
import com.erp.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class FinPaymentController {

    private final FinPaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('finance:payment:list', 'purchase:order:list')")
    public Result<PageResult<FinPayment>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String paymentNo,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(paymentService.pagePayments(
                loginUser.getEnterpriseId(), page, size, paymentNo, supplierId, startDate, endDate));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('finance:payment:list', 'purchase:order:list')")
    public Result<FinPayment> create(@Valid @RequestBody FinPaymentRequest request,
                                      @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(paymentService.createPayment(request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('finance:payment:list', 'purchase:order:list')")
    public Result<Void> confirm(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        paymentService.confirmPayment(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('finance:payment:list', 'purchase:order:list')")
    public Result<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        paymentService.cancelPayment(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }
}
