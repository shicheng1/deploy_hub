package com.deployhub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DeployConfigDTO {

    @NotNull(message = "应用ID不能为空")
    private Long appId;

    private List<Long> serverIds;

    private String version;

    private String serviceName;

    private String deployMode;

    private Long scriptTemplateId;

    private String deployScript;
}
