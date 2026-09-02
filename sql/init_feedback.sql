-- ================================================
-- LLM 回复质量反馈表
-- 用于收集用户的点赞/点踩数据，形成数据飞轮
-- ================================================

CREATE TABLE IF NOT EXISTS llm_feedback (
    id          BIGSERIAL PRIMARY KEY,
    trace_id    VARCHAR(64)  NOT NULL,
    user_id     VARCHAR(64)  NOT NULL,
    agent_name  VARCHAR(32)  NOT NULL,
    rating      SMALLINT     NOT NULL,
    comment     VARCHAR(500) DEFAULT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 索引
    CONSTRAINT idx_trace_id UNIQUE (trace_id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_feedback_agent_rating ON llm_feedback (agent_name, rating);
CREATE INDEX IF NOT EXISTS idx_feedback_created_at ON llm_feedback (created_at);

-- 添加注释
COMMENT ON TABLE llm_feedback IS 'LLM 回复质量反馈表';
COMMENT ON COLUMN llm_feedback.id IS '主键ID';
COMMENT ON COLUMN llm_feedback.trace_id IS '关联请求追踪ID';
COMMENT ON COLUMN llm_feedback.user_id IS '用户ID';
COMMENT ON COLUMN llm_feedback.agent_name IS '回复的Agent名称';
COMMENT ON COLUMN llm_feedback.rating IS '1=赞, -1=踩';
COMMENT ON COLUMN llm_feedback.comment IS '用户补充原因';
COMMENT ON COLUMN llm_feedback.created_at IS '创建时间';
