package com.erp.masterdata.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.masterdata.entity.MdCustomerTag;
import com.erp.masterdata.entity.MdProductAttribute;
import com.erp.masterdata.entity.MdProductTag;
import com.erp.masterdata.service.MdMetadataService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MetadataController {
    private final MdMetadataService metadataService;

    @GetMapping("/api/v1/product-attributes")
    @PreAuthorize("hasAuthority('md:attribute:list')")
    public Result<PageResult<MdProductAttribute>> attributes(
            @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String code, @RequestParam(required = false) String name,
            @RequestParam(required = false) String status, @AuthenticationPrincipal LoginUser user) {
        return Result.success(metadataService.pageAttributes(user.getEnterpriseId(), page, size, code, name, status));
    }

    @GetMapping("/api/v1/product-attributes/{id}")
    @PreAuthorize("hasAuthority('md:attribute:list')")
    public Result<MdProductAttribute> attribute(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        return Result.success(metadataService.getAttribute(id, user.getEnterpriseId()));
    }

    @PostMapping("/api/v1/product-attributes")
    @PreAuthorize("hasAuthority('md:attribute:list')")
    public Result<MdProductAttribute> createAttribute(@RequestBody MdProductAttribute row,
                                                       @AuthenticationPrincipal LoginUser user) {
        return Result.success(metadataService.saveAttribute(null, row, user.getEnterpriseId()));
    }

    @PutMapping("/api/v1/product-attributes/{id}")
    @PreAuthorize("hasAuthority('md:attribute:list')")
    public Result<MdProductAttribute> updateAttribute(@PathVariable Long id, @RequestBody MdProductAttribute row,
                                                       @AuthenticationPrincipal LoginUser user) {
        return Result.success(metadataService.saveAttribute(id, row, user.getEnterpriseId()));
    }

    @DeleteMapping("/api/v1/product-attributes/{id}")
    @PreAuthorize("hasAuthority('md:attribute:list')")
    public Result<Void> deleteAttribute(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        metadataService.deleteAttribute(id, user.getEnterpriseId()); return Result.success();
    }

    @GetMapping("/api/v1/product-tags")
    @PreAuthorize("hasAuthority('md:product-tag:list')")
    public Result<PageResult<MdProductTag>> productTags(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size,
                                                        @RequestParam(required = false) String name,
                                                        @AuthenticationPrincipal LoginUser user) {
        return Result.success(metadataService.pageProductTags(user.getEnterpriseId(), page, size, name));
    }

    @PostMapping("/api/v1/product-tags")
    @PreAuthorize("hasAuthority('md:product-tag:list')")
    public Result<MdProductTag> createProductTag(@RequestBody MdProductTag tag, @AuthenticationPrincipal LoginUser user) {
        return Result.success(metadataService.saveProductTag(null, tag, user.getEnterpriseId()));
    }

    @PutMapping("/api/v1/product-tags/{id}")
    @PreAuthorize("hasAuthority('md:product-tag:list')")
    public Result<MdProductTag> updateProductTag(@PathVariable Long id, @RequestBody MdProductTag tag,
                                                  @AuthenticationPrincipal LoginUser user) {
        return Result.success(metadataService.saveProductTag(id, tag, user.getEnterpriseId()));
    }

    @DeleteMapping("/api/v1/product-tags/{id}")
    @PreAuthorize("hasAuthority('md:product-tag:list')")
    public Result<Void> deleteProductTag(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        metadataService.deleteProductTag(id, user.getEnterpriseId()); return Result.success();
    }

    @GetMapping("/api/v1/customer-tags")
    @PreAuthorize("hasAuthority('md:customer-tag:list')")
    public Result<PageResult<MdCustomerTag>> customerTags(@RequestParam(defaultValue = "1") Integer page,
                                                           @RequestParam(defaultValue = "10") Integer size,
                                                           @RequestParam(required = false) String name,
                                                           @AuthenticationPrincipal LoginUser user) {
        return Result.success(metadataService.pageCustomerTags(user.getEnterpriseId(), page, size, name));
    }

    @PostMapping("/api/v1/customer-tags")
    @PreAuthorize("hasAuthority('md:customer-tag:list')")
    public Result<MdCustomerTag> createCustomerTag(@RequestBody MdCustomerTag tag, @AuthenticationPrincipal LoginUser user) {
        return Result.success(metadataService.saveCustomerTag(null, tag, user.getEnterpriseId()));
    }

    @PutMapping("/api/v1/customer-tags/{id}")
    @PreAuthorize("hasAuthority('md:customer-tag:list')")
    public Result<MdCustomerTag> updateCustomerTag(@PathVariable Long id, @RequestBody MdCustomerTag tag,
                                                    @AuthenticationPrincipal LoginUser user) {
        return Result.success(metadataService.saveCustomerTag(id, tag, user.getEnterpriseId()));
    }

    @DeleteMapping("/api/v1/customer-tags/{id}")
    @PreAuthorize("hasAuthority('md:customer-tag:list')")
    public Result<Void> deleteCustomerTag(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        metadataService.deleteCustomerTag(id, user.getEnterpriseId()); return Result.success();
    }

    @GetMapping("/api/v1/product-metadata/options")
    @PreAuthorize("hasAnyAuthority('md:product:list','md:attribute:list','md:product-tag:list')")
    public Result<Map<String, Object>> productOptions(@AuthenticationPrincipal LoginUser user) {
        return Result.success(metadataService.productMetadataOptions(user.getEnterpriseId()));
    }

    @GetMapping("/api/v1/customer-tags/options")
    @PreAuthorize("hasAnyAuthority('md:customer:list','md:customer-tag:list')")
    public Result<List<MdCustomerTag>> customerTagOptions(@AuthenticationPrincipal LoginUser user) {
        return Result.success(metadataService.customerTagOptions(user.getEnterpriseId()));
    }
}
