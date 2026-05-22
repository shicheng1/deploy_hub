package com.deployhub.controller;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.BatchScriptExecutionRequestDTO;
import com.deployhub.dto.ScriptExecutionRequestDTO;
import com.deployhub.service.ScriptExecutionService;
import com.deployhub.vo.ScriptExecutionVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/script-execution")
public class ScriptExecutionController {

    @Autowired
    private ScriptExecutionService scriptExecutionService;

    @GetMapping("/list")
    public Result<PageResult<ScriptExecutionVO>> list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) Long serverId,
                       @RequestParam(required = false) String status) {
        return scriptExecutionService.list(pageNum, pageSize, serverId, status);
    }

    @GetMapping("/{id}")
    public Result<ScriptExecutionVO> getById(@PathVariable Long id) {
        return scriptExecutionService.getById(id);
    }

    @PostMapping("/execute")
    public Result<Map<String, Object>> execute(@Valid @RequestBody ScriptExecutionRequestDTO dto) {
        return scriptExecutionService.execute(dto);
    }

    @PostMapping("/batch-execute")
    public Result batchExecute(@Valid @RequestBody BatchScriptExecutionRequestDTO dto) {
        return scriptExecutionService.batchExecute(dto);
    }

    @GetMapping("/{id}/log")
    public Result<String> getLog(@PathVariable Long id) {
        return scriptExecutionService.getLog(id);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        return scriptExecutionService.cancel(id);
    }
}