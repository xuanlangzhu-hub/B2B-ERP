package com.erp.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleRequest {
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50)
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100)
    private String roleName;

    private String dataScope;

    private Integer sortNo;

    @Size(max = 500)
    private String remark;
}
