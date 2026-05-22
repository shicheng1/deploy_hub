package com.deployhub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeployRecordVO {

    private Long id;

    private Long appId;

    private Long serverId;

    private String appName;

    private String serverName;

    private String status;

    private String version;

    private String deployScript;

    private String deployLog;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String operator;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
