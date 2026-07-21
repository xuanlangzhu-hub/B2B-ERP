package com.erp.system.controller;

import com.erp.common.Result;
import com.erp.security.LoginUser;
import com.erp.system.dto.EnterpriseRequest;
import com.erp.system.entity.OrgEnterprise;
import com.erp.system.service.OrgEnterpriseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/enterprise/current")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:enterprise:view')")
public class EnterpriseController {
    private final OrgEnterpriseService enterpriseService;

    @GetMapping
    public Result<OrgEnterprise> detail(@AuthenticationPrincipal LoginUser user) {
        return Result.success(enterpriseService.getCurrent(user.getEnterpriseId()));
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody EnterpriseRequest request,
                               @AuthenticationPrincipal LoginUser user) {
        enterpriseService.updateCurrent(user.getEnterpriseId(), user.getUserId(), request);
        return Result.success();
    }
}
