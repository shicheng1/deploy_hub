package com.deployhub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectVO {

    private Long id;

    private String name;

    private String description;

    private Integer appCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
