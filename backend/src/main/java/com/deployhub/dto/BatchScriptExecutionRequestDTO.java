package com.deployhub.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchScriptExecutionRequestDTO {

    @NotEmpty(message = "脚本内容列表不能为空")
    private List<String> scriptContents;

    @NotEmpty(message = "服务器ID列表不能为空")
    private List<Long> serverIds;

    private String operator;
}