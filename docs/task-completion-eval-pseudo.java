/**
 * 任务完成率评估 — 伪代码
 *
 * 核心思路：把用户的一次请求，拆成"期望子任务"和"实际子任务"，对比得出完成率。
 *
 * 评估流程：
 *   用户输入 → LLM提取期望子任务列表 → 对比实际执行的子任务 → 逐项打分 → 汇总
 */

// ══════════════════════════════════════════
// Step 1: 定义数据结构
// ══════════════════════════════════════════

class SubTask {
    String name;          // 子任务名，如 "路线规划"、"美食推荐"、"住宿推荐"
    String agentName;     // 负责的 Agent，如 "RouteAgent"、"FoodAgent"
    String status;        // PENDING / SUCCESS / FAILED / SKIPPED
    String conclusion;    // Agent 返回的结论
    boolean isRealData;   // 结果是否来自真实 API（非 LLM 幻觉）
}

class EvalResult {
    List<SubTask> expectedTasks;   // 期望完成的子任务
    List<SubTask> actualTasks;     // 实际完成的子任务
    double completionRate;         // 任务完成率
    double accuracyRate;           // 结果准确率
    double hallucinationRate;      // 幻觉率
    String summary;                // 评估摘要
}

// ══════════════════════════════════════════
// Step 2: 提取用户期望的子任务
// ══════════════════════════════════════════

/**
 * 用 LLM 从用户输入中提取期望得到哪些维度的服务。
 * 这一步是为了建立"评估基准线"。
 */
List<SubTask> extractExpectedTasks(String userInput) {

    String prompt = """
        用户的需求是：{userInput}
        请分析用户期望得到哪些维度的服务，输出 JSON 数组。
        每个元素包含 name 和 agentName。

        可选的 agentName：RouteAgent, FoodAgent, HotelAgent, WeatherAgent, AttractionAgent

        示例输入："帮我规划北京三天两夜，要有路线、美食和住宿"
        示例输出：
        [
          {"name": "路线规划", "agentName": "RouteAgent"},
          {"name": "美食推荐", "agentName": "FoodAgent"},
          {"name": "住宿推荐", "agentName": "HotelAgent"}
        ]
        """;

    String llmResponse = llmClient.call(prompt);
    return JSON.parseArray(llmResponse, SubTask.class);
}

// ══════════════════════════════════════════
// Step 3: 收集实际执行的子任务
// ══════════════════════════════════════════

/**
 * 从 OrchestratorGraph 的执行记录中，提取实际派发了哪些子任务。
 * 这些数据已经在 GraphState 和 ShortTermMemory 中了。
 */
List<SubTask> collectActualTasks(GraphExecutionRecord record) {

    List<SubTask> actual = new ArrayList<>();

    for (NodeExecution node : record.getNodeExecutions()) {
        if ("subAgent".equals(node.getNodeType())) {
            SubTask task = new SubTask();
            task.name = node.getPlanDetail();
            task.agentName = node.getSubAgentName();
            task.conclusion = node.getConclusion();

            // 判断状态：conclusion 为空或包含"无法获取"则标记 FAILED
            if (task.conclusion == null || task.conclusion.isBlank()
                    || task.conclusion.contains("无法获取")
                    || task.conclusion.contains("建议简化需求")) {
                task.status = "FAILED";
            } else {
                task.status = "SUCCESS";
            }

            // 判断是否真实数据：看该 Agent 的 Tool 调用链中是否命中了 mapApiClient
            task.isRealData = checkToolCallHitRealApi(node);

            actual.add(task);
        }
    }
    return actual;
}

// ══════════════════════════════════════════
// Step 4: 计算任务完成率
// ══════════════════════════════════════════

