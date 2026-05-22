package com.deployhub.service.impl;

import com.deployhub.entity.App;
import com.deployhub.mapper.AppMapper;
import com.deployhub.service.BuildService;
import com.deployhub.util.OsUtil;
import com.deployhub.websocket.DeployWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class BuildServiceImpl implements BuildService {

    @Autowired
    private AppMapper appMapper;

    @Autowired
    private DeployWebSocketHandler webSocketHandler;

    @Override
    public void buildFrontend(Long appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            webSocketHandler.sendBuildStatus(appId, "FAILED", "应用不存在");
            return;
        }
        String projectPath = app.getProjectPath();
        if (projectPath == null || projectPath.isEmpty()) {
            webSocketHandler.sendBuildStatus(appId, "FAILED", "未配置项目路径");
            return;
        }
        String command = app.getFrontendBuildCommand();
        if (command == null || command.isEmpty()) {
            command = "npm run build";
        }
        executeBuild(appId, projectPath, command);
    }

    @Override
    public void buildBackend(Long appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            webSocketHandler.sendBuildStatus(appId, "FAILED", "应用不存在");
            return;
        }
        String projectPath = app.getProjectPath();
        if (projectPath == null || projectPath.isEmpty()) {
            webSocketHandler.sendBuildStatus(appId, "FAILED", "未配置项目路径");
            return;
        }
        String command = app.getBackendBuildCommand();
        if (command == null || command.isEmpty()) {
            command = "mvn clean package -DskipTests";
        }
        executeBuild(appId, projectPath, command);
    }

    @Override
    public void buildAll(Long appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            webSocketHandler.sendBuildStatus(appId, "FAILED", "应用不存在");
            return;
        }
        String projectPath = app.getProjectPath();
        if (projectPath == null || projectPath.isEmpty()) {
            webSocketHandler.sendBuildStatus(appId, "FAILED", "未配置项目路径");
            return;
        }
        String frontendCommand = app.getFrontendBuildCommand();
        if (frontendCommand == null || frontendCommand.isEmpty()) {
            frontendCommand = "npm run build";
        }
        String backendCommand = app.getBackendBuildCommand();
        if (backendCommand == null || backendCommand.isEmpty()) {
            backendCommand = "mvn clean package -DskipTests";
        }
        webSocketHandler.sendBuildLog(appId, "===== 开始前端构建 =====");
        boolean frontendOk = executeBuild(appId, projectPath, frontendCommand);
        if (!frontendOk) {
            webSocketHandler.sendBuildLog(appId, "前端构建失败，中止后端构建");
            return;
        }
        webSocketHandler.sendBuildLog(appId, "===== 前端构建成功，开始后端构建 =====");
        executeBuild(appId, projectPath, backendCommand);
    }

    private boolean executeBuild(Long appId, String workDir, String command) {
        webSocketHandler.sendBuildStatus(appId, "BUILDING");
        try {
            Path dirPath = Paths.get(workDir);
            if (!Files.exists(dirPath)) {
                webSocketHandler.sendBuildStatus(appId, "FAILED", "项目目录不存在: " + workDir);
                return false;
            }
            webSocketHandler.sendBuildLog(appId, "工作目录: " + workDir);
            webSocketHandler.sendBuildLog(appId, "执行命令: " + command);
            
            ProcessBuilder pb = OsUtil.createProcessBuilder(OsUtil.normalizeBuildCommand(command));
            pb.directory(dirPath.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder outputBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    webSocketHandler.sendBuildLog(appId, line);
                    outputBuilder.append(line).append("\n");
                }
            }
            
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                webSocketHandler.sendBuildStatus(appId, "FAILED", "构建超时(30分钟)");
                return false;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("构建成功, 目录={}", workDir);
                webSocketHandler.sendBuildStatus(appId, "SUCCESS");
                return true;
            } else {
                log.error("构建失败, exitCode={}, 目录={}, output={}", exitCode, workDir, outputBuilder.toString());
                webSocketHandler.sendBuildStatus(appId, "FAILED", "构建失败(exitCode=" + exitCode + ")");
                return false;
            }
        } catch (Exception e) {
            log.error("构建异常, 目录={}", workDir, e);
            webSocketHandler.sendBuildStatus(appId, "FAILED", "构建异常: " + e.getMessage());
            return false;
        }
    }
}
