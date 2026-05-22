package com.deployhub.dto;

import com.deployhub.util.OsUtil;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class FileUploadRequestDTO {

    @NotEmpty(message = "服务器ID列表不能为空")
    private List<Long> serverIds;

    private String remotePath = OsUtil.REMOTE_TEMP_DIR;

    private String fileName;

    private String installScript;
}