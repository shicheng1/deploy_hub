package com.deployhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.deployhub.common.Result;
import com.deployhub.dto.DeployConfigDTO;
import com.deployhub.entity.DeployConfig;
import com.deployhub.mapper.DeployConfigMapper;
import com.deployhub.service.DeployConfigService;
import com.deployhub.vo.DeployConfigVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class DeployConfigServiceImpl implements DeployConfigService {

    @Autowired
    private DeployConfigMapper deployConfigMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Result<DeployConfigVO> getByAppId(Long appId) {
        LambdaQueryWrapper<DeployConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeployConfig::getAppId, appId)
               .orderByDesc(DeployConfig::getUpdateTime)
               .last("LIMIT 1");
        DeployConfig config = deployConfigMapper.selectOne(wrapper);
        if (config == null) {
            return Result.error("该应用暂无部署配置");
        }

        DeployConfigVO vo = new DeployConfigVO();
        vo.setId(config.getId());
        vo.setAppId(config.getAppId());
        vo.setVersion(config.getVersion());
        vo.setServiceName(config.getServiceName());
        vo.setDeployMode(config.getDeployMode());
        vo.setScriptTemplateId(config.getScriptTemplateId());
        vo.setDeployScript(config.getDeployScript());
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());

        // 将JSON字符串转换为List<Long>
        vo.setServerIds(parseServerIds(config.getServerIds()));

        return Result.success(vo);
    }

    @Override
    public Result<Void> save(DeployConfigDTO dto) {
        DeployConfig config = new DeployConfig();
        config.setAppId(dto.getAppId());
        config.setVersion(dto.getVersion());
        config.setServiceName(dto.getServiceName());
        config.setDeployMode(dto.getDeployMode());
        config.setScriptTemplateId(dto.getScriptTemplateId());
        config.setDeployScript(dto.getDeployScript());

        // 将List<Long>转换为JSON字符串
        config.setServerIds(serializeServerIds(dto.getServerIds()));

        deployConfigMapper.insert(config);
        log.info("保存部署配置成功, appId: {}", dto.getAppId());
        return Result.success();
    }

    /**
     * 将JSON字符串解析为List<Long>
     *
     * @param json JSON字符串
     * @return 服务器ID列表
     */
    private List<Long> parseServerIds(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析serverIds失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    /**
     * 将List<Long>序列化为JSON字符串
     *
     * @param serverIds 服务器ID列表
     * @return JSON字符串
     */
    private String serializeServerIds(List<Long> serverIds) {
        if (serverIds == null || serverIds.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(serverIds);
        } catch (JsonProcessingException e) {
            log.error("序列化serverIds失败: {}", serverIds, e);
            return "[]";
        }
    }
}
