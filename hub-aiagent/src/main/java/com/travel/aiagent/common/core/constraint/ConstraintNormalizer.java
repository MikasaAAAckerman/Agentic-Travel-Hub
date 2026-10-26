package com.travel.aiagent.common.core.constraint;

import com.travel.aiagent.common.domain.constraint.ConstraintDraftVO;
import com.travel.aiagent.common.domain.constraint.ConstraintSet;
import com.travel.aiagent.common.domain.constraint.EvidenceCheck;
import com.travel.aiagent.common.domain.constraint.GlobalConstraint;
import com.travel.aiagent.common.domain.constraint.Modality;
import com.travel.aiagent.common.domain.constraint.Provenance;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 约束归一化器 —— 把模型抽取的草稿变成可信的全局约束。
 *
 * <p>这里是"模型无权绕过"的闸门，全部由代码执行：
 * <ol>
 *   <li>证据反查：用 {@code quote} 在用户原文里 indexOf 出真实 span，查不到就不算有证据</li>
 *   <li>来源降级：自称 USER_EXPLICIT 却拿不出 span 的，自动降为 USER_IMPLICIT</li>
 *   <li>模态降级：证据核不上 / 模型推断的，一律不许是 MUST</li>
 *   <li>同 topic 去重：同一件事只保留最强的一条</li>
 * </ol>
 *
 * <p><b>注意这里没有任何"领域分类"</b>：{@code topic} 是开放字符串，只做格式清洗，
 * 不做语义校验。约束是什么内容（预算 / 过敏 / 天气），归一化器一概不关心。
 *
 * <p>纯函数，无 IO，可单测。
 */
@Component
public class ConstraintNormalizer {

    private static final int MAX_TEXT = 200;
    private static final int MAX_TOPIC = 40;

    /**
     * @param drafts    模型抽取草稿（可为空）
     * @param userInput 用户原始输入，用于 span 反查
     * @param previous  上一版约束集（多轮对话用；为空表示首轮）
     */
    public ConstraintSet normalize(List<ConstraintDraftVO> drafts, String userInput, ConstraintSet previous) {
        // 抽取失败 / 未抽到：保留上一版，绝不清空真源（一次抖动不该毁掉已有约束）
        if (drafts == null || drafts.isEmpty()) {
            return previous == null ? ConstraintSet.empty() : previous;
        }
        String input = userInput == null ? "" : userInput;
        Map<String, GlobalConstraint> prevByTopic = new HashMap<>();
        if (previous != null) {
            previous.items().forEach(c -> prevByTopic.put(c.topic(), c));
        }

        Map<String, Candidate> best = new LinkedHashMap<>();
        for (ConstraintDraftVO d : drafts) {
            if (d == null || isBlankDraft(d)) {
                continue;
            }
            Candidate cand = toCandidate(d, input, prevByTopic);
            Candidate exist = best.get(cand.topic());
            if (exist == null || cand.strongerThan(exist)) {
                best.put(cand.topic(), cand);
            }
        }
        if (best.isEmpty()) {
            return previous == null ? ConstraintSet.empty() : previous;
        }

        // 排序保证同一输入产出同一 digest（顺序无关）
        List<Candidate> sorted = best.values().stream()
                .sorted(Comparator.comparing(Candidate::topic))
                .toList();

        int nextId = maxId(previous) + 1;
        List<GlobalConstraint> items = new ArrayList<>(sorted.size());
        for (Candidate c : sorted) {
            GlobalConstraint old = prevByTopic.get(c.topic());
            // 同一 topic 复用老 id：跨 revision 保持稳定，回执才对得上
            String id = old != null ? old.id() : "c-" + String.format("%04d", nextId++);
            items.add(new GlobalConstraint(id, c.topic(), c.text(),
                    c.modality(), c.provenance(), c.evidence(), c.evidenceCheck()));
        }

        String revision = sameContent(previous, items) ? previous.revision() : nextRevision(previous);
        return ConstraintSet.of(revision, items);
    }

    // ═══════════════════════════════════════════════════════════
    // 单条转换
    // ═══════════════════════════════════════════════════════════

