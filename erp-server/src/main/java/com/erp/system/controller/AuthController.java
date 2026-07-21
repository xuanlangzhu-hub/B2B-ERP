package com.erp.system.controller;

import com.erp.common.Result;
import com.erp.security.LoginUser;
import com.erp.system.dto.LoginRequest;
import com.erp.system.dto.LoginResponse;
import com.erp.system.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService userService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me(@AuthenticationPrincipal LoginUser user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("enterpriseId", user.getEnterpriseId());
        data.put("defaultStoreId", user.getDefaultStoreId());
        data.put("permissions", user.getPermissions());
        return Result.success(data);
    }
}
