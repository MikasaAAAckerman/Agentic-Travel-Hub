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
 *   <li>historySummary → 压缩摘要层（越早的历史压成一段话）</li>
 *   <li>rounds → 近期历史原文（Active Working Set 层，<b>不含本轮</b>）</li>
 *   <li>currentSubAgentResults → 本轮已派发 subAgent 的执行结果</li>
 * </ul>
 *
 * <p><b>字段声明顺序就是 prompt 顺序</b>（fastjson 按声明序序列化）：
 * 越早的历史越靠前，本轮内容最后——这就是模型该看到的时间轴。
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

    /** 长期记忆 chunk（Persistent Facts 层，本轮占位空数组） */
    private List<String> longTermHistory;

    /** 压缩摘要：最早的历史被压成的一段话，空串表示没有 */
    private String historySummary;

    /** 近期历史原文（<b>不含本轮</b>），按时间正序 */
    private List<RoundVO> rounds;

    /** 用户当前输入 */
    private String userInput;

    /** 本轮已经派发过的 subAgent 执行结果（本轮内存态，未落库） */
    private List<SubAgentResultVO> currentSubAgentResults;
}
