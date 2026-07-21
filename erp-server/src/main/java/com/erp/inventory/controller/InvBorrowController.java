package com.erp.inventory.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.inventory.dto.InvBorrowRequest;
import com.erp.inventory.dto.InvBorrowReturnRequest;
import com.erp.inventory.entity.InvBorrow;
import com.erp.inventory.service.InvBorrowService;
import com.erp.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory-borrows")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('inventory:borrow-out:list','inventory:borrow-in:list')")
public class InvBorrowController {
    private final InvBorrowService borrowService;

    @GetMapping
    public Result<PageResult<InvBorrow>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String borrowNo,
            @RequestParam(required = false) String borrowType,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String partnerName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean overdueOnly,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(borrowService.pageBorrows(loginUser.getEnterpriseId(), page, size,
                borrowNo, borrowType, warehouseId, partnerName, status, overdueOnly));
    }

    @GetMapping("/{id}")
    public Result<InvBorrow> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(borrowService.getDetail(id, loginUser.getEnterpriseId()));
    }

    @PostMapping
    public Result<InvBorrow> create(@Valid @RequestBody InvBorrowRequest request,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(borrowService.createBorrow(request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        borrowService.approveBorrow(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/return")
    public Result<Void> returnBorrow(@PathVariable Long id, @Valid @RequestBody InvBorrowReturnRequest request,
                                     @AuthenticationPrincipal LoginUser loginUser) {
        borrowService.returnBorrow(id, request, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        borrowService.cancelBorrow(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        borrowService.deleteBorrow(id, loginUser.getEnterpriseId());
        return Result.success();
    }
}
