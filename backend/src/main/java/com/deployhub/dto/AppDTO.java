package com.deployhub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppDTO {

    private Long id;

    private Long projectId;

    @NotBlank(message = "应用名称不能为空")
    private String name;

    @NotBlank(message = "应用类型不能为空")
    private String type;

    private String projectPath;

    private String buildCommand;

    private String frontendBuildCommand;

    private String backendBuildCommand;

    private String outputPath;

    private String remoteDeployPath;

    private String configFilePath;

    private String description;
}
