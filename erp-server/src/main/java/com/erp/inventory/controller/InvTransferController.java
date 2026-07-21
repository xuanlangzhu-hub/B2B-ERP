package com.erp.inventory.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.inventory.dto.InvTransferRequest;
import com.erp.inventory.entity.InvTransfer;
import com.erp.inventory.service.InvTransferService;
import com.erp.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory-transfers")
@RequiredArgsConstructor
public class InvTransferController {
    private final InvTransferService transferService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:transfer:list')")
    public Result<PageResult<InvTransfer>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String transferNo,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(transferService.pageTransfers(loginUser.getEnterpriseId(), page, size,
                transferNo, warehouseId, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:transfer:list')")
    public Result<InvTransfer> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(transferService.getDetail(id, loginUser.getEnterpriseId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inventory:transfer:list')")
    public Result<InvTransfer> create(@Valid @RequestBody InvTransferRequest request,
                                       @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(transferService.createTransfer(
                request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('inventory:transfer:list')")
    public Result<Void> approve(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        transferService.approveTransfer(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('inventory:transfer:list')")
    public Result<Void> complete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        transferService.completeTransfer(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('inventory:transfer:list')")
    public Result<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        transferService.cancelTransfer(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:transfer:list')")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        transferService.deleteTransfer(id, loginUser.getEnterpriseId());
        return Result.success();
    }
}
