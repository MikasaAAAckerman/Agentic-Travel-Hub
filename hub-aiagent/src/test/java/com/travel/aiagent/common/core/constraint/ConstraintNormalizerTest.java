package com.travel.aiagent.common.core.constraint;

import com.travel.aiagent.common.domain.constraint.ConstraintDraftVO;
import com.travel.aiagent.common.domain.constraint.ConstraintSet;
import com.travel.aiagent.common.domain.constraint.EvidenceCheck;
import com.travel.aiagent.common.domain.constraint.GlobalConstraint;
import com.travel.aiagent.common.domain.constraint.Modality;
import com.travel.aiagent.common.domain.constraint.Provenance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConstraintNormalizer：模型无权绕过的归一化闸门")
class ConstraintNormalizerTest {

    private static final String INPUT = "我对海鲜过敏，预算5000元，9月17号出发";

    private final ConstraintNormalizer normalizer = new ConstraintNormalizer();

    private static ConstraintDraftVO draft(String topic, String text,
                                           String modality, String provenance, String quote) {
        return new ConstraintDraftVO(topic, text, modality, provenance, quote);
    }

    private static ConstraintDraftVO allergyDraft() {
        return draft("allergy", "用户对海鲜过敏", "MUST", "USER_EXPLICIT", "对海鲜过敏");
    }

    private static GlobalConstraint find(ConstraintSet set, String topic) {
        return set.items().stream()
                .filter(c -> c.topic().equals(topic))
                .findFirst()
                .orElseThrow(() -> new AssertionError("找不到约束 " + topic));
    }

    // ── 证据反查 ────────────────────────────────────────────────

    @Test
    @DisplayName("逐字摘录能在原文找到 -> 生成真实 span 指针，证据通过")
    void quoteFoundProducesSpan() {
        ConstraintSet set = normalizer.normalize(List.of(allergyDraft()), INPUT, null);

        GlobalConstraint c = find(set, "allergy");
        assertThat(c.provenance()).isEqualTo(Provenance.USER_EXPLICIT);
        assertThat(c.evidenceCheck()).isEqualTo(EvidenceCheck.PASSED);
        // "对海鲜过敏" 从下标 1 开始，长度 5
        assertThat(c.evidence()).isEqualTo("USER_INPUT#span[1,6]");
        assertThat(c.modality()).isEqualTo(Modality.MUST);
    }

    @Test
    @DisplayName("自称用户原话但原文找不到 -> 强制降为 USER_IMPLICIT 且不许当硬约束")
    void fabricatedQuoteDowngradesProvenance() {
        ConstraintDraftVO fake = draft("allergy", "用户对花生过敏", "MUST",
                "USER_EXPLICIT", "对花生过敏");

        ConstraintSet set = normalizer.normalize(List.of(fake), INPUT, null);

        GlobalConstraint c = find(set, "allergy");
        assertThat(c.provenance()).isEqualTo(Provenance.USER_IMPLICIT);
        assertThat(c.evidenceCheck()).isEqualTo(EvidenceCheck.FAILED);
        assertThat(c.evidence()).isEmpty();
        assertThat(c.modality()).isEqualTo(Modality.SHOULD);
    }

    // ── 开放主题：不做领域校验 ──────────────────────────────────

    @Test
    @DisplayName("topic 是开放字符串：中文主题也照收，不做任何领域分类校验")
    void chineseTopicKeptAsIs() {
        ConstraintDraftVO d = draft("饮食禁忌", "用户对海鲜过敏", "MUST", "USER_EXPLICIT", "对海鲜过敏");

        ConstraintSet set = normalizer.normalize(List.of(d), INPUT, null);

        assertThat(set.items()).hasSize(1);
        assertThat(set.items().get(0).topic()).isEqualTo("饮食禁忌");
    }

    @Test
    @DisplayName("未知领域主题不会被归类或丢弃 —— 这正是内容盲的前提")
    void unknownDomainNotDropped() {
        ConstraintDraftVO d = draft("颈椎不适", "用户落枕，需要低枕", "SHOULD", "USER_EXPLICIT", "落枕");

        ConstraintSet set = normalizer.normalize(List.of(d), INPUT, null);

        assertThat(set.items()).hasSize(1);
        assertThat(set.items().get(0).topic()).isEqualTo("颈椎不适");
        // 原文里没有"落枕"，证据核不上 -> 降级
        assertThat(set.items().get(0).evidenceCheck()).isEqualTo(EvidenceCheck.FAILED);
    }

