package com.deployhub.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一键部署步骤结果DTO
 * 记录部署过程中每个步骤的执行情况
 *
 * @author MSI-1
 * @date 2026-05-21
 */
@Data
public class DeployStepResult {

    /** 是否全部成功 */
    private boolean success;

    /** 当前执行到的步骤名称 */
    private String currentStep;

    /** 结果消息 */
    private String message;

    /** 各步骤详情列表 */
    private List<StepDetail> steps = new ArrayList<>();

    /**
     * 步骤详情
     */
    @Data
    public static class StepDetail {

        /** 步骤名称 */
        private String stepName;

        /** 是否成功 */
        private boolean success;

        /** 步骤执行消息 */
        private String message;

        /** 执行耗时（毫秒） */
        private long duration;
    }
}
