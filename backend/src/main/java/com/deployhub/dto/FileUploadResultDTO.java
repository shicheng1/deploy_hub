package com.deployhub.dto;

import lombok.Data;

import java.util.List;

@Data
public class FileUploadResultDTO {

    private List<UploadResultItem> results;

    private int successCount;

    private int failCount;

    @Data
    public static class UploadResultItem {
        private Long serverId;
        private String serverName;
        private String serverHost;
        private boolean success;
        private String message;
    }
}