# Agentic-Travel-Hub 监控体系设计笔记

---

## 一、设计背景

### 1.1 项目架构

Agentic-Travel-Hub 是一个多 Agent 协作的旅行规划系统，采用 Orchestrator → SubAgent 两层调度架构：

```
用户输入
  │
  ▼
Orchestrator（总调度师）
  │
  ├─→ RouteAgent（交通专家）    ─┐
  ├─→ WeatherAgent（天气专家）   │
  ├─→ HotelAgent（酒店专家）     ├─ 每个 SubAgent 内部：Planner → Worker → FINISH/CLARIFY
  ├─→ EntertainmentAgent（娱乐） │
  └─→ FoodAgent（美食专家）     ─┘
  │
  ▼
最终旅行方案
```

### 1.2 测试中发现的问题

在多轮测试中，系统暴露出三类典型问题：

#### 问题一：SubAgent 循环调用

```
Orchestrator Round 1  → RouteAgent（查询航班）→ 返回"无航班数据"
Orchestrator Round 2  → RouteAgent（查询航班）→ 返回"无航班数据"
Orchestrator Round 3  → RouteAgent（查询航班）→ 返回"无航班数据"
...（重复 8 次）
Orchestrator Round 9  → 触及 maxLoopTimes 限制，强制结束
```

**根因**：Orchestrator 无法识别"任务已失败"的信号，反复派发相同任务。

#### 问题二：CLARIFY 泛滥

```
Orchestrator → HotelAgent: "查询北京酒店"
HotelAgent → CLARIFY: "请问您的出发地是哪里？"（信息已在用户输入中）
Orchestrator → HotelAgent: "查询北京酒店"（重复派发）
HotelAgent → CLARIFY: "请问您的预算是多少？"（继续追问）
...（CLARIFY 9 次）
```

**根因**：Orchestrator 派发任务时未传递完整的用户上下文，SubAgent 反复追问已有信息。

#### 问题三：SubAgent 空转

```
Orchestrator → EntertainmentAgent: "查询北京景点"
EntertainmentAgent → FINISH: "子任务结束"（无任何有效结论）
Orchestrator → EntertainmentAgent: "查询北京景点"（再次派发）
...（空转 3 次）
```

**根因**：SubAgent 认为任务无法完成时直接 FINISH，但 Orchestrator 不理解空结论的含义。

### 1.3 排查的困难

上述问题在日志中的表现非常隐蔽：

- 日志量巨大：一次请求产生 2000+ 行日志
- 信息分散：同一请求的日志被其他请求的日志穿插
- 缺少上下文：日志中没有"这是第几轮"、"是否重试"等关键信息
- 无法量化：无法直观看到"RouteAgent 被调用了几次"

**结论**：需要一套系统化的监控体系，让问题从"肉眼扫半小时日志"变成"1 分钟定位"。

---

## 二、设计思路

### 2.1 核心原则：层层递进

监控体系采用四层渐进式设计，每一层都建立在前一层的基础上：

```
Layer 4: 告警 & 看板    ← 运维可视化，自动发现问题
         ↑ 依赖
Layer 3: 链路追踪       ← 可视化调用树，看清请求全貌
         ↑ 依赖
Layer 2: 指标埋点       ← 数量化统计，发现趋势和异常
         ↑ 依赖
Layer 1: 结构化日志     ← 一切的基础，让日志可被程序解析
```

### 2.2 为什么要层层递进

| 层次 | 解决的问题 | 不做会怎样 |
|------|-----------|-----------|
| Layer 1 | 日志不可解析 | 后续所有监控都无法从日志中提取信息 |
| Layer 2 | 无法量化问题 | 只知道"有问题"，不知道"有多严重" |
| Layer 3 | 无法看清调用链 | 排查问题仍然需要人工拼凑日志 |
| Layer 4 | 无法自动发现 | 依赖人工巡检，问题发现滞后 |

**关键洞察**：每一层都是下一层的前置条件。

