package com.travel.aiagent.common.domain.constraint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * 下发给子 Agent 的只读快照 —— <b>全量下发，不做领域过滤</b>。
 *
 * <p>为什么不路由：路由是"按 Agent 关注范围过滤"，而"哪个 Agent 关心哪条约束"是
 * 消费者拓扑的表现，不属于真源。现在约束量很小（个位数），全量下发的代价只是几百 token，
 * 换来的是**零丢失**；等约束量上来了，再用 embedding 做**派生索引**召回，
 * 索引可以重建，真源不动。
 *
 * <p>{@code digest} 用于篡改 / 截断检测：子 Agent 原样回显，Orchestrator 比对。
 *
 * @param revision 单调递增；多轮对话里"预算改成 5000"会 bump，旧回执靠它淘汰
 */
public record ConstraintSet(
        String revision,
        String digest,
        List<GlobalConstraint> items
) {

    public static final String EMPTY_REVISION = "rev-0";

    public ConstraintSet {
        revision = (revision == null || revision.isBlank()) ? EMPTY_REVISION : revision;
        items = items == null ? List.of() : List.copyOf(items);
        digest = (digest == null || digest.isBlank()) ? computeDigest(items) : digest;
    }

    public static ConstraintSet empty() {
        return new ConstraintSet(EMPTY_REVISION, computeDigest(List.of()), List.of());
    }

    public static ConstraintSet of(String revision, List<GlobalConstraint> items) {
        List<GlobalConstraint> copy = items == null ? List.of() : List.copyOf(items);
        return new ConstraintSet(revision, computeDigest(copy), copy);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** 需要逐条回执的约束（MUST / SHOULD）；PREFER 只在 Prompt 里合并成一行 */
    public List<GlobalConstraint> hardItems() {
        return items.stream().filter(c -> c.modality() != Modality.PREFER).toList();
    }

    public List<GlobalConstraint> preferItems() {
        return items.stream().filter(c -> c.modality() == Modality.PREFER).toList();
    }

    /**
     * 内容摘要：按 id 排序后逐条序列化，保证同一集合顺序无关。
     *
     * <p>覆盖字段包括 {@code evidence} —— 否则"只改证据不改内容"的篡改检测不到。
     */
    public static String computeDigest(List<GlobalConstraint> items) {
        String canonical = items.stream()
                .sorted(Comparator.comparing(GlobalConstraint::id))
                .map(c -> String.join("|", c.id(), c.topic(),
                        c.modality().name(), c.provenance().name(), c.text(),
                        c.evidence(), c.evidenceCheck().name()))
                .reduce("", (a, b) -> a + "\n" + b);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 必备算法，理论上不可达
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
