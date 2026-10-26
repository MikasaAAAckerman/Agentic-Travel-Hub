package com.travel.aiagent.common.domain.constraint;

import java.util.Objects;

/**
 * 一条全局约束 —— 真源的唯一载体。
 *
 * <p><b>它是不透明载荷（opaque payload）。</b> 约束的"内容是什么"（预算、过敏、天气、路线……）
 * 对审计完全不可见；审计只关心一件事：<b>子 Agent 在引用它的时候有没有出错</b>。
 *
 * <p>因此这里<b>没有 type / appliesTo / dimension</b> —— 任何"这条约束属于哪个领域、
 * 该给哪个 Agent"的信息，都是消费者的关注点，不属于真源。
 * 真源不记录"谁消费我"。
 *
 * <p>不可变：任何"修改"都必须产生新对象 + 新 revision。
 *
 * @param id            程序生成（c-0001），模型不得自造 —— 全链路对账全靠它
 * @param topic         开放字符串，模型自由命名，只用于"同一件事"归并，不校验语义
 * @param text          人读描述，<b>不参与对账</b>
 * @param evidence      证据指针，如 USER_INPUT#span[12,28]；无证据时为空串
 * @param evidenceCheck 证据是否已由程序核对通过
 */
public record GlobalConstraint(
        String id,
        String topic,
        String text,
        Modality modality,
        Provenance provenance,
        String evidence,
        EvidenceCheck evidenceCheck
) {

    public GlobalConstraint {
        Objects.requireNonNull(id, "id 不能为空");
        topic = (topic == null || topic.isBlank()) ? "general" : topic;
        text = text == null ? "" : text;
        modality = modality == null ? Modality.SHOULD : modality;
        provenance = provenance == null ? Provenance.MODEL_DERIVED : provenance;
        evidence = evidence == null ? "" : evidence;
        evidenceCheck = evidenceCheck == null ? EvidenceCheck.NOT_APPLICABLE : evidenceCheck;
    }

    public boolean isHard() {
        return modality == Modality.MUST;
    }

    /** 是否携带可校验的证据指针 */
    public boolean hasEvidence() {
        return evidenceCheck == EvidenceCheck.PASSED && !evidence.isBlank();
    }

    @Override
    public String toString() {
        return "[" + modality + "/" + provenance + "] " + topic + "=" + text;
    }
}
