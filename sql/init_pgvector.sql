-- ============================================================
-- PGVector 初始化脚本
-- 执行顺序：先装扩展，再建表
-- ============================================================

-- 1. 安装 pgvector 扩展（只需执行一次）
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. 创建向量表（知识库文档 RAG）
-- 注意：id 用 TEXT 而非 UUID，因为知识库版本管理需要形如 "工具名_v1.0" 的字符串 ID
CREATE TABLE IF NOT EXISTS travel_hub_vectors (
    id        TEXT PRIMARY KEY,
    content   TEXT,              -- 原文文本
    metadata  JSONB,             -- 元数据（fileName、domain 等）
    embedding VECTOR(1536)      -- DashScope text-embedding-v3 输出维度
);

-- 3. 创建 HNSW 向量索引（加速相似度搜索）
CREATE INDEX IF NOT EXISTS idx_travel_hub_vectors_embedding
    ON travel_hub_vectors USING hnsw (embedding vector_cosine_ops);
