package com.deployhub.deploy;

import com.deployhub.entity.App;
import com.deployhub.entity.DeployRecord;
import com.deployhub.entity.Server;
import com.deployhub.mapper.AppMapper;
import com.deployhub.mapper.DeployRecordMapper;
import com.deployhub.mapper.ServerMapper;
import com.deployhub.script.ScriptEngine;
import com.deployhub.script.ScriptVariableResolver;
import com.deployhub.ssh.SshClient;
import com.deployhub.ssh.SshExecuteResult;
import com.deployhub.ssh.SshPool;
import com.deployhub.util.OsUtil;
import com.deployhub.util.PasswordUtil;
import com.deployhub.websocket.DeployWebSocketHandler;
import com.jcraft.jsch.Session;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
@Component
@Scope("prototype")
public class DeployTask implements Runnable {

    private Long appId;
    private Long serverId;
    private String version;
    private String script;
    private String operator;
    private Long recordId;
    private boolean rollback = false;
    private String deployMode = "FULL";

    @Autowired
    private DeployRecordMapper deployRecordMapper;
    @Autowired
    private ServerMapper serverMapper;
    @Autowired
    private AppMapper appMapper;
    @Autowired
    private SshPool sshPool;
    @Autowired
    private DeployWebSocketHandler webSocketHandler;
    @Autowired
    private ScriptVariableResolver variableResolver;

    @Override
    public void run() {
        DeployRecord record = deployRecordMapper.selectById(recordId);
        if (record == null) {
            log.error("部署记录不存在: {}", recordId);
            return;
        }

        if (rollback) {
            record.setStatus(DeployStatus.ROLLING_BACK.name());
        } else {
            record.setStatus(DeployStatus.DEPLOYING.name());
        }
        record.setStartedAt(LocalDateTime.now());
        deployRecordMapper.updateById(record);

        String action = rollback ? "回滚" : "部署";
        webSocketHandler.sendLog(recordId, "开始" + action + ", 版本: " + version);
        if ("FULL".equals(deployMode)) {
            webSocketHandler.sendStep(recordId, "BUILDING");
        }

        Server server = serverMapper.selectById(serverId);
        if (server == null) {
            handleFailure(record, "服务器不存在: " + serverId);
            webSocketHandler.sendStep(recordId, "FAILED", "服务器不存在: " + serverId);
            return;
        }

        App app = appMapper.selectById(appId);

        String host = server.getHost();
        int port = server.getPort() != null ? server.getPort() : 22;
        String username = server.getUsername();
        String password = PasswordUtil.decrypt(server.getPassword());
        String privateKey = server.getPrivateKey();

        Map<String, String> variables = variableResolver.resolve(app, server, version);

        Session session = null;
        try {
            session = sshPool.borrowSession(host, port, username, password, privateKey);
            webSocketHandler.sendLog(recordId, "SSH连接建立成功, 服务器: " + host);

            if (!rollback && app != null) {
                if ("FULL".equals(deployMode)) {
                    boolean buildOk = buildAndTransfer(session, app, variables);
                    if (!buildOk) {
                        handleFailure(record, "构建或传输失败");
                        webSocketHandler.sendStep(recordId, "FAILED", "构建或传输失败");
                        return;
                    }
                } else if ("DEPLOY_ONLY".equals(deployMode)) {
                    boolean transferOk = transferOnly(session, app, variables);
                    if (!transferOk) {
                        handleFailure(record, "传输失败");
                        webSocketHandler.sendStep(recordId, "FAILED", "传输失败");
                        return;
                    }
                }
            }

            String renderedScript = script;
            if (script != null) {
                renderedScript = ScriptEngine.render(script, variables);
                webSocketHandler.sendLog(recordId, "脚本变量替换完成");
            }

            webSocketHandler.sendStep(recordId, "DEPLOYING");
            webSocketHandler.sendLog(recordId, "执行部署脚本...");
            String normalizedScript = OsUtil.normalizeScriptContent(renderedScript);
            SshExecuteResult result = SshClient.executeCommandWithCallback(session, normalizedScript, line -> {
                webSocketHandler.sendLog(recordId, line);
            });

            if (result.isSuccess()) {
                record.setStatus(DeployStatus.SUCCESS.name());
                record.setDeployLog(result.getOutput());
                record.setFinishedAt(LocalDateTime.now());
                deployRecordMapper.updateById(record);
                webSocketHandler.sendStep(recordId, "COMPLETED");
                webSocketHandler.sendLog(recordId, action + "成功");
            } else {
                String errorMsg = buildErrorMessage(result, action);
                handleFailure(record, errorMsg);
                webSocketHandler.sendStep(recordId, "FAILED", errorMsg);
                webSocketHandler.sendLog(recordId, errorMsg);
            }
        } catch (Exception e) {
            handleFailure(record, e.getMessage());
            webSocketHandler.sendStep(recordId, "FAILED", e.getMessage());
            webSocketHandler.sendLog(recordId, action + "异常: " + e.getMessage());
        } finally {
            if (session != null) {
                sshPool.returnSession(host, port, session);
            }
        }
    }

