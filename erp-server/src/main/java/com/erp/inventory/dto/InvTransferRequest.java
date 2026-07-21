package com.erp.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InvTransferRequest {
    @NotNull(message = "调拨日期不能为空")
    private LocalDate transferDate;

    @NotNull(message = "调出仓库不能为空")
    private Long fromWarehouseId;

    @NotNull(message = "调入仓库不能为空")
    private Long toWarehouseId;

    @NotEmpty(message = "调拨明细不能为空")
    @Valid
    private List<InvTransferItemRequest> items;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
