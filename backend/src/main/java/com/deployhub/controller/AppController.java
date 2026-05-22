package com.deployhub.controller;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.AppDTO;
import com.deployhub.service.AppService;
import com.deployhub.vo.AppVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app")
public class AppController {

    @Autowired
    private AppService appService;

    @GetMapping("/list")
    public Result<PageResult<AppVO>> list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) Long projectId) {
        return appService.list(pageNum, pageSize, name, type, projectId);
    }

    @GetMapping("/{id}")
    public Result<AppVO> getById(@PathVariable Long id) {
        return appService.getById(id);
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody AppDTO dto) {
        return appService.add(dto);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AppDTO dto) {
        dto.setId(id);
        return appService.update(dto);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return appService.delete(id);
    }
}