    private boolean transferOnly(Session session, App app, Map<String, String> variables) {
        String projectPath = app.getProjectPath();
        String outputPath = app.getOutputPath();
        if (outputPath == null || outputPath.isEmpty()) {
            webSocketHandler.sendLog(recordId, "警告: 未配置产物路径，跳过传输");
            return true;
        }

        java.io.File projectDir = projectPath != null ? new java.io.File(projectPath) : null;
        java.io.File outputFile;
        if (new java.io.File(outputPath).isAbsolute()) {
            outputFile = new java.io.File(outputPath);
        } else {
            outputFile = new java.io.File(projectDir, outputPath);
        }
        if (!outputFile.exists()) {
            webSocketHandler.sendLog(recordId, "错误: 产物不存在: " + outputFile.getAbsolutePath());
            return false;
        }

        // 如果产物是目录，自动压缩为zip
        File tempZipFile = null;
        if (outputFile.isDirectory()) {
            webSocketHandler.sendLog(recordId, "检测到产物为目录，自动压缩为zip: " + outputFile.getName());
            try {
                tempZipFile = File.createTempFile("deployhub_" + System.currentTimeMillis(), ".zip");
                tempZipFile.deleteOnExit();
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tempZipFile);
                java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos);
                compressDirectory(outputFile, "", zos);
                zos.close();
                fos.close();
                webSocketHandler.sendLog(recordId, "目录压缩完成: " + tempZipFile.length() + " bytes");
                outputFile = tempZipFile;
            } catch (Exception e) {
                webSocketHandler.sendLog(recordId, "目录压缩失败: " + e.getMessage());
                return false;
            }
        }

        String remotePath = OsUtil.REMOTE_TEMP_DIR + "/deployhub_" + System.currentTimeMillis() + "_" + outputFile.getName();
        webSocketHandler.sendStep(recordId, "TRANSFERRING");
        webSocketHandler.sendLog(recordId, "传输产物到服务器: " + remotePath);
        boolean success = SshClient.uploadFile(session, outputFile.getAbsolutePath(), remotePath);

        // 清理临时 zip 文件
        if (tempZipFile != null && tempZipFile.exists()) {
            tempZipFile.delete();
        }

        if (success) {
            variables.put("REMOTE_FILE_PATH", remotePath);
            webSocketHandler.sendLog(recordId, "产物传输成功");
        } else {
            webSocketHandler.sendLog(recordId, "产物传输失败");
            return false;
        }
        return true;
    }

    private boolean buildAndTransfer(Session session, App app, Map<String, String> variables) {
        String projectPath = app.getProjectPath();
        if (projectPath == null || projectPath.isEmpty()) {
            webSocketHandler.sendLog(recordId, "警告: 未配置项目路径，跳过构建和传输");
            return true;
        }

        java.io.File projectDir = new java.io.File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            webSocketHandler.sendLog(recordId, "错误: 项目路径不存在: " + projectPath);
            return false;
        }

        String buildCommand = app.getBuildCommand();
        if (buildCommand != null && !buildCommand.isEmpty()) {
            webSocketHandler.sendLog(recordId, "开始本地构建, 命令: " + buildCommand);
            try {
                ProcessBuilder pb = OsUtil.createProcessBuilder(OsUtil.normalizeBuildCommand(buildCommand));
                pb.directory(projectDir);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        webSocketHandler.sendLog(recordId, "[BUILD] " + line);
                    }
                }
                boolean finished = process.waitFor(30, TimeUnit.MINUTES);
                if (!finished) {
                    process.destroyForcibly();
                    webSocketHandler.sendLog(recordId, "构建超时(30分钟)");
                    return false;
                }
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    webSocketHandler.sendLog(recordId, "构建失败, 退出码: " + exitCode);
                    return false;
                }
                webSocketHandler.sendLog(recordId, "本地构建成功");
            } catch (Exception e) {
                webSocketHandler.sendLog(recordId, "构建异常: " + e.getMessage());
                return false;
            }
        }

        String outputPath = app.getOutputPath();
        if (outputPath != null && !outputPath.isEmpty()) {
            java.io.File outputFile;
            if (new java.io.File(outputPath).isAbsolute()) {
                outputFile = new java.io.File(outputPath);
            } else {
                outputFile = new java.io.File(projectDir, outputPath);
            }
            if (!outputFile.exists()) {
                webSocketHandler.sendLog(recordId, "错误: 构建产物不存在: " + outputFile.getAbsolutePath());
                return false;
            }

            // 如果产物是目录，自动压缩为zip
            File tempZipFile = null;
            if (outputFile.isDirectory()) {
                webSocketHandler.sendLog(recordId, "检测到产物为目录，自动压缩为zip: " + outputFile.getName());
                try {
                    tempZipFile = File.createTempFile("deployhub_" + System.currentTimeMillis(), ".zip");
                    tempZipFile.deleteOnExit();
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(tempZipFile);
                    java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos);
                    compressDirectory(outputFile, "", zos);
                    zos.close();
                    fos.close();
                    webSocketHandler.sendLog(recordId, "目录压缩完成: " + tempZipFile.length() + " bytes");
                    outputFile = tempZipFile;
                } catch (Exception e) {
                    webSocketHandler.sendLog(recordId, "目录压缩失败: " + e.getMessage());
                    return false;
                }
            }

            String remotePath = OsUtil.REMOTE_TEMP_DIR + "/deployhub_" + System.currentTimeMillis() + "_" + outputFile.getName();
            webSocketHandler.sendStep(recordId, "TRANSFERRING");
            webSocketHandler.sendLog(recordId, "传输产物到服务器: " + remotePath);
            boolean success = SshClient.uploadFile(session, outputFile.getAbsolutePath(), remotePath);

            // 清理临时 zip 文件
            if (tempZipFile != null && tempZipFile.exists()) {
                tempZipFile.delete();
            }

            if (success) {
                variables.put("REMOTE_FILE_PATH", remotePath);
                webSocketHandler.sendLog(recordId, "产物传输成功");
            } else {
                webSocketHandler.sendLog(recordId, "产物传输失败");
                return false;
            }
        }

        return true;
    }

    private String buildErrorMessage(SshExecuteResult result, String action) {
        String error = result.getError();
        if (error != null && !error.isEmpty()) {
            return action + "失败: " + error;
        }
        // stderr 为空时，用 exitCode + stdout 尾部作为错误信息
        String output = result.getOutput();
        String lastLines = "";
        if (output != null && !output.isEmpty()) {
            String[] lines = output.split("\\n");
            int start = Math.max(0, lines.length - 5);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < lines.length; i++) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(lines[i]);
            }
            lastLines = sb.toString();
        }
        return action + "失败, exitCode=" + result.getExitCode()
                + (lastLines.isEmpty() ? "" : ", 输出尾部:\n" + lastLines);
    }

    private void handleFailure(DeployRecord record, String errorMessage) {
        record.setStatus(DeployStatus.FAILED.name());
        record.setErrorMessage(errorMessage);
        record.setFinishedAt(LocalDateTime.now());
        deployRecordMapper.updateById(record);
        log.error("部署失败, recordId={}, error={}", recordId, errorMessage);
    }

    /**
     * 递归压缩目录到 ZipOutputStream
     *
     * @param dir       要压缩的目录
     * @param basePath  zip 内的基准路径
     * @param zos       ZipOutputStream
     * @throws Exception 压缩过程中的异常
     */
    private void compressDirectory(File dir, String basePath, java.util.zip.ZipOutputStream zos) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            String entryPath = basePath.isEmpty() ? file.getName() : basePath + "/" + file.getName();
            if (file.isDirectory()) {
                compressDirectory(file, entryPath, zos);
            } else {
                java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryPath);
                zos.putNextEntry(entry);
                java.io.FileInputStream fis = new java.io.FileInputStream(file);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                fis.close();
                zos.closeEntry();
            }
        }
    }
}
