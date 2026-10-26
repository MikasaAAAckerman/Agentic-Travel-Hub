package com.travel.aiagent.common.domain.constraint;

/**
 * 一条执行裁决。
 *
 * <p><b>没有 evidence 字段</b> —— 与 {@code reason} 合并。
 * 原因：已经不校验工具调用，`evidence` 退化成"一段说明"，和 reason 是同一件事；
 * 两个字段只会诱导模型"一个敷衍、一个认真"。
 *
 * @param constraintId 必须来自下行集合，不得自造；自造即"幽灵回执"
 * @param status       三态之一
 * @param usedAs       下游把它当成什么等级来用 —— 必须与下发时的等级相等
 * @param reason       为什么是这个状态 —— 三态都必填，代码只校验非空
 */
public record Verdict(
        String constraintId,
        VerdictStatus status,
        Modality usedAs,
        String reason
) {

    public Verdict {
        constraintId = constraintId == null ? "" : constraintId.trim();
        status = status == null ? VerdictStatus.UNUSED : status;
        usedAs = usedAs == null ? Modality.SHOULD : usedAs;
        reason = reason == null ? "" : reason.trim();
    }

    public boolean isApplied() {
        return status == VerdictStatus.APPLIED;
    }

    public boolean hasReason() {
        return !reason.isBlank();
    }
}
