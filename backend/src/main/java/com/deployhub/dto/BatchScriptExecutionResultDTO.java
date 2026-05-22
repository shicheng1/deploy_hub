package com.deployhub.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchScriptExecutionResultDTO {

    private List<Long> executionIds;

    private int successCount;

    private int failCount;
}