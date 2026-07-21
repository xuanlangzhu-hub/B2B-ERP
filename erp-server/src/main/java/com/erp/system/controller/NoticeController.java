package com.erp.system.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.security.LoginUser;
import com.erp.system.dto.NoticeRequest;
import com.erp.system.entity.SysNotice;
import com.erp.system.service.SysNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final SysNoticeService noticeService;

    @GetMapping
    public Result<PageResult<SysNotice>> published(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer size,
                                                   @AuthenticationPrincipal LoginUser user) {
        return Result.success(noticeService.pagePublished(user.getEnterpriseId(), user.getUserId(), page, size));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount(@AuthenticationPrincipal LoginUser user) {
        return Result.success(Map.of("count", noticeService.unreadCount(user.getEnterpriseId(), user.getUserId())));
    }

    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        noticeService.markAsRead(id, user.getEnterpriseId(), user.getUserId());
        return Result.success();
    }

    @GetMapping("/manage")
    @PreAuthorize("hasAuthority('system:notice:list')")
    public Result<PageResult<SysNotice>> manage(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                @RequestParam(required = false) String title,
                                                @RequestParam(required = false) String status,
                                                @AuthenticationPrincipal LoginUser user) {
        return Result.success(noticeService.pageManage(user.getEnterpriseId(), page, size, title, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:notice:list')")
    public Result<SysNotice> create(@Valid @RequestBody NoticeRequest request,
                                    @AuthenticationPrincipal LoginUser user) {
        return Result.success(noticeService.createNotice(request, user.getEnterpriseId(), user.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:notice:list')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeRequest request,
                               @AuthenticationPrincipal LoginUser user) {
        noticeService.updateNotice(id, request, user.getEnterpriseId(), user.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('system:notice:list')")
    public Result<Void> publish(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        noticeService.publish(id, user.getEnterpriseId(), user.getUserId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:notice:list')")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        noticeService.deleteNotice(id, user.getEnterpriseId());
        return Result.success();
    }
}
