package com.deployhub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppVO {

    private Long id;

    private Long projectId;

    private String projectName;

    private String name;

    private String type;

    private String projectPath;

    private String buildCommand;

    private String frontendBuildCommand;

    private String backendBuildCommand;

    private String outputPath;

    private String remoteDeployPath;

    private String configFilePath;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
