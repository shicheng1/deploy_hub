package com.deployhub.controller;

import com.deployhub.common.Result;
import com.deployhub.service.ScriptVariableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/script-variable")
public class ScriptVariableController {

    @Autowired
    private ScriptVariableService scriptVariableService;

    @GetMapping("/list")
    public Result<Map<String, Object>> listAll() {
        return scriptVariableService.listAll();
    }

    @GetMapping("/{name}")
    public Result<Map<String, Object>> getByName(@PathVariable String name) {
        return scriptVariableService.getByName(name);
    }
}