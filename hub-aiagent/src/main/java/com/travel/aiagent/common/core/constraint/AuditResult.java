package com.travel.aiagent.common.core.constraint;

import java.util.List;

/**
 * 对账结果。
 *
 * @param score 严重度加权分，0 表示干净
 */
public record AuditResult(
        List<Finding> findings,
        int score,
        AuditVerdict verdict
) {

    public AuditResult {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    public static AuditResult pass() {
        return new AuditResult(List.of(), 0, AuditVerdict.PASS);
    }

    public boolean hasFatal() {
        return findings.stream().anyMatch(f -> f.severity() == Severity.FATAL);
    }

    /** 供日志/指标用的紧凑描述 */
    public String summary() {
        if (findings.isEmpty()) {
            return "PASS";
        }
        StringBuilder sb = new StringBuilder(verdict.name()).append("(score=").append(score).append(")");
        for (Finding f : findings) {
            sb.append(" | ").append(f.type()).append(':').append(f.constraintId());
        }
        return sb.toString();
    }
}
