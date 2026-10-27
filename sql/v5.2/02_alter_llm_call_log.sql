-- ============================================================
-- v5.2 llm_call_log 补两个关联字段
--
-- 追溯链：session_id → user_chat_history.id → llm_call_log.chat_history_id
--   session_id      : 这个会话的所有调用
--   chat_history_id : 这一轮的所有调用（页面重放时按它捞明细）
--   trace_id        : 本次请求（原有字段，保留，用于 join 链路追踪）
-- ============================================================

ALTER TABLE llm_call_log ADD COLUMN IF NOT EXISTS session_id VARCHAR(64);
ALTER TABLE llm_call_log ADD COLUMN IF NOT EXISTS chat_history_id BIGINT;

COMMENT ON COLUMN llm_call_log.session_id IS '会话ID（现阶段等同用户ID）';
COMMENT ON COLUMN llm_call_log.chat_history_id IS '所属聊天记录ID（user_chat_history.id），用于按轮重放执行明细';

-- 按会话查调用链
CREATE INDEX IF NOT EXISTS idx_llm_call_log_session
    ON llm_call_log (session_id, created_at);

-- 按某一轮查调用明细（重放用）
CREATE INDEX IF NOT EXISTS idx_llm_call_log_chat_history
    ON llm_call_log (chat_history_id, id);
