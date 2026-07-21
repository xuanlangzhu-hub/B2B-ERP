package com.erp.masterdata.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.masterdata.entity.MdCustomer;
import com.erp.masterdata.service.MdCustomerService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final MdCustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAuthority('md:customer:list')")
    public Result<PageResult<MdCustomer>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String status) {
        return Result.success(customerService.pageQuery(page, size, customerCode, customerName, contactPhone, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('md:customer:list')")
    public Result<MdCustomer> detail(@PathVariable Long id) {
        return Result.success(customerService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('md:customer:create')")
    public Result<MdCustomer> create(@RequestBody MdCustomer customer,
                                      @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(customerService.create(customer, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('md:customer:update')")
    public Result<Void> update(@PathVariable Long id, @RequestBody MdCustomer customer) {
        customer.setId(id);
        customerService.update(customer);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('md:customer:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.success();
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('md:customer:list')")
    public Result<List<Map<String, Object>>> options(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(customerService.options(loginUser.getEnterpriseId()));
    }
}
