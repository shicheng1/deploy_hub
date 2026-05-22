package com.deployhub.controller;

import com.deployhub.common.Result;
import com.deployhub.service.BuildService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/build")
public class BuildController {

    @Autowired
    private BuildService buildService;

    @PostMapping("/backend/{appId}")
    public Result<String> buildBackend(@PathVariable Long appId) {
        CompletableFuture.runAsync(() -> buildService.buildBackend(appId));
        return Result.success("构建已开始");
    }

    @PostMapping("/frontend/{appId}")
    public Result<String> buildFrontend(@PathVariable Long appId) {
        CompletableFuture.runAsync(() -> buildService.buildFrontend(appId));
        return Result.success("构建已开始");
    }

    @PostMapping("/all/{appId}")
    public Result<String> buildAll(@PathVariable Long appId) {
        CompletableFuture.runAsync(() -> buildService.buildAll(appId));
        return Result.success("构建已开始");
    }
}
