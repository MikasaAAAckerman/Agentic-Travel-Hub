package com.travel.aiagent.common.core.constraint;

import com.travel.aiagent.common.domain.constraint.ConstraintSet;
import com.travel.aiagent.common.domain.constraint.GlobalConstraint;
import com.travel.aiagent.common.domain.constraint.Receipt;
import com.travel.aiagent.common.domain.constraint.Verdict;
import com.travel.aiagent.common.domain.constraint.VerdictStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 约束回执的<b>后置对账器</b> —— 内容盲：纯结构校验，零 IO、零 LLM、可单测。
 *
 * <p>和 {@link PlanConstraintChecker} 的分工：
 * <ul>
 *   <li>前置（planner 之后、worker 之前）：拦"编造/遗漏/无理由"，避免错误进入记忆和工具链</li>
 *   <li>后置（子 Agent 结束后）：拦"缺整份回执 / 链路被篡改 / 版本过期"，并作为前置的兜底</li>
 * </ul>
 *
 * <p><b>不校验工具调用与执行结果</b>：工具结果是客观事实，不需要二次验证；
 * 要验证的是"模型有没有对每条约束交代清楚"。
 */
@Service
public class ConstraintAuditor {

    public AuditResult audit(ConstraintSet set, Receipt receipt) {

        ConstraintSet constraints = set == null ? ConstraintSet.empty() : set;
        Receipt r = receipt == null ? Receipt.empty() : receipt;

        // ── 0. 空集 + 空回执：没有约束要治理，直接放行 ──────────────────
        if (constraints.isEmpty() && r.isBlank()) {
            return AuditResult.pass();
        }

        List<Finding> findings = new ArrayList<>();

        // ── 1. digest：链路被截断/篡改，fail fast ────────────────────────
        if (!r.isBlank() && !constraints.digest().equals(r.digest())) {
            findings.add(Finding.of(FindingType.DIGEST_MISMATCH, Severity.FATAL, "",
                    "下行 digest=" + constraints.digest() + "，回执 digest=" + r.digest()));
        }

        // ── 2. revision：过期回执不参与对账 ─────────────────────────────
        if (!r.isBlank() && !constraints.revision().equals(r.revision())) {
            findings.add(Finding.of(FindingType.STALE_REVISION, Severity.FATAL, "",
                    "当前 revision=" + constraints.revision() + "，回执 revision=" + r.revision()));
        }

        // ── 3. 有约束却完全没有回执（如走轮次上限收尾）──────────────────
        boolean noReceipt = !constraints.isEmpty()
                && r.revision().isBlank() && r.digest().isBlank() && r.verdicts().isEmpty();
        if (noReceipt) {
            findings.add(Finding.of(FindingType.MISSING_RECEIPT, Severity.HIGH, "",
                    "下行 " + constraints.items().size() + " 条约束，回执为空"));
            return summarize(findings);
        }

        // ── 4. 逐条裁决：越界 / 重复 / 状态非法 / 无理由 ──────────────────
        Map<String, GlobalConstraint> byId = new HashMap<>();
        constraints.items().forEach(c -> byId.put(c.id(), c));

        Set<String> answered = new HashSet<>();
        for (Verdict v : r.verdicts()) {
            if (!byId.containsKey(v.constraintId())) {
                findings.add(Finding.of(FindingType.GHOST_RECEIPT, Severity.HIGH, v.constraintId(),
                        "回执引用了下行集合里不存在的约束"));
                continue;
            }
            if (!answered.add(v.constraintId())) {
                findings.add(Finding.of(FindingType.DUPLICATE_VERDICT, Severity.LOW, v.constraintId(),
                        "同一约束被表态了多次"));
            }
            if (!VerdictStatus.isValid(v.status().name())) {
                findings.add(Finding.of(FindingType.ILLEGAL_STATUS, Severity.HIGH, v.constraintId(),
                        "status 非法：" + v.status()));
            }
            if (!v.hasReason()) {
                findings.add(Finding.of(FindingType.EMPTY_REASON, Severity.MEDIUM, v.constraintId(),
                        "没有给出理由"));
            }
        }

        // ── 5. 兜底：完整性（前置校验已拦，这里防"绕过前置"的路径）───────
        for (GlobalConstraint c : constraints.items()) {
            if (!answered.contains(c.id())) {
                findings.add(Finding.of(FindingType.INCOMPLETE_RECEIPT, Severity.HIGH, c.id(),
                        "这条约束没有被表态：" + c.topic()));
            }
        }

        return summarize(findings);
    }

    private AuditResult summarize(List<Finding> findings) {
        if (findings.isEmpty()) {
            return AuditResult.pass();
        }
        int score = findings.stream().mapToInt(f -> f.severity().getWeight()).sum();
        boolean hard = findings.stream()
                .anyMatch(f -> f.severity() == Severity.FATAL || f.severity() == Severity.HIGH);
        return new AuditResult(findings, score, hard ? AuditVerdict.HARD_FAIL : AuditVerdict.SOFT_FAIL);
    }
}
