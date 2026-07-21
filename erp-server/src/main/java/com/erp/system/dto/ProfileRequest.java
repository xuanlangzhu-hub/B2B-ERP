package com.erp.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileRequest {
    @NotBlank(message = "姓名不能为空")
    @Size(max = 50)
    private String realName;
    @Size(max = 30)
    private String phone;
    @Email(message = "邮箱格式不正确")
    @Size(max = 100)
    private String email;
    @Size(max = 500)
    private String avatarUrl;
}
