package com.travel.aiagent.common.repository;

import com.travel.common.domain.FeedbackStats;
import com.travel.common.domain.LlmFeedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 反馈数据仓库
 *
 * 使用 JdbcTemplate 操作 llm_feedback 表
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class FeedbackRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 插入反馈记录
     */
    public int insert(LlmFeedback feedback) {
        String sql = """
                INSERT INTO llm_feedback (trace_id, user_id, agent_name, rating, comment, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        int rows = jdbcTemplate.update(sql,
                feedback.getTraceId(),
                feedback.getUserId(),
                feedback.getAgentName(),
                feedback.getRating(),
                feedback.getComment(),
                Timestamp.valueOf(feedback.getCreatedAt())
        );

        log.info("[Feedback] 插入反馈记录 | traceId={} | rating={} | rows={}",
                feedback.getTraceId(), feedback.getRating(), rows);

        return rows;
    }

    /**
     * 根据 traceId 查询反馈
     */
    public LlmFeedback findByTraceId(String traceId) {
        String sql = "SELECT * FROM llm_feedback WHERE trace_id = ?";

        List<LlmFeedback> results = jdbcTemplate.query(sql, new FeedbackRowMapper(), traceId);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 查询所有反馈（分页）
     */
    public List<LlmFeedback> findAll(int limit, int offset) {
        String sql = "SELECT * FROM llm_feedback ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new FeedbackRowMapper(), limit, offset);
    }

    /**
     * 按 Agent 名称查询反馈
     */
    public List<LlmFeedback> findByAgentName(String agentName) {
        String sql = "SELECT * FROM llm_feedback WHERE agent_name = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new FeedbackRowMapper(), agentName);
    }

    /**
     * 查询点踩的反馈（用于分析）
     */
    public List<LlmFeedback> findDislikes(int limit) {
        String sql = "SELECT * FROM llm_feedback WHERE rating = -1 ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, new FeedbackRowMapper(), limit);
    }

    /**
     * 统计总反馈数
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM llm_feedback";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    /**
     * 统计点赞数
     */
    public long countLikes() {
        String sql = "SELECT COUNT(*) FROM llm_feedback WHERE rating = 1";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    /**
     * 统计点踩数
     */
    public long countDislikes() {
        String sql = "SELECT COUNT(*) FROM llm_feedback WHERE rating = -1";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    /**
     * 按 Agent 统计点赞数
     */
    public long countLikesByAgent(String agentName) {
        String sql = "SELECT COUNT(*) FROM llm_feedback WHERE agent_name = ? AND rating = 1";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, agentName);
        return count != null ? count : 0;
    }

    /**
     * 按 Agent 统计点踩数
     */
    public long countDislikesByAgent(String agentName) {
        String sql = "SELECT COUNT(*) FROM llm_feedback WHERE agent_name = ? AND rating = -1";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, agentName);
        return count != null ? count : 0;
    }

    /**
     * 获取所有 Agent 名称
     */
    public List<String> findAllAgentNames() {
        String sql = "SELECT DISTINCT agent_name FROM llm_feedback";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * 统计点踩原因 Top N
     */
    public List<Map<String, Object>> findTopDislikeReasons(int limit) {
        String sql = """
                SELECT comment, COUNT(*) as count
                FROM llm_feedback
                WHERE rating = -1 AND comment IS NOT NULL AND comment != ''
                GROUP BY comment
                ORDER BY count DESC
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, limit);
    }

    /**
     * 行映射器
     */
    private static class FeedbackRowMapper implements RowMapper<LlmFeedback> {
        @Override
        public LlmFeedback mapRow(ResultSet rs, int rowNum) throws SQLException {
            return LlmFeedback.builder()
                    .id(rs.getLong("id"))
                    .traceId(rs.getString("trace_id"))
                    .userId(rs.getString("user_id"))
                    .agentName(rs.getString("agent_name"))
                    .rating(rs.getInt("rating"))
                    .comment(rs.getString("comment"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .build();
        }
    }
}
