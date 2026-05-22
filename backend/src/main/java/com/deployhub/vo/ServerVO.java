package com.deployhub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServerVO {

    private Long id;

    private String name;

    private String host;

    private Integer port;

    private String username;

    private String authType;

    private String status;

    private String groupName;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
