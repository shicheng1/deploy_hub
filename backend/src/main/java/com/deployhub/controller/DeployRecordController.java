package com.deployhub.controller;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.dto.BatchDeployRequestDTO;
import com.deployhub.dto.DeployRequestDTO;
import com.deployhub.service.DeployRecordService;
import com.deployhub.vo.DeployRecordVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/deploy")
public class DeployRecordController {

    @Autowired
    private DeployRecordService deployRecordService;

    @GetMapping("/list")
    public Result<PageResult<DeployRecordVO>> list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) Long appId,
                       @RequestParam(required = false) Long serverId,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime) {
        return deployRecordService.list(pageNum, pageSize, appId, serverId, status, startTime, endTime);
    }

    @GetMapping("/{id}")
    public Result<DeployRecordVO> getById(@PathVariable Long id) {
        return deployRecordService.getById(id);
    }

    @PostMapping("/trigger")
    public Result trigger(@Valid @RequestBody DeployRequestDTO dto,
                          @RequestParam(defaultValue = "FULL") String deployMode) {
        return deployRecordService.trigger(dto, deployMode);
    }

    /**
     * 批量部署项目中所有应用
     *
     * @param request 批量部署请求，包含项目ID、服务器列表、版本号和部署模式
     * @return 所有部署记录ID列表
     */
    @PostMapping("/batch-trigger")
    public Result<List<Long>> batchTrigger(@Valid @RequestBody BatchDeployRequestDTO request) {
        log.info("收到批量部署请求, projectId={}, serverIds={}, version={}, deployMode={}",
                request.getProjectId(), request.getServerIds(), request.getVersion(), request.getDeployMode());
        return deployRecordService.batchDeploy(request);
    }

    @PostMapping("/{id}/rollback")
    public Result<Void> rollback(@PathVariable Long id) {
        return deployRecordService.rollback(id);
    }

    @GetMapping("/{id}/log")
    public Result<String> getDeployLog(@PathVariable Long id) {
        return deployRecordService.getDeployLog(id);
    }
}
