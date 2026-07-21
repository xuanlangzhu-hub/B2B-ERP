package com.erp.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InvAdjustmentRequest {
    @NotNull(message = "调整日期不能为空")
    private LocalDate adjustmentDate;

    @NotNull(message = "调整仓库不能为空")
    private Long warehouseId;

    @NotBlank(message = "调整类型不能为空")
    @Pattern(regexp = "INCREASE|DECREASE", message = "调整类型不正确")
    private String adjustmentType;

    @NotBlank(message = "调整原因不能为空")
    @Size(max = 500, message = "调整原因不能超过500个字符")
    private String reason;

    @NotEmpty(message = "调整明细不能为空")
    @Valid
    private List<InvAdjustmentItemRequest> items;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