    // ── 模态降级 ────────────────────────────────────────────────

    @Test
    @DisplayName("模型推断 + MUST -> 自动降为 SHOULD")
    void modelDerivedCannotBeMust() {
        ConstraintDraftVO derived = draft("total", "预算大概 5000", "MUST", "MODEL_DERIVED", "");

        ConstraintSet set = normalizer.normalize(List.of(derived), INPUT, null);

        GlobalConstraint c = find(set, "total");
        assertThat(c.modality()).isEqualTo(Modality.SHOULD);
        assertThat(c.provenance()).isEqualTo(Provenance.MODEL_DERIVED);
    }

    // ── 同主题去重 ──────────────────────────────────────────────

    @Test
    @DisplayName("同一 topic 出现多次：保留最强的一条")
    void duplicateTopicKeepsStrongest() {
        ConstraintDraftVO weak = draft("allergy", "可能不爱吃海鲜", "PREFER", "MODEL_DERIVED", "");
        ConstraintDraftVO strong = allergyDraft();

        ConstraintSet set = normalizer.normalize(List.of(weak, strong), INPUT, null);

        assertThat(set.items()).hasSize(1);
        assertThat(find(set, "allergy").modality()).isEqualTo(Modality.MUST);
    }

    @Test
    @DisplayName("不同 topic 即使内容相近也各留一条 —— 宁可重复，不可误合并")
    void differentTopicsNotMerged() {
        ConstraintSet set = normalizer.normalize(List.of(allergyDraft(),
                draft("diet_need", "注意饮食结构", "SHOULD", "USER_EXPLICIT", "对海鲜过敏")), INPUT, null);

        assertThat(set.items()).hasSize(2);
    }

    // ── revision ───────────────────────────────────────────────

    @Test
    @DisplayName("首次 rev-1；内容不变不 bump；内容变了 bump")
    void revisionBumpsOnlyWhenContentChanges() {
        ConstraintSet v1 = normalizer.normalize(List.of(allergyDraft()), INPUT, null);
        assertThat(v1.revision()).isEqualTo("rev-1");

        ConstraintSet v2 = normalizer.normalize(List.of(allergyDraft()), INPUT, v1);
        assertThat(v2.revision()).isEqualTo("rev-1");
        assertThat(v2.digest()).isEqualTo(v1.digest());

        ConstraintDraftVO changed = draft("allergy", "用户对海鲜严重过敏", "MUST",
                "USER_EXPLICIT", "对海鲜过敏");
        ConstraintSet v3 = normalizer.normalize(List.of(changed), INPUT, v2);
        assertThat(v3.revision()).isEqualTo("rev-2");
        assertThat(v3.digest()).isNotEqualTo(v1.digest());
    }

    @Test
    @DisplayName("复用已有 id：新增约束不会打乱老约束的编号")
    void keepsExistingIds() {
        ConstraintSet v1 = normalizer.normalize(List.of(allergyDraft()), INPUT, null);
        String firstId = find(v1, "allergy").id();

        ConstraintDraftVO budget = draft("total", "预算5000元", "MUST", "USER_EXPLICIT", "预算5000元");
        ConstraintSet v2 = normalizer.normalize(List.of(allergyDraft(), budget), INPUT, v1);

        assertThat(find(v2, "allergy").id()).isEqualTo(firstId);
        assertThat(find(v2, "total").id()).isNotEqualTo(firstId);
        assertThat(v2.revision()).isEqualTo("rev-2");
    }

    // ── 容错 ───────────────────────────────────────────────────

    @Test
    @DisplayName("抽取为空时保留上一版，绝不清空真源")
    void emptyDraftsKeepPrevious() {
        ConstraintSet v1 = normalizer.normalize(List.of(allergyDraft()), INPUT, null);

        ConstraintSet v2 = normalizer.normalize(List.of(), INPUT, v1);

        assertThat(v2).isEqualTo(v1);
        assertThat(v2.items()).hasSize(1);
    }

    @Test
    @DisplayName("全空草稿被丢弃，不产生脏约束")
    void blankDraftIsDropped() {
        ConstraintSet set = normalizer.normalize(List.of(draft(null, null, null, null, null)), INPUT, null);
        assertThat(set).isEqualTo(ConstraintSet.empty());
    }
}
