package com.deployhub.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_server")
public class Server extends BaseEntity {

    private String name;

    private String host;

    private Integer port;

    private String username;

    private String authType;

    private String password;

    private String privateKey;

    private String status;

    private String groupName;

    private String description;
}
