package com.erp.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoticeRequest {
    @NotBlank(message = "通知标题不能为空")
    @Size(max = 200)
    private String noticeTitle;
    @NotBlank(message = "通知内容不能为空")
    private String noticeContent;
    @Size(max = 30)
    private String noticeType;
}
