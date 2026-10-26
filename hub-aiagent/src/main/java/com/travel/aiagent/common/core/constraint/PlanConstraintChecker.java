package com.travel.aiagent.common.core.constraint;

import com.travel.aiagent.common.constant.PlanActionEnum;
import com.travel.aiagent.common.domain.ConstraintUseVO;
import com.travel.aiagent.common.domain.PlanDetailVO;
import com.travel.aiagent.common.domain.constraint.ConstraintSet;
import com.travel.aiagent.common.domain.constraint.GlobalConstraint;
import com.travel.aiagent.common.domain.constraint.Modality;
import com.travel.aiagent.common.domain.constraint.VerdictStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 约束回执的<b>前置校验器</b> —— 纯函数，零 IO、零 LLM、零 Spring。
 *
 * <p>时机：{@code doSubAgentPlanReAct()} 之后、<b>worker 执行之前</b>。
 * 因为 LLM 是自回归的：一旦它输出了错误的回执，后续推理会围绕这个错误自我一致地展开，
 * 还可能带着它去调工具（有真实副作用）。<b>必须在错误进入记忆之前掐掉。</b>
 *
 * <p><b>判定强度声明：</b>本类只判断「有没有交代」，不判断「交代得对不对」。
 * {@code reason} 的语义永远大于任何关键词匹配，因此<b>不做内容校验</b>
 * （否则"我不认为它无关"这类句子会被 contains("无关") 误杀，导致 Agent 空转）。
 * 已接受的代价：reason 复述约束原文，本类无法识别。
 */
public final class PlanConstraintChecker {

    /** reason 的判据只有一条：非空白 */
    private static final String ERR_NO_REASON = "没有给出理由";
    private static final String ERR_BAD_STATUS = "的 status 非法：";

    private PlanConstraintChecker() {
    }

    /**
     * @param action        本轮 planner 给出的动作
     * @param plan          planner 的完整输出
     * @param constraintSet 下发下去的约束集（只读）
     */
    public static CheckResult check(String action, PlanDetailVO plan, ConstraintSet constraintSet) {

        // ── 无约束 / 尚未到结论：直接放行 ────────────────────────
        if (constraintSet == null || constraintSet.isEmpty()) {
            return CheckResult.pass();
        }
        if (action == null || !action.equals(PlanActionEnum.FINISH.getType())) {
            // 回执只在 FINISH 那一步产生。中间轮（TOOL_CALL / CLARIFY）即使带了 constraintUse，
            // 也只是模型的中间思考：不校验、不采纳、不进回执 —— 它影响不了账，所以不必管。
            return CheckResult.pass();
        }

        List<String> errors = new ArrayList<>();
        List<ConstraintUseVO> uses = plan == null || plan.getConstraintUse() == null
                ? List.of()
                : plan.getConstraintUse();

        Set<String> expected = new HashSet<>();
        Map<String, GlobalConstraint> byId = new HashMap<>();
        constraintSet.items().forEach(c -> {
            expected.add(c.id());
            byId.put(c.id(), c);
        });

        Set<String> actual = new HashSet<>();
        for (ConstraintUseVO u : uses) {
            if (u != null && u.getConstraintId() != null) {
                actual.add(u.getConstraintId().trim());
            }
        }

        // ── ① 不得缺失 ────────────────────────────────────────
        for (String id : expected) {
            if (!actual.contains(id)) {
                errors.add("约束 " + id + " 没有表态（三态任一都必须给出）");
            }
        }

        // ── ② 不得越界 ────────────────────────────────────────
        for (String id : actual) {
            if (!expected.contains(id)) {
                errors.add("约束 " + id + " 不在下发集合中，属于编造");
            }
        }

        // ── ③ 不得重复 ────────────────────────────────────────
        Set<String> seen = new HashSet<>();
        for (ConstraintUseVO u : uses) {
            if (u == null || u.getConstraintId() == null) {
                continue;
            }
            String id = u.getConstraintId().trim();
            if (!seen.add(id)) {
                errors.add("约束 " + id + " 被表态了多次，每条只允许一次");
            }
        }

        // ── ④ 状态合法 + ⑤ 必有理由 + ⑥ 等级不得被改动 ─────────
        for (ConstraintUseVO u : uses) {
            if (u == null) {
                continue;
            }
            String id = u.getConstraintId() == null ? "" : u.getConstraintId().trim();

            if (!VerdictStatus.isValid(u.getStatus())) {
                errors.add("约束 " + id + ERR_BAD_STATUS + u.getStatus());
            }
            if (u.getReason() == null || u.getReason().isBlank()) {
                errors.add("约束 " + id + " " + ERR_NO_REASON);
            }

            // 等级校验：只判"等不等于"，不区分升级/降级。
            // 这是纯枚举比较，不涉及语义 —— 只要不一致，就说明模型对它等级的理解发生了漂移。
            if (!Modality.isValid(u.getUsedAs())) {
                errors.add("约束 " + id + " 的 usedAs 非法：" + u.getUsedAs()
                        + "（只允许 MUST / SHOULD / PREFER）");
            } else {
                GlobalConstraint recorded = byId.get(id);
                if (recorded != null) {
                    Modality used = Modality.fromCode(u.getUsedAs());
                    if (used != recorded.modality()) {
                        errors.add("约束 " + id + " 为 " + recorded.modality()
                                + " 级，在此次任务规划中被当成了 " + used + " 级使用");
                    }
                }
            }
        }

        return errors.isEmpty() ? CheckResult.pass() : CheckResult.fail(errors);
    }

    /**
     * 校验结果。
     */
    public record CheckResult(boolean ok, List<String> errors) {

        public CheckResult {
            errors = errors == null ? List.of() : List.copyOf(errors);
        }

        static CheckResult pass() {
            return new CheckResult(true, List.of());
        }

        static CheckResult fail(List<String> errors) {
            return new CheckResult(false, errors);
        }

        /** 拼成注入下一轮 Prompt 的多行文本 */
        public String renderErrors() {
            if (errors.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (String e : errors) {
                sb.append("- ").append(e).append('\n');
            }
            return sb.toString().trim();
        }
    }
}
