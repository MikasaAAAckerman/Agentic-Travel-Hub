# 可观测性栈

一键启动 ELK + Prometheus + Grafana + Jaeger，实现日志、指标、链路追踪三层监控。

## 快速启动

```bash
cd docker
docker-compose up -d
```

## 服务列表

| 服务 | 地址 | 说明 |
|------|------|------|
| **Kibana** | http://localhost:5601 | 日志查询与分析 |
| **Elasticsearch** | http://localhost:9200 | 日志存储引擎 |
| **Logstash** | :5044 (Beats) / :19600 (HTTP) | 日志采集管道 |
| **Grafana** | http://localhost:3000 | 可视化面板（admin/admin） |
| **Prometheus** | http://localhost:9090 | 指标查询 |
| **Jaeger** | http://localhost:16686 | 分布式追踪 |

> **端口说明**：Jaeger OTLP 使用 14318 映射容器 4318，Logstash HTTP 使用 19600 映射容器 9600，避免 Windows Hyper-V 端口冲突。

## 数据源配置

Grafana 已自动配置好以下数据源：

- **Prometheus**：采集 Spring Boot Actuator 指标
- **Jaeger**：展示 OpenTelemetry 分布式追踪
- **Elasticsearch**：索引 `travel-hub-*`，用于日志面板

## Dashboard

已预置「Agentic-Travel-Hub 监控面板」，包含 5 大区域：

1. **请求概览**：Agent 调用总次数、工具调用成功/失败、CLARIFY 事件、当前活跃 Agent 数
2. **性能分析**：Agent/Planner/Worker/Orchestrator 耗时分布（P50/P95/P99）
3. **Agent 调用分析**：各 Agent 调用次数与工具调用 Top 排行
4. **异常检测**：错误率趋势、重试率、异常 Agent 分布
5. **ES 日志流**：实时日志查询（需配合 Kibana 深入分析）

## Spring Boot 配置

确保 `application.yml` 中配置正确：

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://localhost:14318/v1/traces
```

## 停止服务

```bash
docker-compose down
```

## 清理数据

```bash
docker-compose down -v  # 删除 volumes
```
