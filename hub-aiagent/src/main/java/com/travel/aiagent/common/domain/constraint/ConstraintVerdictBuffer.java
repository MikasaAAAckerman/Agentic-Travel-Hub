package com.travel.aiagent.common.domain.constraint;

import java.util.ArrayList;
import java.util.List;

/**
 * 裁决累加器 —— 子图内部跨轮传递用的可变持有者。
 *
 * <p><b>为什么不用裸 {@code List<Verdict>} 存 GraphState：</b>
 * 图框架在节点间传递 State 时会对<b>集合类型</b>的值做一次拷贝/转换，
 * 裸 List 读回来元素会变成 {@code LinkedHashMap}，强转直接 ClassCastException。
 * 历史代码里 {@code SubAgentReActContextVO} 能正常工作，正是因为它是个 POJO
 * 而不是 Collection —— 这里沿用同样的形态，用对象包住列表。
 *
 * <p>引用语义：同一 Map 实例在图内跨节点共享，直接 addAll 累积即可。
 */
public class ConstraintVerdictBuffer {

    private final List<Verdict> verdicts = new ArrayList<>();

    public void addAll(List<Verdict> reported) {
        if (reported != null && !reported.isEmpty()) {
            verdicts.addAll(reported);
        }
    }

    public void add(Verdict verdict) {
        if (verdict != null) {
            verdicts.add(verdict);
        }
    }

    /** 快照，避免调用方改动内部状态 */
    public List<Verdict> snapshot() {
        return List.copyOf(verdicts);
    }

    public int size() {
        return verdicts.size();
    }

    public boolean isEmpty() {
        return verdicts.isEmpty();
    }
}
