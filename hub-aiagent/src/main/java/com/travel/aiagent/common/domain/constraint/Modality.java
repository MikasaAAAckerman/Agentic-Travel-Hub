package com.travel.aiagent.common.domain.constraint;

import java.util.Locale;

/**
 * 约束模态：必须满足 / 应该满足 / 仅是偏好。
 *
 * <p>用户说"尽量别走太多路"和"绝对不能走楼梯"是两种约束，混在一起就无法定阈值。
 * 模态必须显式存在，并且由输入文本里的模态词决定，不允许模型自己升档。
 */
public enum Modality {

    /** 必须满足，违反即阻塞 */
    MUST(3, true),
    /** 应该满足，违反记录但不阻塞 */
    SHOULD(2, false),
    /** 偏好，仅参与排序 */
    PREFER(1, false);

    private final int weight;
    private final boolean blocking;

    Modality(int weight, boolean blocking) {
        this.weight = weight;
        this.blocking = blocking;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isBlocking() {
        return blocking;
    }

    /** 降级：只允许往弱的方向走（MUST -> SHOULD -> PREFER），归一化阶段用它兜住"放大器" */
    public Modality weaken() {
        return switch (this) {
            case MUST -> SHOULD;
            case SHOULD -> PREFER;
            case PREFER -> PREFER;
        };
    }

    public static Modality fromCode(String code) {
        if (code == null || code.isBlank()) {
            return SHOULD;
        }
        String c = code.trim().toUpperCase(Locale.ROOT);
        for (Modality m : values()) {
            if (m.name().equals(c)) {
                return m;
            }
        }
        return SHOULD;
    }

    /** 是否合法（校验用，不兜底） */
    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String c = code.trim().toUpperCase(Locale.ROOT);
        for (Modality m : values()) {
            if (m.name().equals(c)) {
                return true;
            }
        }
        return false;
    }
}
