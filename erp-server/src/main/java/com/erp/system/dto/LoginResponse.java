package com.erp.system.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private Long enterpriseId;

    public static LoginResponse of(String token, Long userId, String username, String realName, Long enterpriseId) {
        LoginResponse resp = new LoginResponse();
        resp.token = token;
        resp.userId = userId;
        resp.username = username;
        resp.realName = realName;
        resp.enterpriseId = enterpriseId;
        return resp;
    }
}
