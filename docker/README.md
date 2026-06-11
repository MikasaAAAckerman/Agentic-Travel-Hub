# 可观测性栈

一键启动 Grafana + Prometheus + Jaeger，用于监控 Agentic-Travel-Hub。

## 快速启动

```bash
cd docker
docker-compose up -d
```

## 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| Grafana | http://localhost:3000 | 可视化面板（admin/admin） |
| Prometheus | http://localhost:9090 | 指标查询 |
| Jaeger | http://localhost:16686 | 分布式追踪 |

## 数据源配置

Grafana 已自动配置好以下数据源：

- **Prometheus**：采集 Spring Boot Actuator 指标
- **Jaeger**：展示 OpenTelemetry 分布式追踪

## Dashboard

已预置「Agentic-Travel-Hub 监控面板」，包含：

- Agent 调用总次数
- 工具调用成功/失败统计
- CLARIFY 事件次数
- 当前活跃 Agent 数
- Agent/Planner/Worker/Ochestrator 耗时分布（p50/p95/p99）
- 调用趋势图

## Spring Boot 配置

确保 `application.yml` 中配置正确：

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
```

## 停止服务

```bash
docker-compose down
```

## 清理数据

```bash
docker-compose down -v  # 删除 volumes
```
