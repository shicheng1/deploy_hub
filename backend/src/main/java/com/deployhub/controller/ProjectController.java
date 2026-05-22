package com.deployhub.controller;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.ProjectDTO;
import com.deployhub.service.ProjectService;
import com.deployhub.vo.ProjectVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @GetMapping("/list")
    public Result<PageResult<ProjectVO>> list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String name) {
        return projectService.list(pageNum, pageSize, name);
    }

    @GetMapping("/{id}")
    public Result<ProjectVO> getById(@PathVariable Long id) {
        return projectService.getById(id);
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody ProjectDTO dto) {
        return projectService.add(dto);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProjectDTO dto) {
        return projectService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return projectService.delete(id);
    }
}
