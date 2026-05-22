package com.deployhub.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_nginx_package")
public class NginxPackage extends BaseEntity {

    private String name;

    private String version;

    private String filePath;

    private Long fileSize;

    private String description;
}
