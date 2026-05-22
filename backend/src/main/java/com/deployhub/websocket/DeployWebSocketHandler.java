package com.deployhub.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DeployWebSocketHandler {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendLog(Long recordId, String message) {
        try {
            messagingTemplate.convertAndSend("/topic/deploy/" + recordId, message);
        } catch (Exception e) {
            log.error("推送部署日志失败, recordId={}", recordId, e);
        }
    }

    public void sendStatus(Long recordId, String status) {
        Map<String, Object> message = new HashMap<>();
        message.put("recordId", recordId);
        message.put("status", status);
        message.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/deploy/" + recordId, message);
    }

    public void sendStep(Long recordId, String step) {
        sendStep(recordId, step, null);
    }

    public void sendStep(Long recordId, String step, String stepMessage) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "STEP");
        message.put("step", step);
        message.put("recordId", recordId);
        message.put("timestamp", System.currentTimeMillis());
        if (stepMessage != null && !stepMessage.isEmpty()) {
            message.put("message", stepMessage);
        }
        messagingTemplate.convertAndSend("/topic/deploy/" + recordId, message);
    }

    public void sendScriptLog(Long executionId, String message) {
        try {
            messagingTemplate.convertAndSend("/topic/script-execution/" + executionId, message);
        } catch (Exception e) {
            log.error("推送脚本执行日志失败, executionId={}", executionId, e);
        }
    }

    public void sendBuildLog(Long appId, String line) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "LOG");
            message.put("line", line);
            messagingTemplate.convertAndSend("/topic/build/" + appId, message);
        } catch (Exception e) {
            log.error("推送构建日志失败, appId={}", appId, e);
        }
    }

    public void sendBuildStatus(Long appId, String status) {
        sendBuildStatus(appId, status, null);
    }

    public void sendBuildStatus(Long appId, String status, String error) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "STATUS");
            message.put("status", status);
            if (error != null && !error.isEmpty()) {
                message.put("error", error);
            }
            messagingTemplate.convertAndSend("/topic/build/" + appId, message);
        } catch (Exception e) {
            log.error("推送构建状态失败, appId={}", appId, e);
        }
    }
}
