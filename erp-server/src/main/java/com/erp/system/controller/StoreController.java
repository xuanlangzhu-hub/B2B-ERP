package com.erp.system.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.security.LoginUser;
import com.erp.system.entity.OrgStore;
import com.erp.system.service.OrgStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:store:list')")
public class StoreController {
    private final OrgStoreService storeService;

    @GetMapping
    public Result<PageResult<OrgStore>> list(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size,
                                             @RequestParam(required = false) String storeCode,
                                             @RequestParam(required = false) String storeName,
                                             @RequestParam(required = false) String status,
                                             @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(storeService.pageQuery(loginUser.getEnterpriseId(), page, size,
                storeCode, storeName, status));
    }

    @GetMapping("/{id}")
    public Result<OrgStore> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(storeService.getDetail(id, loginUser.getEnterpriseId()));
    }

    @PostMapping
    public Result<OrgStore> create(@RequestBody OrgStore store, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(storeService.createStore(store, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody OrgStore store,
                               @AuthenticationPrincipal LoginUser loginUser) {
        storeService.updateStore(id, store, loginUser.getEnterpriseId(), loginUser.getUserId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        storeService.deleteStore(id, loginUser.getEnterpriseId());
        return Result.success();
    }

    @GetMapping("/options")
    public Result<List<Map<String, Object>>> options(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(storeService.options(loginUser.getEnterpriseId()));
    }
}
