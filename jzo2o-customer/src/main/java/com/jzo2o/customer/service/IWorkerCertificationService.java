package com.jzo2o.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.customer.model.domain.WorkerCertification;
import com.jzo2o.customer.model.dto.WorkerCertificationUpdateDTO;
import com.jzo2o.customer.model.dto.request.CertificationAuditReqDTO;
import com.jzo2o.customer.model.dto.request.WorkerCertificationAuditPageQueryReqDTO;
import com.jzo2o.customer.model.dto.response.WorkerCertificationAuditResDTO;

/**
 * <p>
 * 服务人员认证信息表 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-09-06
 */
public interface IWorkerCertificationService extends IService<WorkerCertification> {
    /**
     * 分页查询
     * @param workerCertificationAuditPageQueryReqDTO 分页查询条件
     * @return 分页结果
     */
    PageResult<WorkerCertificationAuditResDTO> pageQuery(WorkerCertificationAuditPageQueryReqDTO workerCertificationAuditPageQueryReqDTO);

    /**
     * 审核认证信息
     * @param id                       申请记录id
     * @param certificationAuditReqDTO 审核请求
     */
    void auditCertification(Long id, CertificationAuditReqDTO certificationAuditReqDTO);


    /**
     * 根据服务人员id更新
     *
     * @param workerCertificationUpdateDTO 服务人员认证更新模型
     */
    void updateById(WorkerCertificationUpdateDTO workerCertificationUpdateDTO);
}
