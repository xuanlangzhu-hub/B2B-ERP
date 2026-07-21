package com.erp.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SalReturnRequest {
    @NotNull(message = "原销售单不能为空")
    private Long salesOrderId;
    @NotNull(message = "退货日期不能为空")
    private LocalDate returnDate;
    @NotNull(message = "退货仓库不能为空")
    private Long warehouseId;
    private String returnReason;
    private String remark;
    @NotEmpty(message = "退货明细不能为空")
    @Valid
    private List<SalReturnItemRequest> items;
}
