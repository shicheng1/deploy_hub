package com.deployhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.NginxTemplateDTO;
import com.deployhub.entity.NginxConfig;
import com.deployhub.entity.NginxTemplate;
import com.deployhub.entity.Server;
import com.deployhub.mapper.NginxConfigMapper;
import com.deployhub.mapper.NginxTemplateMapper;
import com.deployhub.mapper.ServerMapper;
import com.deployhub.service.NginxTemplateService;
import com.deployhub.vo.NginxConfigVO;
import com.deployhub.vo.NginxTemplateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NginxTemplateServiceImpl implements NginxTemplateService {

    @Autowired
    private NginxTemplateMapper nginxTemplateMapper;

    @Autowired
    private NginxConfigMapper nginxConfigMapper;

    @Autowired
    private ServerMapper serverMapper;

    @Override
    public Result<PageResult<NginxTemplateVO>> list(int pageNum, int pageSize, String name, String category) {
        Page<NginxTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<NginxTemplate> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(NginxTemplate::getName, name);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(NginxTemplate::getCategory, category);
        }
        wrapper.orderByDesc(NginxTemplate::getCreateTime);
        Page<NginxTemplate> result = nginxTemplateMapper.selectPage(page, wrapper);

        PageResult<NginxTemplateVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(BeanUtil.copyToList(result.getRecords(), NginxTemplateVO.class));
        return Result.success(pageResult);
    }

    @Override
    public Result<NginxTemplateVO> getById(Long id) {
        NginxTemplate template = nginxTemplateMapper.selectById(id);
        if (template == null) {
            return Result.error("Nginx模板不存在");
        }
        return Result.success(BeanUtil.copyProperties(template, NginxTemplateVO.class));
    }

    @Override
    public Result<Void> add(NginxTemplateDTO dto) {
        NginxTemplate template = BeanUtil.copyProperties(dto, NginxTemplate.class);
        nginxTemplateMapper.insert(template);
        return Result.success();
    }

    @Override
    public Result<Void> update(NginxTemplateDTO dto) {
        NginxTemplate template = nginxTemplateMapper.selectById(dto.getId());
        if (template == null) {
            return Result.error("Nginx模板不存在");
        }
        BeanUtil.copyProperties(dto, template, "id", "createTime", "updateTime", "deleted");
        nginxTemplateMapper.updateById(template);
        return Result.success();
    }

    @Override
    public Result<Void> delete(Long id) {
        nginxTemplateMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result<NginxConfigVO> createConfigFromTemplate(Long templateId, Long serverId, String configName) {
        NginxTemplate template = nginxTemplateMapper.selectById(templateId);
        if (template == null) {
            return Result.error("Nginx模板不存在");
        }
        Server server = serverMapper.selectById(serverId);
        if (server == null) {
            return Result.error("目标服务器不存在");
        }

        // 替换模板中的服务器变量
        String configContent = template.getConfigContent();
        configContent = configContent.replace("${SERVER_HOST}", server.getHost());
        configContent = configContent.replace("${SERVER_IP}", server.getHost());

        // 创建Nginx配置
        NginxConfig config = new NginxConfig();
        config.setServerId(serverId);
        config.setName(configName);
        config.setConfigPath("/etc/nginx/conf.d/" + configName + ".conf");
        config.setConfigContent(configContent);
        config.setStatus("DRAFT");
        nginxConfigMapper.insert(config);

        // 构建返回的VO
        NginxConfigVO vo = BeanUtil.copyProperties(config, NginxConfigVO.class);
        vo.setServerName(server.getName());
        vo.setServerHost(server.getHost());
        return Result.success(vo);
    }
}
