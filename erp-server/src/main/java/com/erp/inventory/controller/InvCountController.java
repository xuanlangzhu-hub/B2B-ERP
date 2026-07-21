package com.erp.inventory.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.inventory.dto.InvCountCreateRequest;
import com.erp.inventory.dto.InvCountUpdateRequest;
import com.erp.inventory.entity.InvCount;
import com.erp.inventory.service.InvCountService;
import com.erp.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory-counts")
@RequiredArgsConstructor
public class InvCountController {

    private final InvCountService countService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:count:list')")
    public Result<PageResult<InvCount>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String countNo,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(countService.pageCounts(loginUser.getEnterpriseId(), page, size,
                countNo, warehouseId, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:count:list')")
    public Result<InvCount> detail(@PathVariable Long id,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(countService.getDetail(id, loginUser.getEnterpriseId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inventory:count:list')")
    public Result<InvCount> create(@Valid @RequestBody InvCountCreateRequest request,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(countService.createCount(
                request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:count:list')")
    public Result<InvCount> update(@PathVariable Long id,
                                    @Valid @RequestBody InvCountUpdateRequest request,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(countService.updateActualQuantities(
                id, request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('inventory:count:list')")
    public Result<Void> start(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        countService.startCount(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('inventory:count:list')")
    public Result<Void> submit(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        countService.submitCount(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('inventory:count:list')")
    public Result<Void> approve(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        countService.approveCount(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('inventory:count:list')")
    public Result<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        countService.cancelCount(id, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:count:list')")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        countService.deleteCount(id, loginUser.getEnterpriseId());
        return Result.success();
    }
}
