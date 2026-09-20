package com.travel.aiagent.common.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 进行中的 Agent 会话注册表（v3 停止功能）—— traceId → AgentSession。
 *
 * <p>用户点「停止」时按 traceId 定位会话：置 cancelled 标志 + dispose 图执行订阅 + complete SSE sink。
 *
 * <p>生命周期：
 * <ul>
 *   <li>{@link #register}  — 请求启动时（Controller 拿到 Disposable 后）</li>
 *   <li>{@link #remove}    — stop 接口（幂等）；doFinally 兜底清理（正常/异常/取消都走到）</li>
 *   <li>{@link #isCancelled} — 各 node 检查点读（orchestrator planner / subAgentNode / 子图 planner / worker）</li>
 * </ul>
 */
@Slf4j
@Component
public class AgentSessionRegistry {

    private final ConcurrentHashMap<String, AgentSession> sessions = new ConcurrentHashMap<>();

    /**
     * 注册进行中的会话。traceId 已由前端透传（会话级），是全局唯一的取消定位键。
     */
    public void register(String traceId, Disposable subscription, Sinks.Many<String> sink) {
        AgentSession session = new AgentSession(subscription, sink);
        AgentSession prev = sessions.put(traceId, session);
        if (prev != null) {
            // 理论上同 traceId 并发只有一单（前端同一页面串行发消息），防御性兜底
            log.warn("[AgentSession] traceId={} 已存在进行中的会话，覆盖旧会话", traceId);
        }
    }

    /**
     * 移除并返回会话（幂等：不存在返回 null，调用方直接视为已停止/已结束）。
     */
    public AgentSession remove(String traceId) {
        return sessions.remove(traceId);
    }

    /**
     * 会话是否已被用户标记停止（协作式取消检查点专用）。
     * 会话不存在（未开始/已结束/已 remove）一律返回 false —— 不误伤正常执行。
     */
    public boolean isCancelled(String traceId) {
        AgentSession session = sessions.get(traceId);
        return session != null && session.getCancelled().get();
    }
}
