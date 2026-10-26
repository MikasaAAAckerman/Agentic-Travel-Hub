package com.travel.aiagent.common.domain.constraint;

/**
 * 子 Agent 对某条全局约束的执行裁决 —— 只有三态。
 *
 * <p>设计要点：
 * <ul>
 *   <li><b>没有"未处理"这种默认态</b>：每条约束必须显式表态，不允许沉默。</li>
 *   <li>{@link #UNUSED} 取代了原来的 NOT_APPLICABLE / UNHANDLED：
 *       它陈述的是"我这次没用它"这个<b>事实</b>，而不是"它与我无关"这个<b>判断</b>。
 *       模型做事实陈述的能力，远强于做相关性判断。</li>
 *   <li>三态都<b>必须</b>给出 reason，代码只校验非空，不校验语义。</li>
 * </ul>
 */
public enum VerdictStatus {

    /** 用了，且在 reason 里说明了依据 */
    APPLIED,
    /** 看到了，但这次没用 —— 必须说明为什么不用 */
    UNUSED,
    /** 看到了、该用，但做不到 —— 必须说明卡在哪 */
    VIOLATED;

    public static VerdictStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return UNUSED;
        }
        String c = code.trim().toUpperCase(java.util.Locale.ROOT);
        for (VerdictStatus s : values()) {
            if (s.name().equals(c)) {
                return s;
            }
        }
        return UNUSED;
    }

    /** 是否合法（校验用，不兜底） */
    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String c = code.trim().toUpperCase(java.util.Locale.ROOT);
        for (VerdictStatus s : values()) {
            if (s.name().equals(c)) {
                return true;
            }
        }
        return false;
    }
}