EvalResult evaluate(String userInput, GraphExecutionRecord record) {

    // 1. 提取期望 vs 实际
    List<SubTask> expected = extractExpectedTasks(userInput);
    List<SubTask> actual = collectActualTasks(record);

    // 2. 任务完成率 = 期望中被实际覆盖的比例
    int covered = 0;
    for (SubTask exp : expected) {
        // 用 agentName 匹配：期望的 Agent 是否被实际调度了
        boolean found = actual.stream()
                .anyMatch(act -> act.agentName.equals(exp.agentName)
                              && "SUCCESS".equals(act.status));
        if (found) covered++;
    }
    double completionRate = expected.isEmpty() ? 1.0 : (double) covered / expected.size();

    // 3. 结果准确率 = 实际成功的子任务中，结果来自真实 API 的比例
    long successCount = actual.stream().filter(t -> "SUCCESS".equals(t.status)).count();
    long realDataCount = actual.stream().filter(t -> t.isRealData).count();
    double accuracyRate = successCount == 0 ? 0.0 : (double) realDataCount / successCount;

    // 4. 幻觉率 = 1 - 准确率
    double hallucinationRate = 1.0 - accuracyRate;

    // 5. 汇总
    EvalResult result = new EvalResult();
    result.expectedTasks = expected;
    result.actualTasks = actual;
    result.completionRate = completionRate;
    result.accuracyRate = accuracyRate;
    result.hallucinationRate = hallucinationRate;
    result.summary = String.format(
        "期望%d项子任务，实际完成%d项 | 完成率%.0f%% | 准确率%.0f%% | 幻觉率%.0f%%",
        expected.size(), covered, completionRate * 100, accuracyRate * 100, hallucinationRate * 100
    );
    return result;
}

// ══════════════════════════════════════════
// Step 5: 判断 Tool 调用是否命中真实 API
// ══════════════════════════════════════════

/**
 * 怎么判断一个子任务的结果是"真实数据"还是"LLM 幻觉"？
 *
 * 方案A（简单）：看 Worker 执行时是否调用了 Tool
 *   - 调了 Tool → 结果来自 API → 真实
 *   - 没调 Tool → 结果来自 LLM 生成 → 可能幻觉
 *
 * 方案B（严格）：检查 Tool 调用的返回值是否被正确使用
 *   - Tool 返回了 POI 列表 → Agent 引用了这些 POI → 真实
 *   - Tool 返回了空 → Agent 仍然编造了内容 → 幻觉
 */
boolean checkToolCallHitRealApi(NodeExecution node) {
    // 简单方案：看执行链中有没有 Tool 调用记录
    return node.getToolCalls() != null && !node.getToolCalls().isEmpty();
}

// ══════════════════════════════════════════
// Step 6: 批量评估（用于回归测试）
// ══════════════════════════════════════════

/**
 * 准备一批测试用例，批量跑评估，输出报告。
 * 这就是最朴素的"模型评估集"。
 */
void batchEvaluate() {

    // 测试用例：每条包含用户输入 + 期望的子任务（人工标注）
    List<TestCase> testCases = List.of(
        new TestCase("帮我规划北京三天两夜，要有路线、美食和住宿",
                     List.of("RouteAgent", "FoodAgent", "HotelAgent")),
        new TestCase("上海有什么好吃的？",
                     List.of("FoodAgent")),
        new TestCase("从南京到杭州怎么走最快，顺便推荐沿途景点",
                     List.of("RouteAgent", "AttractionAgent")),
        new TestCase("明天北京天气怎么样，穿什么合适",
                     List.of("WeatherAgent"))
    );

    int totalCompletion = 0;
    int totalAccuracy = 0;

    for (TestCase tc : testCases) {
        // 执行一次完整的 Agent 调用
        GraphExecutionRecord record = orchestratorGraphAgent.execute(tc.input);

        // 评估
        EvalResult eval = evaluate(tc.input, record);

        System.out.println("输入: " + tc.input);
        System.out.println("结果: " + eval.summary);
        System.out.println("---");

        totalCompletion += eval.completionRate;
        totalAccuracy += eval.accuracyRate;
    }

    // 输出整体指标
    System.out.printf("整体完成率: %.0f%%%n", (double) totalCompletion / testCases.size() * 100);
    System.out.printf("整体准确率: %.0f%%%n", (double) totalAccuracy / testCases.size() * 100);
}

// ══════════════════════════════════════════
// 总结
// ══════════════════════════════════════════
//
// 整个评估体系就三件事：
//
// 1. 用户要了什么？   ← LLM 提取期望子任务
// 2. Agent 做了什么？  ← 从 Graph 执行记录收集
// 3. 做的对不对？     ← 期望 vs 实际对比 + Tool 调用链验证
//
// 不需要复杂框架，一个 evaluate() 函数 + 一批测试用例就够起步了。
// 后续可以加：LLM-as-Judge 做结果质量打分、自动回归测试等。
