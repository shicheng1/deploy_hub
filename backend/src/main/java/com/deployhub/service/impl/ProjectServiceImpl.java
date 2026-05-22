package com.deployhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.ProjectDTO;
import com.deployhub.entity.App;
import com.deployhub.entity.Project;
import com.deployhub.mapper.AppMapper;
import com.deployhub.mapper.ProjectMapper;
import com.deployhub.service.ProjectService;
import com.deployhub.vo.ProjectVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private AppMapper appMapper;

    @Override
    public Result<PageResult<ProjectVO>> list(int pageNum, int pageSize, String name) {
        Page<Project> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(Project::getName, name);
        }
        wrapper.orderByDesc(Project::getCreateTime);
        Page<Project> result = projectMapper.selectPage(page, wrapper);

        // 统计每个项目下的应用数量
        List<ProjectVO> voList = BeanUtil.copyToList(result.getRecords(), ProjectVO.class);
        fillAppCount(voList);

        PageResult<ProjectVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(voList);
        return Result.success(pageResult);
    }

    @Override
    public Result<ProjectVO> getById(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            return Result.error("项目不存在");
        }
        ProjectVO vo = BeanUtil.copyProperties(project, ProjectVO.class);
        // 统计该项目下的应用数量
        LambdaQueryWrapper<App> appWrapper = new LambdaQueryWrapper<>();
        appWrapper.eq(App::getProjectId, id);
        Long appCount = appMapper.selectCount(appWrapper);
        vo.setAppCount(appCount.intValue());
        return Result.success(vo);
    }

    @Override
    public Result<Void> add(ProjectDTO dto) {
        Project project = BeanUtil.copyProperties(dto, Project.class);
        projectMapper.insert(project);
        return Result.success();
    }

    @Override
    public Result<Void> update(Long id, ProjectDTO dto) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            return Result.error("项目不存在");
        }
        BeanUtil.copyProperties(dto, project, "id", "createTime", "updateTime", "deleted");
        projectMapper.updateById(project);
        return Result.success();
    }

    @Override
    public Result<Void> delete(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            return Result.error("项目不存在");
        }
        // 检查项目下是否还有应用
        LambdaQueryWrapper<App> appWrapper = new LambdaQueryWrapper<>();
        appWrapper.eq(App::getProjectId, id);
        Long appCount = appMapper.selectCount(appWrapper);
        if (appCount > 0) {
            return Result.error("该项目下还有" + appCount + "个应用，无法删除");
        }
        projectMapper.deleteById(id);
        return Result.success();
    }

    /**
     * 批量填充项目下的应用数量
     *
     * @param voList 项目VO列表
     */
    private void fillAppCount(List<ProjectVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        // 查询所有应用，按projectId分组统计
        LambdaQueryWrapper<App> appWrapper = new LambdaQueryWrapper<>();
        appWrapper.select(App::getProjectId);
        List<App> apps = appMapper.selectList(appWrapper);
        Map<Long, Long> appCountMap = apps.stream()
                .filter(app -> app.getProjectId() != null)
                .collect(Collectors.groupingBy(App::getProjectId, Collectors.counting()));
        // 填充appCount
        for (ProjectVO vo : voList) {
            Long count = appCountMap.getOrDefault(vo.getId(), 0L);
            vo.setAppCount(count.intValue());
        }
    }
}
