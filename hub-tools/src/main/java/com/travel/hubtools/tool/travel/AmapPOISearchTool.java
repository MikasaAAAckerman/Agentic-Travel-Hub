package com.travel.hubtools.tool.travel;

import com.travel.hubtools.client.TavilyApiClient;
import com.travel.hubtools.tool.common.IAgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * POI搜索工具（全部走 Tavily 搜索）
 */
@Slf4j
@Component
public class AmapPOISearchTool implements IAgentTool {

    @Autowired
    private TavilyApiClient tavilyApiClient;

    @Tool(name = "poiKeywordSearchTool", description = "根据关键词搜索指定地点的详细信息，包括名称、地址、电话、评分等喵！")
    public String poiKeywordSearchTool(@ToolParam(description = "搜索关键词") String keywords, @ToolParam(description = "查询城市") String city) {
        log.info("开始进入方法poiKeywordSearchTool，参数为 keywords -> {}, city -> {}", keywords, city);
        String query = String.format("%s %s 地址 电话 评分", city, keywords);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "poiAroundSearchTool", description = "搜索指定坐标周边的POI信息，如餐厅、酒店、景点等喵！")
    public String poiAroundSearchTool(@ToolParam(description = "搜索关键词") String keywords, @ToolParam(description = "中心位置") String location, @ToolParam(description = "搜索半径（米）") Integer radius) {
        log.info("开始进入方法poiAroundSearchTool，参数为 keywords -> {}, location -> {}, radius -> {}", keywords, location, radius);
        String query = String.format("%s附近 %s 推荐", location, keywords);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "weatherQueryTool", description = "查询指定城市的实时天气信息，包括温度、湿度、风力等喵！")
    public String weatherQueryTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法weatherQueryTool，参数为 city -> {}", city);
        String query = String.format("%s 实时天气 温度 湿度 风力", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "poiDetailSearchTool", description = "查询指定POI的详细信息，包括地址、电话、评分、营业时间等喵！")
    public String poiDetailSearchTool(@ToolParam(description = "POI名称") String poiName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法poiDetailSearchTool，参数为 poiName -> {}, city -> {}", poiName, city);
        String query = String.format("%s %s 地址 电话 评分 营业时间", city, poiName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "keywordAutocompleteTool", description = "根据输入的关键词提供自动补全建议喵！")
    public String keywordAutocompleteTool(@ToolParam(description = "搜索关键词") String keywords, @ToolParam(description = "查询城市") String city) {
        log.info("开始进入方法keywordAutocompleteTool，参数为 keywords -> {}, city -> {}", keywords, city);
        String query = String.format("%s %s 推荐 搜索", city, keywords);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "districtQueryTool", description = "查询指定城市的行政区划信息喵！")
    public String districtQueryTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法districtQueryTool，参数为 city -> {}", city);
        String query = String.format("%s 行政区划 区县", city);
        return tavilyApiClient.searchAsText(query, 3);
    }
}
