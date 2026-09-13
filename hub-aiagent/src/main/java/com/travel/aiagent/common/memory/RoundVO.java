package com.travel.aiagent.common.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一轮用户请求（第一套：多轮对话级短期记忆）。
 *
 * <p>对应 SillyTavern 里的「一轮」：用户说一句话 → Agent 完整执行一次 → 最终回复。
 * 只记录「用户说了什么 + 最终回了什么」，不含 agent 执行细节（执行细节属于第二套执行轨迹）。
 *
 * <p>三种结束方式都会写入 finalReply：
 * <ul>
 *   <li>finish —— 正常规划完成后的总结</li>
 *   <li>clarify —— 需要用户补充信息的话术</li>
 *   <li>overMaxLoopTimes —— 超轮次的强制总结</li>
 * </ul>
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

    /** 最终回复给用户的话（收尾时写入，覆盖三种结束方式） */
    private String finalReply;
}
