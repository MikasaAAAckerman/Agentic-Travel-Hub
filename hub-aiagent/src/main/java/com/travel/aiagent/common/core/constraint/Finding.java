package com.travel.aiagent.common.core.constraint;

/**
 * 一条对账发现。
 *
 * @param constraintId 关联的约束 id，可为空（如 digest 不匹配这类整体性错误）
 */
public record Finding(
        FindingType type,
        Severity severity,
        String constraintId,
        String detail
) {

    public Finding {
        constraintId = constraintId == null ? "" : constraintId;
        detail = detail == null ? "" : detail;
    }

    public static Finding of(FindingType type, Severity severity, String constraintId, String detail) {
        return new Finding(type, severity, constraintId, detail);
    }

    @Override
    public String toString() {
        return "[" + severity + "/" + type + "] " + constraintId + " " + detail;
    }
}
