package com.travel.hubtools.tool.search;

import com.travel.hubtools.client.TavilyApiClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TavilySearchTool {

    @Resource
    private TavilyApiClient tavilyApiClient;

    @Tool(description = "互联网实时搜索工具。当用户询问最新资讯、实时信息、2025年及以后的事件、或任何超出知识库范围的问题时，使用此工具搜索互联网获取最新信息。")
    public String webSearchTool(
            @ToolParam(description = "搜索关键词，尽量简洁精准") String query) {
        log.info("[TavilySearchTool] webSearchTool | query={}", query);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(description = "旅游攻略搜索工具。当用户询问目的地攻略、景点推荐、美食推荐、旅行经验分享等旅游相关信息时，使用此工具搜索互联网获取最新攻略。")
    public String travelGuideSearchTool(
            @ToolParam(description = "搜索关键词，如'广州小众景点推荐'、'大连美食攻略'") String query) {
        log.info("[TavilySearchTool] travelGuideSearchTool | query={}", query);
        String enrichedQuery = "旅游攻略 " + query;
        return tavilyApiClient.searchAsText(enrichedQuery, 5);
    }

    @Tool(description = "新闻资讯搜索工具。当用户询问最新新闻、近期活动、展会、演唱会、节庆等时效性信息时，使用此工具搜索。")
    public String newsSearchTool(
            @ToolParam(description = "搜索关键词，如'广州2026年6月活动'") String query) {
        log.info("[TavilySearchTool] newsSearchTool | query={}", query);
        return tavilyApiClient.searchAsText(query, 3);
    }
}
