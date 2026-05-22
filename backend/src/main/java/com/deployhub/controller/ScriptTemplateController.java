package com.deployhub.controller;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.ScriptTemplateDTO;
import com.deployhub.service.ScriptTemplateService;
import com.deployhub.vo.ScriptTemplateVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/script")
public class ScriptTemplateController {

    @Autowired
    private ScriptTemplateService scriptTemplateService;

    @GetMapping("/list")
    public Result<PageResult<ScriptTemplateVO>> list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) Long appId,
                       @RequestParam(required = false) String category) {
        return scriptTemplateService.list(pageNum, pageSize, name, appId, category);
    }

    @GetMapping("/{id}")
    public Result<ScriptTemplateVO> getById(@PathVariable Long id) {
        return scriptTemplateService.getById(id);
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody ScriptTemplateDTO dto) {
        return scriptTemplateService.add(dto);
    }

    @PostMapping("/upload")
    public Result<Void> upload(@RequestParam("file") MultipartFile file,
                               @RequestParam(value = "name", required = false) String name,
                               @RequestParam(value = "description", required = false) String description) throws IOException {
        return scriptTemplateService.upload(file, name, description);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ScriptTemplateDTO dto) {
        dto.setId(id);
        return scriptTemplateService.update(dto);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return scriptTemplateService.delete(id);
    }
}