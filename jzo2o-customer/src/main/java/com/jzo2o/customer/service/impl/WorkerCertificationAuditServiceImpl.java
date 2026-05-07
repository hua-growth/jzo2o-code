package com.jzo2o.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.customer.enums.CertificationStatusEnum;
import com.jzo2o.customer.mapper.WorkerCertificationAuditMapper;
import com.jzo2o.customer.model.domain.WorkerCertification;
import com.jzo2o.customer.model.domain.WorkerCertificationAudit;
import com.jzo2o.customer.model.dto.request.WorkerCertificationAuditAddReqDTO;
import com.jzo2o.customer.model.dto.response.RejectReasonResDTO;
import com.jzo2o.customer.service.IWorkerCertificationAuditService;
import com.jzo2o.customer.service.IWorkerCertificationService;
import com.jzo2o.mvc.utils.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class WorkerCertificationAuditServiceImpl extends ServiceImpl<WorkerCertificationAuditMapper, WorkerCertificationAudit> implements IWorkerCertificationAuditService {
    @Resource
    private IWorkerCertificationService workerCertificationService;

    /**
     * 服务人员申请资质认证
     *
     * @param workerCertificationAuditAddReqDTO 认证申请请求体
     */
    @Override
    @Transactional
    public void applyCertification(WorkerCertificationAuditAddReqDTO workerCertificationAuditAddReqDTO) {
        //1.新增申请资质认证记录
        WorkerCertificationAudit workerCertificationAudit = BeanUtil.toBean(workerCertificationAuditAddReqDTO, WorkerCertificationAudit.class);
        //默认未审核
        workerCertificationAudit.setAuditStatus(0);
        baseMapper.insert(workerCertificationAudit);
        //查询认证记录
        Long serveProviderId = workerCertificationAuditAddReqDTO.getServeProviderId();
        WorkerCertification workerCertification = workerCertificationService.getById(serveProviderId);
        if(ObjectUtil.isNotNull(workerCertification)){
            //2.将认证信息状态更新为认证中
            workerCertification.setCertificationStatus(CertificationStatusEnum.PROGRESSING.getStatus());//认证中
            workerCertificationService.updateById(workerCertification);
        }else{
            workerCertification = new WorkerCertification();
            workerCertification.setId(serveProviderId);
            workerCertification.setCertificationStatus(CertificationStatusEnum.PROGRESSING.getStatus());//认证中
            workerCertificationService.save(workerCertification);
        }
    }

    /**
     * 查询当前用户最近驳回原因
     *
     * @return 驳回原因
     */
    @Override
    public RejectReasonResDTO queryCurrentUserLastRejectReason() {
        LambdaQueryWrapper<WorkerCertificationAudit> queryWrapper = Wrappers.<WorkerCertificationAudit>lambdaQuery()
                .eq(WorkerCertificationAudit::getServeProviderId, UserContext.currentUserId())
                .orderByDesc(WorkerCertificationAudit::getCreateTime)
                .last("limit 1");
        WorkerCertificationAudit workerCertificationAudit = baseMapper.selectOne(queryWrapper);
        return new RejectReasonResDTO(workerCertificationAudit.getRejectReason());
    }
}