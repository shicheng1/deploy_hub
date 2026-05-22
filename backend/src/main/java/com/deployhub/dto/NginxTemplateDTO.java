package com.deployhub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NginxTemplateDTO {

    private Long id;

    @NotBlank(message = "模板名称不能为空")
    private String name;

    private String description;

    private String category;

    private String configContent;

    private String sourceServerIds;
}
