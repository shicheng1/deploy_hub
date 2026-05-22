package com.deployhub.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_nginx_template")
public class NginxTemplate extends BaseEntity {

    private String name;

    private String description;

    private String category;

    private String configContent;

    private String sourceServerIds;
}
