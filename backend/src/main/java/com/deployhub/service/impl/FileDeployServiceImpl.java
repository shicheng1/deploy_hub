package com.deployhub.service.impl;

import com.deployhub.common.Result;
import com.deployhub.dto.FileUploadRequestDTO;
import com.deployhub.dto.FileUploadResultDTO;
import com.deployhub.entity.Server;
import com.deployhub.mapper.ServerMapper;
import com.deployhub.service.FileDeployService;
import com.deployhub.ssh.SshPool;
import com.deployhub.util.OsUtil;
import com.deployhub.util.PasswordUtil;
import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileDeployServiceImpl implements FileDeployService {

    @Autowired
    private ServerMapper serverMapper;

    @Autowired
    private SshPool sshPool;

    @Override
    public Result<FileUploadResultDTO> uploadFileToServers(MultipartFile file, List<Long> serverIds, String remotePath, String fileName) throws IOException {
        List<FileUploadResultDTO.UploadResultItem> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        String actualFileName = fileName != null && !fileName.isEmpty() ? fileName : file.getOriginalFilename();

        for (Long serverId : serverIds) {
            FileUploadResultDTO.UploadResultItem item = new FileUploadResultDTO.UploadResultItem();
            item.setServerId(serverId);

            Server server = serverMapper.selectById(serverId);
            if (server == null) {
                item.setServerName("未知");
                item.setServerHost("未知");
                item.setSuccess(false);
                item.setMessage("服务器不存在");
                failCount++;
                results.add(item);
                continue;
            }

            item.setServerName(server.getName());
            item.setServerHost(server.getHost());

            try {
                uploadFileToServer(file, server, remotePath, actualFileName);
                item.setSuccess(true);
                item.setMessage("上传成功");
                successCount++;
            } catch (Exception e) {
                item.setSuccess(false);
                item.setMessage("上传失败: " + e.getMessage());
                failCount++;
                log.error("文件上传失败, serverId={}, error={}", serverId, e.getMessage());
            }

            results.add(item);
        }

        FileUploadResultDTO resultDTO = new FileUploadResultDTO();
        resultDTO.setResults(results);
        resultDTO.setSuccessCount(successCount);
        resultDTO.setFailCount(failCount);

        return Result.success(resultDTO);
    }

    @Override
    public Result<FileUploadResultDTO> deployWithScript(MultipartFile file, FileUploadRequestDTO request) throws IOException {
        String remotePath = request.getRemotePath() != null ? request.getRemotePath() : OsUtil.REMOTE_TEMP_DIR;
        String fileName = request.getFileName() != null && !request.getFileName().isEmpty()
            ? request.getFileName() : file.getOriginalFilename();

        Result<FileUploadResultDTO> uploadResult = uploadFileToServers(file, request.getServerIds(), remotePath, fileName);
        FileUploadResultDTO resultDTO = uploadResult.getData();

        if (request.getInstallScript() != null && !request.getInstallScript().isEmpty()) {
            for (FileUploadResultDTO.UploadResultItem item : resultDTO.getResults()) {
                if (!item.isSuccess()) {
                    continue;
                }

                Server server = serverMapper.selectById(item.getServerId());
                if (server == null) {
                    continue;
                }

                try {
                    executeScriptOnServer(server, request.getInstallScript().replace("${FILE_PATH}", remotePath + "/" + fileName));
                    item.setMessage(item.getMessage() + "; 脚本执行成功");
                } catch (Exception e) {
                    log.error("执行安装脚本失败, serverId={}, error={}", item.getServerId(), e.getMessage());
                    item.setSuccess(false);
                    item.setMessage(item.getMessage() + "; 脚本执行失败: " + e.getMessage());
                    resultDTO.setSuccessCount(resultDTO.getSuccessCount() - 1);
                    resultDTO.setFailCount(resultDTO.getFailCount() + 1);
                }
            }
        }

        return uploadResult;
    }

    private void uploadFileToServer(MultipartFile file, Server server, String remotePath, String fileName) throws Exception {
        String host = server.getHost();
        int port = server.getPort() != null ? server.getPort() : 22;
        String username = server.getUsername();
        String password = PasswordUtil.decrypt(server.getPassword());
        String privateKey = server.getPrivateKey();

        Session session = sshPool.borrowSession(host, port, username, password, privateKey);
        try {
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(5000);

            try {
                channel.cd(remotePath);
            } catch (SftpException e) {
                channel.mkdir(remotePath);
                channel.cd(remotePath);
            }

            try (InputStream inputStream = file.getInputStream()) {
                channel.put(inputStream, fileName);
            }

            channel.disconnect();
        } finally {
            sshPool.returnSession(host, port, session);
        }
    }

    private void executeScriptOnServer(Server server, String scriptContent) throws Exception {
        String host = server.getHost();
        int port = server.getPort() != null ? server.getPort() : 22;
        String username = server.getUsername();
        String password = PasswordUtil.decrypt(server.getPassword());
        String privateKey = server.getPrivateKey();

        Session session = sshPool.borrowSession(host, port, username, password, privateKey);
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            String normalizedScript = OsUtil.normalizeScriptContent(scriptContent);
            channel.setCommand(normalizedScript);
            InputStream inputStream = channel.getInputStream();
            InputStream errStream = channel.getExtInputStream();
            channel.connect(5000);

            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            byte[] buffer = new byte[1024];

            Thread outThread = new Thread(() -> {
                try {
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        output.append(new String(buffer, 0, len));
                    }
                } catch (IOException ignored) {}
            });
            Thread errThread = new Thread(() -> {
                try {
                    int len;
                    while ((len = errStream.read(buffer)) != -1) {
                        errorOutput.append(new String(buffer, 0, len));
                    }
                } catch (IOException ignored) {}
            });

            outThread.start();
            errThread.start();

            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            outThread.join(5000);
            errThread.join(5000);

            if (channel.getExitStatus() != 0) {
                throw new RuntimeException("脚本执行失败, exitCode=" + channel.getExitStatus() + ", output=" + errorOutput.toString());
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            sshPool.returnSession(host, port, session);
        }
    }
}
