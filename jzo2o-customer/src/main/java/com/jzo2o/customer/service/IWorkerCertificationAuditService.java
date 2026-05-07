package com.jzo2o.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.customer.model.domain.WorkerCertificationAudit;
import com.jzo2o.customer.model.dto.request.WorkerCertificationAuditAddReqDTO;
import com.jzo2o.customer.model.dto.response.RejectReasonResDTO;

public interface IWorkerCertificationAuditService extends IService<WorkerCertificationAudit> {
    /**
     * 服务人员申请资质认证
     * @param workerCertificationAuditAddReqDTO 认证申请请求体
     */
    void applyCertification(WorkerCertificationAuditAddReqDTO workerCertificationAuditAddReqDTO);

    /**
     * 查询当前用户最近驳回原因
     * @return 驳回原因
     */
    RejectReasonResDTO queryCurrentUserLastRejectReason();
}