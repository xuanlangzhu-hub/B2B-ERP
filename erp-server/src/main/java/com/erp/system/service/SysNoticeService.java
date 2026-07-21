package com.erp.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.system.dto.NoticeRequest;
import com.erp.system.entity.SysNotice;
import com.erp.system.entity.SysNoticeRead;
import com.erp.system.mapper.SysNoticeMapper;
import com.erp.system.mapper.SysNoticeReadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SysNoticeService extends ServiceImpl<SysNoticeMapper, SysNotice> {
    private final SysNoticeReadMapper readMapper;

    public PageResult<SysNotice> pagePublished(Long enterpriseId, Long userId, Integer page, Integer size) {
        Page<SysNotice> result = page(new Page<>(page, size), new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getEnterpriseId, enterpriseId)
                .eq(SysNotice::getPublishStatus, "PUBLISHED")
                .orderByDesc(SysNotice::getPublishedAt));
        markRead(result.getRecords(), userId);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public long unreadCount(Long enterpriseId, Long userId) {
        List<Long> ids = lambdaQuery().eq(SysNotice::getEnterpriseId, enterpriseId)
                .eq(SysNotice::getPublishStatus, "PUBLISHED").list()
                .stream().map(SysNotice::getId).toList();
        if (ids.isEmpty()) return 0;
        long readCount = readMapper.selectCount(new LambdaQueryWrapper<SysNoticeRead>()
                .eq(SysNoticeRead::getUserId, userId).in(SysNoticeRead::getNoticeId, ids));
        return ids.size() - readCount;
    }

    @Transactional
    public void markAsRead(Long id, Long enterpriseId, Long userId) {
        SysNotice notice = requireNotice(id, enterpriseId);
        if (!"PUBLISHED".equals(notice.getPublishStatus())) throw new BusinessException("通知尚未发布");
        if (readMapper.selectCount(new LambdaQueryWrapper<SysNoticeRead>()
                .eq(SysNoticeRead::getNoticeId, id).eq(SysNoticeRead::getUserId, userId)) == 0) {
            SysNoticeRead record = new SysNoticeRead();
            record.setNoticeId(id);
            record.setUserId(userId);
            record.setReadAt(LocalDateTime.now());
            readMapper.insert(record);
        }
    }

    public PageResult<SysNotice> pageManage(Long enterpriseId, Integer page, Integer size,
                                             String title, String status) {
        Page<SysNotice> result = page(new Page<>(page, size), new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(title), SysNotice::getNoticeTitle, title)
                .eq(StrUtil.isNotBlank(status), SysNotice::getPublishStatus, status)
                .orderByDesc(SysNotice::getCreatedAt));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public SysNotice createNotice(NoticeRequest request, Long enterpriseId, Long operatorId) {
        SysNotice notice = new SysNotice().setEnterpriseId(enterpriseId)
                .setNoticeTitle(request.getNoticeTitle().trim())
                .setNoticeContent(request.getNoticeContent().trim())
                .setNoticeType(StrUtil.blankToDefault(request.getNoticeType(), "SYSTEM"))
                .setPublishStatus("DRAFT").setCreatedBy(operatorId).setUpdatedBy(operatorId);
        save(notice);
        return notice;
    }

    @Transactional
    public void updateNotice(Long id, NoticeRequest request, Long enterpriseId, Long operatorId) {
        SysNotice notice = requireNotice(id, enterpriseId);
        if ("PUBLISHED".equals(notice.getPublishStatus())) throw new BusinessException("已发布通知不能修改");
        notice.setNoticeTitle(request.getNoticeTitle().trim())
                .setNoticeContent(request.getNoticeContent().trim())
                .setNoticeType(StrUtil.blankToDefault(request.getNoticeType(), "SYSTEM"))
                .setUpdatedBy(operatorId);
        updateById(notice);
    }

    @Transactional
    public void publish(Long id, Long enterpriseId, Long operatorId) {
        SysNotice notice = requireNotice(id, enterpriseId);
        notice.setPublishStatus("PUBLISHED").setPublishedAt(LocalDateTime.now()).setUpdatedBy(operatorId);
        updateById(notice);
    }

    @Transactional
    public void deleteNotice(Long id, Long enterpriseId) {
        SysNotice notice = requireNotice(id, enterpriseId);
        if ("PUBLISHED".equals(notice.getPublishStatus())) throw new BusinessException("已发布通知不能删除");
        removeById(id);
    }

    private SysNotice requireNotice(Long id, Long enterpriseId) {
        SysNotice notice = lambdaQuery().eq(SysNotice::getId, id)
                .eq(SysNotice::getEnterpriseId, enterpriseId).one();
        if (notice == null) throw new BusinessException("通知不存在");
        return notice;
    }

    private void markRead(List<SysNotice> notices, Long userId) {
        if (notices.isEmpty()) return;
        Set<Long> readIds = new HashSet<>(readMapper.selectList(new LambdaQueryWrapper<SysNoticeRead>()
                .eq(SysNoticeRead::getUserId, userId)
                .in(SysNoticeRead::getNoticeId, notices.stream().map(SysNotice::getId).toList()))
                .stream().map(SysNoticeRead::getNoticeId).toList());
        notices.forEach(notice -> notice.setRead(readIds.contains(notice.getId())));
    }
}
