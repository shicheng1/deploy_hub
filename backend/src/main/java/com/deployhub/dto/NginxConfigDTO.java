package com.deployhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NginxConfigDTO {

    private Long id;

    @NotNull(message = "服务器ID不能为空")
    private Long serverId;

    @NotBlank(message = "配置名称不能为空")
    private String name;

    @NotBlank(message = "配置路径不能为空")
    private String configPath;

    private String configContent;

    private String description;
}
