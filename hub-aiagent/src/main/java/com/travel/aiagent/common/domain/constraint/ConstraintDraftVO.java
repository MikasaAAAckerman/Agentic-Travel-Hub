package com.travel.aiagent.common.domain.constraint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 约束草稿 —— Planner 一次调用里随计划一起吐出来的原始约束（尚未归一化）。
 *
 * <p><b>内容是开放的</b>：{@code topic} 由模型自由命名，代码不校验语义，只做必要清洗。
 * 这样加任何新领域（医疗、金融、天气）都不需要改这个结构。
 *
 * <p>刻意让模型给 {@code quote}（原文逐字摘录）而不是 span 下标：
 * 模型给下标必错，给摘录则可以用 {@code String.indexOf} 反查出真实区间。
 *
 * <p>它只是"草稿"：能不能升 MUST、证据是否成立、要不要去重，
 * 全部由 {@code ConstraintNormalizer} 用代码裁定，模型无权绕过。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConstraintDraftVO {

    /** 主题：开放短词，同一件事必须复用同一个 topic，如 allergy / total / date_range */
    private String topic;

    /** 一句话约束，人读 */
    private String text;

    /** MUST / SHOULD / PREFER */
    private String modality;

    /** USER_EXPLICIT / USER_IMPLICIT / MEMORY / MODEL_DERIVED */
    private String provenance;

    /** 原文逐字摘录，用于程序反查区间；推断类约束留空 */
    private String quote;
}
