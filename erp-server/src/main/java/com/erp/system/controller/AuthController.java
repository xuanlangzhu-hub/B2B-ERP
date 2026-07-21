package com.erp.system.controller;

import com.erp.common.Result;
import com.erp.security.LoginUser;
import com.erp.system.dto.LoginRequest;
import com.erp.system.dto.LoginResponse;
import com.erp.system.dto.RegisterRequest;
import com.erp.system.dto.RegisterResponse;
import com.erp.system.dto.ProfileRequest;
import com.erp.system.dto.ChangePasswordRequest;
import com.erp.system.entity.OrgStore;
import com.erp.system.entity.SysUser;
import com.erp.system.service.SysUserService;
import com.erp.system.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService userService;
    private final RegistrationService registrationService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(registrationService.register(request));
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

    @GetMapping("/profile")
    public Result<SysUser> profile(@AuthenticationPrincipal LoginUser user) {
        return Result.success(userService.profile(user.getUserId(), user.getEnterpriseId()));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody ProfileRequest request,
                                      @AuthenticationPrincipal LoginUser user) {
        userService.updateProfile(user.getUserId(), user.getEnterpriseId(), request);
        return Result.success();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                       @AuthenticationPrincipal LoginUser user) {
        userService.changePassword(user.getUserId(), user.getEnterpriseId(), request);
        return Result.success();
    }

    @GetMapping("/stores")
    public Result<List<OrgStore>> stores(@AuthenticationPrincipal LoginUser user) {
        return Result.success(userService.accessibleStores(user.getUserId(), user.getEnterpriseId()));
    }

    @PutMapping("/default-store/{storeId}")
    public Result<Void> changeStore(@PathVariable Long storeId, @AuthenticationPrincipal LoginUser user) {
        userService.changeDefaultStore(user.getUserId(), user.getEnterpriseId(), storeId);
        return Result.success();
    }
}
