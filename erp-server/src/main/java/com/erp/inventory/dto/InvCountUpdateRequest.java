package com.erp.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class InvCountUpdateRequest {
    @NotEmpty(message = "盘点明细不能为空")
    @Valid
    private List<InvCountItemRequest> items;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
