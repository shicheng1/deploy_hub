package com.deployhub.service;

import com.deployhub.common.Result;
import com.deployhub.entity.SystemConfig;

import java.util.List;

public interface SystemConfigService {
    Result<List<SystemConfig>> list(String configGroup);
    Result<SystemConfig> getByKey(String configKey);
    Result<Void> save(SystemConfig config);
    Result<Void> update(Long id, SystemConfig config);
    Result<Void> delete(Long id);
    String getConfigValue(String configKey, String defaultValue);
}
