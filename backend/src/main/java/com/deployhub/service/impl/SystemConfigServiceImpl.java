package com.deployhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.deployhub.common.Result;
import com.deployhub.entity.SystemConfig;
import com.deployhub.mapper.SystemConfigMapper;
import com.deployhub.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Override
    public Result<List<SystemConfig>> list(String configGroup) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        if (configGroup != null && !configGroup.isEmpty()) {
            wrapper.eq(SystemConfig::getConfigGroup, configGroup);
        }
        wrapper.orderByAsc(SystemConfig::getConfigGroup, SystemConfig::getId);
        return Result.success(systemConfigMapper.selectList(wrapper));
    }

    @Override
    public Result<SystemConfig> getByKey(String configKey) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, configKey);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);
        if (config == null) {
            return Result.error("配置项不存在");
        }
        return Result.success(config);
    }

    @Override
    public Result<Void> save(SystemConfig config) {
        systemConfigMapper.insert(config);
        return Result.success();
    }

    @Override
    public Result<Void> update(Long id, SystemConfig config) {
        SystemConfig existing = systemConfigMapper.selectById(id);
        if (existing == null) {
            return Result.error("配置项不存在");
        }
        systemConfigMapper.updateById(config);
        return Result.success();
    }

    @Override
    public Result<Void> delete(Long id) {
        systemConfigMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, configKey);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);
        if (config != null && config.getConfigValue() != null) {
            return config.getConfigValue();
        }
        return defaultValue;
    }
}
