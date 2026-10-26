package com.travel.aiagent.common.core.constraint;

import com.travel.aiagent.common.domain.ConstraintUseVO;
import com.travel.aiagent.common.domain.PlanDetailVO;
import com.travel.aiagent.common.domain.constraint.ConstraintSet;
import com.travel.aiagent.common.domain.constraint.EvidenceCheck;
import com.travel.aiagent.common.domain.constraint.GlobalConstraint;
import com.travel.aiagent.common.domain.constraint.Modality;
import com.travel.aiagent.common.domain.constraint.Provenance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlanConstraintChecker：前置校验（planner 之后、worker 之前）")
class PlanConstraintCheckerTest {

    private static GlobalConstraint gc(String id, Modality modality) {
        return new GlobalConstraint(id, "topic_" + id, "text-" + id, modality,
                Provenance.USER_EXPLICIT, "USER_INPUT#span[0,1]", EvidenceCheck.PASSED);
    }

    private static ConstraintSet set(String... ids) {
        return ConstraintSet.of("rev-1",
                Arrays.stream(ids).map(id -> gc(id, Modality.MUST)).toList());
    }

    private static ConstraintSet setOf(Modality modality, String... ids) {
        return ConstraintSet.of("rev-1",
                Arrays.stream(ids).map(id -> gc(id, modality)).toList());
    }

    private static PlanDetailVO plan(String action, ConstraintUseVO... uses) {
        PlanDetailVO p = new PlanDetailVO();
        p.setAction(action);
        p.setConstraintUse(List.of(uses));
        return p;
    }

    private static ConstraintUseVO use(String id, String status, String usedAs, String reason) {
        return new ConstraintUseVO(id, status, usedAs, reason);
    }

    private static ConstraintUseVO ok(String id) {
        return use(id, "UNUSED", "MUST", "这次用不上");
    }

    @Test
    @DisplayName("没有约束时一律放行")
    void noConstraintAlwaysPasses() {
        assertThat(PlanConstraintChecker.check("FINISH", plan("FINISH"), ConstraintSet.empty()).ok())
                .isTrue();
    }

    @Test
    @DisplayName("中间轮（TOOL_CALL）不校验：它只是模型的中间思考，不进回执")
    void nonFinishRoundsAreNotChecked() {
        PlanDetailVO p = plan("TOOL_CALL",
                use("c-0001", "APPLIED", "MUST", "先这么写着"),
                use("c-9999", "APPLIED", "MUST", "随手编的"));

        assertThat(PlanConstraintChecker.check("TOOL_CALL", p, set("c-0001")).ok()).isTrue();
    }

    @Test
    @DisplayName("FINISH 且全量对齐 + 都有理由 + 等级一致 = 通过")
    void finishFullyAlignedPasses() {
        PlanDetailVO p = plan("FINISH",
                use("c-0001", "APPLIED", "MUST", "推荐结果里没有海鲜"),
                use("c-0002", "UNUSED", "SHOULD", "本次不涉及预算分配"));

        assertThat(PlanConstraintChecker.check("FINISH", p,
                ConstraintSet.of("rev-1", List.of(
                        gc("c-0001", Modality.MUST), gc("c-0002", Modality.SHOULD)))).ok())
                .isTrue();
    }

    @Test
    @DisplayName("缺失一条 -> 点名到 id")
    void missingIsReported() {
        PlanDetailVO p = plan("FINISH", ok("c-0001"));

        PlanConstraintChecker.CheckResult r =
                PlanConstraintChecker.check("FINISH", p, set("c-0001", "c-0002"));

        assertThat(r.ok()).isFalse();
        assertThat(r.renderErrors()).contains("c-0002").contains("没有表态");
    }

    @Test
    @DisplayName("越界一条 -> 判编造")
    void ghostIsReported() {
        PlanDetailVO p = plan("FINISH",
                ok("c-0001"),
                use("c-9999", "APPLIED", "MUST", "编的"));

        PlanConstraintChecker.CheckResult r =
                PlanConstraintChecker.check("FINISH", p, set("c-0001"));

        assertThat(r.ok()).isFalse();
        assertThat(r.renderErrors()).contains("c-9999").contains("编造");
    }

