package com.travel.hubtools.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 对接 Tavily Search API 的 HTTP Client
 * 提供实时互联网搜索能力，弥补 LLM 知识库时效性不足的问题
 */
@Slf4j
@Component
public class TavilyApiClient {

    @Value("${tavily-api-secret:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String TAVILY_BASE_URL = "https://api.tavily.com";

    /**
     * 互联网搜索
     *
     * @param query         搜索关键词（必填）
     * @param searchDepth   搜索深度：basic（快速）/ advanced（深度）
     * @param maxResults    最大结果数（默认5）
     * @param topic         搜索主题：general / news
     * @param includeAnswer 是否包含 AI 摘要答案
     * @return 搜索结果 JSON，包含 results[]、answer、response_time 等字段
     */
    public JSONObject search(String query, String searchDepth, Integer maxResults,
                             String topic, boolean includeAnswer) {
        log.info("[TavilyApiClient] 开始搜索 | query={}, depth={}, maxResults={}", query, searchDepth, maxResults);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("query", query);
            requestBody.put("search_depth", searchDepth != null ? searchDepth : "basic");
            requestBody.put("max_results", maxResults != null ? maxResults : 5);
            requestBody.put("topic", topic != null ? topic : "general");
            requestBody.put("include_answer", includeAnswer);
            requestBody.put("include_raw_content", false);
            requestBody.put("include_images", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    TAVILY_BASE_URL + "/search",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JSONObject result = JSON.parseObject(response.getBody());
            log.info("[TavilyApiClient] 搜索成功 | results={}, responseTime={}",
                    result.getJSONArray("results") != null ? result.getJSONArray("results").size() : 0,
                    result.getString("response_time"));
            return result;

        } catch (Exception e) {
            log.error("[TavilyApiClient] 搜索异常 | query={}", query, e);
            return null;
        }
    }

    /**
     * 简化版搜索：只传 query，使用默认参数
     */
    public JSONObject search(String query) {
        return search(query, "basic", 5, "general", true);
    }

    /**
     * 搜索并返回格式化的文本结果（适合直接注入 prompt）
     *
     * @param query      搜索关键词
     * @param maxResults 最大结果数
     * @return 格式化的搜索结果文本
     */
    public String searchAsText(String query, Integer maxResults) {
        JSONObject result = search(query, "basic", maxResults != null ? maxResults : 3, "general", true);
        if (result == null) {
            return "搜索失败，未能获取到结果";
        }

        StringBuilder sb = new StringBuilder();

        // AI 摘要
        String answer = result.getString("answer");
        if (answer != null && !answer.isEmpty()) {
            sb.append("【摘要】").append(answer).append("\n\n");
        }

        // 搜索结果
        JSONArray results = result.getJSONArray("results");
        if (results != null && !results.isEmpty()) {
            sb.append("【搜索结果】\n");
            for (int i = 0; i < results.size(); i++) {
                JSONObject item = results.getJSONObject(i);
                sb.append(String.format("%d. %s\n   %s\n   %s\n\n",
                        i + 1,
                        item.getString("title"),
                        item.getString("url"),
                        item.getString("content")));
            }
        }

        return sb.toString();
    }
}
