package com.erp.masterdata.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.masterdata.entity.MdUnit;
import com.erp.masterdata.service.MdUnitService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
public class UnitController {

    private final MdUnitService unitService;

    @GetMapping
    @PreAuthorize("hasAuthority('md:unit:list')")
    public Result<PageResult<MdUnit>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String unitCode,
            @RequestParam(required = false) String unitName,
            @RequestParam(required = false) String status) {
        return Result.success(unitService.pageQuery(page, size, unitCode, unitName, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('md:unit:list')")
    public Result<MdUnit> detail(@PathVariable Long id) {
        return Result.success(unitService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('md:unit:create')")
    public Result<MdUnit> create(@RequestBody MdUnit unit,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(unitService.create(unit, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('md:unit:update')")
    public Result<Void> update(@PathVariable Long id, @RequestBody MdUnit unit) {
        unit.setId(id);
        unitService.update(unit);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('md:unit:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        unitService.delete(id);
        return Result.success();
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('md:unit:list')")
    public Result<List<Map<String, Object>>> options(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(unitService.options(loginUser.getEnterpriseId()));
    }
}
