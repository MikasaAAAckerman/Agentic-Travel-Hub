package com.travel.aiagent.common.core.constraint;

import com.travel.aiagent.common.domain.constraint.ConstraintSet;
import com.travel.aiagent.common.domain.constraint.EvidenceCheck;
import com.travel.aiagent.common.domain.constraint.GlobalConstraint;
import com.travel.aiagent.common.domain.constraint.Modality;
import com.travel.aiagent.common.domain.constraint.Provenance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConstraintSet：digest 与强弱分组")
class ConstraintSetTest {

    private static GlobalConstraint gc(String id, String topic, String evidence) {
        return new GlobalConstraint(id, topic, "text-" + id, Modality.MUST,
                Provenance.USER_EXPLICIT, evidence, EvidenceCheck.PASSED);
    }

    @Test
    @DisplayName("digest 与顺序无关：同一集合不同排列得到同一摘要")
    void digestIsOrderIndependent() {
        GlobalConstraint a = gc("c-0001", "allergy", "USER_INPUT#span[0,1]");
        GlobalConstraint b = gc("c-0002", "total", "USER_INPUT#span[2,3]");

        String d1 = ConstraintSet.of("rev-1", List.of(a, b)).digest();
        String d2 = ConstraintSet.of("rev-1", List.of(b, a)).digest();

        assertThat(d1).isEqualTo(d2).hasSize(16);
    }

    @Test
    @DisplayName("内容变了 digest 必须变，否则篡改检测失效")
    void digestChangesWithContent() {
        GlobalConstraint a = gc("c-0001", "allergy", "USER_INPUT#span[0,1]");
        GlobalConstraint tampered = new GlobalConstraint("c-0001", "allergy",
                "被改过的文本", Modality.MUST, Provenance.USER_EXPLICIT,
                "USER_INPUT#span[0,1]", EvidenceCheck.PASSED);

        assertThat(ConstraintSet.of("rev-1", List.of(a)).digest())
                .isNotEqualTo(ConstraintSet.of("rev-1", List.of(tampered)).digest());
    }

    @Test
    @DisplayName("只改证据也必须让 digest 变 —— 否则'改证据不改内容'的篡改检测不到")
    void digestCoversEvidence() {
        GlobalConstraint a = gc("c-0001", "allergy", "USER_INPUT#span[0,1]");
        GlobalConstraint sameTextOtherEvidence = gc("c-0001", "allergy", "USER_INPUT#span[0,9]");

        assertThat(ConstraintSet.of("rev-1", List.of(a)).digest())
                .isNotEqualTo(ConstraintSet.of("rev-1", List.of(sameTextOtherEvidence)).digest());
    }

    @Test
    @DisplayName("PREFER 不要求逐条回执，单独分组")
    void preferSeparatedFromHard() {
        GlobalConstraint must = gc("c-0001", "allergy", "USER_INPUT#span[0,1]");
        GlobalConstraint prefer = new GlobalConstraint("c-0002", "vibe", "想要安静", Modality.PREFER,
                Provenance.USER_EXPLICIT, "USER_INPUT#span[9,10]", EvidenceCheck.PASSED);

        ConstraintSet set = ConstraintSet.of("rev-1", List.of(must, prefer));

        assertThat(set.hardItems()).hasSize(1);
        assertThat(set.preferItems()).hasSize(1);
    }

    @Test
    @DisplayName("empty 集合是只读且可用")
    void emptySet() {
        ConstraintSet empty = ConstraintSet.empty();
        assertThat(empty.isEmpty()).isTrue();
        assertThat(empty.revision()).isEqualTo(ConstraintSet.EMPTY_REVISION);
        assertThat(empty.hardItems()).isEmpty();
    }
}
