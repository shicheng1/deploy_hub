package com.deployhub.controller;

import com.deployhub.common.Result;
import com.deployhub.entity.SystemConfig;
import com.deployhub.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/config")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @GetMapping("/list")
    public Result<List<SystemConfig>> list(@RequestParam(required = false) String configGroup) {
        return systemConfigService.list(configGroup);
    }

    @GetMapping("/{configKey}")
    public Result<SystemConfig> getByKey(@PathVariable String configKey) {
        return systemConfigService.getByKey(configKey);
    }

    @PostMapping
    public Result<Void> save(@RequestBody SystemConfig config) {
        return systemConfigService.save(config);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SystemConfig config) {
        config.setId(id);
        return systemConfigService.update(id, config);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return systemConfigService.delete(id);
    }
}
