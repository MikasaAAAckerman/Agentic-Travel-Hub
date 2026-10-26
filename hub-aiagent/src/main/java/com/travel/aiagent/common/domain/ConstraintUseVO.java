package com.travel.aiagent.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 子 Agent 对一条全局约束的执行裁决（模型输出侧）。
 *
 * <p>刻意用 String 承载 status / usedAs：模型对枚举的遵循度不稳定，
 * 真正的合法性判定在 {@code PlanConstraintChecker} 里由代码完成，这里只做承载。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConstraintUseVO {

    /** 必须原样复制下行约束的 id，不得自造 */
    private String constraintId;

    /** APPLIED / UNUSED / VIOLATED */
    private String status;

    /**
     * 你在这次任务里把它当成什么等级来对待：MUST / SHOULD / PREFER。
     *
     * <p>必须与下发时的等级一致。不一致就是"等级被改动了"——
     * 这是纯枚举比较，不涉及任何语义判断。
     */
    private String usedAs;

    /** 为什么是这个状态 —— 三态都必填 */
    private String reason;
}
