package com.deployhub.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_nginx_config")
public class NginxConfig extends BaseEntity {

    private Long serverId;

    private String name;

    private String configPath;

    private String configContent;

    private String status;

    private String description;
}
