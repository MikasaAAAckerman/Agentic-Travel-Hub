package com.travel.aiagent.common.repository;

import com.travel.common.domain.LlmCallLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * LLM 调用日志 Repository
 *
 * 使用 JdbcTemplate 操作 llm_call_log 表
 */
@Repository
@Slf4j
public class LlmCallLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public LlmCallLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * RowMapper：将 ResultSet 映射为 LlmCallLog 对象
     */
    private final RowMapper<LlmCallLog> rowMapper = (ResultSet rs, int rowNum) -> LlmCallLog.builder()
            .id(rs.getLong("id"))
            .traceId(rs.getString("trace_id"))
            .callType(rs.getString("call_type"))
            .agentName(rs.getString("agent_name"))
            .modelName(rs.getString("model_name"))
            .version(rs.getString("version"))
            .systemPrompt(rs.getString("system_prompt"))
            .userInput(rs.getString("user_input"))
            .historyContext(rs.getString("history_context"))
            .llmOutput(rs.getString("llm_output"))
            .parsedOutput(rs.getString("parsed_output"))
            .toolCalls(rs.getString("tool_calls"))
            .toolResults(rs.getString("tool_results"))
            .promptTokens(rs.getInt("prompt_tokens"))
            .completionTokens(rs.getInt("completion_tokens"))
            .totalTokens(rs.getInt("total_tokens"))
            .durationMs(rs.getLong("duration_ms"))
            .success(rs.getBoolean("success"))
            .errorMessage(rs.getString("error_message"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    /**
     * 插入一条调用日志
     */
    public void insert(LlmCallLog callLog) {
        String sql = """
                INSERT INTO llm_call_log (
                    trace_id, call_type, agent_name, model_name, version,
                    system_prompt, user_input, history_context,
                    llm_output, parsed_output,
                    tool_calls, tool_results,
                    prompt_tokens, completion_tokens, total_tokens,
                    duration_ms, success, error_message, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try {
            jdbcTemplate.update(sql,
                    callLog.getTraceId(),
                    callLog.getCallType(),
                    callLog.getAgentName(),
                    callLog.getModelName(),
                    callLog.getVersion(),
                    callLog.getSystemPrompt(),
                    callLog.getUserInput(),
                    callLog.getHistoryContext(),
                    callLog.getLlmOutput(),
                    callLog.getParsedOutput(),
                    callLog.getToolCalls(),
                    callLog.getToolResults(),
                    callLog.getPromptTokens(),
                    callLog.getCompletionTokens(),
                    callLog.getTotalTokens(),
                    callLog.getDurationMs(),
                    callLog.getSuccess(),
                    callLog.getErrorMessage(),
                    Timestamp.valueOf(callLog.getCreatedAt() != null ? callLog.getCreatedAt() : LocalDateTime.now())
            );
            log.debug("[LlmCallLog] 插入成功 | traceId={} | callType={}", callLog.getTraceId(), callLog.getCallType());
        } catch (Exception e) {
            log.error("[LlmCallLog] 插入失败 | traceId={}", callLog.getTraceId(), e);
        }
    }

    /**
     * 按 traceId 查询（查完整调用链）
     */
    public List<LlmCallLog> findByTraceId(String traceId) {
        String sql = "SELECT * FROM llm_call_log WHERE trace_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, rowMapper, traceId);
    }

    /**
     * 按调用类型查询
     */
    public List<LlmCallLog> findByCallType(String callType) {
        String sql = "SELECT * FROM llm_call_log WHERE call_type = ? ORDER BY created_at DESC LIMIT 100";
        return jdbcTemplate.query(sql, rowMapper, callType);
    }

    /**
     * 按Agent名称查询
     */
    public List<LlmCallLog> findByAgentName(String agentName) {
        String sql = "SELECT * FROM llm_call_log WHERE agent_name = ? ORDER BY created_at DESC LIMIT 100";
        return jdbcTemplate.query(sql, rowMapper, agentName);
    }

    /**
     * 查询失败的调用
     */
    public List<LlmCallLog> findFailures() {
        String sql = "SELECT * FROM llm_call_log WHERE success = FALSE ORDER BY created_at DESC LIMIT 100";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * 查询最近N条记录
     */
    public List<LlmCallLog> findRecent(int limit) {
        String sql = "SELECT * FROM llm_call_log ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, rowMapper, limit);
    }

    /**
     * 统计各调用类型的数量
     */
    public long countByCallType(String callType) {
        String sql = "SELECT COUNT(*) FROM llm_call_log WHERE call_type = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, callType);
        return count != null ? count : 0;
    }

    /**
     * 统计总Token消耗
     */
    public long sumTotalTokens() {
        String sql = "SELECT COALESCE(SUM(total_tokens), 0) FROM llm_call_log";
        Long sum = jdbcTemplate.queryForObject(sql, Long.class);
        return sum != null ? sum : 0;
    }

    /**
     * 统计平均耗时
     */
    public double avgDurationByCallType(String callType) {
        String sql = "SELECT COALESCE(AVG(duration_ms), 0) FROM llm_call_log WHERE call_type = ? AND success = TRUE";
        Double avg = jdbcTemplate.queryForObject(sql, Double.class, callType);
        return avg != null ? avg : 0;
    }
}
