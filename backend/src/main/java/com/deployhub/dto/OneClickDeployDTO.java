package com.deployhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OneClickDeployDTO {
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @NotNull(message = "服务器ID不能为空")
    private Long serverId;

    private Long packageId;

    @NotBlank(message = "配置名称不能为空")
    private String configName;

    private String configPath;
}
