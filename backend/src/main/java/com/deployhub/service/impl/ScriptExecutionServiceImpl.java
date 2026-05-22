package com.deployhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.BatchScriptExecutionRequestDTO;
import com.deployhub.dto.BatchScriptExecutionResultDTO;
import com.deployhub.dto.ScriptExecutionRequestDTO;
import com.deployhub.entity.ScriptExecution;
import com.deployhub.entity.Server;
import com.deployhub.mapper.ScriptExecutionMapper;
import com.deployhub.mapper.ServerMapper;
import com.deployhub.service.ScriptExecutionService;
import com.deployhub.ssh.SshPool;
import com.deployhub.util.OsUtil;
import com.deployhub.util.PasswordUtil;
import com.deployhub.vo.ScriptExecutionVO;
import com.deployhub.websocket.DeployWebSocketHandler;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScriptExecutionServiceImpl implements ScriptExecutionService {

    @Autowired
    private ScriptExecutionMapper scriptExecutionMapper;

    @Autowired
    private ServerMapper serverMapper;

    @Autowired
    private SshPool sshPool;

    @Autowired
    private DeployWebSocketHandler webSocketHandler;

    private final Map<Long, ChannelExec> executingChannels = new ConcurrentHashMap<>();

    @Override
    public Result<PageResult<ScriptExecutionVO>> list(int pageNum, int pageSize, Long serverId, String status) {
        Page<ScriptExecution> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ScriptExecution> wrapper = new LambdaQueryWrapper<>();
        if (serverId != null) {
            wrapper.eq(ScriptExecution::getServerId, serverId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ScriptExecution::getStatus, status);
        }
        wrapper.orderByDesc(ScriptExecution::getCreateTime);
        Page<ScriptExecution> result = scriptExecutionMapper.selectPage(page, wrapper);

        List<ScriptExecutionVO> voList = result.getRecords().stream().map(execution -> {
            ScriptExecutionVO vo = BeanUtil.copyProperties(execution, ScriptExecutionVO.class);
            Server server = serverMapper.selectById(execution.getServerId());
            if (server != null) {
                vo.setServerName(server.getName());
                vo.setServerHost(server.getHost());
            }
            return vo;
        }).collect(Collectors.toList());

        PageResult<ScriptExecutionVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(voList);
        return Result.success(pageResult);
    }

    @Override
    public Result<ScriptExecutionVO> getById(Long id) {
        ScriptExecution execution = scriptExecutionMapper.selectById(id);
        if (execution == null) {
            return Result.error("脚本执行记录不存在");
        }
        ScriptExecutionVO vo = BeanUtil.copyProperties(execution, ScriptExecutionVO.class);
        Server server = serverMapper.selectById(execution.getServerId());
        if (server != null) {
            vo.setServerName(server.getName());
            vo.setServerHost(server.getHost());
        }
        return Result.success(vo);
    }

    @Override
    public Result<Map<String, Object>> execute(ScriptExecutionRequestDTO dto) {
        List<Long> executionIds = new ArrayList<>();
        for (Long serverId : dto.getServerIds()) {
            ScriptExecution execution = new ScriptExecution();
            execution.setScriptContent(dto.getScriptContent());
            execution.setServerId(serverId);
            execution.setStatus("PENDING");
            execution.setOperator(dto.getOperator());
            execution.setCreateTime(LocalDateTime.now());
            execution.setUpdateTime(LocalDateTime.now());
            scriptExecutionMapper.insert(execution);

            Long executionId = execution.getId();
            executionIds.add(executionId);
            CompletableFuture.runAsync(() -> {
                executeOnServer(executionId, serverId, dto.getScriptContent());
            });
        }
        return Result.success(Map.of("executionIds", executionIds));
    }

    @Override
    public Result<BatchScriptExecutionResultDTO> batchExecute(BatchScriptExecutionRequestDTO dto) {
        List<Long> executionIds = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (String scriptContent : dto.getScriptContents()) {
            for (Long serverId : dto.getServerIds()) {
                try {
                    ScriptExecution execution = new ScriptExecution();
                    execution.setScriptContent(scriptContent);
                    execution.setServerId(serverId);
                    execution.setStatus("PENDING");
                    execution.setOperator(dto.getOperator());
                    execution.setCreateTime(LocalDateTime.now());
                    execution.setUpdateTime(LocalDateTime.now());
                    scriptExecutionMapper.insert(execution);

                    Long executionId = execution.getId();
                    executionIds.add(executionId);
                    successCount++;

                    CompletableFuture.runAsync(() -> {
                        executeOnServer(executionId, serverId, scriptContent);
                    });
                } catch (Exception e) {
                    log.error("创建执行记录失败, serverId={}, error={}", serverId, e.getMessage());
                    failCount++;
                }
            }
        }

        BatchScriptExecutionResultDTO resultDTO = new BatchScriptExecutionResultDTO();
        resultDTO.setExecutionIds(executionIds);
        resultDTO.setSuccessCount(successCount);
        resultDTO.setFailCount(failCount);

        return Result.success(resultDTO);
    }

    private void executeOnServer(Long executionId, Long serverId, String scriptContent) {
        ScriptExecution execution = scriptExecutionMapper.selectById(executionId);
        if (execution == null) return;

        execution.setStatus("RUNNING");
        execution.setStartedAt(LocalDateTime.now());
        scriptExecutionMapper.updateById(execution);

        StringBuilder logBuilder = new StringBuilder();
        
        webSocketHandler.sendScriptLog(executionId, "开始执行脚本, 服务器ID: " + serverId);
        logBuilder.append("开始执行脚本, 服务器ID: ").append(serverId).append("\n");

        Server server = serverMapper.selectById(serverId);
        if (server == null) {
            handleExecutionFailure(executionId, execution, "服务器不存在: " + serverId, logBuilder);
            return;
        }

        String host = server.getHost();
        int port = server.getPort() != null ? server.getPort() : 22;
        String username = server.getUsername();
        String password = PasswordUtil.decrypt(server.getPassword());
        String privateKey = server.getPrivateKey();

        Session session = null;
        ChannelExec channel = null;
        try {
            session = sshPool.borrowSession(host, port, username, password, privateKey);
            String connMsg = "SSH连接建立成功, 服务器: " + host;
            webSocketHandler.sendScriptLog(executionId, connMsg);
            logBuilder.append(connMsg).append("\n");
            execution.setLog(logBuilder.toString());
            scriptExecutionMapper.updateById(execution);

            channel = (ChannelExec) session.openChannel("exec");
            String normalizedScript = OsUtil.normalizeScriptContent(scriptContent);
            channel.setCommand(normalizedScript);
            executingChannels.put(executionId, channel);

            java.io.InputStream inputStream = channel.getInputStream();
            java.io.InputStream errStream = channel.getExtInputStream();
            channel.connect();

            byte[] buffer = new byte[1024];
            while (true) {
                while (inputStream.available() > 0) {
                    int len = inputStream.read(buffer, 0, buffer.length);
                    if (len < 0) break;
                    String output = new String(buffer, 0, len);
                    webSocketHandler.sendScriptLog(executionId, output);
                    logBuilder.append(output);
                    execution.setLog(logBuilder.toString());
                    scriptExecutionMapper.updateById(execution);
                }
                while (errStream.available() > 0) {
                    int len = errStream.read(buffer, 0, buffer.length);
                    if (len < 0) break;
                    String output = "[ERROR] " + new String(buffer, 0, len);
                    webSocketHandler.sendScriptLog(executionId, output);
                    logBuilder.append(output);
                    execution.setLog(logBuilder.toString());
                    scriptExecutionMapper.updateById(execution);
                }
                if (channel.isClosed()) {
                    while (inputStream.available() > 0) {
                        int len = inputStream.read(buffer, 0, buffer.length);
                        if (len < 0) break;
                        String output = new String(buffer, 0, len);
                        webSocketHandler.sendScriptLog(executionId, output);
                        logBuilder.append(output);
                    }
                    while (errStream.available() > 0) {
                        int len = errStream.read(buffer, 0, buffer.length);
                        if (len < 0) break;
                        String output = "[ERROR] " + new String(buffer, 0, len);
                        webSocketHandler.sendScriptLog(executionId, output);
                        logBuilder.append(output);
                    }
                    break;
                }
                Thread.sleep(100);
            }

            execution.setLog(logBuilder.toString());
            
            if (channel.getExitStatus() == 0) {
                execution.setStatus("SUCCESS");
                execution.setFinishedAt(LocalDateTime.now());
                scriptExecutionMapper.updateById(execution);
                String successMsg = "脚本执行成功";
                webSocketHandler.sendScriptLog(executionId, successMsg);
                logBuilder.append(successMsg).append("\n");
            } else {
                String errorMsg = "脚本执行失败, 退出码: " + channel.getExitStatus();
                handleExecutionFailure(executionId, execution, errorMsg, logBuilder);
            }
        } catch (Exception e) {
            String errorMsg = "脚本执行异常: " + e.getMessage();
            handleExecutionFailure(executionId, execution, errorMsg, logBuilder);
        } finally {
            executingChannels.remove(executionId);
            if (channel != null) {
                try {
                    channel.disconnect();
                } catch (Exception e) { /* ignore */ }
            }
            if (session != null) {
                sshPool.returnSession(host, port, session);
            }
        }
    }

    private void handleExecutionFailure(Long executionId, ScriptExecution execution, String errorMessage, StringBuilder logBuilder) {
        execution.setStatus("FAILED");
        execution.setErrorMessage(errorMessage);
        execution.setFinishedAt(LocalDateTime.now());
        if (logBuilder != null) {
            logBuilder.append(errorMessage).append("\n");
            execution.setLog(logBuilder.toString());
        }
        scriptExecutionMapper.updateById(execution);
        webSocketHandler.sendScriptLog(executionId, errorMessage);
        log.error("脚本执行失败, executionId={}, error={}", executionId, errorMessage);
    }

    @Override
    public Result<String> getLog(Long id) {
        ScriptExecution execution = scriptExecutionMapper.selectById(id);
        if (execution == null) {
            return Result.error("脚本执行记录不存在");
        }
        return Result.success(execution.getLog() != null ? execution.getLog() : "");
    }

    @Override
    public Result<Void> cancel(Long id) {
        ChannelExec channel = executingChannels.remove(id);
        if (channel != null && !channel.isClosed()) {
            try {
                channel.disconnect();
                ScriptExecution execution = scriptExecutionMapper.selectById(id);
                if (execution != null) {
                    execution.setStatus("CANCELLED");
                    execution.setFinishedAt(LocalDateTime.now());
                    execution.setErrorMessage("用户已取消执行");
                    scriptExecutionMapper.updateById(execution);
                    webSocketHandler.sendScriptLog(id, "脚本执行已取消");
                }
                return Result.success();
            } catch (Exception e) {
                log.error("取消脚本执行失败, id={}", id, e);
                return Result.error("取消失败: " + e.getMessage());
            }
        }
        return Result.error("未找到正在执行的任务或任务已完成");
    }
}