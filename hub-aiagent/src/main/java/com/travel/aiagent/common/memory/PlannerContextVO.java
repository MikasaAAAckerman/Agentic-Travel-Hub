package com.travel.aiagent.common.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 给 Orchestrator Planner 的结构化上下文 —— 五层时间轴。
 *
 * <p>从"最稳定"到"最新"依次排列，字段顺序即 prompt 顺序
 * （fastjson 按声明序序列化，null 字段不输出）：
 * <ol>
 *   <li>{@code userProfile} —— 跨会话稳定偏好（人格层），每条一句，上限 10 条</li>
 *   <li>{@code historySummary} —— 早期历史压缩摘要，上限 3 段 / 2000 字</li>
 *   <li>{@code recentRounds} —— 近期历史原文（未压缩），上限 20 轮，<b>不含本轮</b></li>
 *   <li>{@code userInput} —— 本轮用户输入</li>
 *   <li>{@code currentSubAgentResults} —— 本轮已派发 subAgent 的结果（内存态）</li>
 * </ol>
 *
 * <p>历史上曾用名：{@code userTag}（→ userProfile）、{@code rounds}（→ recentRounds）。
 * 原 {@code longTermHistory} 已删除：它装的是本会话近期历史，与 {@code recentRounds} 重合，
 * 名字又暗示跨会话，容易误导。
 *
 * <p>无数据的字段（{@code userProfile}、{@code historySummary}）保持 null，
 * 让该字段从 prompt 中消失 —— 模型看不到字段，就不会误以为"查过了但为空"。
 * {@code recentRounds} 为空时保留空数组，因为它表达的是"确实没有历史"。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannerContextVO {

    /** ① 跨会话稳定偏好，每条一句，上限 10 条；暂无数据时为 null */
    private List<String> userProfile;

    /** ② 早期历史压缩摘要；无压缩时为 null */
    private String historySummary;

    /** ③ 近期历史原文（未压缩，≤20 轮，不含本轮） */
    private List<RoundVO> recentRounds;

    /** ④ 本轮用户原始输入 */
    private String userInput;

    /** ⑤ 本轮已派发 subAgent 的执行结果 */
    private List<SubAgentResultVO> currentSubAgentResults;
}