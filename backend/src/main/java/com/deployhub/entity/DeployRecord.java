package com.deployhub.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_deploy_record")
public class DeployRecord extends BaseEntity {

    private Long appId;

    private Long serverId;

    private String status;

    private String version;

    private String deployScript;

    private String deployLog;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String operator;
}
