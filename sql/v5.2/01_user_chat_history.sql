-- ============================================================
-- v5.2 聊天记录（原文 + 摘要）
--
-- 设计要点：
-- 1. 请求进入时先 INSERT（此时用户输入已存在），收尾时 UPDATE 补 final_reply
-- 2. 每个字段只写一次：input 冻结 / output 冻结 / compacted_by 冻结
-- 3. trace_id 是幂等键（前端重连不会插两行）
-- 4. 摘要是"另一类记录"，原文永不删除 → 压缩可回滚
-- ============================================================

-- ----------------------------
-- 聊天记录原文
-- ----------------------------
CREATE TABLE IF NOT EXISTS user_chat_history (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id    VARCHAR(64)  NOT NULL,
    trace_id      VARCHAR(64)  NOT NULL,
    user_input    TEXT         NOT NULL,
    final_reply   TEXT,
    finish_reason VARCHAR(32),
    input_token   INT          NOT NULL DEFAULT 0,
    output_token  INT          NOT NULL DEFAULT 0,
    compacted_by  BIGINT,
    compacted_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finished_at   TIMESTAMPTZ,
    CONSTRAINT uk_history_trace UNIQUE (trace_id)
);

COMMENT ON COLUMN user_chat_history.id IS '主键';
COMMENT ON COLUMN user_chat_history.session_id IS '会话ID（现阶段等同用户ID）';
COMMENT ON COLUMN user_chat_history.trace_id IS '本轮请求的链路ID，同时作为幂等键';
COMMENT ON COLUMN user_chat_history.user_input IS '用户输入（写入即冻结）';
COMMENT ON COLUMN user_chat_history.final_reply IS '最终回复（收尾时写一次；失败/中断时可能为 NULL）';
COMMENT ON COLUMN user_chat_history.finish_reason IS '结束原因：FINISH/CLARIFY/OVER_MAX_LOOP_TIMES/CANCELLED/FAILED';
COMMENT ON COLUMN user_chat_history.input_token IS '本轮所有 LLM 调用的输入 token 合计';
COMMENT ON COLUMN user_chat_history.output_token IS '本轮所有 LLM 调用的输出 token 合计';
COMMENT ON COLUMN user_chat_history.compacted_by IS '被哪条摘要遮蔽；NULL 表示仍参与上下文组装';
COMMENT ON COLUMN user_chat_history.compacted_at IS '被遮蔽的时间';
COMMENT ON COLUMN user_chat_history.created_at IS '一轮开始时间（用户按下回车）';
COMMENT ON COLUMN user_chat_history.finished_at IS '回复写定的时间';

-- 按会话取历史
CREATE INDEX IF NOT EXISTS idx_history_session
    ON user_chat_history (session_id, id);

-- 只索引"可见行"，组装上下文时走这条
CREATE INDEX IF NOT EXISTS idx_history_visible
    ON user_chat_history (session_id, id)
    WHERE compacted_by IS NULL;

-- ----------------------------
-- 聊天记录摘要（完全不更新）
-- ----------------------------
CREATE TABLE IF NOT EXISTS user_chat_history_compact (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id     VARCHAR(64) NOT NULL,
    content        TEXT        NOT NULL,
    begin_id       BIGINT      NOT NULL,
    end_id         BIGINT      NOT NULL,
    covered_rounds INT         NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_compact_range CHECK (end_id > begin_id)
);

COMMENT ON TABLE user_chat_history_compact IS '聊天记录压缩摘要；覆盖区间为半开区间 [begin_id, end_id)';
COMMENT ON COLUMN user_chat_history_compact.content IS '摘要正文';
COMMENT ON COLUMN user_chat_history_compact.begin_id IS '覆盖区间下界（含）';
COMMENT ON COLUMN user_chat_history_compact.end_id IS '覆盖区间上界（不含）';
COMMENT ON COLUMN user_chat_history_compact.covered_rounds IS '覆盖了多少轮，便于展示与校验';

CREATE INDEX IF NOT EXISTS idx_compact_session
    ON user_chat_history_compact (session_id, id);
