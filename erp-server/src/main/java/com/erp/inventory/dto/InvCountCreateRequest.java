package com.erp.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InvCountCreateRequest {
    @NotNull(message = "盘点日期不能为空")
    private LocalDate countDate;

    @NotNull(message = "盘点仓库不能为空")
    private Long warehouseId;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