    @Test
    @DisplayName("重复表态 -> 拒绝")
    void duplicateIsReported() {
        PlanDetailVO p = plan("FINISH",
                ok("c-0001"),
                use("c-0001", "APPLIED", "MUST", "又用上了"));

        PlanConstraintChecker.CheckResult r =
                PlanConstraintChecker.check("FINISH", p, set("c-0001"));

        assertThat(r.ok()).isFalse();
        assertThat(r.renderErrors()).contains("被表态了多次");
    }

    @Test
    @DisplayName("非法 status -> 拒绝")
    void illegalStatusIsReported() {
        PlanDetailVO p = plan("FINISH", use("c-0001", "DONE", "MUST", "笼统"));

        PlanConstraintChecker.CheckResult r =
                PlanConstraintChecker.check("FINISH", p, set("c-0001"));

        assertThat(r.ok()).isFalse();
        assertThat(r.renderErrors()).contains("status 非法");
    }

    @Test
    @DisplayName("没写理由 -> 拒绝（三态一视同仁）")
    void blankReasonIsReported() {
        for (String status : List.of("APPLIED", "UNUSED", "VIOLATED")) {
            PlanDetailVO p = plan("FINISH", use("c-0001", status, "MUST", "   "));

            PlanConstraintChecker.CheckResult r =
                    PlanConstraintChecker.check("FINISH", p, set("c-0001"));

            assertThat(r.ok()).as("status=%s", status).isFalse();
            assertThat(r.renderErrors()).contains("没有给出理由");
        }
    }

    @Test
    @DisplayName("理由只是随便一句也放行：只判非空，不判语义")
    void anyNonBlankReasonPasses() {
        PlanDetailVO p = plan("FINISH", use("c-0001", "UNUSED", "MUST", "无关"));

        assertThat(PlanConstraintChecker.check("FINISH", p, set("c-0001")).ok()).isTrue();
    }

    @Test
    @DisplayName("等级被改动 -> 拒绝（MUST 被当成 SHOULD 用）")
    void levelMismatchIsReported() {
        PlanDetailVO p = plan("FINISH", use("c-0001", "APPLIED", "SHOULD", "按软约束处理的"));

        PlanConstraintChecker.CheckResult r =
                PlanConstraintChecker.check("FINISH", p, set("c-0001"));

        assertThat(r.ok()).isFalse();
        assertThat(r.renderErrors())
                .contains("c-0001")
                .contains("MUST")
                .contains("SHOULD")
                .contains("被当成了");
    }

    @Test
    @DisplayName("等级被抬高同样拒绝（SHOULD 被当成 MUST 用）—— 只判等不等于")
    void levelUpgradeIsAlsoRejected() {
        PlanDetailVO p = plan("FINISH", use("c-0001", "APPLIED", "MUST", "当硬约束执行的"));

        PlanConstraintChecker.CheckResult r =
                PlanConstraintChecker.check("FINISH", p, setOf(Modality.SHOULD, "c-0001"));

        assertThat(r.ok()).isFalse();
        assertThat(r.renderErrors()).contains("被当成了");
    }

    @Test
    @DisplayName("usedAs 缺失或非法 -> 拒绝")
    void missingOrIllegalUsedAsIsReported() {
        PlanConstraintChecker.CheckResult blank =
                PlanConstraintChecker.check("FINISH",
                        plan("FINISH", use("c-0001", "APPLIED", "  ", "理由")), set("c-0001"));
        assertThat(blank.ok()).isFalse();
        assertThat(blank.renderErrors()).contains("usedAs 非法");

        PlanConstraintChecker.CheckResult illegal =
                PlanConstraintChecker.check("FINISH",
                        plan("FINISH", use("c-0001", "APPLIED", "URGENT", "理由")), set("c-0001"));
        assertThat(illegal.ok()).isFalse();
        assertThat(illegal.renderErrors()).contains("usedAs 非法");
    }

    @Test
    @DisplayName("CLARIFY 不判")
    void clarifyIsNotChecked() {
        PlanDetailVO p = plan("CLARIFY");

        assertThat(PlanConstraintChecker.check("CLARIFY", p, set("c-0001")).ok()).isTrue();
    }
}
