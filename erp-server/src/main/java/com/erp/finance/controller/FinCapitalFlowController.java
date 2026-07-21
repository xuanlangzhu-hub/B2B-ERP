package com.erp.finance.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.finance.entity.FinCapitalFlow;
import com.erp.finance.service.FinCapitalFlowService;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/capital-flows")
@RequiredArgsConstructor
public class FinCapitalFlowController {

    private final FinCapitalFlowService capitalFlowService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:flow:list')")
    public Result<PageResult<FinCapitalFlow>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String flowType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(capitalFlowService.pageFlows(
                loginUser.getEnterpriseId(), page, size, flowType, startDate, endDate));
    }
}
