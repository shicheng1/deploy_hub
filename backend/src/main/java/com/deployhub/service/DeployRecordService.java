package com.deployhub.service;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.BatchDeployRequestDTO;
import com.deployhub.dto.DeployRequestDTO;
import com.deployhub.vo.DeployRecordVO;

import java.util.List;

public interface DeployRecordService {

    Result<PageResult<DeployRecordVO>> list(int pageNum, int pageSize, Long appId, Long serverId, String status, String startTime, String endTime);

    Result<DeployRecordVO> getById(Long id);

    Result<List<Long>> trigger(DeployRequestDTO dto, String deployMode);

    /**
     * 批量部署项目中所有应用
     *
     * @param request 批量部署请求
     * @return 所有部署记录ID列表
     */
    Result<List<Long>> batchDeploy(BatchDeployRequestDTO request);

    Result<Void> rollback(Long id);

    Result<String> getDeployLog(Long id);
}
