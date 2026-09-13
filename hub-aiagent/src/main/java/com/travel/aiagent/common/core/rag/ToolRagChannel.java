package com.travel.aiagent.common.core.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import java.util.List;

/**
 * Tool RAG 召回策略接口
 * 支持多种召回通道：百炼云端、本地多路召回等
 */
public interface ToolRagChannel {

    /**
     * 根据查询召回相关文档
     *
     * @param query 查询内容
     * @return 召回的文档列表
     */
    List<Document> retrieve(Query query);

    /**
     * 获取通道名称
     *
     * @return 通道标识
     */
    String getChannelName();
}
