package com.deployhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.deploy.DeployEngine;
import com.deployhub.deploy.DeployStatus;
import com.deployhub.deploy.DeployTask;
import com.deployhub.dto.BatchDeployRequestDTO;
import com.deployhub.dto.DeployRequestDTO;
import com.deployhub.entity.App;
import com.deployhub.entity.DeployConfig;
import com.deployhub.entity.DeployRecord;
import com.deployhub.entity.Server;
import com.deployhub.mapper.AppMapper;
import com.deployhub.mapper.DeployConfigMapper;
import com.deployhub.mapper.DeployRecordMapper;
import com.deployhub.mapper.ServerMapper;
import com.deployhub.service.DeployRecordService;
import com.deployhub.vo.DeployConfigVO;
import com.deployhub.vo.DeployRecordVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeployRecordServiceImpl implements DeployRecordService {

    @Autowired
    private DeployRecordMapper deployRecordMapper;

    @Autowired
    private AppMapper appMapper;

    @Autowired
    private ServerMapper serverMapper;

    @Autowired
    private DeployConfigMapper deployConfigMapper;

    @Autowired
    private DeployEngine deployEngine;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Result<PageResult<DeployRecordVO>> list(int pageNum, int pageSize, Long appId, Long serverId, String status, String startTime, String endTime) {
        Page<DeployRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DeployRecord> wrapper = new LambdaQueryWrapper<>();
        if (appId != null) {
            wrapper.eq(DeployRecord::getAppId, appId);
        }
        if (serverId != null) {
            wrapper.eq(DeployRecord::getServerId, serverId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DeployRecord::getStatus, status);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(DeployRecord::getCreateTime, LocalDateTime.parse(startTime, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(DeployRecord::getCreateTime, LocalDateTime.parse(endTime, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        wrapper.orderByDesc(DeployRecord::getCreateTime);
        Page<DeployRecord> result = deployRecordMapper.selectPage(page, wrapper);

        List<DeployRecordVO> voList = result.getRecords().stream().map(record -> {
            DeployRecordVO vo = BeanUtil.copyProperties(record, DeployRecordVO.class);
            App app = appMapper.selectById(record.getAppId());
            if (app != null) {
                vo.setAppName(app.getName());
            }
            Server server = serverMapper.selectById(record.getServerId());
            if (server != null) {
                vo.setServerName(server.getName());
            }
            return vo;
        }).collect(Collectors.toList());

        PageResult<DeployRecordVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(voList);
        return Result.success(pageResult);
    }

    @Override
    public Result<DeployRecordVO> getById(Long id) {
        DeployRecord record = deployRecordMapper.selectById(id);
        if (record == null) {
            return Result.error("部署记录不存在");
        }
        DeployRecordVO vo = BeanUtil.copyProperties(record, DeployRecordVO.class);
        App app = appMapper.selectById(record.getAppId());
        if (app != null) {
            vo.setAppName(app.getName());
        }
        Server server = serverMapper.selectById(record.getServerId());
        if (server != null) {
            vo.setServerName(server.getName());
        }
        return Result.success(vo);
    }

    @Override
    public Result<List<Long>> trigger(DeployRequestDTO dto, String deployMode) {
        List<Long> recordIds = new java.util.ArrayList<>();
        for (Long serverId : dto.getServerIds()) {
            DeployTask task = applicationContext.getBean(DeployTask.class);
            task.setAppId(dto.getAppId());
            task.setServerId(serverId);
            task.setVersion(dto.getVersion());
            task.setScript(dto.getDeployScript());
            task.setOperator(dto.getOperator());
            task.setDeployMode(deployMode);
            Long recordId = deployEngine.submitDeploy(task);
            recordIds.add(recordId);
        }
        return Result.success(recordIds);
    }

    /**
     * 批量部署项目中所有应用
     * <p>
     * 查询项目下所有应用，为每个应用获取其部署配置（如有），
     * 然后为每个应用在每个服务器上触发部署任务
     *
     * @param request 批量部署请求
     * @return 所有部署记录ID列表
     */
    @Override
    public Result<List<Long>> batchDeploy(BatchDeployRequestDTO request) {
        // 查询项目下所有应用
        LambdaQueryWrapper<App> appWrapper = new LambdaQueryWrapper<>();
        appWrapper.eq(App::getProjectId, request.getProjectId());
        List<App> apps = appMapper.selectList(appWrapper);

        if (apps == null || apps.isEmpty()) {
            return Result.error("该项目下没有应用");
        }

        List<Long> recordIds = new ArrayList<>();
        String deployMode = request.getDeployMode() != null ? request.getDeployMode() : "FULL";

        for (App app : apps) {
            // 获取应用已保存的部署配置
            DeployConfig config = getLatestDeployConfig(app.getId());

            // 确定使用的服务器列表：优先使用请求中的服务器，其次使用配置中的服务器
            List<Long> serverIds = request.getServerIds();
            if ((serverIds == null || serverIds.isEmpty()) && config != null) {
                serverIds = parseServerIds(config.getServerIds());
            }

            if (serverIds == null || serverIds.isEmpty()) {
                log.warn("应用[{}]没有可用的服务器配置，跳过部署", app.getName());
                continue;
            }

            // 确定版本号：优先使用请求中的版本，其次使用配置中的版本
            String version = request.getVersion() != null ? request.getVersion() : null;
            if (version == null && config != null) {
                version = config.getVersion();
            }

            // 确定部署脚本：优先使用配置中的脚本
            String deployScript = null;
            if (config != null) {
                deployScript = config.getDeployScript();
            }

            // 确定部署模式：优先使用请求中的模式，其次使用配置中的模式
            String effectiveDeployMode = deployMode;
            if ("FULL".equals(effectiveDeployMode) && config != null && config.getDeployMode() != null) {
                effectiveDeployMode = config.getDeployMode();
            }

            // 为每个服务器创建部署任务
            for (Long serverId : serverIds) {
                DeployTask task = applicationContext.getBean(DeployTask.class);
                task.setAppId(app.getId());
                task.setServerId(serverId);
                task.setVersion(version);
                task.setScript(deployScript);
                task.setOperator("batch");
                task.setDeployMode(effectiveDeployMode);

                Long recordId = deployEngine.submitDeploy(task);
                recordIds.add(recordId);
                log.info("批量部署 - 提交应用[{}]到服务器[{}]的部署任务, recordId={}", app.getName(), serverId, recordId);
            }
        }

        if (recordIds.isEmpty()) {
            return Result.error("没有可部署的应用或服务器配置");
        }

        log.info("批量部署完成, 共提交{}个部署任务", recordIds.size());
        return Result.success(recordIds);
    }

    /**
     * 获取应用最新的部署配置
     *
     * @param appId 应用ID
     * @return 最新的部署配置，不存在则返回null
     */
    private DeployConfig getLatestDeployConfig(Long appId) {
        LambdaQueryWrapper<DeployConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeployConfig::getAppId, appId)
               .orderByDesc(DeployConfig::getUpdateTime)
               .last("LIMIT 1");
        return deployConfigMapper.selectOne(wrapper);
    }

    /**
     * 将JSON字符串解析为服务器ID列表
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
        } catch (Exception e) {
            log.error("解析serverIds失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Result<Void> rollback(Long id) {
        DeployRecord record = deployRecordMapper.selectById(id);
        if (record == null) {
            return Result.error("部署记录不存在");
        }

        record.setStatus(DeployStatus.ROLLING_BACK.name());
        record.setUpdateTime(LocalDateTime.now());
        deployRecordMapper.updateById(record);

        DeployTask task = applicationContext.getBean(DeployTask.class);
        task.setAppId(record.getAppId());
        task.setServerId(record.getServerId());
        task.setVersion(record.getVersion());
        task.setScript(record.getDeployScript());
        task.setOperator(record.getOperator());
        task.setRollback(true);

        deployEngine.submitDeploy(task);
        return Result.success();
    }

    @Override
    public Result<String> getDeployLog(Long id) {
        DeployRecord record = deployRecordMapper.selectById(id);
        if (record == null) {
            return Result.error("部署记录不存在");
        }
        return Result.success(record.getDeployLog());
    }
}
