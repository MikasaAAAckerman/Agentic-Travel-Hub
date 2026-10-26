package com.travel.aiagent.common.core.constraint;

/**
 * 对账结论。
 *
 * <ul>
 *   <li>{@link #PASS}：全部通过，可进入下一轮规划</li>
 *   <li>{@link #SOFT_FAIL}：只记录不阻断（如没有给理由这类中等问题）</li>
 *   <li>{@link #HARD_FAIL}：必须修复；前置拦截 + 重试已经处理过，这里是兜底告警</li>
 * </ul>
 */
public enum AuditVerdict {
    PASS, SOFT_FAIL, HARD_FAIL;

    public boolean blocking() {
        return this == HARD_FAIL;
    }
}
