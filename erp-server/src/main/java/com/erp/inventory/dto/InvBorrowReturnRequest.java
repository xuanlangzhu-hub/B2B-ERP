package com.erp.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InvBorrowReturnRequest {
    @NotNull(message = "归还日期不能为空")
    private LocalDate returnDate;

    @NotEmpty(message = "归还明细不能为空")
    @Valid
    private List<InvBorrowReturnItemRequest> items;
}
