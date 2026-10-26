package com.travel.aiagent.common.core.constraint;

import com.travel.aiagent.common.domain.constraint.ConstraintSet;
import com.travel.aiagent.common.domain.constraint.EvidenceCheck;
import com.travel.aiagent.common.domain.constraint.GlobalConstraint;
import com.travel.aiagent.common.domain.constraint.Modality;
import com.travel.aiagent.common.domain.constraint.Provenance;
import com.travel.aiagent.common.domain.constraint.Receipt;
import com.travel.aiagent.common.domain.constraint.Verdict;
import com.travel.aiagent.common.domain.constraint.VerdictStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConstraintAuditor：后置对账（内容盲，纯结构）")
class ConstraintAuditorTest {

    private final ConstraintAuditor auditor = new ConstraintAuditor();

    private static GlobalConstraint gc(String id) {
        return new GlobalConstraint(id, "topic_" + id, "text-" + id, Modality.MUST,
                Provenance.USER_EXPLICIT, "USER_INPUT#span[0,1]", EvidenceCheck.PASSED);
    }

    private ConstraintSet set(GlobalConstraint... items) {
        return ConstraintSet.of("rev-1", List.of(items));
    }

    private Receipt receipt(ConstraintSet set, Verdict... verdicts) {
        return Receipt.ofVerdicts(set.revision(), set.digest(), List.of(verdicts));
    }

    private static Verdict ok(String id) {
        return new Verdict(id, VerdictStatus.UNUSED, Modality.MUST, "这次用不上");
    }

    @Test
    @DisplayName("无约束 + 空回执 = PASS")
    void emptyEverythingPasses() {
        AuditResult r = auditor.audit(ConstraintSet.empty(), Receipt.empty());
        assertThat(r.verdict()).isEqualTo(AuditVerdict.PASS);
        assertThat(r.score()).isZero();
    }

    @Test
    @DisplayName("全量对齐 + 都有理由 = PASS")
    void allCoveredPasses() {
        ConstraintSet s = set(gc("c-0001"), gc("c-0002"));

        AuditResult r = auditor.audit(s, receipt(s, ok("c-0001"), ok("c-0002")));

        assertThat(r.verdict()).isEqualTo(AuditVerdict.PASS);
    }

    @Test
    @DisplayName("回执引用了不存在的 id = 幽灵回执")
    void ghostReceiptIsFlagged() {
        ConstraintSet s = set(gc("c-0001"));

        AuditResult r = auditor.audit(s, receipt(s, ok("c-0001"), ok("c-9999")));

        assertThat(r.findings()).extracting(Finding::type).contains(FindingType.GHOST_RECEIPT);
        assertThat(r.verdict()).isEqualTo(AuditVerdict.HARD_FAIL);
    }

    @Test
    @DisplayName("同一约束被表态多次 = 重复裁决")
    void duplicateVerdictIsFlagged() {
        ConstraintSet s = set(gc("c-0001"));

        AuditResult r = auditor.audit(s, receipt(s, ok("c-0001"), ok("c-0001")));

        assertThat(r.findings()).extracting(Finding::type).contains(FindingType.DUPLICATE_VERDICT);
    }

    @Test
    @DisplayName("没有理由 = EMPTY_REASON（三态一视同仁）")
    void emptyReasonIsFlagged() {
        ConstraintSet s = set(gc("c-0001"));

        AuditResult r = auditor.audit(s,
                receipt(s, new Verdict("c-0001", VerdictStatus.APPLIED, Modality.MUST, "   ")));

        assertThat(r.findings()).extracting(Finding::type).contains(FindingType.EMPTY_REASON);
    }

    @Test
    @DisplayName("缺失某条约束 = 回执不完整")
    void incompleteReceiptIsFlagged() {
        ConstraintSet s = set(gc("c-0001"), gc("c-0002"));

        AuditResult r = auditor.audit(s, receipt(s, ok("c-0001")));

        assertThat(r.findings()).extracting(Finding::type).contains(FindingType.INCOMPLETE_RECEIPT);
        assertThat(r.verdict()).isEqualTo(AuditVerdict.HARD_FAIL);
    }

    @Test
    @DisplayName("digest 对不上 = 链路被篡改（FATAL）")
    void digestMismatchIsFatal() {
        ConstraintSet s = set(gc("c-0001"));
        Receipt tampered = new Receipt("rev-1", "deadbeefdeadbeef",
                List.of(ok("c-0001")));

        AuditResult r = auditor.audit(s, tampered);

        assertThat(r.findings()).extracting(Finding::type).contains(FindingType.DIGEST_MISMATCH);
        assertThat(r.hasFatal()).isTrue();
    }

    @Test
    @DisplayName("revision 过期 = 旧回执，直接判死")
    void staleRevisionIsFatal() {
        ConstraintSet s = ConstraintSet.of("rev-2", List.of(gc("c-0001")));
        Receipt stale = new Receipt("rev-1", s.digest(),
                List.of(ok("c-0001")));

        AuditResult r = auditor.audit(s, stale);

        assertThat(r.findings()).extracting(Finding::type).contains(FindingType.STALE_REVISION);
        assertThat(r.hasFatal()).isTrue();
    }

    @Test
    @DisplayName("有约束但完全没有回执：只报一次 MISSING_RECEIPT")
    void missingReceiptReportedOnce() {
        ConstraintSet s = ConstraintSet.of("rev-1", List.of(gc("c-0001"), gc("c-0002")));

        AuditResult r = auditor.audit(s, Receipt.empty());

        assertThat(r.findings()).extracting(Finding::type).containsExactly(FindingType.MISSING_RECEIPT);
        assertThat(r.verdict()).isEqualTo(AuditVerdict.HARD_FAIL);
    }

    @Test
    @DisplayName("1 条 FATAL 的分数压过 3 条 LOW：数量不等于好坏")
    void severityWeightingBeatsCounting() {
        ConstraintSet s = set(gc("c-0001"));
        Receipt tampered = new Receipt("rev-1", "deadbeefdeadbeef",
                List.of(ok("c-0001"), ok("c-0001")));

        AuditResult r = auditor.audit(s, tampered);

        assertThat(r.score()).isGreaterThanOrEqualTo(Severity.FATAL.getWeight());
        assertThat(r.verdict()).isEqualTo(AuditVerdict.HARD_FAIL);
    }
}
