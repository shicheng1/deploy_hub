package com.deployhub.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_script_execution")
public class ScriptExecution extends BaseEntity {

    private String scriptContent;

    private Long serverId;

    private String status;

    private String log;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String operator;
}
