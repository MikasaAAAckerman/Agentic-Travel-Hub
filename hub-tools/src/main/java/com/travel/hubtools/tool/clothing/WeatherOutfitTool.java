package com.travel.hubtools.tool.clothing;

import com.travel.hubtools.client.TavilyApiClient;
import com.travel.hubtools.tool.common.IAgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 天气穿搭工具（全部走 Tavily 搜索）
 */
@Slf4j
@Component
public class WeatherOutfitTool implements IAgentTool {

    @Autowired
    private TavilyApiClient tavilyApiClient;

    @Tool(name = "realTimeOutfitTool", description = "根据当前天气提供穿衣建议的工具喵！")
    public String realTimeOutfitTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法realTimeOutfitTool，参数为 city -> {}", city);
        String query = String.format("%s 今天天气 穿衣建议 温度", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "uvIndexProtectionTool", description = "防晒必备！查询紫外线指数并推荐防晒措施。")
    public String uvIndexProtectionTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法uvIndexProtectionTool，参数为 city -> {}", city);
        String query = String.format("%s 紫外线指数 防晒 防晒霜", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "tempDifferenceLayeringTool", description = "早晚温差大时的穿衣建议工具喵！")
    public String tempDifferenceLayeringTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法tempDifferenceLayeringTool，参数为 city -> {}", city);
        String query = String.format("%s 早晚温差 穿衣 洋葱式穿搭", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "rainGearSuggestionTool", description = "下雨天穿什么？查询降雨概率并推荐雨具和防水穿搭。")
    public String rainGearSuggestionTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法rainGearSuggestionTool，参数为 city -> {}", city);
        String query = String.format("%s 降雨概率 雨具 防水穿搭 雨伞", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "extremeWeatherOutfitTool", description = "极端天气（高温、寒潮、沙尘暴）的防护穿搭建议。")
    public String extremeWeatherOutfitTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法extremeWeatherOutfitTool，参数为 city -> {}", city);
        String query = String.format("%s 极端天气 高温 寒潮 防护穿搭", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "humidityFabricSuggestionTool", description = "潮湿天气穿什么面料不闷热？")
    public String humidityFabricSuggestionTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法humidityFabricSuggestionTool，参数为 city -> {}", city);
        String query = String.format("%s 湿度 面料 透气 速干 穿衣", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "windChillOutfitTool", description = "大风天体感温度更低，如何防风保暖？")
    public String windChillOutfitTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法windChillOutfitTool，参数为 city -> {}", city);
        String query = String.format("%s 风力 体感温度 防风 保暖", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "outdoorSportsOutfitTool", description = "户外运动穿搭建议（跑步、登山、骑行等）。")
    public String outdoorSportsOutfitTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "运动类型") String sportType) {
        log.info("开始进入方法outdoorSportsOutfitTool，参数为 city -> {}, sportType -> {}", city, sportType);
        String query = String.format("%s %s 户外运动穿搭 装备", city, sportType);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "laundryDryingIndexTool", description = "今天适合洗衣服吗？查询晾晒指数。")
    public String laundryDryingIndexTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法laundryDryingIndexTool，参数为 city -> {}", city);
        String query = String.format("%s 晾晒指数 洗衣服 天气", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "sweatIndexOutfitTool", description = "容易出汗的人穿什么最舒适？")
    public String sweatIndexOutfitTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法sweatIndexOutfitTool，参数为 city -> {}", city);
        String query = String.format("%s 出汗 透气 速干 面料 穿衣", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "nightwearSuggestionTool", description = "晚上睡觉穿什么？根据夜间温度推荐睡衣。")
    public String nightwearSuggestionTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法nightwearSuggestionTool，参数为 city -> {}", city);
        String query = String.format("%s 夜间温度 睡衣 睡眠", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "businessTravelPackingTool", description = "出差行李清单：根据天气和行程天数推荐携带衣物。")
    public String businessTravelPackingTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "行程天数") int days) {
        log.info("开始进入方法businessTravelPackingTool，参数为 city -> {}, days -> {}", city, days);
        String query = String.format("%s %d天 出差行李清单 衣物 天气", city, days);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "aqiPollenProtectionTool", description = "空气质量差或花粉季的防护建议。")
    public String aqiPollenProtectionTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法aqiPollenProtectionTool，参数为 city -> {}", city);
        String query = String.format("%s 空气质量 花粉 口罩 防护", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "culturalDressCodeTool", description = "参观寺庙、教堂等场所的着装要求。")
    public String culturalDressCodeTool(@ToolParam(description = "目的地") String destination) {
        log.info("开始进入方法culturalDressCodeTool，参数为 destination -> {}", destination);
        String query = String.format("%s 着装要求 寺庙 教堂 文化礼仪", destination);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "bugActivityProtectionTool", description = "蚊虫多的季节/地区如何防蚊穿搭？")
    public String bugActivityProtectionTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法bugActivityProtectionTool，参数为 city -> {}", city);
        String query = String.format("%s 蚊虫 防蚊 长袖 驱蚊", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "businessSuitSurvivalTool", description = "夏天穿西装不闷热的秘诀。")
    public String businessSuitSurvivalTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法businessSuitSurvivalTool，参数为 city -> {}", city);
        String query = String.format("%s 夏天西装 轻薄面料 职业装 透气", city);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "terrainFootwearTool", description = "根据地形（山地、沙滩、石板路）推荐鞋子。")
    public String terrainFootwearTool(@ToolParam(description = "目的地") String destination, @ToolParam(description = "地形类型") String terrain) {
        log.info("开始进入方法terrainFootwearTool，参数为 destination -> {}, terrain -> {}", destination, terrain);
        String query = String.format("%s %s 鞋子 登山鞋 徒步鞋", destination, terrain);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "indoorClimateOutfitTool", description = "室内空调太冷/太热怎么穿？")
    public String indoorClimateOutfitTool(@ToolParam(description = "场景") String scenario) {
        log.info("开始进入方法indoorClimateOutfitTool，参数为 scenario -> {}", scenario);
        String query = String.format("%s 空调 穿衣 室内温度", scenario);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "luggageSpaceOutfitTool", description = "行李箱空间有限，如何精简穿搭？")
    public String luggageSpaceOutfitTool(@ToolParam(description = "行程天数") int days) {
        log.info("开始进入方法luggageSpaceOutfitTool，参数为 days -> {}", days);
        String query = String.format("%d天旅行 精简穿搭 行李箱 百搭", days);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "scenicColorOutfitTool", description = "拍照穿什么颜色最上镜？根据景点风格推荐穿搭颜色。")
    public String scenicColorOutfitTool(@ToolParam(description = "景点名称") String scenicSpot) {
        log.info("开始进入方法scenicColorOutfitTool，参数为 scenicSpot -> {}", scenicSpot);
        String query = String.format("%s 拍照穿搭 颜色 上镜 拍照", scenicSpot);
        return tavilyApiClient.searchAsText(query, 3);
    }
}
