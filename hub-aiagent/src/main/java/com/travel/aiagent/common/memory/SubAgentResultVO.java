package com.travel.aiagent.common.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一轮内 orchestrator 派发的某个 subAgent 的执行结果（第一套对话级记忆）。
 *
 * <p>挂在 {@link RoundVO#subAgentResults} 下，记录「这轮我派了哪个 subAgent、它回了什么」。
 * 供下一轮 orchestrator planner 读取——避免「orchestrator 不知道自己调用过 subAgent」的失忆，
 * 防止重复派发同一 subAgent（如反复派 EntertainmentAgent）。
 *
 * <p>conclusion 存【全文】不摘要，保证最大程度信息不丢失；
 * 是否失败不额外标记，交给 LLM 自行理解（当前模型能力足够）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubAgentResultVO {

    /** 哪个 subAgent（如 EntertainmentAgent） */
    private String agent;

    /** 执行结果全文（不摘要，保信息） */
    private String conclusion;
}
