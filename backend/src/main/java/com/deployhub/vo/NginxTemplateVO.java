package com.deployhub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NginxTemplateVO {

    private Long id;

    private String name;

    private String description;

    private String category;

    private String configContent;

    private String sourceServerIds;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
