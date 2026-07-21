package com.erp.masterdata.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.masterdata.entity.MdSupplier;
import com.erp.masterdata.service.MdSupplierService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final MdSupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAuthority('md:supplier:list')")
    public Result<PageResult<MdSupplier>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String supplierCode,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(supplierService.pageQuery(loginUser.getEnterpriseId(), page, size, supplierCode, supplierName, contactPhone, categoryId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('md:supplier:list')")
    public Result<MdSupplier> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(supplierService.getDetail(id, loginUser.getEnterpriseId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('md:supplier:create')")
    public Result<MdSupplier> create(@RequestBody MdSupplier supplier,
                                      @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(supplierService.create(supplier, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('md:supplier:update')")
    public Result<Void> update(@PathVariable Long id, @RequestBody MdSupplier supplier,
                               @AuthenticationPrincipal LoginUser loginUser) {
        supplier.setId(id);
        supplierService.update(supplier, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('md:supplier:delete')")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        supplierService.delete(id, loginUser.getEnterpriseId());
        return Result.success();
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('md:supplier:list')")
    public Result<List<Map<String, Object>>> options(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(supplierService.options(loginUser.getEnterpriseId()));
    }
}
