package com.deployhub.service.impl;

import com.deployhub.service.ScriptGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScriptGeneratorServiceImpl implements ScriptGeneratorService {

    @Override
    public String generateScript(String projectType, String appName, String remoteDeployPath,
                                 String configFilePath, String jvmParams, String serverPort, Boolean reloadNginx) {
        if ("JAVA".equalsIgnoreCase(projectType)) {
            return generateJavaScript(appName, remoteDeployPath, configFilePath, jvmParams, serverPort);
        } else if ("FRONTEND".equalsIgnoreCase(projectType)) {
            return generateFrontendScript(appName, remoteDeployPath, reloadNginx);
        } else {
            throw new RuntimeException("不支持的项目类型: " + projectType);
        }
    }

    private String generateJavaScript(String appName, String remoteDeployPath, String configFilePath,
                                      String jvmParams, String serverPort) {
        String effectiveJvmParams = (jvmParams != null && !jvmParams.isEmpty())
                ? jvmParams : "-Xms512m -Xmx2048m";

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n\n");

        sb.append("APP_NAME=${APP_NAME}\n");
        sb.append("REMOTE_FILE_PATH=${REMOTE_FILE_PATH}\n");
        sb.append("REMOTE_DEPLOY_PATH=${REMOTE_DEPLOY_PATH}\n");
        sb.append("CONFIG_FILE_PATH=${CONFIG_FILE_PATH}\n");
        sb.append("JVM_PARAMS=\"").append(effectiveJvmParams).append("\"\n");
        if (serverPort != null && !serverPort.isEmpty()) {
            sb.append("SERVER_PORT=").append(serverPort).append("\n");
        } else {
            sb.append("SERVER_PORT=${SERVER_PORT}\n");
        }
        sb.append("LOG_DIR=\"${REMOTE_DEPLOY_PATH}/logs\"\n");
        sb.append("CONFIG_DIR=\"${REMOTE_DEPLOY_PATH}/config\"\n");
        sb.append("SERVICE_NAME=\"${APP_NAME}\"\n\n");

        sb.append("set -e\n\n");

        sb.append("echo \"========== 停止旧服务 ==========\"\n");
        sb.append("OLD_PID=$(ps -ef | grep \"${APP_NAME}\\.jar\" | grep -v grep | awk '{print $2}')\n");
        sb.append("if [ -n \"${OLD_PID}\" ]; then\n");
        sb.append("    echo \"发现旧进程, PID: ${OLD_PID}, 正在停止...\"\n");
        sb.append("    kill ${OLD_PID}\n");
        sb.append("    sleep 5\n");
        sb.append("    OLD_PID=$(ps -ef | grep \"${APP_NAME}\\.jar\" | grep -v grep | awk '{print $2}')\n");
        sb.append("    if [ -n \"${OLD_PID}\" ]; then\n");
        sb.append("        echo \"进程未停止, 强制终止...\"\n");
        sb.append("        kill -9 ${OLD_PID}\n");
        sb.append("        sleep 2\n");
        sb.append("    fi\n");
        sb.append("    echo \"旧服务已停止\"\n");
        sb.append("else\n");
        sb.append("    echo \"未发现旧进程\"\n");
        sb.append("fi\n\n");

        sb.append("echo \"========== 创建目录 ==========\"\n");
        sb.append("mkdir -p ${REMOTE_DEPLOY_PATH}\n");
        sb.append("mkdir -p ${CONFIG_DIR}\n");
        sb.append("mkdir -p ${LOG_DIR}\n");
        sb.append("echo \"目录创建完成\"\n\n");

        sb.append("echo \"========== 传输JAR文件 ==========\"\n");
        sb.append("cp ${REMOTE_FILE_PATH} ${REMOTE_DEPLOY_PATH}/${APP_NAME}.jar\n");
        sb.append("echo \"JAR文件传输完成\"\n\n");

        if (configFilePath != null && !configFilePath.isEmpty()) {
            sb.append("echo \"========== 处理配置文件 ==========\"\n");
            sb.append("if [ -n \"${CONFIG_FILE_PATH}\" ] && [ -f \"${CONFIG_FILE_PATH}\" ]; then\n");
            sb.append("    if [ -f \"${CONFIG_DIR}/application.yml\" ]; then\n");
            sb.append("        echo \"备份旧配置文件...\"\n");
            sb.append("        cp ${CONFIG_DIR}/application.yml ${CONFIG_DIR}/application.yml.bak.${TIMESTAMP}\n");
            sb.append("    fi\n");
            sb.append("    echo \"复制配置文件: ${CONFIG_FILE_PATH} -> ${CONFIG_DIR}/application.yml\"\n");
            sb.append("    cp ${CONFIG_FILE_PATH} ${CONFIG_DIR}/application.yml\n");
            sb.append("else\n");
            sb.append("    if [ -f \"${CONFIG_DIR}/application.yml.bak.${TIMESTAMP}\" ]; then\n");
            sb.append("        echo \"恢复配置文件备份...\"\n");
            sb.append("        cp ${CONFIG_DIR}/application.yml.bak.${TIMESTAMP} ${CONFIG_DIR}/application.yml\n");
            sb.append("    fi\n");
            sb.append("fi\n\n");
        }

        sb.append("echo \"========== 检测Java路径 ==========\"\n");
        sb.append("JAVA_PATH=$(which java 2>/dev/null || echo \"\")\n");
        sb.append("if [ -z \"${JAVA_PATH}\" ]; then\n");
        sb.append("    if [ -x \"/usr/bin/java\" ]; then\n");
        sb.append("        JAVA_PATH=\"/usr/bin/java\"\n");
        sb.append("    elif [ -x \"/usr/local/java/bin/java\" ]; then\n");
        sb.append("        JAVA_PATH=\"/usr/local/java/bin/java\"\n");
        sb.append("    else\n");
        sb.append("        echo \"错误: 未找到Java环境\"\n");
        sb.append("        exit 1\n");
        sb.append("    fi\n");
        sb.append("fi\n");
        sb.append("echo \"Java路径: ${JAVA_PATH}\"\n\n");

        sb.append("echo \"========== 创建systemd服务 ==========\"\n");
        sb.append("cat > /etc/systemd/system/${SERVICE_NAME}.service << EOF\n");
        sb.append("[Unit]\n");
        sb.append("Description=${APP_NAME} Service\n");
        sb.append("After=network.target\n\n");
        sb.append("[Service]\n");
        sb.append("Type=simple\n");
        sb.append("User=root\n");
        sb.append("ExecStart=${JAVA_PATH} ${JVM_PARAMS} -Dserver.port=${SERVER_PORT} -jar ${REMOTE_DEPLOY_PATH}/${APP_NAME}.jar --spring.config.additional-location=${CONFIG_DIR}/\n");
        sb.append("ExecStop=/bin/kill -15 \\$MAINPID\n");
        sb.append("Restart=on-failure\n");
        sb.append("RestartSec=10\n");
        sb.append("StandardOutput=journal\n");
        sb.append("StandardError=journal\n\n");
        sb.append("[Install]\n");
        sb.append("WantedBy=multi-user.target\n");
        sb.append("EOF\n\n");

        sb.append("systemctl daemon-reload\n");
        sb.append("systemctl enable ${SERVICE_NAME}\n");
        sb.append("echo \"systemd服务创建完成\"\n\n");

        sb.append("echo \"========== 启动服务 ==========\"\n");
        sb.append("systemctl start ${SERVICE_NAME}\n");
        sb.append("sleep 5\n\n");

        sb.append("echo \"========== 健康检查 ==========\"\n");
        sb.append("MAX_RETRY=30\n");
        sb.append("RETRY_COUNT=0\n");
        sb.append("while [ ${RETRY_COUNT} -lt ${MAX_RETRY} ]; do\n");
        sb.append("    if systemctl is-active --quiet ${SERVICE_NAME}; then\n");
        sb.append("        echo \"服务启动成功\"\n");
        sb.append("        break\n");
        sb.append("    fi\n");
        sb.append("    RETRY_COUNT=$((RETRY_COUNT + 1))\n");
        sb.append("    echo \"等待服务启动... (${RETRY_COUNT}/${MAX_RETRY})\"\n");
        sb.append("    sleep 2\n");
        sb.append("done\n\n");

        sb.append("if ! systemctl is-active --quiet ${SERVICE_NAME}; then\n");
        sb.append("    echo \"错误: 服务启动失败\"\n");
        sb.append("    systemctl status ${SERVICE_NAME} --no-pager\n");
        sb.append("    journalctl -u ${SERVICE_NAME} -n 50 --no-pager\n");
        sb.append("    exit 1\n");
        sb.append("fi\n\n");

        sb.append("echo \"========== 清理临时文件 ==========\"\n");
        sb.append("if [ -f \"${REMOTE_FILE_PATH}\" ]; then\n");
        sb.append("    rm -f ${REMOTE_FILE_PATH}\n");
        sb.append("    echo \"临时文件已清理\"\n");
        sb.append("fi\n\n");

        sb.append("echo \"========== 部署完成 ==========\"\n");
        sb.append("echo \"应用: ${APP_NAME}\"\n");
        sb.append("echo \"部署路径: ${REMOTE_DEPLOY_PATH}\"\n");
        sb.append("echo \"端口: ${SERVER_PORT}\"\n");
        sb.append("echo \"JVM参数: ${JVM_PARAMS}\"\n");

        return sb.toString();
    }

    private String generateFrontendScript(String appName, String remoteDeployPath, Boolean reloadNginx) {
        boolean shouldReloadNginx = (reloadNginx != null) ? reloadNginx : true;

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n\n");

        sb.append("APP_NAME=${APP_NAME}\n");
        sb.append("REMOTE_FILE_PATH=${REMOTE_FILE_PATH}\n");
        sb.append("REMOTE_DEPLOY_PATH=${REMOTE_DEPLOY_PATH}\n");
        sb.append("BACKUP_DIR=\"${REMOTE_DEPLOY_PATH}/backup/${TIMESTAMP}\"\n\n");

        sb.append("set -e\n\n");

        sb.append("echo \"========== 备份旧版本 ==========\"\n");
        sb.append("if [ -d \"${REMOTE_DEPLOY_PATH}/dist\" ] && [ \"$(ls -A ${REMOTE_DEPLOY_PATH}/dist)\" ]; then\n");
        sb.append("    mkdir -p ${BACKUP_DIR}\n");
        sb.append("    cp -r ${REMOTE_DEPLOY_PATH}/dist/* ${BACKUP_DIR}/\n");
        sb.append("    echo \"旧版本已备份到: ${BACKUP_DIR}\"\n");
        sb.append("else\n");
        sb.append("    echo \"无旧版本需要备份\"\n");
        sb.append("fi\n\n");

        sb.append("echo \"========== 创建部署目录 ==========\"\n");
        sb.append("mkdir -p ${REMOTE_DEPLOY_PATH}\n");
        sb.append("echo \"部署目录已就绪\"\n\n");

        sb.append("echo \"========== 解压dist文件 ==========\"\n");
        sb.append("if [ -f \"${REMOTE_FILE_PATH}\" ]; then\n");
        sb.append("    cd ${REMOTE_DEPLOY_PATH}\n");
        sb.append("    unzip -o ${REMOTE_FILE_PATH} -d dist\n");
        sb.append("    echo \"文件解压完成\"\n");
        sb.append("else\n");
        sb.append("    echo \"错误: 未找到部署文件 ${REMOTE_FILE_PATH}\"\n");
        sb.append("    exit 1\n");
        sb.append("fi\n\n");

        sb.append("echo \"========== 设置文件权限 ==========\"\n");
        sb.append("chmod -R 755 ${REMOTE_DEPLOY_PATH}/dist\n");
        sb.append("echo \"权限设置完成\"\n\n");

        sb.append("echo \"========== 验证部署 ==========\"\n");
        sb.append("if [ -f \"${REMOTE_DEPLOY_PATH}/dist/index.html\" ]; then\n");
        sb.append("    echo \"部署验证通过: index.html 存在\"\n");
        sb.append("else\n");
        sb.append("    echo \"错误: 部署验证失败, index.html 不存在\"\n");
        sb.append("    echo \"正在回滚...\"\n");
        sb.append("    if [ -d \"${BACKUP_DIR}\" ] && [ \"$(ls -A ${BACKUP_DIR})\" ]; then\n");
        sb.append("        rm -rf ${REMOTE_DEPLOY_PATH}/dist\n");
        sb.append("        mkdir -p ${REMOTE_DEPLOY_PATH}/dist\n");
        sb.append("        cp -r ${BACKUP_DIR}/* ${REMOTE_DEPLOY_PATH}/dist/\n");
        sb.append("        echo \"已回滚到旧版本\"\n");
        sb.append("    fi\n");
        sb.append("    exit 1\n");
        sb.append("fi\n\n");

        if (shouldReloadNginx) {
            sb.append("echo \"========== 重载Nginx ==========\"\n");
            sb.append("if command -v nginx &> /dev/null; then\n");
            sb.append("    nginx -t && nginx -s reload\n");
            sb.append("    echo \"Nginx重载成功\"\n");
            sb.append("else\n");
            sb.append("    echo \"警告: 未找到Nginx, 跳过重载\"\n");
            sb.append("fi\n\n");
        }

        sb.append("echo \"========== 清理临时文件 ==========\"\n");
        sb.append("if [ -f \"${REMOTE_FILE_PATH}\" ]; then\n");
        sb.append("    rm -f ${REMOTE_FILE_PATH}\n");
        sb.append("    echo \"临时文件已清理\"\n");
        sb.append("fi\n\n");

        sb.append("echo \"========== 部署完成 ==========\"\n");
        sb.append("echo \"应用: ${APP_NAME}\"\n");
        sb.append("echo \"部署路径: ${REMOTE_DEPLOY_PATH}/dist\"");

        return sb.toString();
    }
}
