package com.deployhub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScriptTemplateDTO {

    private Long id;

    @NotBlank(message = "模板名称不能为空")
    private String name;

    private Long appId;

    @NotBlank(message = "模板内容不能为空")
    private String content;

    private String description;

    private String category;
}