package com.deployhub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectDTO {

    private Long id;

    @NotBlank(message = "项目名称不能为空")
    private String name;

    private String description;
}
