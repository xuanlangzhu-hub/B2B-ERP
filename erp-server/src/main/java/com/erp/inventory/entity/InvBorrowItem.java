package com.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("inv_borrow_item")
public class InvBorrowItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long borrowId;
    private Integer lineNo;
    private Long productId;
    private String productCode;
    private String productName;
    private Long unitId;
    private BigDecimal quantity;
    private BigDecimal returnedQuantity;
    private BigDecimal unitCost;
    private String remark;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String unitName;
    @TableField(exist = false)
    private BigDecimal remainingQuantity;
}
