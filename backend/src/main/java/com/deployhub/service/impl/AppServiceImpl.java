package com.deployhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.AppDTO;
import com.deployhub.entity.App;
import com.deployhub.entity.Project;
import com.deployhub.mapper.AppMapper;
import com.deployhub.mapper.ProjectMapper;
import com.deployhub.service.AppService;
import com.deployhub.vo.AppVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppServiceImpl implements AppService {

    @Autowired
    private AppMapper appMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Override
    public Result<PageResult<AppVO>> list(int pageNum, int pageSize, String name, String type, Long projectId) {
        Page<App> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<App> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(App::getName, name);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(App::getType, type);
        }
        if (projectId != null) {
            wrapper.eq(App::getProjectId, projectId);
        }
        wrapper.orderByDesc(App::getCreateTime);
        Page<App> result = appMapper.selectPage(page, wrapper);

        PageResult<AppVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(BeanUtil.copyToList(result.getRecords(), AppVO.class));
        // 填充项目名称
        pageResult.getRecords().forEach(this::fillProjectName);
        return Result.success(pageResult);
    }

    @Override
    public Result<AppVO> getById(Long id) {
        App app = appMapper.selectById(id);
        if (app == null) {
            return Result.error("应用不存在");
        }
        AppVO vo = BeanUtil.copyProperties(app, AppVO.class);
        fillProjectName(vo);
        return Result.success(vo);
    }

    @Override
    public Result<Void> add(AppDTO dto) {
        App app = BeanUtil.copyProperties(dto, App.class);
        appMapper.insert(app);
        return Result.success();
    }

    @Override
    public Result<Void> update(AppDTO dto) {
        App app = appMapper.selectById(dto.getId());
        if (app == null) {
            return Result.error("应用不存在");
        }
        BeanUtil.copyProperties(dto, app, "id", "createTime", "updateTime", "deleted");
        appMapper.updateById(app);
        return Result.success();
    }

    @Override
    public Result<Void> delete(Long id) {
        appMapper.deleteById(id);
        return Result.success();
    }

    /**
     * 填充应用所属项目名称
     *
     * @param vo 应用VO对象
     */
    private void fillProjectName(AppVO vo) {
        if (vo != null && vo.getProjectId() != null) {
            Project project = projectMapper.selectById(vo.getProjectId());
            if (project != null) {
                vo.setProjectName(project.getName());
            }
        }
    }
}
