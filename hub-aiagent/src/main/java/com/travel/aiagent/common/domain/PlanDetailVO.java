package com.travel.aiagent.common.domain;

import com.travel.aiagent.common.annotation.PromptField;
import lombok.Data;

/**
 * 计划详情类，Planner 输出的结构化决策。
 *
 * <p>字段上的 {@code @PromptField} 自动同步到 {@code SystemPrompt} 的 JSON 示例，
 * 改字段时改注解即可，不会出现 Prompt 与实体不同步。
 */
@Data
public class PlanDetailVO {

    @PromptField(value = "你的思考过程，简述为什么要做这一步", example = "需要先查询天气才能给出穿搭建议")
    private String thought;

    @PromptField(value = "下一步动作：TOOL_CALL 或 FINISH 或 CLARIFY 或 SUB_AGENT_CALL ", example = "TOOL_CALL")
    private String action;

    @PromptField(value = "如果 action=TOOL_CALL，这里填要调用的工具描述", example = "查询广州天气")
    private String toolDescription;

    @PromptField(value = "如果 action=FINISH 或 CLARIFY，这里写最终回复用户的话术，否则留空", example = "已完成全部规划...")
    private String conclusion;

    @PromptField(value = "需要执行的计划详情（自然语言描述）", example = "查询目的地明天的实时天气情况")
    private String planDetail;

    @PromptField(value = "如果 action=SUB_AGENT_CALL，那么这里填需要调用的子Agent模块名 ", example = "FoodAgent")
    private String subAgentName;

}
