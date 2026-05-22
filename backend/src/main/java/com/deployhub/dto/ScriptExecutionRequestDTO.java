package com.deployhub.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ScriptExecutionRequestDTO {

    @NotEmpty(message = "脚本内容不能为空")
    private String scriptContent;

    @NotEmpty(message = "服务器ID列表不能为空")
    private List<Long> serverIds;

    private String operator;
}
