package com.erp.system.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.security.LoginUser;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.service.OrgWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final OrgWarehouseService warehouseService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:warehouse:list')")
    public Result<PageResult<OrgWarehouse>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) String status) {
        return Result.success(warehouseService.pageQuery(page, size, warehouseCode, warehouseName, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:warehouse:list')")
    public Result<OrgWarehouse> detail(@PathVariable Long id) {
        return Result.success(warehouseService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:warehouse:create')")
    public Result<OrgWarehouse> create(@RequestBody OrgWarehouse warehouse,
                                        @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(warehouseService.create(warehouse, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:warehouse:update')")
    public Result<Void> update(@PathVariable Long id, @RequestBody OrgWarehouse warehouse) {
        warehouse.setId(id);
        warehouseService.update(warehouse);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:warehouse:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return Result.success();
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('system:warehouse:list')")
    public Result<List<Map<String, Object>>> options(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(warehouseService.options(loginUser.getEnterpriseId()));
    }
}