- 没有结构化日志 → 指标埋点无法从日志中提取数据
- 没有指标数据 → 链路追踪无法标注异常 Span
- 没有链路数据 → 看板无法展示调用树
- 没有看板 → 告警规则无处配置

### 2.3 每一层的设计哲学

#### Layer 1：结构化日志 —— "让日志说人话"

**设计目标**：把日志从"给人看的文字"变成"能被程序解析的结构化数据"。

**核心手段**：
- MDC（Mapped Diagnostic Context）注入上下文字段
- JSON 格式输出，便于 jq/ELK 解析
- 关键事件标记（AGENT_INVOKE / CLARIFY / TOOL_ERROR 等）

**设计考量**：
- 侵入性最小：只改日志格式，不改业务逻辑
- 立即可用：不需要额外基础设施（Prometheus/Jaeger）
- 向后兼容：普通文本日志仍然保留，JSON 是额外输出

#### Layer 2：Micrometer 指标 —— "用数字说话"

**设计目标**：把日志中的离散事件聚合成可查询的指标。

**核心手段**：
- Counter：计数器（调用次数、失败次数）
- Histogram：分布（耗时的 p50/p95/p99）
- Gauge：瞬时值（当前活跃 Agent 数）

**设计考量**：
- 选 Micrometer 而非自定义计数器：Spring Boot 原生支持，与 Prometheus 生态无缝集成
- 选 Histogram 而非 Summary：Histogram 支持服务端聚合，适合多实例部署
- 标签（Tag）设计：`agentName`、`toolName`、`resultType` 三个维度，支持灵活下钻

#### Layer 3：OpenTelemetry 链路追踪 —— "看见全貌"

**设计目标**：把一次请求的完整调用链可视化。

**核心手段**：
- Trace：一次用户请求的完整生命周期
- Span：每个 Agent 调用、每次工具调用
- Attribute：Agent 名称、任务描述、执行结果

**设计考量**：
- 选 OpenTelemetry 而非 Sleuth：OTel 是 CNCF 标准，厂商中立，未来可对接任意后端
- Span 粒度设计：Orchestrator 每轮一个 Span，SubAgent 每次调用一个 Span，工具调用一个 Span
- 异常标注：失败的 Span 标记 `StatusCode.ERROR`，在 Jaeger 上显示为红色

#### Layer 4：Grafana 看板 + 告警 —— "主动发现问题"

**设计目标**：从被动排查变成主动监控，问题发生时自动通知。

**核心手段**：
- 4 个核心看板：请求概览、Agent 分析、异常检测、性能分析
- 告警规则：循环调用、CLARIFY 泛滥、工具失败率、超时

**设计考量**：
- 看板分层：总览 → 分析 → 异常 → 性能，从宏观到微观
- 告警分级：Critical（循环调用）→ Warning（CLARIFY 泛滥）→ Info（仅记录）
- 阈值设计：基于历史数据的 p95 设定，避免误报

---

## 三、各层详细设计

### 3.1 Layer 1：结构化日志

#### 关键事件定义

| 事件类型 | 触发时机 | 关键字段 | 监控目标 |
|---------|---------|---------|---------|
| `TASK_DISPATCH` | Orchestrator 派发任务 | agentName, taskDesc | 任务去重 |
| `AGENT_INVOKE` | SubAgent 开始执行 | agentName, round, isRetry | 调用次数 |
| `AGENT_FINISH` | SubAgent 返回结果 | agentName, resultType, conclusion | 空转检测 |
| `AGENT_RETRY` | SubAgent 内部重试 | agentName, toolName, errorMsg | 重试风暴 |
| `CLARIFY` | SubAgent 发出澄清请求 | agentName, question | CLARIFY 泛滥 |
| `ORCHESTRATOR_ROUND` | Orchestrator 完成一轮 | round, duration | 循环轮数 |
| `ORCHESTRATOR_FINISH` | Orchestrator 输出最终结果 | totalRounds, totalDuration | 总耗时 |
| `TOOL_CALL` | 工具调用 | toolName, input, output | 工具成功率 |
| `TOOL_ERROR` | 工具失败 | toolName, errorMsg | 失败原因 |

