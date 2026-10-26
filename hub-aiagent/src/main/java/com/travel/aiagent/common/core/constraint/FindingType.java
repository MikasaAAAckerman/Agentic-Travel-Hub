package com.travel.aiagent.common.core.constraint;

/**
 * 对账发现的违规类型 —— 全部可以由代码判定，不需要 LLM 参与。
 *
 * <p>注意这里<b>没有任何"语义/内容"类判定</b>（比如"这条约束用得对不对"、"
 * 工具到底有没有被正确使用"）。内容层面的评价属于 LLM-as-Judge，不属于这套结构审计。
 */
public enum FindingType {

    /** 回执 digest 与下行不一致：渲染/序列化链路被截断或篡改 */
    DIGEST_MISMATCH,
    /** 回执 revision 与当前版本不一致：过期回执 */
    STALE_REVISION,
    /** 完全没有回执（如子 Agent 走轮次上限收尾），无法验证 */
    MISSING_RECEIPT,
    /** 回执不完整：有约束没有被表态（前置校验已拦，这里是兜底） */
    INCOMPLETE_RECEIPT,
    /** 回执里的 constraintId 不在下行集合里 —— 幽灵回执/编造 */
    GHOST_RECEIPT,
    /** 同一约束被表态多次 */
    DUPLICATE_VERDICT,
    /** status 不在三态之内 */
    ILLEGAL_STATUS,
    /** 没有给出理由（前置校验已拦，这里是兜底） */
    EMPTY_REASON
}
