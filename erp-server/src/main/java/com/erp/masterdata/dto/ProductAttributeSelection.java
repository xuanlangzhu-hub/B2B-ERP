package com.erp.masterdata.dto;

import lombok.Data;

@Data
public class ProductAttributeSelection {
    private Long attributeId;
    private Long attributeValueId;
    private String customValue;
}
