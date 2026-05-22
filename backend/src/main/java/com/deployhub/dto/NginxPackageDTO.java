package com.deployhub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NginxPackageDTO {

    private Long id;

    @NotBlank(message = "包名称不能为空")
    private String name;

    @NotBlank(message = "版本号不能为空")
    private String version;

    private String description;
}
