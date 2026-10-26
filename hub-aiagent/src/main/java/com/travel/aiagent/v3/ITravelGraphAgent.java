package com.travel.aiagent.v3;

import com.travel.aiagent.common.domain.SubAgentOutput;
import com.travel.aiagent.common.domain.constraint.ConstraintSet;

import java.util.function.Consumer;

/**
 * TravelSubAgent接口
 *
 * <p>注意这里<b>没有 consumes()</b>：约束是全量下发的。
 * "哪个 Agent 关心哪条约束"是消费者拓扑的表现，不该由生产者回答；
 * 等约束量上来了，再用 embedding 做**派生索引**召回，索引可以重建，真源不动。
 */
public interface ITravelGraphAgent {

    /**
     * 实现类名称
     */
    public String name();

    /**
     * 实现类的业务描述
     */
    public String description();

    /**
     * 执行方法。
     *
     * @param task        上游下发的子任务
     * @param constraints 全局约束只读快照（全量；唯一写者是 Orchestrator，本方法不得修改）
     * @param traceId     请求级链路追踪ID（透传，关联 llm_call_log 追踪）
     * @return 双通道产出：conclusion 给人看，receipt 给程序对账
     */
    public SubAgentOutput execute(String task, ConstraintSet constraints,
                                  String userId, String chatId, String traceId, Consumer<String> progress);

}
