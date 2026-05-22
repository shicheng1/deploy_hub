package com.deployhub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ServerDTO {

    private Long id;

    @NotBlank(message = "服务器名称不能为空")
    private String name;

    @NotBlank(message = "主机地址不能为空")
    private String host;

    private Integer port;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "认证类型不能为空")
    private String authType;

    private String password;

    private String privateKey;

    private String status;

    private String groupName;

    private String description;
}
