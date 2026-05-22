package com.deployhub.controller;

import com.deployhub.common.PageResult;
import com.deployhub.common.Result;
import com.deployhub.service.NginxPackageService;
import com.deployhub.vo.NginxPackageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/nginx-package")
public class NginxPackageController {

    @Autowired
    private NginxPackageService nginxPackageService;

    @GetMapping("/list")
    public Result<PageResult<NginxPackageVO>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    @RequestParam(required = false) String name) {
        return nginxPackageService.list(pageNum, pageSize, name);
    }

    @GetMapping("/{id}")
    public Result<NginxPackageVO> getById(@PathVariable Long id) {
        return nginxPackageService.getById(id);
    }

    @PostMapping("/upload")
    public Result<Void> upload(@RequestParam("file") MultipartFile file,
                                @RequestParam String name,
                                @RequestParam String version,
                                @RequestParam(required = false) String description) {
        return nginxPackageService.upload(file, name, version, description);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return nginxPackageService.delete(id);
    }

    @PostMapping("/{id}/install")
    public Result<String> installToServer(@PathVariable Long id,
                                           @RequestParam Long serverId) {
        return nginxPackageService.installToServer(id, serverId);
    }
}
