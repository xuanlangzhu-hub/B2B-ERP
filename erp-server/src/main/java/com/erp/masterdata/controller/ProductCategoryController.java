package com.erp.masterdata.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.masterdata.entity.MdProductCategory;
import com.erp.masterdata.service.MdProductCategoryService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final MdProductCategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('md:category:list')")
    public Result<PageResult<MdProductCategory>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(categoryService.pageQuery(loginUser.getEnterpriseId(), page, size, categoryCode, categoryName, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('md:category:list')")
    public Result<MdProductCategory> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(categoryService.getDetail(id, loginUser.getEnterpriseId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('md:category:create')")
    public Result<MdProductCategory> create(@RequestBody MdProductCategory category,
                                             @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(categoryService.create(category, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('md:category:update')")
    public Result<Void> update(@PathVariable Long id, @RequestBody MdProductCategory category,
                               @AuthenticationPrincipal LoginUser loginUser) {
        category.setId(id);
        categoryService.update(category, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('md:category:delete')")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        categoryService.delete(id, loginUser.getEnterpriseId());
        return Result.success();
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('md:category:list')")
    public Result<List<Map<String, Object>>> options(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(categoryService.options(loginUser.getEnterpriseId()));
    }
}
