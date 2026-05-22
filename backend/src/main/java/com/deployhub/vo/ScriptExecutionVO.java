package com.deployhub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScriptExecutionVO {

    private Long id;

    private String scriptContent;

    private Long serverId;

    private String serverName;

    private String serverHost;

    private String status;

    private String log;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String operator;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
