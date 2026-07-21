package com.erp.masterdata.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.masterdata.entity.MdCustomerCategory;
import com.erp.masterdata.entity.MdCustomerLevel;
import com.erp.masterdata.entity.MdSupplierCategory;
import com.erp.masterdata.service.MdPartnerConfigService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PartnerConfigController {
    private final MdPartnerConfigService configService;

    @GetMapping("/api/v1/customer-categories")
    @PreAuthorize("hasAuthority('md:customer-category:list')")
    public Result<PageResult<MdCustomerCategory>> customerCategories(
            @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String code, @RequestParam(required = false) String name,
            @RequestParam(required = false) String status, @AuthenticationPrincipal LoginUser user) {
        return Result.success(configService.pageCustomerCategories(user.getEnterpriseId(), page, size, code, name, status));
    }

    @PostMapping("/api/v1/customer-categories")
    @PreAuthorize("hasAuthority('md:customer-category:list')")
    public Result<MdCustomerCategory> createCustomerCategory(@RequestBody MdCustomerCategory row,
                                                               @AuthenticationPrincipal LoginUser user) {
        return Result.success(configService.saveCustomerCategory(null, row, user.getEnterpriseId()));
    }

    @PutMapping("/api/v1/customer-categories/{id}")
    @PreAuthorize("hasAuthority('md:customer-category:list')")
    public Result<MdCustomerCategory> updateCustomerCategory(@PathVariable Long id, @RequestBody MdCustomerCategory row,
                                                               @AuthenticationPrincipal LoginUser user) {
        return Result.success(configService.saveCustomerCategory(id, row, user.getEnterpriseId()));
    }

    @DeleteMapping("/api/v1/customer-categories/{id}")
    @PreAuthorize("hasAuthority('md:customer-category:list')")
    public Result<Void> deleteCustomerCategory(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        configService.deleteCustomerCategory(id, user.getEnterpriseId()); return Result.success();
    }

    @GetMapping("/api/v1/customer-categories/options")
    @PreAuthorize("hasAuthority('md:customer:list')")
    public Result<List<Map<String, Object>>> customerCategoryOptions(@AuthenticationPrincipal LoginUser user) {
        return Result.success(configService.customerCategoryOptions(user.getEnterpriseId()));
    }

    @GetMapping("/api/v1/customer-levels")
    @PreAuthorize("hasAuthority('md:customer-level:list')")
    public Result<PageResult<MdCustomerLevel>> customerLevels(
            @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String code, @RequestParam(required = false) String name,
            @RequestParam(required = false) String status, @AuthenticationPrincipal LoginUser user) {
        return Result.success(configService.pageCustomerLevels(user.getEnterpriseId(), page, size, code, name, status));
    }

    @PostMapping("/api/v1/customer-levels")
    @PreAuthorize("hasAuthority('md:customer-level:list')")
    public Result<MdCustomerLevel> createCustomerLevel(@RequestBody MdCustomerLevel row,
                                                         @AuthenticationPrincipal LoginUser user) {
        return Result.success(configService.saveCustomerLevel(null, row, user.getEnterpriseId()));
    }

    @PutMapping("/api/v1/customer-levels/{id}")
    @PreAuthorize("hasAuthority('md:customer-level:list')")
    public Result<MdCustomerLevel> updateCustomerLevel(@PathVariable Long id, @RequestBody MdCustomerLevel row,
                                                         @AuthenticationPrincipal LoginUser user) {
        return Result.success(configService.saveCustomerLevel(id, row, user.getEnterpriseId()));
    }

    @DeleteMapping("/api/v1/customer-levels/{id}")
    @PreAuthorize("hasAuthority('md:customer-level:list')")
    public Result<Void> deleteCustomerLevel(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        configService.deleteCustomerLevel(id, user.getEnterpriseId()); return Result.success();
    }

    @GetMapping("/api/v1/customer-levels/options")
    @PreAuthorize("hasAuthority('md:customer:list')")
    public Result<List<Map<String, Object>>> customerLevelOptions(@AuthenticationPrincipal LoginUser user) {
        return Result.success(configService.customerLevelOptions(user.getEnterpriseId()));
    }

    @GetMapping("/api/v1/supplier-categories")
    @PreAuthorize("hasAuthority('md:supplier-category:list')")
    public Result<PageResult<MdSupplierCategory>> supplierCategories(
            @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String code, @RequestParam(required = false) String name,
            @RequestParam(required = false) String status, @AuthenticationPrincipal LoginUser user) {
        return Result.success(configService.pageSupplierCategories(user.getEnterpriseId(), page, size, code, name, status));
    }

    @PostMapping("/api/v1/supplier-categories")
    @PreAuthorize("hasAuthority('md:supplier-category:list')")
    public Result<MdSupplierCategory> createSupplierCategory(@RequestBody MdSupplierCategory row,
                                                               @AuthenticationPrincipal LoginUser user) {
        return Result.success(configService.saveSupplierCategory(null, row, user.getEnterpriseId()));
    }

    @PutMapping("/api/v1/supplier-categories/{id}")
    @PreAuthorize("hasAuthority('md:supplier-category:list')")
    public Result<MdSupplierCategory> updateSupplierCategory(@PathVariable Long id, @RequestBody MdSupplierCategory row,
                                                               @AuthenticationPrincipal LoginUser user) {
        return Result.success(configService.saveSupplierCategory(id, row, user.getEnterpriseId()));
    }

    @DeleteMapping("/api/v1/supplier-categories/{id}")
    @PreAuthorize("hasAuthority('md:supplier-category:list')")
    public Result<Void> deleteSupplierCategory(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        configService.deleteSupplierCategory(id, user.getEnterpriseId()); return Result.success();
    }

    @GetMapping("/api/v1/supplier-categories/options")
    @PreAuthorize("hasAuthority('md:supplier:list')")
    public Result<List<Map<String, Object>>> supplierCategoryOptions(@AuthenticationPrincipal LoginUser user) {
        return Result.success(configService.supplierCategoryOptions(user.getEnterpriseId()));
    }
}
