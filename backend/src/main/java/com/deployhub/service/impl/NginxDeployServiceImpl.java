package com.deployhub.service.impl;

import com.deployhub.common.Result;
import com.deployhub.dto.DeployStepResult;
import com.deployhub.dto.DeployStepResult.StepDetail;
import com.deployhub.entity.NginxConfig;
import com.deployhub.entity.NginxPackage;
import com.deployhub.entity.NginxTemplate;
import com.deployhub.entity.Server;
import com.deployhub.mapper.NginxConfigMapper;
import com.deployhub.mapper.NginxPackageMapper;
import com.deployhub.mapper.NginxTemplateMapper;
import com.deployhub.mapper.ServerMapper;
import com.deployhub.service.NginxCommandService;
import com.deployhub.service.NginxDeployService;
import com.deployhub.ssh.SshExecuteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

/**
 * Nginx一键部署服务实现类
 * 实现从模板一键部署Nginx到目标服务器的完整流程
 *
 * @author MSI-1
 * @date 2026-05-21
 */
@Slf4j
@Service
public class NginxDeployServiceImpl implements NginxDeployService {

    @Autowired
    private NginxTemplateMapper nginxTemplateMapper;

    @Autowired
    private ServerMapper serverMapper;

    @Autowired
    private NginxPackageMapper nginxPackageMapper;

    @Autowired
    private NginxConfigMapper nginxConfigMapper;

    @Autowired
    private NginxCommandService nginxCommandService;

