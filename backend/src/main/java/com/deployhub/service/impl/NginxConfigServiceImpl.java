package com.deployhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.NginxConfigDTO;
import com.deployhub.entity.NginxConfig;
import com.deployhub.entity.NginxTemplate;
import com.deployhub.entity.Server;
import com.deployhub.mapper.NginxConfigMapper;
import com.deployhub.mapper.NginxTemplateMapper;
import com.deployhub.mapper.ServerMapper;
import com.deployhub.service.NginxCommandService;
import com.deployhub.service.NginxConfigService;
import com.deployhub.ssh.SshExecuteResult;
import com.deployhub.vo.NginxConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NginxConfigServiceImpl implements NginxConfigService {

    @Autowired
    private NginxConfigMapper nginxConfigMapper;

    @Autowired
    private ServerMapper serverMapper;

    @Autowired
    private NginxTemplateMapper nginxTemplateMapper;

    @Autowired
    private NginxCommandService nginxCommandService;

    @Override
    public Result<PageResult<NginxConfigVO>> list(int pageNum, int pageSize, String name, Long serverId) {
        Page<NginxConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<NginxConfig> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(NginxConfig::getName, name);
        }
        if (serverId != null) {
            wrapper.eq(NginxConfig::getServerId, serverId);
        }
        wrapper.orderByDesc(NginxConfig::getCreateTime);
        Page<NginxConfig> result = nginxConfigMapper.selectPage(page, wrapper);

        List<NginxConfigVO> voList = result.getRecords().stream().map(config -> {
            NginxConfigVO vo = BeanUtil.copyProperties(config, NginxConfigVO.class);
            fillServerInfo(vo, config.getServerId());
            return vo;
        }).collect(Collectors.toList());

        PageResult<NginxConfigVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(voList);
        return Result.success(pageResult);
    }

    @Override
    public Result<NginxConfigVO> getById(Long id) {
        NginxConfig config = nginxConfigMapper.selectById(id);
        if (config == null) {
            return Result.error("Nginx配置不存在");
        }
        NginxConfigVO vo = BeanUtil.copyProperties(config, NginxConfigVO.class);
        fillServerInfo(vo, config.getServerId());
        return Result.success(vo);
    }

    @Override
    public Result<Void> add(NginxConfigDTO dto) {
        NginxConfig config = BeanUtil.copyProperties(dto, NginxConfig.class);
        config.setStatus("DRAFT");
        nginxConfigMapper.insert(config);
        return Result.success();
    }

    @Override
    public Result<Void> update(NginxConfigDTO dto) {
        NginxConfig config = nginxConfigMapper.selectById(dto.getId());
        if (config == null) {
            return Result.error("Nginx配置不存在");
        }
        BeanUtil.copyProperties(dto, config, "id", "createTime", "updateTime", "deleted", "status");
        nginxConfigMapper.updateById(config);
        return Result.success();
    }

    @Override
    public Result<Void> delete(Long id) {
        nginxConfigMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result<String> testConfig(Long id) {
        NginxConfig config = nginxConfigMapper.selectById(id);
        if (config == null) {
            return Result.error("Nginx配置不存在");
        }
        Server server = serverMapper.selectById(config.getServerId());
        if (server == null) {
            return Result.error("关联服务器不存在");
        }

        try {
            SshExecuteResult result = nginxCommandService.testConfig(server);
            String output = result.getOutput();
            if (result.getError() != null && !result.getError().isEmpty()) {
                output = (output != null ? output + "\n" : "") + result.getError();
            }
            return Result.success(output);
        } catch (Exception e) {
            log.error("测试Nginx配置失败, configId={}", id, e);
            return Result.error("测试Nginx配置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> deployConfig(Long id) {
        NginxConfig config = nginxConfigMapper.selectById(id);
        if (config == null) {
            return Result.error("Nginx配置不存在");
        }
        Server server = serverMapper.selectById(config.getServerId());
        if (server == null) {
            return Result.error("关联服务器不存在");
        }

        try {
            // 1. 写入配置文件
            SshExecuteResult writeResult = nginxCommandService.writeConfig(server, config.getConfigPath(), config.getConfigContent());
            if (!writeResult.isSuccess()) {
                config.setStatus("ERROR");
                nginxConfigMapper.updateById(config);
                return Result.error("写入配置文件失败: " + writeResult.getError());
            }

            // 2. 测试配置语法
            SshExecuteResult testResult = nginxCommandService.testConfig(server);
            if (!testResult.isSuccess()) {
                config.setStatus("ERROR");
                nginxConfigMapper.updateById(config);
                return Result.error("Nginx配置语法测试失败: " + testResult.getOutput() + "\n" + testResult.getError());
            }

            // 3. 重载Nginx
            SshExecuteResult reloadResult = nginxCommandService.reload(server);
            if (!reloadResult.isSuccess()) {
                // reload失败可能是因为nginx未运行，尝试restart
                SshExecuteResult restartResult = nginxCommandService.restart(server);
                if (!restartResult.isSuccess()) {
                    config.setStatus("ERROR");
                    nginxConfigMapper.updateById(config);
                    return Result.error("重载/启动Nginx失败: " + restartResult.getError());
                }
            }

            config.setStatus("DEPLOYED");
            nginxConfigMapper.updateById(config);
            return Result.success();
        } catch (Exception e) {
            log.error("部署Nginx配置失败, configId={}", id, e);
            config.setStatus("ERROR");
            nginxConfigMapper.updateById(config);
            return Result.error("部署Nginx配置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<NginxConfigVO> saveAsTemplate(Long id, String templateName, String templateDesc) {
        NginxConfig config = nginxConfigMapper.selectById(id);
        if (config == null) {
            return Result.error("Nginx配置不存在");
        }

        NginxTemplate template = new NginxTemplate();
        template.setName(templateName);
        template.setDescription(templateDesc);
        template.setConfigContent(config.getConfigContent());
        template.setSourceServerIds("[" + config.getServerId() + "]");
        nginxTemplateMapper.insert(template);

        return getById(id);
    }

    /**
     * 填充服务器信息到VO
     */
    private void fillServerInfo(NginxConfigVO vo, Long serverId) {
        if (serverId != null) {
            Server server = serverMapper.selectById(serverId);
            if (server != null) {
                vo.setServerName(server.getName());
                vo.setServerHost(server.getHost());
            }
        }
    }
}
