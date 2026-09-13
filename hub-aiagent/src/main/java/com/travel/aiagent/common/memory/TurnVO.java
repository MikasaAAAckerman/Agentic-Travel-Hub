package com.travel.aiagent.common.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一次 Agent 执行（v3 结构化短期记忆的内层单元）。
 *
 * <p>对应「一次 subAgent 执行」：orchestrator 下发原始 plan → subAgent 执行 → 产出结论。
 * 挂在外层 {@link RoundVO} 之下，一轮（一次用户请求）可包含多次 agent 执行。
 *
 * <p>对比旧版 {@link ShortTermMemory} 的平铺 String 拼接，
 * 它把「谁、干了什么、结果如何」拆成结构化字段，debug 时能看清每步。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnVO {

    /** 哪个 subAgent（如 WeatherGraphAgent） */
    private String agent;

    /** orchestrator 下发的原始 plan */
    private String plan;

    /** 执行结果（本轮保留全文，不做摘要） */
    private String conclusion;
}
