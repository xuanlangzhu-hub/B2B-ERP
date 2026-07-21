package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.system.dto.EnterpriseRequest;
import com.erp.system.entity.OrgEnterprise;
import com.erp.system.mapper.OrgEnterpriseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgEnterpriseService extends ServiceImpl<OrgEnterpriseMapper, OrgEnterprise> {
    public OrgEnterprise getCurrent(Long enterpriseId) {
        OrgEnterprise enterprise = lambdaQuery().eq(OrgEnterprise::getId, enterpriseId).one();
        if (enterprise == null) throw new BusinessException("企业不存在");
        return enterprise;
    }

    @Transactional
    public void updateCurrent(Long enterpriseId, Long operatorId, EnterpriseRequest request) {
        OrgEnterprise enterprise = getCurrent(enterpriseId);
        enterprise.setEnterpriseName(request.getEnterpriseName().trim())
                .setContactName(request.getContactName())
                .setContactPhone(request.getContactPhone())
                .setAddress(request.getAddress())
                .setLogoUrl(request.getLogoUrl())
                .setRemark(request.getRemark())
                .setUpdatedBy(operatorId);
        updateById(enterprise);
    }
}
