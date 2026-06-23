-- ============================================================
-- LLM 调用日志表
-- 记录每次 Planner / Worker / Router 调用 LLM 的入参与出参
-- ============================================================

CREATE TABLE IF NOT EXISTS llm_call_log (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trace_id        VARCHAR(64)   NOT NULL,           -- 链路追踪ID，关联 Jaeger
    call_type       VARCHAR(32)   NOT NULL,           -- 调用类型：PLANNER / WORKER / ROUTER / MEMORY / EVAL
    agent_name      VARCHAR(64)   NOT NULL,           -- Agent名称：OrchestratorGraphAgent / FlightExpert 等
    model_name      VARCHAR(32)   NOT NULL,           -- 模型名称：deepseek / qwen
    version         VARCHAR(16)   DEFAULT 'v3',       -- 架构版本：v0/v1/v2/v3

    -- 输入信息
    system_prompt   TEXT,                              -- 系统提示词
    user_input      TEXT          NOT NULL,            -- 用户输入 / Planner输出
    history_context TEXT,                              -- 历史上下文

    -- 输出信息
    llm_output      TEXT,                              -- LLM原始输出
    parsed_output   TEXT,                              -- 解析后的结构化输出（JSON）

    -- 工具调用信息
    tool_calls      TEXT,                              -- 工具调用列表（JSON数组）
    tool_results    TEXT,                              -- 工具执行结果（JSON数组）

    -- Token 统计
    prompt_tokens   INT           DEFAULT 0,           -- 输入Token数
    completion_tokens INT         DEFAULT 0,           -- 输出Token数
    total_tokens    INT           DEFAULT 0,           -- 总Token数

    -- 性能指标
    duration_ms     BIGINT        NOT NULL,            -- 调用耗时（毫秒）
    success         BOOLEAN       NOT NULL DEFAULT TRUE, -- 是否成功
    error_message   TEXT,                              -- 错误信息

    -- 时间戳
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 索引设计
-- ============================================================

-- 按链路追踪ID查询（查完整调用链）
CREATE INDEX idx_llm_call_log_trace_id ON llm_call_log(trace_id);

-- 按调用类型查询（查所有Planner调用）
CREATE INDEX idx_llm_call_log_call_type ON llm_call_log(call_type);

-- 按Agent名称查询（查某个Agent的调用历史）
CREATE INDEX idx_llm_call_log_agent_name ON llm_call_log(agent_name);

-- 按模型名称查询（统计各模型调用次数）
CREATE INDEX idx_llm_call_log_model_name ON llm_call_log(model_name);

-- 按创建时间查询（查最近的调用记录）
CREATE INDEX idx_llm_call_log_created_at ON llm_call_log(created_at);

-- 按成功状态查询（查失败的调用）
CREATE INDEX idx_llm_call_log_success ON llm_call_log(success);

-- ============================================================
-- 注释
-- ============================================================

COMMENT ON TABLE llm_call_log IS 'LLM调用日志表，记录每次Planner/Worker/Router调用LLM的入参与出参';
COMMENT ON COLUMN llm_call_log.trace_id IS '链路追踪ID，关联Jaeger Trace';
COMMENT ON COLUMN llm_call_log.call_type IS '调用类型：PLANNER(规划)/WORKER(执行)/ROUTER(路由)/MEMORY(记忆压缩)/EVAL(评测)';
COMMENT ON COLUMN llm_call_log.agent_name IS 'Agent名称：OrchestratorGraphAgent/FlightExpert/HotelExpert等';
COMMENT ON COLUMN llm_call_log.model_name IS '模型名称：deepseek(Planner)/qwen(Worker)';
COMMENT ON COLUMN llm_call_log.tool_calls IS '工具调用列表，格式：[{"name":"flightSearch","args":"{...}"}]';
COMMENT ON COLUMN llm_call_log.tool_results IS '工具执行结果，格式：[{"name":"flightSearch","result":"..."}]';
