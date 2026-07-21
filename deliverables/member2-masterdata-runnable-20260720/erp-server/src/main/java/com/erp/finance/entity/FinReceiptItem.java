package com.erp.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("fin_receipt_item")
public class FinReceiptItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long receiptId;
    private Long receivableId;
    private String sourceNo;
    private BigDecimal allocatedAmount;
    private LocalDateTime createdAt;
}
