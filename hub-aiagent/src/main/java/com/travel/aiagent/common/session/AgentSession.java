package com.travel.aiagent.common.session;

import lombok.Data;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单个进行中的 Agent 会话上下文（v3 停止功能）。
 *
 * <p>一次 {@code /chat/stream/v3} 请求 = 一个 AgentSession，按 traceId 注册：
 * <pre>
 *   register(traceId, disposable, sink)   ← 请求启动时（Controller）
 *   stop 接口：cancelled=true → dispose → sink.complete   ← 用户点停止
 *   doFinally：registry.remove(traceId)                    ← 任何方式结束都清理
 * </pre>
 *
 * <p>取消语义 = 协作式取消：
 * <ul>
 *   <li>{@link #dispose()} 物理掐断图执行订阅（拦「还没开始的 node」）</li>
 *   <li>{@link #cancelled} 业务检查点（各 node 开头查 registry.isCancelled，兜底「正在跑的同步阻塞调用」）</li>
 * </ul>
 */
@Data
public class AgentSession {

    /** graph 执行订阅 —— dispose 即停止（不再启动新的 node / LLM / 工具） */
    private final Disposable subscription;

    /** 前端 SSE 的 sink —— 停止时也要 complete 掉，否则前端挂死 */
    private final Sinks.Many<String> sink;

    /** 协作式取消标志：stop 接口置 true，各 node 检查点读 */
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
}
