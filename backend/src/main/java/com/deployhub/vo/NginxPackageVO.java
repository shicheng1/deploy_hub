package com.deployhub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NginxPackageVO {

    private Long id;

    private String name;

    private String version;

    private String filePath;

    private Long fileSize;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
