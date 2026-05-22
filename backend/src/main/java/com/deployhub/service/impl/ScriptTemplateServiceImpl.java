package com.deployhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.ScriptTemplateDTO;
import com.deployhub.entity.App;
import com.deployhub.entity.ScriptTemplate;
import com.deployhub.mapper.AppMapper;
import com.deployhub.mapper.ScriptTemplateMapper;
import com.deployhub.service.ScriptTemplateService;
import com.deployhub.vo.ScriptTemplateVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScriptTemplateServiceImpl implements ScriptTemplateService {

    @Autowired
    private ScriptTemplateMapper scriptTemplateMapper;

    @Autowired
    private AppMapper appMapper;

    @Override
    public Result<PageResult<ScriptTemplateVO>> list(int pageNum, int pageSize, String name, Long appId, String category) {
        Page<ScriptTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ScriptTemplate> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(ScriptTemplate::getName, name);
        }
        if (appId != null) {
            if (appId == 0) {
                wrapper.isNull(ScriptTemplate::getAppId);
            } else {
                wrapper.eq(ScriptTemplate::getAppId, appId);
            }
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(ScriptTemplate::getCategory, category);
        }
        wrapper.orderByDesc(ScriptTemplate::getCreateTime);
        Page<ScriptTemplate> result = scriptTemplateMapper.selectPage(page, wrapper);

        List<ScriptTemplateVO> voList = result.getRecords().stream().map(template -> {
            ScriptTemplateVO vo = BeanUtil.copyProperties(template, ScriptTemplateVO.class);
            if (template.getAppId() != null) {
                App app = appMapper.selectById(template.getAppId());
                if (app != null) {
                    vo.setAppName(app.getName());
                }
            }
            return vo;
        }).collect(Collectors.toList());

        PageResult<ScriptTemplateVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(voList);
        return Result.success(pageResult);
    }

    @Override
    public Result<ScriptTemplateVO> getById(Long id) {
        ScriptTemplate template = scriptTemplateMapper.selectById(id);
        if (template == null) {
            return Result.error("脚本模板不存在");
        }
        ScriptTemplateVO vo = BeanUtil.copyProperties(template, ScriptTemplateVO.class);
        if (template.getAppId() != null) {
            App app = appMapper.selectById(template.getAppId());
            if (app != null) {
                vo.setAppName(app.getName());
            }
        }
        return Result.success(vo);
    }

    @Override
    public Result<Void> add(ScriptTemplateDTO dto) {
        ScriptTemplate template = BeanUtil.copyProperties(dto, ScriptTemplate.class);
        scriptTemplateMapper.insert(template);
        return Result.success();
    }

    @Override
    public Result<Void> upload(MultipartFile file, String name, String description) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        
        String scriptName = StrUtil.isBlank(name) ? file.getOriginalFilename() : name;
        if (StrUtil.isBlank(scriptName)) {
            scriptName = "uploaded_script";
        }
        
        String content = new String(file.getBytes());
        
        ScriptTemplate template = new ScriptTemplate();
        template.setName(scriptName);
        template.setContent(content);
        template.setDescription(description);
        scriptTemplateMapper.insert(template);
        
        return Result.success();
    }

    @Override
    public Result<Void> update(ScriptTemplateDTO dto) {
        ScriptTemplate template = scriptTemplateMapper.selectById(dto.getId());
        if (template == null) {
            return Result.error("脚本模板不存在");
        }
        BeanUtil.copyProperties(dto, template, "id", "createTime", "updateTime", "deleted");
        scriptTemplateMapper.updateById(template);
        return Result.success();
    }

    @Override
    public Result<Void> delete(Long id) {
        scriptTemplateMapper.deleteById(id);
        return Result.success();
    }
}