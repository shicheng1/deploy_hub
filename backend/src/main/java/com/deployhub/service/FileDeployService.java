package com.deployhub.service;

import com.deployhub.common.Result;
import com.deployhub.dto.FileUploadRequestDTO;
import com.deployhub.dto.FileUploadResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileDeployService {

    Result<FileUploadResultDTO> uploadFileToServers(MultipartFile file, List<Long> serverIds, String remotePath, String fileName) throws IOException;

    Result<FileUploadResultDTO> deployWithScript(MultipartFile file, FileUploadRequestDTO request) throws IOException;
}