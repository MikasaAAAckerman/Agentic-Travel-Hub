package com.travel.aiagent.common.core.constraint;

import com.travel.aiagent.common.domain.constraint.Modality;
import com.travel.aiagent.common.domain.constraint.Receipt;
import com.travel.aiagent.common.domain.constraint.Verdict;
import com.travel.aiagent.common.domain.constraint.VerdictStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回执跨 Graph 边界的编解码。
 *
 * <p>这组用例是给冒烟测试抓到的那个 bug 上的锁：
 * 图框架会对跨子图边界的对象做隐式序列化，{@code List<Verdict>} 的元素会退化成 Map，
 * 所以回执必须走显式 JSON。
 */
@DisplayName("Receipt：跨边界 JSON 编解码")
class ReceiptJsonTest {

    @Test
    @DisplayName("往返后裁决仍是 Verdict 类型，reason 不丢")
    void roundTripKeepsVerdictType() {
        Receipt original = Receipt.ofVerdicts("rev-1", "abcd1234abcd1234", List.of(
                new Verdict("c-0001", VerdictStatus.APPLIED, Modality.MUST, "推荐的三家餐厅都标注了无海鲜"),
                new Verdict("c-0002", VerdictStatus.UNUSED, Modality.SHOULD, "本次任务只涉及餐厅，不涉及签证")));

        Receipt restored = Receipt.parse(original.toJson());

        assertThat(restored.revision()).isEqualTo("rev-1");
        assertThat(restored.digest()).isEqualTo("abcd1234abcd1234");
        assertThat(restored.verdicts()).hasSize(2);
        assertThat(restored.verdicts()).allMatch(v -> v instanceof Verdict);
        assertThat(restored.verdicts().get(0).status()).isEqualTo(VerdictStatus.APPLIED);
        assertThat(restored.verdicts().get(0).reason()).isEqualTo("推荐的三家餐厅都标注了无海鲜");
        assertThat(restored.verdicts().get(0).usedAs()).isEqualTo(Modality.MUST);
        assertThat(restored.verdicts().get(1).status()).isEqualTo(VerdictStatus.UNUSED);
        assertThat(restored.verdicts().get(1).usedAs()).isEqualTo(Modality.SHOULD);
    }

    @Test
    @DisplayName("空回执往返仍是空回执")
    void emptyRoundTrip() {
        assertThat(Receipt.parse(Receipt.empty().toJson()).isBlank()).isTrue();
    }

    @Test
    @DisplayName("脏 JSON 不抛异常，降级成空回执交给审计器报 MISSING_RECEIPT")
    void malformedJsonDegradesToEmpty() {
        assertThat(Receipt.parse("{not json").isBlank()).isTrue();
        assertThat(Receipt.parse("").isBlank()).isTrue();
        assertThat(Receipt.parse(null).isBlank()).isTrue();
    }

    @Test
    @DisplayName("未知 status 落到 UNUSED，不会打挂整轮")
    void unknownStatusFallsBack() {
        Receipt restored = Receipt.parse(
                "{\"revision\":\"rev-1\",\"digest\":\"x\",\"verdicts\":[{\"constraintId\":\"c-1\",\"status\":\"WHATEVER\",\"reason\":\"随便\"}]}");

        assertThat(restored.verdicts().get(0).status()).isEqualTo(VerdictStatus.UNUSED);
    }
}
