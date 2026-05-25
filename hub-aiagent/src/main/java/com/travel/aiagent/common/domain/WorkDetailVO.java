package com.travel.aiagent.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果类，用于记录工具执行成功与否，以及大模型调用工具的结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkDetailVO {

    /**
     * 工具是否执行成功
     */
    private boolean isSuccess;

    /**
     * 工具执行后的结果文字
     */
    private String conclusion;

}
