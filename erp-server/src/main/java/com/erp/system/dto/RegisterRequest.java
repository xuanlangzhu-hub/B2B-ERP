package com.erp.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度应为8到64位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 100, message = "企业名称不能超过100位")
    private String enterpriseName;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 50, message = "联系人不能超过50位")
    private String realName;

    @NotBlank(message = "联系电话不能为空")
    @Size(max = 30, message = "联系电话不能超过30位")
    private String phone;

    @Size(max = 255, message = "企业地址不能超过255位")
    private String enterpriseAddress;

    @NotBlank(message = "门店名称不能为空")
    @Size(max = 100, message = "门店名称不能超过100位")
    private String storeName;

    @Size(max = 255, message = "门店地址不能超过255位")
    private String storeAddress;
}
