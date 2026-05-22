package com.deployhub.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DeployRequestDTO {

    @NotNull(message = "应用ID不能为空")
    private Long appId;

    @NotEmpty(message = "服务器ID列表不能为空")
    private List<Long> serverIds;

    private String version;

    private String deployScript;

    private String operator;
}
