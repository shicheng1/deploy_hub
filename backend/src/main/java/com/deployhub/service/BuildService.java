package com.deployhub.service;

public interface BuildService {
    void buildBackend(Long appId);
    void buildFrontend(Long appId);
    void buildAll(Long appId);
}
