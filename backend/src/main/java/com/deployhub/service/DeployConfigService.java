package com.deployhub.service;

import com.deployhub.common.Result;
import com.deployhub.dto.DeployConfigDTO;
import com.deployhub.vo.DeployConfigVO;

public interface DeployConfigService {

    /**
     * 根据应用ID获取最新的部署配置
     *
     * @param appId 应用ID
     * @return 部署配置
     */
    Result<DeployConfigVO> getByAppId(Long appId);

    /**
     * 保存部署配置
     *
     * @param dto 部署配置DTO
     * @return 操作结果
     */
    Result<Void> save(DeployConfigDTO dto);
}
