-- ============================================================
-- PDF 多模态 RAG 初始化脚本
-- 依赖：先执行 init_pgvector.sql（安装 vector 扩展）
-- ============================================================

-- ------------------------------------------------------------
-- 1. 文档元数据表（PDF 原件登记 + 解析状态）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pdf_document (
    id          TEXT PRIMARY KEY,       -- docId（UUID）
    file_name   TEXT NOT NULL,          -- 原始文件名
    file_path   TEXT,                   -- 原件存储路径（本地磁盘，未来可换 OSS）
    page_count  INT,                    -- 页数
    chunk_count INT,                    -- 文本分块数
    image_count INT,                    -- 页面图片数（多模态向量）
    status      TEXT,                   -- PARSING / READY / FAILED
    error_msg   TEXT,                   -- 失败原因
    created_at  TIMESTAMP DEFAULT now()
);

-- ------------------------------------------------------------
-- 2. 页面图片向量表（多模态跨模态检索）
--    模型：qwen3-vl-embedding（1536 维，与文本表同维度，DDL/索引写法统一）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS travel_pdf_images (
    id        TEXT PRIMARY KEY,         -- {docId}_p{page}
    content   TEXT,                     -- 页面图文描述（VLM 生成，图片的"文本侧身"）
    metadata  JSONB,                    -- docId / fileName / page / imagePath 等
    embedding VECTOR(1536)              -- qwen3-vl-embedding 输出维度
);

CREATE INDEX IF NOT EXISTS idx_travel_pdf_images_embedding
    ON travel_pdf_images USING hnsw (embedding vector_cosine_ops);

-- ------------------------------------------------------------
-- 说明：PDF 文本分块复用 travel_hub_vectors 表（1536 维），
--       靠 metadata.type 隔离：
--         type = 'tool_knowledge' → 工具知识库（LocalToolRagChannel）
--         type = 'pdf_knowledge'  → PDF 分块（PdfRagChannel）
--         type = 'user_preference'→ 用户长期偏好（LongTermMemory）
--       检索时用 filterExpression 过滤，互不污染。
-- ------------------------------------------------------------
