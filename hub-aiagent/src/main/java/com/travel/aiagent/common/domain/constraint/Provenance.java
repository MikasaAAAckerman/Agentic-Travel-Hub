package com.travel.aiagent.common.domain.constraint;

import java.util.Locale;

/**
 * 约束来源等级 —— 决定它能有多"硬"。
 *
 * <p>真正需要治理的不是"用户这句话可不可信"，而是
 * "从这句话到这条结构化约束，模型有没有放大"。
 */
public enum Provenance {

    /** 用户原话，有 span 可回指，可升 MUST */
    USER_EXPLICIT(3, "用户原话（有原文依据）"),
    /** 由原话 + 确定性规则推出（如"我和老婆" -> 2 人），可升 MUST */
    USER_IMPLICIT(2, "由用户原话推出"),
    /** 历史轮次已确认的约束 */
    MEMORY(2, "历史轮次已确认"),
    /** 模型推断，永不允许升 MUST */
    MODEL_DERIVED(1, "模型推断（无原文依据）");

    private final int rank;
    private final String description;

    Provenance(int rank, String description) {
        this.rank = rank;
        this.description = description;
    }

    public int getRank() {
        return rank;
    }

    public String getDescription() {
        return description;
    }

    /** 只有拿到原文依据的约束才允许成为 MUST */
    public boolean canPromoteToMust() {
        return rank >= USER_IMPLICIT.rank;
    }

    public Provenance downgrade() {
        return this == USER_EXPLICIT ? USER_IMPLICIT : MODEL_DERIVED;
    }

    public static Provenance fromCode(String code) {
        if (code == null || code.isBlank()) {
            return MODEL_DERIVED;
        }
        String c = code.trim().toUpperCase(Locale.ROOT);
        for (Provenance p : values()) {
            if (p.name().equals(c)) {
                return p;
            }
        }
        return MODEL_DERIVED;
    }
}
