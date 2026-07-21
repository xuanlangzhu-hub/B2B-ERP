package com.erp.inventory.controller;

import com.erp.common.Result;
import com.erp.inventory.service.DashboardService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('dashboard:summary')")
    public Result<Map<String, Object>> summary(@AuthenticationPrincipal LoginUser user) {
        return Result.success(dashboardService.summary(user.getEnterpriseId()));
    }
}
