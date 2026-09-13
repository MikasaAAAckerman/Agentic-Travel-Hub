package com.travel.aiagent.common.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 一轮用户请求（v3 结构化短期记忆的外层单元）。
 *
 * <p>对应 SillyTavern 里的「一轮」：用户说一句话 → Agent 完整执行一次。
 * 因为多 Agent 编排是「1:N」展开（一次请求 → N 个 subAgent 执行），
 * 所以一轮里含 {@code turns}（所有 agent 执行）而不是一对一平铺。
 *
 * <p>示例：
 * <pre>
 * {
 *   "round": 1,
 *   "userInput": "我要去广州3天",
 *   "turns": [ {agent, plan, conclusion}, ... ],
 *   "finalReply": "已为你规划广州3天行程..."
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundVO {

    /** 第几轮对话（从 1 开始） */
    private int round;

    /** 用户这一轮说了什么 */
    private String userInput;

    /** 这次请求触发的所有 agent 执行 */
    private List<TurnVO> turns;

    /** 最终回复给用户的话（一次请求收尾时写入） */
    private String finalReply;
}