#### MDC 字段设计

```java
public class AgentMDC {
    public static final String TRACE_ID = "traceId";      // 请求唯一标识
    public static final String AGENT_NAME = "agentName";   // Agent 名称
    public static final String ROUND = "round";            // 当前轮数
    public static final String IS_RETRY = "isRetry";       // 是否重试
    public static final String EVENT_TYPE = "eventType";   // 事件类型
}
```

#### 日志输出示例

```json
{
  "timestamp": "2026-05-29T17:46:11.212",
  "level": "INFO",
  "traceId": "abc-123-def",
  "agentName": "RouteAgent",
  "round": "3",
  "isRetry": "true",
  "eventType": "AGENT_INVOKE",
  "message": "SubAgent 开始执行，任务：查询上海到北京的航班"
}
```

#### 快速排查示例

```bash
# 找出所有重试调用
cat travel-hub.log | jq 'select(.isRetry == "true")'

# 找出某个 Agent 的所有日志
cat travel-hub.log | jq 'select(.agentName == "RouteAgent")'

# 找出所有 CLARIFY 请求
cat travel-hub.log | jq 'select(.eventType == "CLARIFY")'

# 统计每个 Agent 的调用次数
cat travel-hub.log | jq -s 'group_by(.agentName) | map({agent: .[0].agentName, count: length})'
```

---

### 3.2 Layer 2：Micrometer 指标

#### 指标清单

| 指标名 | 类型 | 含义 | 标签 | 告警阈值 |
|--------|------|------|------|---------|
| `orchestrator.round.count` | Histogram | Orchestrator 轮数 | - | p95 > 8 |
| `orchestrator.duration.seconds` | Histogram | 端到端耗时 | - | p95 > 60s |
| `subagent.invoke.count` | Counter | SubAgent 调用次数 | agentName | - |
| `subagent.retry.count` | Counter | SubAgent 重试次数 | agentName | 占比 > 30% |
| `subagent.clarify.count` | Counter | CLARIFY 请求次数 | agentName | > 3/5min |
| `subagent.empty.finish` | Counter | 空转 FINISH 次数 | agentName | > 0 |
| `tool.call.count` | Counter | 工具调用次数 | toolName | - |
| `tool.error.count` | Counter | 工具失败次数 | toolName | 失败率 > 20% |
| `tool.duration.seconds` | Histogram | 工具调用耗时 | toolName | p95 > 10s |

#### AgentMetrics 组件设计

```java
@Component
public class AgentMetrics {
    private final MeterRegistry registry;
    
    // 核心指标
    private final DistributionSummary orchestratorRounds;
    private final Timer orchestratorDuration;
    private final Map<String, Counter> subagentInvokeCounters;
    private final Counter subagentRetryCounter;
    private final Counter subagentClarifyCounter;
    private final Counter subagentEmptyFinish;
    private final Counter toolCallCounter;
    private final Counter toolErrorCounter;
    private final Timer toolDuration;
    
    // 提供 record 方法供业务代码调用
    public void recordRound(int round) { ... }
    public void recordClarify(String agentName) { ... }
    public void recordToolError(String toolName, String errorMsg) { ... }
    public void recordToolCall(String toolName, Duration duration) { ... }
}
```

#### Prometheus 查询示例

```promql
# 最近 5 分钟的重试次数
sum(rate(subagent_retry_count[5m]))

# 每个 Agent 的调用次数
sum by (agentName) (subagent_invoke_count)

# Orchestrator 轮数的 p95
histogram_quantile(0.95, rate(orchestrator_round_count_bucket[5m]))

# 工具失败率
sum(rate(tool_error_count[5m])) / sum(rate(tool_call_count[5m]))
```

---

