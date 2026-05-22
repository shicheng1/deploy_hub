package com.deployhub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScriptTemplateVO {

    private Long id;

    private String name;

    private Long appId;

    private String appName;

    private String content;

    private String description;

    private String category;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}