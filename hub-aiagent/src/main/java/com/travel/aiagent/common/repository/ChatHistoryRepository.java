package com.travel.aiagent.common.repository;

import com.travel.aiagent.common.domain.ChatHistoryCompactDO;
import com.travel.aiagent.common.domain.ChatHistoryDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天记录 Repository —— 用 JdbcTemplate 操作 {@code user_chat_history} 与
 * {@code user_chat_history_compact}。
 *
 * <p>写入模型是"两段式"：
 * <ol>
 *   <li>{@link #openRound} —— 请求进入时调用，用户输入一存在就先落一行，拿回 id</li>
 *   <li>{@link #closeRound} —— 收尾时补 final_reply，带 {@code final_reply IS NULL} 守卫，保证只写一次</li>
 * </ol>
 *
 * <p>这样 {@code llm_call_log.chat_history_id} 在执行期间就已经可用，不需要"先领号"或回填。
 */
@Repository
@Slf4j
public class ChatHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChatHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ═══════════════════════════════════════════════════════════
    // 写
    // ═══════════════════════════════════════════════════════════

    /**
     * 开一轮：用户输入已存在，先落一次。
     *
     * <p>幂等：同一 {@code trace_id} 重复调用不会插第二行，仍然返回既有 id。
     * （`DO UPDATE` 而不是 `DO NOTHING`，是为了让 `RETURNING` 在冲突时也能返回行。）
     *
     * @return 聊天记录 id；写库失败返回 null（**不阻塞主流程**，记忆丢了比请求失败好）
     */
    public Long openRound(String sessionId, String traceId, String userInput) {
        String sql = """
                INSERT INTO user_chat_history (session_id, trace_id, user_input)
                VALUES (?, ?, ?)
                ON CONFLICT (trace_id) DO UPDATE SET session_id = EXCLUDED.session_id
                RETURNING id
                """;
        try {
            return jdbcTemplate.queryForObject(sql, Long.class, sessionId, traceId, userInput);
        } catch (DataAccessException e) {
            log.error("[ChatHistory] 开轮写入失败 | sessionId={} | traceId={}", sessionId, traceId, e);
            return null;
        }
    }

    /**
     * 收一轮：补最终回复。
     *
     * <p>守卫 {@code final_reply IS NULL}：重复收尾不会覆盖第一次的结果。
     *
     * @return true = 本次写入成功；false = 已经写过（或不存在）
     */
    public boolean closeRound(Long chatHistoryId, String finalReply, String finishReason,
                              int inputToken, int outputToken) {
        if (chatHistoryId == null) {
            return false;
        }
        String sql = """
                UPDATE user_chat_history
                SET final_reply = ?, finish_reason = ?, input_token = ?, output_token = ?, finished_at = now()
                WHERE id = ? AND final_reply IS NULL
                """;
        try {
            int rows = jdbcTemplate.update(sql, finalReply, finishReason, inputToken, outputToken, chatHistoryId);
            if (rows == 0) {
                log.warn("[ChatHistory] 收轮被跳过（已经写过 final_reply）| id={}", chatHistoryId);
            }
            return rows > 0;
        } catch (DataAccessException e) {
            log.error("[ChatHistory] 收轮写入失败 | id={}", chatHistoryId, e);
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 读
    // ═══════════════════════════════════════════════════════════

    /**
     * 最近 N 轮可见记录（倒序取，调用方如需正序自行反转）。
     *
     * <p>{@code final_reply IS NOT NULL}：半截记录（用户说了话但系统挂了）不参与上下文。
     */
    public List<ChatHistoryDO> findRecentVisible(String sessionId, int limit) {
        String sql = """
                SELECT * FROM user_chat_history
                WHERE session_id = ? AND final_reply IS NOT NULL AND compacted_by IS NULL
                ORDER BY id DESC LIMIT ?
                """;
        return jdbcTemplate.query(sql, historyRowMapper, sessionId, limit);
    }

    /** 某会话的全部可见记录（正序） */
    public List<ChatHistoryDO> findVisible(String sessionId) {
        String sql = """
                SELECT * FROM user_chat_history
                WHERE session_id = ? AND final_reply IS NOT NULL AND compacted_by IS NULL
                ORDER BY id
                """;
        return jdbcTemplate.query(sql, historyRowMapper, sessionId);
    }

    /** 某会话的全部摘要（正序） */
    public List<ChatHistoryCompactDO> findCompacts(String sessionId) {
        String sql = "SELECT * FROM user_chat_history_compact WHERE session_id = ? ORDER BY id";
        return jdbcTemplate.query(sql, compactRowMapper, sessionId);
    }

    /** 可见原文中**最早**的 N 条（压缩只吃前缀，保证摘要永远排在最前） */
    public List<ChatHistoryDO> findVisibleOldest(String sessionId, int limit) {
        String sql = """
                SELECT * FROM user_chat_history
                WHERE session_id = ? AND final_reply IS NOT NULL AND compacted_by IS NULL
                ORDER BY id ASC LIMIT ?
                """;
        return jdbcTemplate.query(sql, historyRowMapper, sessionId, limit);
    }

    /** 可见原文条数（压缩时用来判断"能压多少、要留多少"） */
    public int countVisible(String sessionId) {
        String sql = """
                SELECT COUNT(*) FROM user_chat_history
                WHERE session_id = ? AND final_reply IS NOT NULL AND compacted_by IS NULL
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, sessionId);
        return count != null ? count : 0;
    }

    /** 可见记录的 token 合计 —— 压缩触发用 */
    public int sumVisibleTokens(String sessionId) {
        String sql = """
                SELECT COALESCE(SUM(input_token + output_token), 0) FROM user_chat_history
                WHERE session_id = ? AND final_reply IS NOT NULL AND compacted_by IS NULL
                """;
        Integer sum = jdbcTemplate.queryForObject(sql, Integer.class, sessionId);
        return sum != null ? sum : 0;
    }

    /**
     * 落一次压缩：先插摘要、再标记遮蔽，两者同一事务。
     *
     * <p>标记行数与覆盖轮数不符（说明期间被别的线程压过）就抛异常回滚，
     * 避免留下"有摘要、原文没遮蔽"的孤儿记录。
     *
     * @return 新摘要的 id
     */
    @Transactional
    public long saveCompaction(String sessionId, List<ChatHistoryDO> covered, String content) {
        long beginId = covered.get(0).getId();
        long endId = covered.get(covered.size() - 1).getId() + 1;

        String insertSql = """
                INSERT INTO user_chat_history_compact (session_id, content, begin_id, end_id, covered_rounds)
                VALUES (?, ?, ?, ?, ?) RETURNING id
                """;
        Long compactId = jdbcTemplate.queryForObject(insertSql, Long.class,
                sessionId, content, beginId, endId, covered.size());
        if (compactId == null) {
            throw new IllegalStateException("插入摘要失败，未返回 id");
        }

        String markSql = """
                UPDATE user_chat_history SET compacted_by = ?, compacted_at = now()
                WHERE session_id = ? AND id >= ? AND id < ? AND compacted_by IS NULL
                """;
        int rows = jdbcTemplate.update(markSql, compactId, sessionId, beginId, endId);
        if (rows != covered.size()) {
            throw new IllegalStateException("遮蔽行数不符 | 期望=" + covered.size() + " | 实际=" + rows
                    + " | 说明区间已被并发压缩");
        }
        return compactId;
    }

    /** 清空会话（先删摘要再删原文，避免悬挂的 compacted_by） */
    public void clearSession(String sessionId) {
        jdbcTemplate.update("DELETE FROM user_chat_history_compact WHERE session_id = ?", sessionId);
        jdbcTemplate.update("DELETE FROM user_chat_history WHERE session_id = ?", sessionId);
    }

    // ═══════════════════════════════════════════════════════════
    // RowMapper
    // ═══════════════════════════════════════════════════════════

    private final RowMapper<ChatHistoryDO> historyRowMapper = (ResultSet rs, int rowNum) -> ChatHistoryDO.builder()
            .id(rs.getLong("id"))
            .sessionId(rs.getString("session_id"))
            .traceId(rs.getString("trace_id"))
            .userInput(rs.getString("user_input"))
            .finalReply(rs.getString("final_reply"))
            .finishReason(rs.getString("finish_reason"))
            .inputToken(rs.getInt("input_token"))
            .outputToken(rs.getInt("output_token"))
            .compactedBy(nullableLong(rs, "compacted_by"))
            .compactedAt(toLocalDateTime(rs.getTimestamp("compacted_at")))
            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
            .finishedAt(toLocalDateTime(rs.getTimestamp("finished_at")))
            .build();

    private final RowMapper<ChatHistoryCompactDO> compactRowMapper = (ResultSet rs, int rowNum) ->
            ChatHistoryCompactDO.builder()
                    .id(rs.getLong("id"))
                    .sessionId(rs.getString("session_id"))
                    .content(rs.getString("content"))
                    .beginId(rs.getLong("begin_id"))
                    .endId(rs.getLong("end_id"))
                    .coveredRounds(rs.getInt("covered_rounds"))
                    .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                    .build();

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
