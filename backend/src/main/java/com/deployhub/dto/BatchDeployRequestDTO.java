package com.deployhub.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量部署请求DTO
 * <p>
 * 用于一次性部署项目中所有应用到指定服务器
 *
 * @author MSI-1
 * @date 2026-05-20
 */
@Data
public class BatchDeployRequestDTO {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @NotEmpty(message = "服务器列表不能为空")
    private List<Long> serverIds;

    private String version;

    private String deployMode;
}