    private Candidate toCandidate(ConstraintDraftVO d,
                                  String userInput,
                                  Map<String, GlobalConstraint> prevByTopic) {

        String topic = normalizeTopic(d.getTopic());
        String text = trimTo(d.getText(), MAX_TEXT);
        if (text.isBlank()) {
            text = topic;
        }
        Modality modality = Modality.fromCode(d.getModality());
        Provenance provenance = Provenance.fromCode(d.getProvenance());

        // ── 证据反查 ──
        String evidence = "";
        EvidenceCheck check;
        String quote = d.getQuote();
        int idx = (quote == null || quote.isBlank()) ? -1 : userInput.indexOf(quote);
        if (idx >= 0) {
            evidence = "USER_INPUT#span[" + idx + "," + (idx + quote.length()) + "]";
            check = EvidenceCheck.PASSED;
        } else if (provenance == Provenance.MODEL_DERIVED) {
            // 推断类约束天然无原文依据，不算校验失败
            check = EvidenceCheck.NOT_APPLICABLE;
        } else {
            GlobalConstraint old = prevByTopic.get(topic);
            if (old != null && old.hasEvidence()) {
                // 多轮延续的约束：沿用上一版已核验过的证据指针
                evidence = old.evidence();
                check = EvidenceCheck.PASSED;
            } else {
                check = EvidenceCheck.FAILED;
            }
        }

        // ── 降级规则（模型无权绕过）──
        if (provenance == Provenance.USER_EXPLICIT && check != EvidenceCheck.PASSED) {
            provenance = Provenance.USER_IMPLICIT;   // 拿不出 span 就不许自称原话
        }
        if (modality == Modality.MUST && check == EvidenceCheck.FAILED) {
            modality = modality.weaken();            // 证据核不上，就不许当硬约束
        }
        if (modality == Modality.MUST && !provenance.canPromoteToMust()) {
            modality = modality.weaken();            // 模型推断不许升硬约束
        }

        return new Candidate(topic, text, modality, provenance, evidence, check);
    }

    // ═══════════════════════════════════════════════════════════
    // 工具方法
    // ═══════════════════════════════════════════════════════════

    /**
     * topic 归一：小写、空白转下划线、去掉会破坏 digest 的分隔符。
     * 保留非 ASCII 字符（中文 topic 可读且稳定），只做必要清洗 —— <b>不做语义校验</b>。
     */
    static String normalizeTopic(String raw) {
        if (raw == null || raw.isBlank()) {
            return "general";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "_")
                .replace('|', '_')
                .replace('\n', '_')
                .replace('\r', '_');
        if (s.length() > MAX_TOPIC) {
            s = s.substring(0, MAX_TOPIC);
        }
        return s.isBlank() ? "general" : s;
    }

    private static String trimTo(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static boolean isBlankDraft(ConstraintDraftVO d) {
        return isBlank(d.getTopic()) && isBlank(d.getText()) && isBlank(d.getQuote());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 内容是否与上一版完全一致（忽略 id 变化），一致则 revision 不 bump */
    private static boolean sameContent(ConstraintSet previous, List<GlobalConstraint> items) {
        if (previous == null || previous.items().size() != items.size()) {
            return false;
        }
        Map<String, GlobalConstraint> old = new HashMap<>();
        previous.items().forEach(c -> old.put(c.topic(), c));
        for (GlobalConstraint c : items) {
            GlobalConstraint o = old.get(c.topic());
            if (o == null
                    || o.modality() != c.modality()
                    || o.provenance() != c.provenance()
                    || !o.text().equals(c.text())
                    || o.evidenceCheck() != c.evidenceCheck()
                    || !o.evidence().equals(c.evidence())) {
                return false;
            }
        }
        return true;
    }

    private static String nextRevision(ConstraintSet previous) {
        if (previous == null || previous.isEmpty() || ConstraintSet.EMPTY_REVISION.equals(previous.revision())) {
            return "rev-1";
        }
        String rev = previous.revision();
        try {
            int n = Integer.parseInt(rev.substring(rev.indexOf('-') + 1));
            return "rev-" + (n + 1);
        } catch (RuntimeException e) {
            return "rev-1";
        }
    }

    private static int maxId(ConstraintSet previous) {
        if (previous == null) {
            return 0;
        }
        int max = 0;
        for (GlobalConstraint c : previous.items()) {
            String id = c.id();
            if (id != null && id.startsWith("c-")) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(2)));
                } catch (NumberFormatException ignored) {
                    // 忽略脏 id
                }
            }
        }
        return max;
    }

    /** 单条草稿归一后的中间态（尚无 id） */
    private record Candidate(
            String topic,
            String text,
            Modality modality,
            Provenance provenance,
            String evidence,
            EvidenceCheck evidenceCheck
    ) {
        /** 同 topic 冲突时的择优：模态 > 来源 > 证据 */
        boolean strongerThan(Candidate o) {
            if (modality.getWeight() != o.modality.getWeight()) {
                return modality.getWeight() > o.modality.getWeight();
            }
            if (provenance.getRank() != o.provenance.getRank()) {
                return provenance.getRank() > o.provenance.getRank();
            }
            boolean mine = evidenceCheck == EvidenceCheck.PASSED;
            boolean theirs = o.evidenceCheck == EvidenceCheck.PASSED;
            return mine && !theirs;
        }
    }
}