    /**
     * 一键部署Nginx
     * 完整流程：安装Nginx -> 写入配置 -> 测试配置 -> 重载Nginx
     *
     * @param templateId 模板ID
     * @param serverId   目标服务器ID
     * @param packageId  安装包ID（可选）
     * @param configName 配置名称
     * @param configPath 配置文件路径
     * @return 部署步骤结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<DeployStepResult> oneClickDeploy(Long templateId, Long serverId, Long packageId,
                                                   String configName, String configPath) {
        log.info("开始一键部署, templateId={}, serverId={}, packageId={}, configName={}, configPath={}",
                templateId, serverId, packageId, configName, configPath);

        DeployStepResult result = new DeployStepResult();

        // ==================== 前置校验：获取模板、服务器、安装包 ====================
        NginxTemplate template = nginxTemplateMapper.selectById(templateId);
        if (template == null) {
            return Result.error("模板不存在, templateId=" + templateId);
        }

        Server server = serverMapper.selectById(serverId);
        if (server == null) {
            return Result.error("服务器不存在, serverId=" + serverId);
        }

        NginxPackage nginxPackage = null;
        if (packageId != null) {
            nginxPackage = nginxPackageMapper.selectById(packageId);
            if (nginxPackage == null) {
                return Result.error("安装包不存在, packageId=" + packageId);
            }
        }

        // 替换模板变量
        String configContent = replaceTemplateVariables(template.getConfigContent(), server);

        // 为lambda表达式捕获final变量
        final Long finalPackageId = packageId;
        final NginxPackage finalNginxPackage = nginxPackage;
        final String finalConfigPath = configPath;
        final String finalConfigContent = configContent;

        // ==================== 步骤1：检查并安装Nginx ====================
        StepDetail installStep = executeStep("检查并安装Nginx", () -> {
            boolean installed = nginxCommandService.checkNginxInstalled(server);
            if (installed) {
                log.info("Nginx已安装, server={}", server.getHost());
                return StepResult.success("Nginx已安装，跳过安装步骤");
            }

            // Nginx未安装，需要安装
            if (finalPackageId == null || finalNginxPackage == null) {
                return StepResult.fail("Nginx未安装且未指定安装包，无法继续部署");
            }

            // 上传安装包
            log.info("开始上传安装包, package={}, server={}", finalNginxPackage.getName(), server.getHost());
            String localFilePath = finalNginxPackage.getFilePath();
            String remoteFileName = new File(localFilePath).getName();
            SshExecuteResult uploadResult = nginxCommandService.uploadPackage(server, localFilePath, remoteFileName);
            if (!uploadResult.isSuccess()) {
                return StepResult.fail("安装包上传失败: " + uploadResult.getError());
            }

            // 编译安装
            log.info("开始编译安装Nginx, server={}", server.getHost());
            SshExecuteResult installResult = nginxCommandService.installFromPackage(server, remoteFileName);
            if (!installResult.isSuccess()) {
                return StepResult.fail("Nginx安装失败: " + installResult.getError());
            }

            // 验证安装结果
            boolean installedAfter = nginxCommandService.checkNginxInstalled(server);
            if (!installedAfter) {
                return StepResult.fail("Nginx安装完成但验证失败，请检查安装日志");
            }

            return StepResult.success("Nginx安装成功, 版本: " + finalNginxPackage.getVersion());
        });
        result.getSteps().add(installStep);
        result.setCurrentStep(installStep.getStepName());

        if (!installStep.isSuccess()) {
            result.setSuccess(false);
            result.setMessage("部署失败: " + installStep.getMessage());
            return Result.error(result.getMessage());
        }

        // ==================== 步骤2：写入配置文件 ====================
        StepDetail writeConfigStep = executeStep("写入配置文件", () -> {
            log.info("写入配置文件, server={}, path={}", server.getHost(), finalConfigPath);
            SshExecuteResult writeResult = nginxCommandService.writeConfig(server, finalConfigPath, finalConfigContent);
            if (!writeResult.isSuccess()) {
                return StepResult.fail("配置文件写入失败: " + writeResult.getError());
            }
            return StepResult.success("配置文件写入成功, 路径: " + finalConfigPath);
        });
        result.getSteps().add(writeConfigStep);
        result.setCurrentStep(writeConfigStep.getStepName());

        if (!writeConfigStep.isSuccess()) {
            result.setSuccess(false);
            result.setMessage("部署失败: " + writeConfigStep.getMessage());
            return Result.error(result.getMessage());
        }

        // ==================== 步骤3：测试配置语法 ====================
        StepDetail testConfigStep = executeStep("测试配置语法", () -> {
            log.info("测试Nginx配置语法, server={}", server.getHost());
            SshExecuteResult testResult = nginxCommandService.testConfig(server);
            if (!testResult.isSuccess()) {
                String errorMsg = testResult.getOutput() != null ? testResult.getOutput() : testResult.getError();
                return StepResult.fail("配置语法测试失败: " + errorMsg);
            }
            String output = testResult.getOutput() != null ? testResult.getOutput() : "syntax is ok";
            return StepResult.success("配置语法测试通过: " + output);
        });
        result.getSteps().add(testConfigStep);
        result.setCurrentStep(testConfigStep.getStepName());

        if (!testConfigStep.isSuccess()) {
            result.setSuccess(false);
            result.setMessage("部署失败: " + testConfigStep.getMessage());
            return Result.error(result.getMessage());
        }

        // ==================== 步骤4：重载/启动Nginx ====================
        StepDetail reloadStep = executeStep("重载Nginx服务", () -> {
            log.info("重载Nginx服务, server={}", server.getHost());
            // 先尝试reload，如果失败则尝试restart
            SshExecuteResult reloadResult = nginxCommandService.reload(server);
            if (reloadResult.isSuccess()) {
                return StepResult.success("Nginx重载成功");
            }

            // reload失败，可能是Nginx未运行，尝试restart
            log.info("Nginx reload失败，尝试restart, server={}", server.getHost());
            SshExecuteResult restartResult = nginxCommandService.restart(server);
            if (!restartResult.isSuccess()) {
                return StepResult.fail("Nginx启动失败: " + restartResult.getError());
            }
            return StepResult.success("Nginx启动成功（首次启动）");
        });
        result.getSteps().add(reloadStep);
        result.setCurrentStep(reloadStep.getStepName());

        if (!reloadStep.isSuccess()) {
            result.setSuccess(false);
            result.setMessage("部署失败: " + reloadStep.getMessage());
            return Result.error(result.getMessage());
        }

        // ==================== 步骤5：保存配置记录到数据库 ====================
        try {
            NginxConfig configRecord = new NginxConfig();
            configRecord.setServerId(serverId);
            configRecord.setName(configName);
            configRecord.setConfigPath(finalConfigPath);
            configRecord.setConfigContent(finalConfigContent);
            configRecord.setStatus("DEPLOYED");
            configRecord.setDescription("一键部署: 模板[" + template.getName() + "]");
            nginxConfigMapper.insert(configRecord);
            log.info("配置记录已保存, configName={}", configName);
        } catch (Exception e) {
            log.error("保存配置记录失败", e);
            // 配置已部署成功，仅记录日志不影响部署结果
        }

        result.setSuccess(true);
        result.setMessage("一键部署完成，所有步骤执行成功");
        log.info("一键部署完成, server={}, configName={}", server.getHost(), configName);
        return Result.success(result);
    }

    /**
     * 替换模板中的变量占位符
     * 支持: ${SERVER_HOST}, ${SERVER_IP}
     *
     * @param configContent 模板配置内容
     * @param server        服务器实体
     * @return 替换后的配置内容
     */
    private String replaceTemplateVariables(String configContent, Server server) {
        if (configContent == null || server == null) {
            return configContent;
        }
        return configContent
                .replace("${SERVER_HOST}", server.getHost())
                .replace("${SERVER_IP}", server.getHost());
    }

    /**
     * 执行部署步骤并记录耗时
     *
     * @param stepName 步骤名称
     * @param action   步骤执行逻辑
     * @return 步骤详情
     */
    private StepDetail executeStep(String stepName, StepAction action) {
        StepDetail detail = new StepDetail();
        detail.setStepName(stepName);
        long startTime = System.currentTimeMillis();
        try {
            StepResult stepResult = action.execute();
            detail.setSuccess(stepResult.success);
            detail.setMessage(stepResult.message);
        } catch (Exception e) {
            log.error("步骤[{}]执行异常", stepName, e);
            detail.setSuccess(false);
            detail.setMessage("步骤执行异常: " + e.getMessage());
        }
        detail.setDuration(System.currentTimeMillis() - startTime);
        return detail;
    }

    /**
     * 步骤执行函数式接口
     */
    @FunctionalInterface
    private interface StepAction {
        StepResult execute();
    }

    /**
     * 步骤执行结果（内部使用）
     */
    private static class StepResult {
        final boolean success;
        final String message;

        private StepResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        static StepResult success(String message) {
            return new StepResult(true, message);
        }

        static StepResult fail(String message) {
            return new StepResult(false, message);
        }
    }
}
