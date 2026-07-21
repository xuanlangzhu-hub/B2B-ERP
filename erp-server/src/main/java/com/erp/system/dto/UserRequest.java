package com.erp.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50)
    private String username;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50)
    private String realName;

    @Size(max = 30)
    private String phone;

    @Size(max = 100)
    private String email;

    private Long defaultStoreId;

    @Size(max = 500)
    private String remark;
}
