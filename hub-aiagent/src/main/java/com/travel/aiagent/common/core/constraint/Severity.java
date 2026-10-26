package com.travel.aiagent.common.core.constraint;

/**
 * 严重度。用加权分而不是计数：
 * 3 条 LOW 遗漏也抵不上 1 条 FATAL 伪造，"错误最少"不等于"错误最好"。
 */
public enum Severity {

    /** 直接判死：链路被篡改 / 证据伪造 / 过期回执 */
    FATAL(100),
    /** 必须修复：MUST 遗漏、幽灵回执 */
    HIGH(10),
    /** 记录并降级：假满足 */
    MEDIUM(3),
    /** 只记指标：SHOULD 遗漏 */
    LOW(1);

    private final int weight;

    Severity(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
