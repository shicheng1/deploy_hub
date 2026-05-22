package com.deployhub.dto;

import lombok.Data;

@Data
public class ScriptGeneratorRequestDTO {

    private String projectType;
    private String appName;
    private String remoteDeployPath;
    private String configFilePath;
    private String jvmParams;
    private String serverPort;
    private Boolean reloadNginx;
}
