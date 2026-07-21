package com.erp.masterdata.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.service.MdProductService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final MdProductService productService;

    @GetMapping
    @PreAuthorize("hasAuthority('md:product:list')")
    public Result<PageResult<MdProduct>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(productService.pageQuery(loginUser.getEnterpriseId(), page, size, productCode, productName, barcode, categoryId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('md:product:list')")
    public Result<MdProduct> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(productService.getDetail(id, loginUser.getEnterpriseId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('md:product:create')")
    public Result<MdProduct> create(@RequestBody MdProduct product,
                                     @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(productService.create(product, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('md:product:update')")
    public Result<Void> update(@PathVariable Long id, @RequestBody MdProduct product,
                               @AuthenticationPrincipal LoginUser loginUser) {
        product.setId(id);
        productService.update(product, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('md:product:delete')")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        productService.delete(id, loginUser.getEnterpriseId());
        return Result.success();
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('md:product:list')")
    public Result<List<Map<String, Object>>> options(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(productService.options(loginUser.getEnterpriseId()));
    }
}
