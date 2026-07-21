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
public class InvBorrowRequest {
    @NotBlank(message = "借用类型不能为空")
    @Pattern(regexp = "BORROW_IN|BORROW_OUT", message = "借用类型不正确")
    private String borrowType;

    @NotNull(message = "借用日期不能为空")
    private LocalDate borrowDate;
    private LocalDate expectedReturnDate;

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @NotBlank(message = "往来单位类型不能为空")
    @Pattern(regexp = "CUSTOMER|SUPPLIER|OTHER", message = "往来单位类型不正确")
    private String partnerType;
    private Long partnerId;

    @NotBlank(message = "往来单位名称不能为空")
    @Size(max = 200, message = "往来单位名称不能超过200个字符")
    private String partnerName;

    @NotEmpty(message = "借用明细不能为空")
    @Valid
    private List<InvBorrowItemRequest> items;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
