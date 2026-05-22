package com.deployhub.controller;

import com.deployhub.common.Result;
import com.deployhub.dto.DeployConfigDTO;
import com.deployhub.service.DeployConfigService;
import com.deployhub.vo.DeployConfigVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deploy-config")
public class DeployConfigController {

    @Autowired
    private DeployConfigService deployConfigService;

    /**
     * 获取应用最新的部署配置
     *
     * @param appId 应用ID
     * @return 部署配置
     */
    @GetMapping("/{appId}")
    public Result<DeployConfigVO> getByAppId(@PathVariable Long appId) {
        return deployConfigService.getByAppId(appId);
    }

    /**
     * 保存部署配置
     *
     * @param dto 部署配置DTO
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> save(@Valid @RequestBody DeployConfigDTO dto) {
        return deployConfigService.save(dto);
    }
}
