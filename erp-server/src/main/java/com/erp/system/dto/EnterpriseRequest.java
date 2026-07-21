package com.erp.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EnterpriseRequest {
    @NotBlank(message = "企业名称不能为空")
    @Size(max = 100)
    private String enterpriseName;
    @Size(max = 50)
    private String contactName;
    @Size(max = 30)
    private String contactPhone;
    @Size(max = 255)
    private String address;
    @Size(max = 500)
    private String logoUrl;
    @Size(max = 500)
    private String remark;
}
