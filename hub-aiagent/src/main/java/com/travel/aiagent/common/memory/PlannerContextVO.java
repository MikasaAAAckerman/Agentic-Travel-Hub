package com.travel.aiagent.common.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 给 Orchestrator Planner 的结构化上下文。
 *
 * <p>对应 Context Engineering 四层：
 * <ul>
 *   <li>userTag → Identity &amp; Role 层（本轮占位空数组）</li>
 *   <li>longTermHistory → Persistent Facts 层（本轮占位空数组，待 recallPreferences 接入）</li>
 *   <li>rounds → Active Working Set 层（本轮落地，结构化对话轮次）</li>
 * </ul>
 *
 * <p>最终序列化成 JSON 传给 planner 的 userMessage，替代原来的平铺字符串。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannerContextVO {

    /** 用户标签（Identity 层，本轮占位空数组） */
    private List<String> userTag;

    /** 用户当前输入 */
    private String userInput;

    /** 长期记忆 chunk（Persistent Facts 层，本轮占位空数组） */
    private List<String> longTermHistory;

    /** 短期记忆的对话轮次（Active Working Set 层，本轮落地） */
    private List<RoundVO> rounds;
}
