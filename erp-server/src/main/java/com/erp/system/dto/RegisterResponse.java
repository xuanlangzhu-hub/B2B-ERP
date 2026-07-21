package com.erp.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {
    private Long enterpriseId;
    private Long userId;
    private String username;
}
