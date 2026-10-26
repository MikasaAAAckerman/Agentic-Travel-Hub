package com.travel.aiagent.v3;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.travel.aiagent.common.constant.GraphStateKey;
import com.travel.aiagent.common.core.constraint.ConstraintAuditor;
import com.travel.aiagent.common.core.constraint.ConstraintNormalizer;
import com.travel.aiagent.common.core.constraint.ConstraintStore;
import com.travel.aiagent.common.core.planner.PlannerService;
import com.travel.aiagent.common.core.worker.WorkerService;
import com.travel.aiagent.common.domain.ConstraintUseVO;
import com.travel.aiagent.common.domain.PlanDetailVO;
import com.travel.aiagent.common.domain.WorkDetailVO;
import com.travel.aiagent.common.domain.constraint.ConstraintDraftVO;
import com.travel.aiagent.common.memory.PlannerContextVO;
import com.travel.aiagent.common.memory.RoundMemory;
import com.travel.aiagent.common.memory.ShortTermMemory;
import com.travel.aiagent.common.memory.SubAgentReActContextVO;
import com.travel.aiagent.common.metrics.AgentMetrics;
import com.travel.aiagent.common.session.AgentSessionRegistry;
import com.travel.aiagent.v3.agents.BaseTravelGraphAgent;
import com.travel.aiagent.v3.agents.FoodGraphAgent;
import com.travel.aiagent.v3.edge.OrchestratorGraphEdge;
import com.travel.aiagent.v3.edge.SubAgentGraphEdge;
import com.travel.aiagent.v3.graph.OrchestratorGraph;
import com.travel.aiagent.v3.graph.SubAgentGraph;
import com.travel.aiagent.v3.node.OrchestratorGraphNode;
import com.travel.aiagent.v3.node.SubAgentGraphNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 约束治理冒烟测试 —— 不花一分钱 token，把整条链路真跑一遍。
 *
 * <p>它验证的不是算法（那是 {@code PlanConstraintCheckerTest} / {@code ConstraintAuditorTest} 的活），
 * 而是<b>接线</b>：
 * <pre>
 *   OrchestratorGraph (真 StateGraph)
 *     └─ planner (假) → FoodAgent (真子图)
 *          └─ planner (假) → [前置校验] → worker (假) → finish (真)
 *     └─ subAgentNode → ConstraintAuditor (真) → AgentMetrics (真 SimpleMeterRegistry)
 * </pre>
 *
 * <p>关键词：<b>前置拦截</b>——回执不合格时，工具一次都不该被调用。
 */
@DisplayName("冒烟：约束治理全链路（假 Planner/Worker，真图拓扑）")
class ConstraintGovernanceSmokeTest {

    private static final String USER_ID = "u-smoke";
    private static final String CHAT_ID = "c-smoke";
    private static final String TRACE_ID = "tr-smoke";
    private static final String USER_INPUT = "我对海鲜过敏，预算5000，9月17号到23号去广州";

    private SimpleMeterRegistry registry;
    private FakePlannerService planner;
    private WorkerService worker;
    private ConstraintStore constraintStore;
    private RoundMemory roundMemory;
    private ShortTermMemory shortTermMemory;
    private AgentSessionRegistry sessionRegistry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        planner = new FakePlannerService();
        worker = mock(WorkerService.class);
        // 用真的 ConstraintStore + 真的 Normalizer：一步抽取的归一化/版本化必须被覆盖到
        constraintStore = new ConstraintStore(new ConstraintNormalizer());
        roundMemory = mock(RoundMemory.class);
        shortTermMemory = mock(ShortTermMemory.class);
        sessionRegistry = mock(AgentSessionRegistry.class);

