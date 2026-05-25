package com.travel.hubtools.tool.travel;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.travel.hubtools.client.MapApiClient;
import com.travel.hubtools.tool.common.IAgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 高德地图POI搜索工具
 * 提供地点搜索、周边搜索等功能
 */
@Slf4j
@Component
public class AmapPOISearchTool implements IAgentTool {

    @Autowired
    private MapApiClient mapApiClient;

    /**
     * 地点关键字搜索工具
     *
     * @param keywords 搜索关键词（如：肯德基、朝阳公园、酒店）
     * @param city     查询城市（如：北京、上海）
     * @return String (搜索结果列表)
     * @Description 根据关键词搜索指定地点的详细信息，包括名称、地址、电话、评分等喵！
     */
    @Tool(name = "poiKeywordSearchTool", description = "根据关键词搜索指定地点的详细信息，包括名称、地址、电话、评分等喵！")
    public String poiKeywordSearchTool(@ToolParam(description = "搜索关键词") String keywords,
                                       @ToolParam(description = "查询城市") String city) {
        log.info("开始进入方法poiKeywordSearchTool，参数为 keywords -> {}, city -> {}", keywords, city);

        try {
            // 调用高德地图POI搜索API
            JSONObject result = mapApiClient.searchPOI(keywords, city, null, 10, 1);

            if (result == null) {
                return "抱歉喵，地点搜索失败，请稍后再试喵~";
            }

            // 解析结果
            String count = result.getString("count");
            JSONArray pois = result.getJSONArray("pois");

            if (pois == null || pois.isEmpty()) {
                return "抱歉喵，没有找到相关的地点信息喵~";
            }

            // 构建搜索结果列表
            StringBuilder response = new StringBuilder();
            response.append(String.format("找到 %s 个相关地点喵！\n\n", count));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                JSONObject poi = pois.getJSONObject(i);
                String name = poi.getString("name");
                String address = poi.getString("address");
                String tel = poi.getString("tel");
                String type = poi.getString("type");

                response.append(String.format("%d. 【%s】\n", i + 1, name));
                response.append(String.format("   类型：%s\n", type));
                response.append(String.format("   地址：%s\n", address != null ? address : "未知"));

                if (tel != null && !tel.isEmpty()) {
                    response.append(String.format("   电话：%s\n", tel));
                }

                // 如果有评分信息
                JSONObject bizExt = poi.getJSONObject("biz_ext");
                if (bizExt != null) {
                    String rating = bizExt.getString("rating");
                    String cost = bizExt.getString("cost");
                    if (rating != null && !rating.isEmpty()) {
                        response.append(String.format("   评分：%s 分\n", rating));
                    }
                    if (cost != null && !cost.isEmpty()) {
                        response.append(String.format("   人均消费：%s 元\n", cost));
                    }
                }

                response.append("\n");
            }

            response.append("需要更多详情可以告诉我喵~");

            log.info("工具poiKeywordSearchTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具poiKeywordSearchTool执行异常", e);
            return "抱歉喵，地点搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 周边地点搜索工具
     *
     * @param location 中心点位置（如：北京市朝阳区三里屯）
     * @param keywords 搜索关键词（如：餐厅、咖啡厅）
     * @param radius   搜索半径（米，默认1000米）
     * @return String (周边地点列表)
     * @Description 在指定位置附近搜索相关地点，支持设置搜索半径喵！
     */
    @Tool(name = "poiAroundSearchTool", description = "在指定位置附近搜索相关地点，支持设置搜索半径喵！")
    public String poiAroundSearchTool(@ToolParam(description = "中心点位置") String location,
                                      @ToolParam(description = "搜索关键词") String keywords,
                                      @ToolParam(description = "搜索半径（米）") Integer radius,
                                      @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法poiAroundSearchTool，参数为 location -> {}, keywords -> {}, radius -> {}, city -> {}", location, keywords, radius, city);

        try {
            // 将地址转换为经纬度
            String coord = mapApiClient.geocode(location, city);

            if (coord == null) {
                return "抱歉喵，无法找到位置的坐标信息，请检查地址是否正确喵~";
            }

            // 调用高德地图POI周边搜索API
            JSONObject result = mapApiClient.searchPOIAround(keywords, coord, radius, null);

            if (result == null) {
                return "抱歉喵，周边搜索失败，请稍后再试喵~";
            }

            // 解析结果
            String count = result.getString("count");
            JSONArray pois = result.getJSONArray("pois");

            if (pois == null || pois.isEmpty()) {
                return "抱歉喵，附近没有找到相关的地点喵~";
            }

            // 构建搜索结果列表
            StringBuilder response = new StringBuilder();
            response.append(String.format("附近找到 %s 个相关地点喵！\n\n", count));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                JSONObject poi = pois.getJSONObject(i);
                String name = poi.getString("name");
                String address = poi.getString("address");
                String distance = poi.getString("distance"); // 距离中心点的距离
                String tel = poi.getString("tel");

                response.append(String.format("%d. 【%s】\n", i + 1, name));
                response.append(String.format("   距离：%s 米\n", distance));
                response.append(String.format("   地址：%s\n", address != null ? address : "未知"));

                if (tel != null && !tel.isEmpty()) {
                    response.append(String.format("   电话：%s\n", tel));
                }

                response.append("\n");
            }

            response.append("需要导航可以告诉我喵~");

            log.info("工具poiAroundSearchTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具poiAroundSearchTool执行异常", e);
            return "抱歉喵，周边搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 天气查询工具
     *
     * @param city 城市名称（如：北京、上海）
     * @return String (天气信息)
     * @Description 查询指定城市的实时天气和未来天气预报喵！
     */
    @Tool(name = "weatherQueryTool", description = "查询指定城市的实时天气和未来天气预报喵！")
    public String weatherQueryTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法weatherQueryTool，参数为 city -> {}", city);

        try {
            // 需要将城市名转换为adcode，这里简化处理，直接使用城市名
            // 实际应用中应该先查询城市的adcode
            String adcode = getCityAdcode(city);

            if (adcode == null) {
                return "抱歉喵，无法找到城市的编码，请检查城市名称是否正确喵~";
            }

            // 调用高德地图天气查询API
            JSONObject result = mapApiClient.weatherInfo(adcode);

            if (result == null) {
                return "抱歉喵，天气查询失败，请稍后再试喵~";
            }

            // 解析结果
            JSONArray lives = result.getJSONArray("lives");

            if (lives == null || lives.isEmpty()) {
                return "抱歉喵，没有获取到天气信息喵~";
            }

            JSONObject live = lives.getJSONObject(0);
            String province = live.getString("province");
            String cityName = live.getString("city");
            String weather = live.getString("weather");
            String temperature = live.getString("temperature");
            String windDirection = live.getString("winddirection");
            String windPower = live.getString("windpower");
            String humidity = live.getString("humidity");

            String response = String.format(
                    "【%s %s】天气喵！\n" +
                            "天气状况：%s\n" +
                            "当前温度：%s℃\n" +
                            "风向：%s\n" +
                            "风力：%s级\n" +
                            "湿度：%s%%\n\n" +
                            "出门记得看天气喵~",
                    province, cityName, weather, temperature,
                    windDirection, windPower, humidity
            );

            log.info("工具weatherQueryTool执行完成");
            return response;

        } catch (Exception e) {
            log.error("工具weatherQueryTool执行异常", e);
            return "抱歉喵，天气查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * POI详情查询工具
     *
     * @param poiId POI ID（从搜索结果中获取）
     * @return String (POI详细信息)
     * @Description 根据POI ID查询地点的详细信息，包括营业时间、标签、深度评分等喵！
     */
    @Tool(name = "poiDetailSearchTool", description = "根据POI ID查询地点的详细信息，包括营业时间、标签、深度评分等喵！")
    public String poiDetailSearchTool(@ToolParam(description = "POI ID") String poiId) {
        log.info("开始进入方法poiDetailSearchTool，参数为 poiId -> {}", poiId);

        try {
            JSONObject result = mapApiClient.searchPOIDetail(poiId);

            if (result == null) {
                return "抱歉喵，POI详情查询失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            if (pois == null || pois.isEmpty()) {
                return "抱歉喵，没有找到该POI的详细信息喵~";
            }

            JSONObject poi = pois.getJSONObject(0);
            String name = poi.getString("name");
            String address = poi.getString("address");
            String tel = poi.getString("tel");
            String type = poi.getString("type");
            String businessArea = poi.getString("business_area");
            String email = poi.getString("email");

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】详细信息喵！\n\n", name));
            response.append(String.format("类型：%s\n", type != null ? type : "未知"));
            response.append(String.format("地址：%s\n", address != null ? address : "未知"));
            if (businessArea != null && !businessArea.isEmpty()) {
                response.append(String.format("商圈：%s\n", businessArea));
            }
            if (tel != null && !tel.isEmpty()) {
                response.append(String.format("电话：%s\n", tel));
            }
            if (email != null && !email.isEmpty()) {
                response.append(String.format("邮箱：%s\n", email));
            }

            JSONObject bizExt = poi.getJSONObject("biz_ext");
            if (bizExt != null) {
                String rating = bizExt.getString("rating");
                String cost = bizExt.getString("cost");
                String openTime = bizExt.getString("opentime");
                String mealOra = bizExt.getString("meal_ordering");
                if (rating != null) response.append(String.format("评分：%s 分\n", rating));
                if (cost != null) response.append(String.format("人均消费：%s 元\n", cost));
                if (openTime != null) response.append(String.format("营业时间：%s\n", openTime));
                if (mealOra != null) response.append(String.format("订餐信息：%s\n", mealOra));
            }

            response.append("需要导航可以告诉我喵~");
            log.info("工具poiDetailSearchTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具poiDetailSearchTool执行异常", e);
            return "抱歉喵，POI详情查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 关键词输入提示工具
     *
     * @param keywords 搜索关键词
     * @param city     查询城市
     * @return String (自动补全建议列表)
     * @Description 输入地点关键词时提供智能补全建议，帮助快速定位目标地点喵！
     */
    @Tool(name = "keywordAutocompleteTool", description = "输入地点关键词时提供智能补全建议，帮助快速定位目标地点喵！")
    public String keywordAutocompleteTool(@ToolParam(description = "搜索关键词") String keywords, @ToolParam(description = "查询城市") String city) {
        log.info("开始进入方法keywordAutocompleteTool，参数为 keywords -> {}, city -> {}", keywords, city);

        try {
            JSONObject result = mapApiClient.inputTips(keywords, city);

            if (result == null) {
                return "抱歉喵，关键词提示失败，请稍后再试喵~";
            }

            JSONArray tips = result.getJSONArray("tips");
            if (tips == null || tips.isEmpty()) {
                return String.format("抱歉喵，没有找到\"%s\"的匹配建议喵~", keywords);
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("\"%s\"搜索建议喵！\n\n", keywords));

            for (int i = 0; i < Math.min(tips.size(), 10); i++) {
                JSONObject tip = tips.getJSONObject(i);
                String name = tip.getString("name");
                String district = tip.getString("district");
                String address = tip.getString("address");

                response.append(String.format("%d. %s\n", i + 1, name));
                if (district != null && !district.isEmpty()) {
                    response.append(String.format("   区域：%s\n", district));
                }
                if (address != null && !address.isEmpty()) {
                    response.append(String.format("   地址：%s\n", address));
                }
                response.append("\n");
            }

            response.append("选一个告诉我，帮你查详情喵~");
            log.info("工具keywordAutocompleteTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具keywordAutocompleteTool执行异常", e);
            return "抱歉喵，关键词提示过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 行政区划查询工具
     *
     * @param keywords    城市或区域名称
     * @param subdistrict 显示下级行政区级数（0-3）
     * @return String (行政区划信息)
     * @Description 查询城市或区域的行政区划信息，包括adcode、边界、下级区域等喵！
     */
    @Tool(name = "districtQueryTool", description = "查询城市或区域的行政区划信息，包括adcode、边界、下级区域等喵！")
    public String districtQueryTool(@ToolParam(description = "城市或区域名称") String keywords, @ToolParam(description = "显示下级级数（0-3）") Integer subdistrict) {
        log.info("开始进入方法districtQueryTool，参数为 keywords -> {}, subdistrict -> {}", keywords, subdistrict);

        try {
            JSONObject result = mapApiClient.districtQuery(keywords, subdistrict != null ? subdistrict : 1);

            if (result == null) {
                return "抱歉喵，行政区划查询失败，请稍后再试喵~";
            }

            JSONArray districts = result.getJSONArray("districts");
            if (districts == null || districts.isEmpty()) {
                return String.format("抱歉喵，没有找到\"%s\"的行政区划信息喵~", keywords);
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】行政区划查询结果喵！\n\n", keywords));
            formatDistrict(districts, response, 0);

            log.info("工具districtQueryTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具districtQueryTool执行异常", e);
            return "抱歉喵，行政区划查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 递归格式化行政区划结果
     */
    private void formatDistrict(JSONArray districts, StringBuilder sb, int depth) {
        String indent = "  ".repeat(depth);
        for (int i = 0; i < districts.size(); i++) {
            JSONObject district = districts.getJSONObject(i);
            String name = district.getString("name");
            String adcode = district.getString("adcode");
            String level = district.getString("level");
            String center = district.getString("center");

            sb.append(String.format("%s- %s（%s）\n", indent, name, level));
            sb.append(String.format("%s  adcode：%s\n", indent, adcode));
            if (center != null && !center.isEmpty()) {
                sb.append(String.format("%s  中心：%s\n", indent, center));
            }

            JSONArray subDistricts = district.getJSONArray("districts");
            if (subDistricts != null && !subDistricts.isEmpty()) {
                formatDistrict(subDistricts, sb, depth + 1);
            }
        }
    }

    /**
     * 获取城市adcode（使用行政区划API动态查询）
     */
    private String getCityAdcode(String city) {
        // 使用行政区划API动态获取adcode
        JSONObject result = mapApiClient.districtQuery(city, 0);
        if (result != null) {
            JSONArray districts = result.getJSONArray("districts");
            if (districts != null && !districts.isEmpty()) {
                String adcode = districts.getJSONObject(0).getString("adcode");
                if (adcode != null && !adcode.isEmpty()) {
                    return adcode;
                }
            }
        }
        // 兜底：使用地理编码获取
        String coord = mapApiClient.geocode(city, city);
        if (coord != null) {
            return null;
        }
        return null;
    }
}