### 3.3 Layer 3：OpenTelemetry 链路追踪

#### Span 层级设计

```
Trace: 用户请求 "上海→北京3日游"
│
├─ Root Span: Orchestrator (总耗时)
│  │
│  ├─ Span: Round 1
│  │  └─ Span: RouteAgent.searchFlights
│  │     └─ Span: TavilyApiClient.search (工具调用)
│  │
│  ├─ Span: Round 2
│  │  └─ Span: WeatherAgent.queryWeather
│  │     └─ Span: TavilyApiClient.search
│  │
│  ├─ Span: Round 3
│  │  └─ Span: HotelAgent.searchHotel
│  │     ├─ Span: TavilyApiClient.search (搜索酒店)
│  │     ├─ Span: TavilyApiClient.search (查看详情)
│  │     └─ Span: TavilyApiClient.search (对比价格)
│  │
│  └─ Span: Round 4
│     └─ Span: Orchestrator.summarize (汇总输出)
```

#### Span 属性设计

| 属性 | 类型 | 说明 |
|------|------|------|
| `agent.name` | String | Agent 名称 |
| `agent.type` | String | Agent 类型（orchestrator/subagent） |
| `agent.task` | String | 任务描述 |
| `agent.round` | int | 当前轮数 |
| `agent.isRetry` | boolean | 是否重试 |
| `agent.resultType` | String | 结果类型（FINISH/CLARIFY/RETRY） |
| `tool.name` | String | 工具名称 |
| `tool.input` | String | 工具输入（截断） |
| `tool.output` | String | 工具输出（截断） |
| `tool.success` | boolean | 是否成功 |

#### 异常标注

```java
// 工具调用失败
span.setStatus(StatusCode.ERROR, "工具执行失败: " + errorMsg);
span.recordException(new RuntimeException(errorMsg));

// CLARIFY 请求
span.setAttribute("agent.clarify", true);
span.setAttribute("agent.clarify.question", question);

// 空转 FINISH
span.setAttribute("agent.empty.finish", true);
```

---

### 3.4 Layer 4：Grafana 看板 + 告警

#### 看板 1：请求概览

| 面板 | 类型 | 数据源 |
|------|------|--------|
| 总请求数 | Stat | `orchestrator_round_count_count` |
| 成功率 | Gauge | `成功请求数 / 总请求数` |
| 平均轮数 | Stat | `orchestrator_round_count_avg` |
| 平均耗时 | Stat | `orchestrator_duration_seconds_avg` |
| 请求趋势 | Time Series | `rate(orchestrator_round_count[5m])` |
| 成功/失败占比 | Pie | `sum by (result) (...)` |

#### 看板 2：Agent 调用分析

| 面板 | 类型 | 数据源 |
|------|------|--------|
| 各 Agent 调用次数 | Bar Chart | `sum by (agentName) (subagent_invoke_count)` |
| 各 Agent 重试率 | Bar Chart | `subagent_retry_count / subagent_invoke_count` |
| Agent 调用趋势 | Time Series | `rate(subagent_invoke_count[5m])` |
| Agent 耗时分布 | Heatmap | `subagent_duration_seconds_bucket` |

#### 看板 3：异常检测

| 面板 | 类型 | 数据源 |
|------|------|--------|
| 循环调用次数 | Stat | `subagent_retry_count` |
| CLARIFY 次数 | Stat | `subagent_clarify_count` |
| 空转 FINISH 次数 | Stat | `subagent_empty_finish` |
| 工具失败次数 | Stat | `tool_error_count` |
| 异常事件时间轴 | Time Series | `sum by (eventType) (...)` |
| 最近异常列表 | Table | Logs → `select(.isRetry == "true")` |

#### 看板 4：性能分析

