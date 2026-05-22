package com.deployhub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NginxConfigVO {

    private Long id;

    private Long serverId;

    private String serverName;

    private String serverHost;

    private String name;

    private String configPath;

    private String configContent;

    private String status;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