        when(sessionRegistry.isCancelled(any())).thenReturn(false);
        when(roundMemory.getRounds(any(), any())).thenReturn(List.of());
        when(worker.doWorkWithRagParallel(any()))
                .thenReturn(new WorkDetailVO(true, "找到 3 家可避开海鲜的餐厅"));
    }

    // ═══════════════════════════════════════════════════════════
    // 用例
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("正常链路：TOOL_CALL 留空 → FINISH 全量对齐 → 对账 PASS")
    void happyPathPassesAudit() {
        planner.setSubScript(List.of(
                toolCall(),
                finish(use("c-0001", "APPLIED", "推荐的三家餐厅都标注了无海鲜"))));

        Optional<OverAllState> state = runGraph();

        assertThat(state).isPresent();
        assertThat(state.get().value(GraphStateKey.ORCHESTRATOR_AGENT_CONCLUSION.getKey(), ""))
                .isEqualTo("已为你规划好广州行程");

        // 约束真的进了子 Agent 的 System Prompt
        assertThat(planner.capturedBlock.get()).contains("c-0001").contains("用户对海鲜过敏");
        assertThat(planner.capturedRequireReceipt.get()).isTrue();

        assertThat(counter("constraint.audit.total", "result", "pass")).isEqualTo(1.0);
        assertThat(counter("constraint.verdict.total", "status", "applied")).isEqualTo(1.0);
        assertThat(counterNoTag("constraint.audit.reject.total")).isZero();
        assertThat(counter("constraint.violation.total", "type", "incomplete_receipt")).isZero();
    }

    @Test
    @DisplayName("中间轮的 constraintUse 不进账：不校验、不采纳，回执只认 FINISH 轮")
    void middleRoundVerdictsAreNotCounted() {
        // TOOL_CALL 轮带了 3 条（含编造 id），必须被无视
        planner.setSubScript(List.of(
                toolCall(
                        use("c-0001", "APPLIED", "先这么写着"),
                        use("c-0002", "APPLIED", "先这么写着"),
                        use("c-9999", "APPLIED", "随手编的")),
                finish(use("c-0001", "APPLIED", "推荐的三家餐厅都标注了无海鲜"))));

        runGraph();

        // 只有 FINISH 轮那一条进了回执
        assertThat(counter("constraint.verdict.total", "status", "applied")).isEqualTo(1.0);
        assertThat(counter("constraint.audit.total", "result", "pass")).isEqualTo(1.0);
        assertThat(counterNoTag("constraint.audit.reject.total")).isZero();
    }

    @Test
    @DisplayName("回执不合格 → 本轮不执行工具，回 planner 重来；第二轮补齐后通过")
    void missingVerdictTriggersReplanThenPasses() {
        planner.setSubScript(List.of(
                toolCall(),
                // 第一次 FINISH 漏了 c-0001 → 应被拒
                finish(),
                // 重试后补齐
                finish(use("c-0001", "APPLIED", "推荐的三家餐厅都标注了无海鲜"))));

        runGraph();

        assertThat(counterNoTag("constraint.audit.reject.total")).isEqualTo(1.0);
        assertThat(counter("constraint.verdict.total", "status", "applied")).isEqualTo(1.0);
        assertThat(counter("constraint.audit.total", "result", "pass")).isEqualTo(1.0);

        // 重试的 prompt 里必须带上被拒原因，否则温度 0 会复读
        assertThat(planner.capturedRejectHints).anyMatch(hint -> hint.contains("被程序拒绝"));
    }

    @Test
    @DisplayName("一直不合格 → 轮次耗尽走 overMaxLoopTimes，回执缺失被兜底抓住")
    void alwaysInvalidEndsAtMaxLoopTimes() {
        // 脚本只有两条，之后粘住最后一条（永远少一条裁决）
        planner.setSubScript(List.of(toolCall(), finish()));

        runGraph();

        assertThat(counterNoTag("constraint.audit.reject.total")).isGreaterThan(0.0);
        assertThat(counter("constraint.violation.total", "type", "missing_receipt")).isEqualTo(1.0);
        // 全程没有任何工具调用（每次都被前置拦住）
        assertThat(counter("constraint.verdict.total", "status", "applied")).isZero();
    }

    @Test
    @DisplayName("等级被改动 → 本轮不执行工具，回 planner 重来；改回原等级后通过")
    void levelDriftTriggersReplanThenPasses() {
        planner.setSubScript(List.of(
                toolCall(),
                // 第一次 FINISH：把 MUST 当成 SHOULD 用 → 应被拒
                finish(useAs("c-0001", "APPLIED", "SHOULD", "我按软约束处理的")),
                // 重试后等级改回 MUST
                finish(use("c-0001", "APPLIED", "推荐的三家餐厅都标注了无海鲜"))));

        runGraph();

        assertThat(counterNoTag("constraint.audit.reject.total")).isEqualTo(1.0);
        assertThat(counter("constraint.audit.total", "result", "pass")).isEqualTo(1.0);
        assertThat(planner.capturedRejectHints)
                .anyMatch(hint -> hint.contains("被当成了"));
    }

    // ═══════════════════════════════════════════════════════════
    // 图装配：真拓扑 + 假模型层
    // ═══════════════════════════════════════════════════════════

    private Optional<OverAllState> runGraph() {
        AgentMetrics metrics = new AgentMetrics(registry);

        SubAgentGraphNode subNode = new SubAgentGraphNode();
        ReflectionTestUtils.setField(subNode, "plannerService", planner);
        ReflectionTestUtils.setField(subNode, "workerService", worker);
        ReflectionTestUtils.setField(subNode, "shortTermMemory", shortTermMemory);
        ReflectionTestUtils.setField(subNode, "agentSessionRegistry", sessionRegistry);
        ReflectionTestUtils.setField(subNode, "agentMetrics", metrics);

        SubAgentGraphEdge subEdge = new SubAgentGraphEdge();
        ReflectionTestUtils.setField(subEdge, "maxLoopTimes", 5);

        SubAgentGraph subGraph = new SubAgentGraph();
        ReflectionTestUtils.setField(subGraph, "subAgentGraphNodes", subNode);
        ReflectionTestUtils.setField(subGraph, "subAgentGraphEdge", subEdge);
        ReflectionTestUtils.setField(subGraph, "maxLoopTimes", 5);

        FoodGraphAgent foodAgent = new FoodGraphAgent(subGraph);
        Map<String, BaseTravelGraphAgent> agents = Map.of("FoodAgent", foodAgent);

        OrchestratorGraphNode orchNode = new OrchestratorGraphNode();
        ReflectionTestUtils.setField(orchNode, "plannerService", planner);
        ReflectionTestUtils.setField(orchNode, "subGraphAgentsMap", agents);
        ReflectionTestUtils.setField(orchNode, "shortTermMemory", shortTermMemory);
        ReflectionTestUtils.setField(orchNode, "roundMemory", roundMemory);
        ReflectionTestUtils.setField(orchNode, "agentSessionRegistry", sessionRegistry);
        ReflectionTestUtils.setField(orchNode, "constraintStore", constraintStore);
        ReflectionTestUtils.setField(orchNode, "constraintAuditor", new ConstraintAuditor());
        ReflectionTestUtils.setField(orchNode, "agentMetrics", metrics);

        OrchestratorGraphEdge orchEdge = new OrchestratorGraphEdge();
        ReflectionTestUtils.setField(orchEdge, "subGraphAgentsMap", agents);
        ReflectionTestUtils.setField(orchEdge, "maxLoopTimes", 5);

        OrchestratorGraph orchGraph = new OrchestratorGraph();
        ReflectionTestUtils.setField(orchGraph, "subGraphAgentsMap", agents);
        ReflectionTestUtils.setField(orchGraph, "orchestratorGraphEdge", orchEdge);
        ReflectionTestUtils.setField(orchGraph, "orchestratorGraphNode", orchNode);
        ReflectionTestUtils.setField(orchGraph, "maxLoopTimes", 5);

        CompiledGraph graph = orchGraph.buildGraph();

        Map<String, Object> init = new HashMap<>();
        init.put(GraphStateKey.USER_INPUT.getKey(), USER_INPUT);
        init.put(GraphStateKey.USER_ID.getKey(), USER_ID);
        init.put(GraphStateKey.CHAT_ID.getKey(), CHAT_ID);
        init.put(GraphStateKey.LOOP_TIMES.getKey(), 0);
        init.put(GraphStateKey.TRACE_ID.getKey(), TRACE_ID);

        return graph.invoke(init);
    }

    // ═══════════════════════════════════════════════════════════
    // 测试夹具
    // ═══════════════════════════════════════════════════════════

    /** 默认按下发时的 MUST 等级表态 */
    private static ConstraintUseVO use(String id, String status, String reason) {
        return new ConstraintUseVO(id, status, "MUST", reason);
    }

    /** 指定等级（用于验证"等级被改动"会被前置拦住） */
    private static ConstraintUseVO useAs(String id, String status, String usedAs, String reason) {
        return new ConstraintUseVO(id, status, usedAs, reason);
    }

    private static PlanDetailVO toolCall(ConstraintUseVO... uses) {
        PlanDetailVO plan = new PlanDetailVO();
        plan.setAction("TOOL_CALL");
        plan.setPlanDetail("搜索可避开海鲜的餐厅");
        plan.setToolDescription("searchRestaurant");
        plan.setConstraintUse(List.of(uses));
        return plan;
    }

    private static PlanDetailVO finish(ConstraintUseVO... uses) {
        PlanDetailVO plan = new PlanDetailVO();
        plan.setAction("FINISH");
        plan.setConclusion("推荐 3 家可避开海鲜的餐厅");
        plan.setConstraintUse(List.of(uses));
        return plan;
    }

    private double counter(String name, String tagKey, String tagValue) {
        Counter c = registry.find(name).tag(tagKey, tagValue).counter();
        return c == null ? 0.0 : c.count();
    }

    private double counterNoTag(String name) {
        Counter c = registry.find(name).counter();
        return c == null ? 0.0 : c.count();
    }

    /**
     * 假 Planner：按剧本吐结构化决策，不碰模型。
     *
     * <p>子 Agent 剧本是"粘性"的：脚本用完后一直重复最后一条，
     * 这样可以模拟"模型死活改不对"直到轮次耗尽。
     */
    static class FakePlannerService extends PlannerService {

        private final AtomicInteger orchestratorCalls = new AtomicInteger();
        private final AtomicInteger subAgentCalls = new AtomicInteger();
        final AtomicReference<String> capturedBlock = new AtomicReference<>();
        final AtomicReference<Boolean> capturedRequireReceipt = new AtomicReference<>(false);
        final List<String> capturedRejectHints = new ArrayList<>();

        private List<PlanDetailVO> subScript = List.of();

        void setSubScript(List<PlanDetailVO> subScript) {
            this.subScript = subScript;
        }

        @Override
        public PlanDetailVO doOrchestratorAgentPlan(PlannerContextVO context, String systemPrompt) {
            PlanDetailVO plan = new PlanDetailVO();
            if (orchestratorCalls.getAndIncrement() == 0) {
                plan.setThought("先让美食专家查餐厅");
                plan.setAction("SUB_AGENT_CALL");
                plan.setSubAgentName("FoodAgent");
                plan.setPlanDetail("找广州能避开海鲜的餐厅");
                // 一步抽取：约束跟着这次计划一起返回
                plan.setConstraints(firstDispatchDrafts());
            } else {
                plan.setAction("FINISH");
                plan.setConclusion("已为你规划好广州行程");
            }
            return plan;
        }

        @Override
        public PlanDetailVO doSubAgentPlanReAct(SubAgentReActContextVO context, String subAgentName,
                                                String constraintBlock, boolean requireReceipt) {
            capturedBlock.set(constraintBlock);
            capturedRequireReceipt.set(requireReceipt);
            if (constraintBlock != null && constraintBlock.contains("被程序拒绝")) {
                capturedRejectHints.add(constraintBlock);
            }

            int index = subAgentCalls.getAndIncrement();
            if (subScript.isEmpty()) {
                return finish();
            }
            // 粘性：超出脚本后一直返回最后一条
            return subScript.get(Math.min(index, subScript.size() - 1));
        }
    }

    /** 一步抽取：Planner 第一次派发时"随计划一起"吐出来的约束草稿 */
    private static List<ConstraintDraftVO> firstDispatchDrafts() {
        return List.of(new ConstraintDraftVO(
                "allergy", "用户对海鲜过敏", "MUST", "USER_EXPLICIT", "对海鲜过敏"));
    }
}
