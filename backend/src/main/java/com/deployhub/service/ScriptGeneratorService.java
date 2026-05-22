package com.deployhub.service;

public interface ScriptGeneratorService {

    String generateScript(String projectType, String appName, String remoteDeployPath,
                          String configFilePath, String jvmParams, String serverPort, Boolean reloadNginx);
}
