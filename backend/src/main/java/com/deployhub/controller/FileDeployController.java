package com.deployhub.controller;

import com.deployhub.common.Result;
import com.deployhub.dto.FileUploadRequestDTO;
import com.deployhub.dto.FileUploadResultDTO;
import com.deployhub.service.FileDeployService;
import com.deployhub.util.OsUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/file-deploy")
public class FileDeployController {

    @Autowired
    private FileDeployService fileDeployService;

    @PostMapping("/upload")
    public Result<FileUploadResultDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("serverIds") List<Long> serverIds,
            @RequestParam(value = "remotePath", defaultValue = "/tmp") String remotePath,
            @RequestParam(value = "fileName", required = false) String fileName) throws IOException {
        
        log.info("开始上传文件到服务器, fileName={}, serverIds={}", fileName != null ? fileName : file.getOriginalFilename(), serverIds);
        return fileDeployService.uploadFileToServers(file, serverIds, remotePath, fileName);
    }

    @PostMapping("/deploy")
    public Result<FileUploadResultDTO> deployWithScript(
            @RequestParam("file") MultipartFile file,
            @Valid FileUploadRequestDTO request) throws IOException {
        
        log.info("开始部署文件并执行脚本, fileName={}, serverIds={}", request.getFileName(), request.getServerIds());
        return fileDeployService.deployWithScript(file, request);
    }
}