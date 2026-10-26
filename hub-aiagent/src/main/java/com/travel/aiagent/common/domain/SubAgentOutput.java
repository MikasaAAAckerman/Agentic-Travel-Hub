package com.travel.aiagent.common.domain;

import com.travel.aiagent.common.domain.constraint.Receipt;

/**
 * 子 Agent 的一次执行产出 —— 双通道。
 *
 * <p>{@code conclusion} 给人看、给 Orchestrator 组织最终答案；
 * {@code receipt} 给程序对账。
 *
 * <p>不再携带"证据快照"：工具调用结果被视为客观事实，不需要二次验证，
 * 因此约束回执里也不再有工具级的证据指针。
 */
public record SubAgentOutput(
        String conclusion,
        Receipt receipt
) {

    public SubAgentOutput {
        conclusion = conclusion == null ? "" : conclusion;
        receipt = receipt == null ? Receipt.empty() : receipt;
    }

    public static SubAgentOutput of(String conclusion) {
        return new SubAgentOutput(conclusion, Receipt.empty());
    }
}
