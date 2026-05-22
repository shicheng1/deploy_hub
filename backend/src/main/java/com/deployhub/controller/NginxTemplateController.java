package com.deployhub.controller;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.NginxTemplateDTO;
import com.deployhub.service.NginxTemplateService;
import com.deployhub.vo.NginxConfigVO;
import com.deployhub.vo.NginxTemplateVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/nginx-template")
public class NginxTemplateController {

    @Autowired
    private NginxTemplateService nginxTemplateService;

    @GetMapping("/list")
    public Result<PageResult<NginxTemplateVO>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(required = false) String name,
                                                     @RequestParam(required = false) String category) {
        return nginxTemplateService.list(pageNum, pageSize, name, category);
    }

    @GetMapping("/{id}")
    public Result<NginxTemplateVO> getById(@PathVariable Long id) {
        return nginxTemplateService.getById(id);
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody NginxTemplateDTO dto) {
        return nginxTemplateService.add(dto);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NginxTemplateDTO dto) {
        dto.setId(id);
        return nginxTemplateService.update(dto);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return nginxTemplateService.delete(id);
    }

    @PostMapping("/{id}/create-config")
    public Result<NginxConfigVO> createConfigFromTemplate(@PathVariable Long id,
                                                           @RequestParam Long serverId,
                                                           @RequestParam String configName) {
        return nginxTemplateService.createConfigFromTemplate(id, serverId, configName);
    }
}
