package com.deployhub.service;

import com.deployhub.common.Result;
import com.deployhub.dto.DeployStepResult;

/**
 * Nginx一键部署服务接口
 * 提供从模板一键部署Nginx到目标服务器的完整流程
 *
 * @author MSI-1
 * @date 2026-05-21
 */
public interface NginxDeployService {

    /**
     * 一键部署Nginx
     * 完整流程：安装Nginx -> 写入配置 -> 测试配置 -> 重载Nginx
     *
     * @param templateId 模板ID
     * @param serverId   目标服务器ID
     * @param packageId  安装包ID（可选，服务器已安装Nginx时可不传）
     * @param configName 配置名称
     * @param configPath 配置文件在服务器上的路径
     * @return 部署步骤结果
     */
    Result<DeployStepResult> oneClickDeploy(Long templateId, Long serverId, Long packageId,
                                            String configName, String configPath);
}
