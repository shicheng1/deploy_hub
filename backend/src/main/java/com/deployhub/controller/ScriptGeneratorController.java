package com.deployhub.controller;

import com.deployhub.common.Result;
import com.deployhub.dto.ScriptGeneratorRequestDTO;
import com.deployhub.service.ScriptGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/script-generator")
public class ScriptGeneratorController {

    @Autowired
    private ScriptGeneratorService scriptGeneratorService;

    @PostMapping("/generate")
    public Result<String> generate(@RequestBody ScriptGeneratorRequestDTO request) {
        String script = scriptGeneratorService.generateScript(
                request.getProjectType(),
                request.getAppName(),
                request.getRemoteDeployPath(),
                request.getConfigFilePath(),
                request.getJvmParams(),
                request.getServerPort(),
                request.getReloadNginx()
        );
        return Result.success(script);
    }
}