| 面板 | 类型 | 数据源 |
|------|------|--------|
| Orchestrator 轮数分布 | Histogram | `orchestrator_round_count_bucket` |
| 端到端耗时分布 | Histogram | `orchestrator_duration_seconds_bucket` |
| 工具调用耗时 Top 5 | Bar Chart | `topk(5, avg by (toolName) (tool_duration_seconds))` |
| SubAgent 耗时分布 | Histogram | `subagent_duration_seconds_bucket` |

#### 告警规则

| 告警名称 | PromQL 表达式 | 持续时间 | 严重程度 |
|---------|--------------|---------|---------|
| 循环调用 | `sum(rate(subagent_retry_count[5m])) > 3` | 1m | Critical |
| CLARIFY 泛滥 | `sum(rate(subagent_clarify_count[5m])) > 3` | 1m | Warning |
| Orchestrator 超时 | `histogram_quantile(0.95, rate(orchestrator_duration_seconds_bucket[5m])) > 60` | 5m | Critical |
| 工具失败率高 | `sum(rate(tool_error_count[10m])) / sum(rate(tool_call_count[10m])) > 0.2` | 10m | Warning |
| 空转 FINISH | `sum(rate(subagent_empty_finish[5m])) > 0` | 1m | Info |

---

## 四、实施路线图

```
Layer 1 - 结构化日志
├── Day 1-2: Logback JSON 配置 + MDC 工具类
├── Day 3-4: 在 Orchestrator/SubAgent 关键位置埋点
└── Day 5: 测试 + 验证日志可解析性

Layer 2 - Micrometer 指标
├── Day 1-2: 指标定义 + AgentMetrics 组件
├── Day 3-4: 在关键位置埋点
└── Day 5: Prometheus 接入 + 验证

Layer 3 - OpenTelemetry 链路追踪
├── Day 1-2: OTel Starter 接入 + Span 创建
├── Day 3-4: Jaeger 部署 + 验证调用树
└── Day 5: 优化 Span 属性

Layer 4 - Grafana 看板
├── Day 1-2: Dashboard 设计 + 4 个面板
├── Day 3-4: 告警规则配置
└── Day 5: 文档 + 面试演练
```

---


## 五、技术选型对比

| 技术 | 备选方案 | 选择理由 |
|------|---------|---------|
| 日志框架 | Logback vs Log4j2 | Spring Boot 默认 Logback，生态成熟 |
| 指标框架 | Micrometer vs 自定义计数器 | Spring Boot 原生支持，Prometheus 生态无缝集成 |
| 链路追踪 | OpenTelemetry vs Sleuth | OTel 是 CNCF 标准，厂商中立，Sleuth 已停止维护 |
| 看板工具 | Grafana vs Kibana | Grafana 对 Prometheus 支持更好，更适合指标监控 |
| 告警工具 | Grafana Alerting vs Alertmanager | Grafana 8.0+ 内置告警，减少组件依赖 |

---

## 六、关键指标定义

### 6.1 健康指标

| 指标 | 健康范围 | 告警阈值 | 说明 |
|------|---------|---------|------|
| Orchestrator 轮数 | 3-6 轮 | > 8 轮 | 正常请求应在 3-6 轮内完成 |
| 端到端耗时 | 15-45s | > 60s | 包含所有 Agent 调用时间 |
| SubAgent 调用次数 | 5-6 次 | > 8 次 | 每个 SubAgent 调用 1 次 |
| CLARIFY 次数 | 0-1 次 | > 3 次 | 正常请求不应频繁 CLARIFY |
| 工具失败率 | < 5% | > 20% | Tavily API 成功率应很高 |

### 6.2 异常模式

| 模式 | 识别方式 | 严重程度 |
|------|---------|---------|
| 循环调用 | 同一 Agent 连续调用 > 3 次 | Critical |
| CLARIFY 泛滥 | 5 分钟内 CLARIFY > 3 次 | Warning |
| 空转 FINISH | SubAgent 返回空结论 | Info |
| 工具风暴 | 同一工具 5 分钟内调用 > 20 次 | Warning |
| Orchestrator 卡死 | 一轮耗时 > 30s | Critical |
