package com.deployhub.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeployConfigVO {

    private Long id;

    private Long appId;

    private List<Long> serverIds;

    private String version;

    private String serviceName;

    private String deployMode;

    private Long scriptTemplateId;

    private String deployScript;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
