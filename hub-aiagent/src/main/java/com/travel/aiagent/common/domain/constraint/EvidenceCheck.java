package com.travel.aiagent.common.domain.constraint;

/**
 * 证据校验结果。
 *
 * <p>"让模型逐字摘录原文"只是要求，不是校验。只有程序真的去原文里找了一遍，
 * 才能说这条证据成立。
 */
public enum EvidenceCheck {

    /** 指针/摘录已在原文或调用记录中核对通过 */
    PASSED,
    /** 核对失败，相关约束必须降级 */
    FAILED,
    /** 无需证据（如模型推断、领域静态规则） */
    NOT_APPLICABLE
}
