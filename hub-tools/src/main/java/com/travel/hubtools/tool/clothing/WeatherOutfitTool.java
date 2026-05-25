package com.travel.hubtools.tool.clothing;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.travel.hubtools.client.MapApiClient;
import com.travel.hubtools.tool.common.IAgentTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 查天气并建议穿戴工具
 */
@Slf4j
@Component
public class WeatherOutfitTool implements IAgentTool {

    @Resource
    private ChatModel dashScopeChatModel;

    @Resource
    private MapApiClient mapApiClient;

    /**
     * 基础实时气温与穿衣指数工具
     *
     * @param city 城市名称
     * @return String (包含气温、风力及对应的如"建议穿长袖加薄外套"等基础穿衣指数)
     * @Description 这是一个根据指定城市的当前实时气温、体感温度，提供最基础穿衣层数和衣物厚度建议的工具喵。用于回答"今天北京冷不冷，该穿几件"的问题。
     */
    @Tool(name = "realTimeOutfitTool", description = "这是一个根据指定城市的当前实时气温、体感温度，提供最基础穿衣层数和衣物厚度建议的工具喵。用于回答\"今天北京冷不冷，该穿几件\"的问题。")
    public String realTimeOutfitTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法realTimeOutfitTool，参数为 city -> {}", city);

        try {
            // 需要将城市名转换为adcode，这里简化处理，直接使用城市名
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

            // 根据天气和温度给出穿衣建议
            String outfitAdvice = generateOutfitAdvice(temperature, weather, windPower);

            String response = String.format(
                    "【%s %s】实时天气喵！\n" +
                            "天气状况：%s\n" +
                            "当前温度：%s℃\n" +
                            "风向：%s\n" +
                            "风力：%s级\n" +
                            "湿度：%s%%\n\n" +
                            "穿衣建议：%s喵~",
                    province, cityName, weather, temperature,
                    windDirection, windPower, humidity, outfitAdvice
            );

            log.info("工具realTimeOutfitTool执行完成");
            return response;

        } catch (Exception e) {
            log.error("工具realTimeOutfitTool执行异常", e);
            return "抱歉喵，天气查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 根据温度、天气和风力生成穿衣建议
     */
    private String generateOutfitAdvice(String temperature, String weather, String windPower) {
        int temp = Integer.parseInt(temperature);
        int wind = Integer.parseInt(windPower);

        StringBuilder advice = new StringBuilder();

        // 根据温度给出基础建议
        if (temp >= 30) {
            advice.append("天气炎热，建议穿短袖T恤、短裤、裙子等夏季服装，注意防晒");
        } else if (temp >= 25) {
            advice.append("天气较热，建议穿短袖、薄衬衫等透气服装");
        } else if (temp >= 20) {
            advice.append("温度适宜，建议穿长袖T恤、薄外套或衬衫");
        } else if (temp >= 15) {
            advice.append("稍有凉意，建议穿长袖加薄外套或针织衫");
        } else if (temp >= 10) {
            advice.append("天气较凉，建议穿毛衣、夹克或风衣");
        } else if (temp >= 5) {
            advice.append("天气寒冷，建议穿厚毛衣、棉服或薄羽绒服");
        } else {
            advice.append("天气非常寒冷，建议穿厚羽绒服、棉衣，戴帽子和手套");
        }

        // 根据天气补充建议
        if (weather.contains("雨")) {
            advice.append("，记得带雨伞，建议穿防水鞋");
        } else if (weather.contains("雪")) {
            advice.append("，注意防滑，建议穿防滑雪地靴");
        }

        // 根据风力补充建议
        if (wind >= 5) {
            advice.append("，风力较大，建议穿防风外套，戴帽子");
        }

        return advice.toString();
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

    /**
     * 紫外线防御穿搭工具
     *
     * @param city 城市名称
     * @param date 日期
     * @return String (紫外线等级，以及对应的防晒装备建议)
     * @Description 专门用于查询特定地区紫外线（UV）指数，并据此推荐防晒衣、墨镜、遮阳伞等物理防晒装备的工具喵。当用户问及"去三亚要带什么防晒装备"时调用。
     */
    @Tool(name = "uvIndexProtectionTool", description = "专门用于查询特定地区紫外线（UV）指数，并据此推荐防晒衣、墨镜、遮阳伞等物理防晒装备的工具喵。当用户问及\"去三亚要带什么防晒装备\"时调用。")
    public String uvIndexProtectionTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "日期") String date) {
        log.info("开始进入方法uvIndexProtectionTool，参数为 city -> {}, date -> {}", city, date);

        try {
            String adcode = getCityAdcode(city);
            if (adcode == null) {
                return "抱歉喵，无法找到城市的编码，请检查城市名称是否正确喵~";
            }

            JSONObject result = mapApiClient.weatherInfo(adcode);
            if (result == null) {
                return "抱歉喵，天气查询失败，请稍后再试喵~";
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s紫外线防护指南喵！\n\n", city, date));

            // 获取预报数据
            JSONArray forecasts = result.getJSONArray("forecasts");
            if (forecasts != null && !forecasts.isEmpty()) {
                JSONObject forecast = forecasts.getJSONObject(0);
                JSONArray casts = forecast.getJSONArray("casts");
                if (casts != null && !casts.isEmpty()) {
                    for (int i = 0; i < Math.min(casts.size(), 3); i++) {
                        JSONObject cast = casts.getJSONObject(i);
                        String dayWeather = cast.getString("dayweather");
                        String dayTemp = cast.getString("daytemp");
                        String nightTemp = cast.getString("nighttemp");
                        String dateStr = cast.getString("date");

                        response.append(String.format("%s：白天%s %s℃ / 夜间%s℃\n", dateStr, dayWeather, dayTemp, nightTemp));

                        // 根据天气推断紫外线强度
                        if (dayWeather.contains("晴") || dayWeather.contains("多云")) {
                            int highTemp = Integer.parseInt(dayTemp);
                            if (highTemp >= 30) {
                                response.append("   ☀ 紫外线极强！必备SPF50+防晒霜、遮阳伞、墨镜、防晒衣喵~\n");
                            } else if (highTemp >= 20) {
                                response.append("   ☀ 紫外线较强，建议涂防晒霜、戴墨镜和遮阳帽喵~\n");
                            } else {
                                response.append("   紫外线中等，日常防晒即可喵~\n");
                            }
                        } else {
                            response.append("   紫外线较弱，无需特殊防晒喵~\n");
                        }
                    }
                }
            } else {
                response.append("暂未获取到紫外线预报数据喵~\n");
                response.append("夏季晴天建议常备防晒装备，阴雨天则可省略喵~");
            }

            log.info("工具uvIndexProtectionTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具uvIndexProtectionTool执行异常", e);
            return "抱歉喵，紫外线查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 昼夜温差叠穿策略工具
     *
     * @param city 城市名称
     * @param date 日期
     * @return String (温差数值，以及早中晚的衣物增减建议)
     * @Description 查询目的地当日最高温与最低温的差值，并提供"洋葱式叠穿法"等具体搭配建议的工具喵。用于解决早晚冷、中午热的穿衣尴尬。
     */
    @Tool(name = "tempDifferenceLayeringTool", description = "查询目的地当日最高温与最低温的差值，并提供\"洋葱式叠穿法\"等具体搭配建议的工具喵。用于解决早晚冷、中午热的穿衣尴尬。")
    public String tempDifferenceLayeringTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "日期") String date) {
        log.info("开始进入方法tempDifferenceLayeringTool，参数为 city -> {}, date -> {}", city, date);

        try {
            String adcode = getCityAdcode(city);
            if (adcode == null) {
                return "抱歉喵，无法找到城市的编码，请检查城市名称是否正确喵~";
            }

            JSONObject result = mapApiClient.weatherInfo(adcode);
            if (result == null) {
                return "抱歉喵，天气查询失败，请稍后再试喵~";
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s昼夜温差叠穿指南喵！\n\n", city, date));

            JSONArray forecasts = result.getJSONArray("forecasts");
            if (forecasts != null && !forecasts.isEmpty()) {
                JSONObject forecast = forecasts.getJSONObject(0);
                JSONArray casts = forecast.getJSONArray("casts");
                if (casts != null && !casts.isEmpty()) {
                    for (int i = 0; i < Math.min(casts.size(), 3); i++) {
                        JSONObject cast = casts.getJSONObject(i);
                        int dayTemp = Integer.parseInt(cast.getString("daytemp"));
                        int nightTemp = Integer.parseInt(cast.getString("nighttemp"));
                        int diff = dayTemp - nightTemp;
                        String dateStr = cast.getString("date");

                        response.append(String.format("%s：白天%d℃ / 夜间%d℃（温差%d℃）\n", dateStr, dayTemp, nightTemp, diff));

                        if (diff >= 15) {
                            response.append("   温差极大！强烈推荐洋葱式叠穿法：\n");
                            response.append("   内层：短袖/薄长袖T恤（中午单穿）\n");
                            response.append("   中层：针织开衫或薄毛衣（早晚套上）\n");
                            response.append("   外层：风衣或薄羽绒服（清晨/深夜穿）\n");
                            response.append("   随时可脱可穿，灵活应对温度变化喵~\n");
                        } else if (diff >= 10) {
                            response.append("   温差较大，建议洋葱式叠穿：\n");
                            response.append("   内搭长袖T恤，外穿夹克或薄外套，中午可脱掉外套喵~\n");
                        } else if (diff >= 5) {
                            response.append("   温差适中，早晚加一件薄外套即可喵~\n");
                        } else {
                            response.append("   温差较小，全天穿着保持一致即可喵~\n");
                        }
                    }
                }
            } else {
                response.append("暂未获取到温差预报数据喵~\n");
            }

            log.info("工具tempDifferenceLayeringTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具tempDifferenceLayeringTool执行异常", e);
            return "抱歉喵，温差查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 降水概率与雨具鞋履推荐工具
     *
     * @param city 城市名称
     * @param date 日期
     * @return String (降水概率，以及针对性的防水衣物和鞋类建议)
     * @Description 查询目的地的降水概率和降水量，并精确推荐是否需要携带雨伞、雨衣，以及是否应该穿着防水鞋/防滑鞋的工具喵。
     */
    @Tool(name = "rainGearSuggestionTool", description = "查询目的地的降水概率和降水量，并精确推荐是否需要携带雨伞、雨衣，以及是否应该穿着防水鞋/防滑鞋的工具喵。")
    public String rainGearSuggestionTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "日期") String date) {
        log.info("开始进入方法rainGearSuggestionTool，参数为 city -> {}, date -> {}", city, date);

        try {
            String adcode = getCityAdcode(city);
            if (adcode == null) {
                return "抱歉喵，无法找到城市的编码，请检查城市名称是否正确喵~";
            }

            JSONObject result = mapApiClient.weatherInfo(adcode);
            if (result == null) {
                return "抱歉喵，天气查询失败，请稍后再试喵~";
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s雨具建议喵！\n\n", city, date));

            JSONArray forecasts = result.getJSONArray("forecasts");
            if (forecasts != null && !forecasts.isEmpty()) {
                JSONObject forecast = forecasts.getJSONObject(0);
                JSONArray casts = forecast.getJSONArray("casts");
                if (casts != null && !casts.isEmpty()) {
                    for (int i = 0; i < Math.min(casts.size(), 3); i++) {
                        JSONObject cast = casts.getJSONObject(i);
                        String dayWeather = cast.getString("dayweather");
                        String nightWeather = cast.getString("nightweather");
                        String dayPower = cast.getString("daypower");
                        String nightPower = cast.getString("nightpower");
                        String dateStr = cast.getString("date");

                        response.append(String.format("%s：\n", dateStr));
                        response.append(String.format("  白天：%s（%s级风）\n", dayWeather, dayPower));
                        response.append(String.format("  夜间：%s（%s级风）\n", nightWeather, nightPower));

                        boolean dayRain = dayWeather.contains("雨");
                        boolean nightRain = nightWeather.contains("雨");
                        if (dayRain || nightRain) {
                            response.append("  ☔ 有降水！建议携带雨伞或雨衣，穿防水鞋/防滑鞋喵~\n");
                            // 根据风力判断是否适合打伞
                            int maxDp = Integer.parseInt(dayPower.split("-")[1]);
                            int maxNp = Integer.parseInt(nightPower.split("-")[1]);
                            if (Math.max(maxDp, maxNp) >= 5) {
                                response.append("  ⚠ 风力较大，打伞困难，强烈建议穿雨衣代替喵~\n");
                            }
                        } else {
                            response.append("  ☀ 无降水预报，无需携带雨具喵~\n");
                        }
                    }
                }
            } else {
                // 从实时天气判断
                JSONArray lives = result.getJSONArray("lives");
                if (lives != null && !lives.isEmpty()) {
                    JSONObject live = lives.getJSONObject(0);
                    String weather = live.getString("weather");
                    if (weather.contains("雨")) {
                        response.append("当前正在下雨！请务必携带雨具喵~\n");
                    } else {
                        response.append(String.format("当前天气：%s，暂不需雨具喵~\n", weather));
                    }
                }
            }

            log.info("工具rainGearSuggestionTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具rainGearSuggestionTool执行异常", e);
            return "抱歉喵，降水查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 商旅多日打包清单生成工具
     *
     * @param city      目的地
     * @param startDate 出发日
     * @param endDate   返程日
     * @param purpose   出行目的，如"商务"或"度假"
     * @return String (按天数和天气汇总的行李箱衣物装箱清单)
     * @Description 这是一个极其强大的工具！根据用户出差或旅行的起始日期、结束日期和目的地，综合那几天的天气趋势，生成一份包含内衣、外套、正装数量的极简打包清单喵。
     */
    @Tool(name = "businessTravelPackingTool", description = "这是一个极其强大的工具！根据用户出差或旅行的起始日期、结束日期和目的地，综合那几天的天气趋势，生成一份包含内衣、外套、正装数量的极简打包清单喵。")
    public String businessTravelPackingTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "出发日") String startDate, @ToolParam(description = "返程日") String endDate, @ToolParam(description = "出行目的") String purpose) {
        log.info("开始进入方法businessTravelPackingTool，参数为 city -> {}, startDate -> {}, endDate -> {}, purpose -> {}", city, startDate, endDate, purpose);

        String promptText = String.format(
                """
                        请根据以下信息生成旅行打包清单：
                        目的地：%s
                        出发日期：%s
                        返程日期：%s
                        出行目的：%s
                        
                        请提供按天数和天气汇总的行李箱衣物装箱清单，包含内衣、外套、正装数量等。""",
                city, startDate, endDate, purpose
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具businessTravelPackingTool执行完成");
        return result;
    }

    /**
     * 极端灾害天气防御穿搭工具
     *
     * @param city 城市名称
     * @param date 日期
     * @return String (预警级别及对应的硬核防护装备，如冲锋衣、防风镜、防滑雪地靴，或直接强烈建议"取消行程待在室内")
     * @Description 查询目的地是否有台风、暴雪、冰雹或沙尘暴等极端天气预警，并提供极其严肃的防御性穿搭或避险建议喵。当用户计划前往可能发生自然灾害的地区时必须调用！
     */
    @Tool(name = "extremeWeatherOutfitTool", description = "查询目的地是否有台风、暴雪、冰雹或沙尘暴等极端天气预警，并提供极其严肃的防御性穿搭或避险建议喵。当用户计划前往可能发生自然灾害的地区时必须调用！")
    public String extremeWeatherOutfitTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "日期") String date) {
        log.info("开始进入方法extremeWeatherOutfitTool，参数为 city -> {}, date -> {}", city, date);
        try {
            String adcode = getCityAdcode(city);
            if (adcode == null) return "抱歉喵，无法找到城市的编码喵~";

            JSONObject result = mapApiClient.weatherInfo(adcode);
            if (result == null) return "抱歉喵，天气查询失败喵~";

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s极端天气评估喵！\n\n", city, date));

            JSONArray lives = result.getJSONArray("lives");
            if (lives != null && !lives.isEmpty()) {
                JSONObject live = lives.getJSONObject(0);
                String weather = live.getString("weather");
                String windPower = live.getString("windpower");
                int wind = Integer.parseInt(windPower);

                boolean extreme = weather.contains("台风") || weather.contains("暴雪")
                        || weather.contains("冰雹") || weather.contains("沙尘暴") || wind >= 8;
                if (extreme) {
                    response.append(String.format("⚠ 当前天气：%s，风力%s级\n", weather, windPower));
                    response.append("有极端天气风险！强烈建议穿冲锋衣、防风镜、防滑鞋，必要时取消户外行程待在室内喵！\n");
                } else {
                    response.append(String.format("当前天气：%s，风力%s级\n", weather, windPower));
                    response.append("未检测到极端天气预警，正常出行即可喵~\n");
                }
            }

            JSONArray forecasts = result.getJSONArray("forecasts");
            if (forecasts != null && !forecasts.isEmpty()) {
                JSONObject f = forecasts.getJSONObject(0);
                JSONArray casts = f.getJSONArray("casts");
                if (casts != null && !casts.isEmpty()) {
                    JSONObject today = casts.getJSONObject(0);
                    response.append(String.format("今日预报：白天%s %s℃ / 夜间%s %s℃\n",
                            today.getString("dayweather"), today.getString("daytemp"),
                            today.getString("nightweather"), today.getString("nighttemp")));
                }
            }
            response.append("出行前建议查看当地气象局最新预警喵~");
            log.info("工具extremeWeatherOutfitTool执行完毕");
            return response.toString();
        } catch (Exception e) {
            log.error("工具extremeWeatherOutfitTool执行异常", e);
            return "抱歉喵，查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 湿度与体感面料推荐工具
     *
     * @param city 城市名称
     * @return String (比如去广州出差遇到90%湿度的回南天，工具会明确返回"极度闷热潮湿，强烈建议穿着速干、透气的冰丝或棉麻材质，多带备用内衣"；如果是去北方干冷地区，则提示防静电穿搭)
     * @Description 专门查询当地空气相对湿度，并针对"高温高湿（闷热）"或"低温极燥（易起静电）"的特殊环境，推荐最舒适衣物面料的工具喵。
     */
    @Tool(name = "humidityFabricSuggestionTool", description = "专门查询当地空气相对湿度，并针对\"高温高湿（闷热）\"或\"低温极燥（易起静电）\"的特殊环境，推荐最舒适衣物面料的工具喵。")
    public String humidityFabricSuggestionTool(@ToolParam(description = "城市名称") String city) {
        log.info("开始进入方法humidityFabricSuggestionTool，参数为 city -> {}", city);

        try {
            String adcode = getCityAdcode(city);
            if (adcode == null) {
                return "抱歉喵，无法找到城市的编码，请检查城市名称是否正确喵~";
            }

            JSONObject result = mapApiClient.weatherInfo(adcode);
            if (result == null) {
                return "抱歉喵，天气查询失败，请稍后再试喵~";
            }

            JSONArray lives = result.getJSONArray("lives");
            if (lives == null || lives.isEmpty()) {
                return "抱歉喵，没有获取到天气信息喵~";
            }

            JSONObject live = lives.getJSONObject(0);
            String humidity = live.getString("humidity");
            String temperature = live.getString("temperature");
            int temp = Integer.parseInt(temperature);
            int hum = Integer.parseInt(humidity);

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】湿度与面料建议喵！\n\n", city));
            response.append(String.format("当前温度：%s℃ | 湿度：%s%%\n\n", temperature, humidity));

            if (temp >= 25 && hum >= 70) {
                response.append("高温高湿环境！强烈建议穿着速干、透气的冰丝或棉麻材质，多带备用内衣喵~\n");
                response.append("避免纯棉贴身衣物（吸汗不易干），推荐莫代尔、竹纤维等排汗面料喵~");
            } else if (temp < 10 && hum < 40) {
                response.append("低温干燥环境！注意防静电，建议穿纯棉内衣，外搭羊毛或羊绒材质喵~\n");
                response.append("避免化纤面料（易产生静电），随身携带护手霜和润唇膏喵~");
            } else if (hum >= 80) {
                response.append("湿度极高！衣物晾干困难，强烈建议酒店使用烘干机，多备换洗衣物喵~\n");
                response.append("推荐速干面料、防水外套，不建议穿厚重牛仔裤喵~");
            } else {
                response.append(String.format("当前湿度%d%%，体感适中喵~ 正常穿着即可，棉质和混纺面料均可喵~", hum));
            }

            log.info("工具humidityFabricSuggestionTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具humidityFabricSuggestionTool执行异常", e);
            return "抱歉喵，湿度查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 风力风寒指数穿搭工具
     *
     * @param city 城市名称
     * @param date 日期
     * @return String (如遇7级大风，返回"风力极大，建议穿抗风夹克、戴防风帽，绝对禁止穿宽松短裙，且打伞会被吹翻，建议穿雨衣")
     * @Description 专门查询当地风速大小和风寒效应，给出防风保暖细节建议的工具喵。当用户问"海边风大不大，穿裙子会不会走光"或"要不要带伞"时极其有用！
     */
    @Tool(name = "windChillOutfitTool", description = "专门查询当地风速大小和风寒效应，给出防风保暖细节建议的工具喵。当用户问\"海边风大不大，穿裙子会不会走光\"或\"要不要带伞\"时极其有用！")
    public String windChillOutfitTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "日期") String date) {
        log.info("开始进入方法windChillOutfitTool，参数为 city -> {}, date -> {}", city, date);

        try {
            String adcode = getCityAdcode(city);
            if (adcode == null) {
                return "抱歉喵，无法找到城市的编码，请检查城市名称是否正确喵~";
            }

            JSONObject result = mapApiClient.weatherInfo(adcode);
            if (result == null) {
                return "抱歉喵，天气查询失败，请稍后再试喵~";
            }

            JSONArray lives = result.getJSONArray("lives");
            if (lives == null || lives.isEmpty()) {
                return "抱歉喵，没有获取到天气信息喵~";
            }

            JSONObject live = lives.getJSONObject(0);
            String windDirection = live.getString("winddirection");
            String windPower = live.getString("windpower");
            String temperature = live.getString("temperature");
            int wind = Integer.parseInt(windPower);
            int temp = Integer.parseInt(temperature);

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s风力与风寒评估喵！\n\n", city, date));
            response.append(String.format("风向：%s | 风力：%s级 | 温度：%s℃\n\n", windDirection, windPower, temperature));

            if (wind >= 7) {
                response.append("风力极大！强烈建议穿抗风夹克、戴防风帽，绝对禁止穿宽松短裙喵！\n");
                response.append("打伞会被吹翻，建议穿雨衣代替！户外活动需格外小心喵~\n");
            } else if (wind >= 5) {
                response.append("风力较大！建议穿防风外套，戴帽子固定发型，短裙有走光风险喵~\n");
                response.append("撑伞需用力，建议使用防风伞或穿雨衣喵~\n");
            } else if (wind >= 3) {
                response.append(String.format("%d级微风，体感舒适，正常穿着即可，穿裙子也安全喵~\n", wind));
            } else {
                response.append("风力较小或无风，不用担心走光和发型喵~\n");
            }

            // 风寒效应评估
            if (temp < 10 && wind >= 4) {
                response.append(String.format("风寒效应明显！体感温度约%d℃，比实际气温低，注意保暖喵~", temp - wind * 2));
            }

            log.info("工具windChillOutfitTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具windChillOutfitTool执行异常", e);
            return "抱歉喵，风力查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 户外运动专业气象穿搭工具
     *
     * @param city      城市名称
     * @param sportType 运动类型，如：滑雪、高海拔徒步、马拉松
     * @return String (返回专业的"三层穿衣法"指南：排汗层、保暖层、防风外层的具体材质要求及装备清单)
     * @Description 根据当日天气和紫外线情况，为准备在目的地进行徒步、登山、滑雪等特定户外运动的用户，提供极其专业的运动装备建议喵。
     */
    @Tool(name = "outdoorSportsOutfitTool", description = "根据当日天气和紫外线情况，为准备在目的地进行徒步、登山、滑雪等特定户外运动的用户，提供极其专业的运动装备建议喵。")
    public String outdoorSportsOutfitTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "运动类型") String sportType) {
        log.info("开始进入方法outdoorSportsOutfitTool，参数为 city -> {}, sportType -> {}", city, sportType);

        try {
            String adcode = getCityAdcode(city);
            if (adcode == null) return "抱歉喵，无法找到城市的编码喵~";
            JSONObject result = mapApiClient.weatherInfo(adcode);
            if (result == null) return "抱歉喵，天气查询失败喵~";

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s运动装备建议喵！\n\n", city, sportType));

            JSONArray lives = result.getJSONArray("lives");
            if (lives != null && !lives.isEmpty()) {
                JSONObject live = lives.getJSONObject(0);
                int temp = Integer.parseInt(live.getString("temperature"));
                int humidity = Integer.parseInt(live.getString("humidity"));
                int wind = Integer.parseInt(live.getString("windpower"));
                response.append(String.format("实时：%s℃ 湿度%s%% 风力%s级\n\n", live.getString("temperature"), live.getString("humidity"), live.getString("windpower")));

                // 三层穿衣法建议
                response.append("【排汗层】");
                if (temp > 25) response.append("速干短袖或运动背心\n");
                else if (temp > 10) response.append("速干长袖T恤\n");
                else response.append("美利奴羊毛内衣\n");

                response.append("【保暖层】");
                if (temp > 20) response.append("无需\n");
                else if (temp > 10) response.append("薄抓绒衣\n");
                else response.append("厚抓绒或轻羽绒\n");

                response.append("【防风外层】");
                if (wind >= 5) response.append("硬壳冲锋衣（防风防水）\n");
                else if (sportType.contains("滑雪")) response.append("滑雪服\n");
                else response.append("软壳夹克即可\n");

                if (humidity > 70) response.append("\n⚠ 湿度较高，注意排汗防潮喵~");
            }
            log.info("工具outdoorSportsOutfitTool执行完毕");
            return response.toString();
        } catch (Exception e) {
            log.error("工具outdoorSportsOutfitTool执行异常", e);
            return "抱歉喵，查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 花粉过敏与空气质量防护工具
     *
     * @param city  城市名称
     * @param month 月份，用于判断花粉季
     * @return String (AQI数值/花粉指数，以及明确建议是否需要佩戴N95口罩、全包围护目镜，或建议穿长袖长裤以隔离过敏原)
     * @Description 查询目的地的空气质量指数（AQI）或当季花粉浓度，给出健康防护性质的穿搭建议喵。用于保护用户的呼吸道和皮肤。
     */
    @Tool(name = "aqiPollenProtectionTool", description = "查询目的地的空气质量指数（AQI）或当季花粉浓度，给出健康防护性质的穿搭建议喵。用于保护用户的呼吸道和皮肤。")
    public String aqiPollenProtectionTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "月份") String month) {
        log.info("开始进入方法aqiPollenProtectionTool，参数为 city -> {}, month -> {}", city, month);

        String promptText = String.format(
                """
                        请查询城市【%s】在%s的空气质量或花粉浓度：
                        
                        请提供：
                        1. AQI数值或花粉指数
                        2. 是否需要佩戴N95口罩、全包围护目镜
                        3. 是否建议穿长袖长裤以隔离过敏原""",
                city, month
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具aqiPollenProtectionTool执行完成");
        return result;
    }

    /**
     * 晾晒指数与换洗频次工具
     *
     * @param city     城市名称
     * @param tripDays 行程天数
     * @return String (晾晒指数，如"极难晾干"，及内衣袜子的额外携带数量建议)
     * @Description 根据目的地的湿度和天气情况，评估衣物自然晾干的难度，推荐换洗衣物的携带数量。比如用户要去广州出差遇到极其潮湿的回南天，此工具会强烈建议额外携带双倍的内衣和袜子喵！
     */
    @Tool(name = "laundryDryingIndexTool", description = "根据目的地的湿度和天气情况，评估衣物自然晾干的难度，推荐换洗衣物的携带数量。比如用户要去广州出差遇到极其潮湿的回南天，此工具会强烈建议额外携带双倍的内衣和袜子喵！")
    public String laundryDryingIndexTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "行程天数") int tripDays) {
        log.info("开始进入方法laundryDryingIndexTool，参数为 city -> {}, tripDays -> {}", city, tripDays);

        String promptText = String.format(
                """
                        请评估城市【%s】的晾晒难度和换洗建议：
                        行程天数：%d天
                        
                        请提供：
                        1. 晾晒指数（如极难晾干）
                        2. 内衣袜子的额外携带数量建议""",
                city, tripDays
        );

        try {
            String adcode = getCityAdcode(city);
            if (adcode == null) return "抱歉喵，无法找到城市的编码喵~";
            JSONObject result = mapApiClient.weatherInfo(adcode);
            if (result == null) return "抱歉喵，天气查询失败喵~";

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】晾晒指数（%d天行程）喵！\n\n", city, tripDays));

            JSONArray lives = result.getJSONArray("lives");
            if (lives != null && !lives.isEmpty()) {
                JSONObject live = lives.getJSONObject(0);
                int humidity = Integer.parseInt(live.getString("humidity"));
                String weather = live.getString("weather");

                if (humidity > 80 || weather.contains("雨")) {
                    response.append(String.format("湿度%d%%，天气%s → 极难晾干！\n", humidity, weather));
                    response.append(String.format("强烈建议携带%d套内衣袜子（行程天数×1.5），酒店烘干机必备喵~\n", (int) (tripDays * 1.5)));
                } else if (humidity > 60) {
                    response.append(String.format("湿度%d%% → 晾干较慢，建议多带%d套备用喵~\n", humidity, tripDays / 2 + 1));
                } else {
                    response.append(String.format("湿度%d%% → 晾晒无忧，正常携带%d套即可喵~\n", humidity, tripDays / 2 + 1));
                }
            }
            log.info("工具laundryDryingIndexTool执行完毕");
            return response.toString();
        } catch (Exception e) {
            log.error("工具laundryDryingIndexTool执行异常", e);
            return "抱歉喵，查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 宗教与文化着装禁忌工具 — 无高德API，保留LLM
     */
    @Tool(name = "culturalDressCodeTool", description = "查询目的地或特定景点是否有严格的宗教或文化着装要求，避免用户因穿着短裤、吊带或未戴头巾而被拒绝进入特定场所喵。")
    public String culturalDressCodeTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "景点名称") String attractionName) {
        log.info("开始进入方法culturalDressCodeTool，参数为 city -> {}, attractionName -> {}", city, attractionName);
        String promptText = String.format("请查询城市【%s】%s景点的着装要求：着装红线（如必须遮挡肩膀和膝盖、需准备头巾等）", city, attractionName);
        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具culturalDressCodeTool执行完毕");
        return result;
    }

    /**
     * 出汗指数与备用衣物推荐工具
     */
    @Tool(name = "sweatIndexOutfitTool", description = "综合当地气温、湿度和用户的活动强度，预测用户的出汗量，并给出是否需要随身携带备用替换衣物或止汗剂的建议喵。")
    public String sweatIndexOutfitTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "活动强度") String activityLevel) {
        log.info("开始进入方法sweatIndexOutfitTool，参数为 city -> {}, activityLevel -> {}", city, activityLevel);
        try {
            String adcode = getCityAdcode(city);
            if (adcode == null) return "抱歉喵，无法找到城市的编码喵~";
            JSONObject result = mapApiClient.weatherInfo(adcode);
            if (result == null) return "抱歉喵，天气查询失败喵~";

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】出汗指数预测（%s活动）喵！\n\n", city, activityLevel));

            JSONArray lives = result.getJSONArray("lives");
            if (lives != null && !lives.isEmpty()) {
                JSONObject live = lives.getJSONObject(0);
                int temp = Integer.parseInt(live.getString("temperature"));
                int humidity = Integer.parseInt(live.getString("humidity"));
                int sweatScore = temp + humidity / 10;

                if (activityLevel.contains("剧烈")) sweatScore += 10;
                else if (activityLevel.contains("休闲")) sweatScore -= 5;

                if (sweatScore > 40) {
                    response.append(String.format("%d℃ + 湿度%d%% → 极易出汗！强烈建议随身带备用T恤和毛巾喵~\n", temp, humidity));
                } else if (sweatScore > 30) {
                    response.append(String.format("%d℃ + 湿度%d%% → 可能出汗，建议带一件备用T恤喵~\n", temp, humidity));
                } else {
                    response.append(String.format("%d℃ + 湿度%d%% → 出汗风险低，正常穿着即可喵~\n", temp, humidity));
                }
                response.append("户外活动建议携带止汗剂喵~");
            }
            log.info("工具sweatIndexOutfitTool执行完毕");
            return response.toString();
        } catch (Exception e) {
            log.error("工具sweatIndexOutfitTool执行异常", e);
            return "抱歉喵，查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 蚊虫活跃度与防护穿搭工具
     *
     * @param city        城市名称
     * @param environment 环境类型，如：市区、热带雨林、湖边
     * @return String (蚊虫预警级别及对应的穿搭，如"强烈建议穿着浅色长袖长裤，避免暴露脚踝，并携带强效驱蚊液")
     * @Description 专门查询目的地（尤其是野外、热带或多水域地区）当季的蚊虫活跃指数，提供物理防蚊防虫的穿衣策略喵。
     */
    @Tool(name = "bugActivityProtectionTool", description = "专门查询目的地（尤其是野外、热带或多水域地区）当季的蚊虫活跃指数，提供物理防蚊防虫的穿衣策略喵。")
    public String bugActivityProtectionTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "环境类型") String environment) {
        log.info("开始进入方法bugActivityProtectionTool，参数为 city -> {}, environment -> {}", city, environment);

        String promptText = String.format(
                """
                        请查询城市【%s】在%s环境的蚊虫活跃情况：
                        
                        请提供：
                        1. 蚊虫预警级别
                        2. 对应的穿搭建议（如强烈建议穿着浅色长袖长裤，避免暴露脚踝）
                        3. 是否需携带强效驱蚊液""",
                city, environment
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具bugActivityProtectionTool执行完成");
        return result;
    }

    /**
     * 极端温差下的商务正装生存工具
     *
     * @param city      城市名称
     * @param eventType 活动类型，如：高端晚宴、商务面谈
     * @return String (例如："室外35度极热，建议通勤穿便装，将西服外套装入防皱衣袋，到达冷气充足的会场后再换上以免汗湿")
     * @Description 专门为需要穿厚重西装或礼服参加会议，但室外温度极高（或极低）的用户，提供"通勤与会场切换"的穿衣与携带策略喵。
     */
    @Tool(name = "businessSuitSurvivalTool", description = "专门为需要穿厚重西装或礼服参加会议，但室外温度极高（或极低）的用户，提供\"通勤与会场切换\"的穿衣与携带策略喵。")
    public String businessSuitSurvivalTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "活动类型") String eventType) {
        log.info("开始进入方法businessSuitSurvivalTool，参数为 city -> {}, eventType -> {}", city, eventType);

        String promptText = String.format(
                """
                        请根据城市【%s】的天气情况，为%s活动提供商务正装穿搭策略：
                        
                        请提供：
                        1. \"通勤与会场切换\"的穿衣与携带策略
                        2. 比如室外极热时建议通勤穿便装，到达会场后再换上西服""",
                city, eventType
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具businessSuitSurvivalTool执行完成");
        return result;
    }

    /**
     * 地形与降水联合鞋履推荐工具
     *
     * @param city             城市名称
     * @param terrain          地形，如：山地、石板路、沙滩、城市柏油路
     * @param weatherCondition 天气状况
     * @return String (比如："雨天+山地"绝对推荐防滑登山靴，严禁穿平底鞋；"晴天+欧洲石板路"强烈推荐厚底运动鞋，严禁高跟鞋以免卡跟喵)
     * @Description 这是一个结合当地降水情况和景点具体地形，提供极其专业的鞋子建议的工具喵。当用户问"下雨天去爬黄山穿什么"或者"去欧洲老城区石板路逛街穿什么"时调用！
     */
    @Tool(name = "terrainFootwearTool", description = "这是一个结合当地降水情况和景点具体地形，提供极其专业的鞋子建议的工具喵。当用户问\"下雨天去爬黄山穿什么\"或者\"去欧洲老城区石板路逛街穿什么\"时调用！")
    public String terrainFootwearTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "地形") String terrain, @ToolParam(description = "天气状况") String weatherCondition) {
        log.info("开始进入方法terrainFootwearTool，参数为 city -> {}, terrain -> {}, weatherCondition -> {}", city, terrain, weatherCondition);

        String promptText = String.format(
                """
                        请根据城市【%s】的%s地形和%s天气，提供专业鞋履建议：
                        
                        请提供：
                        1. 针对性的鞋子推荐（如雨天+山地绝对推荐防滑登山靴）
                        2. 严禁穿的鞋类（如严禁高跟鞋以免卡跟）""",
                city, terrain, weatherCondition
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具terrainFootwearTool执行完成");
        return result;
    }

    /**
     * 室内外极端温差对策工具
     *
     * @param destinationRegion 目标地区
     * @return String (强烈建议采用"内薄外极厚"穿法，如里面穿单件长袖T恤，外面直接套极地羽绒服，方便进入室内秒脱，绝对禁止穿厚重的高领毛衣喵)
     * @Description 专门解决某些地区"室外冻死，室内热死"的穿衣痛点喵！比如冬季去日本北海道或中国东北，室外零下20度，室内暖气零上25度。
     */
    @Tool(name = "indoorClimateOutfitTool", description = "专门解决某些地区\"室外冻死，室内热死\"的穿衣痛点喵！比如冬季去日本北海道或中国东北，室外零下20度，室内暖气零上25度。")
    public String indoorClimateOutfitTool(@ToolParam(description = "目标地区") String destinationRegion) {
        log.info("开始进入方法indoorClimateOutfitTool，参数为 destinationRegion -> {}", destinationRegion);

        String promptText = String.format(
                """
                        请分析%s地区的室内外温差情况：
                        
                        请提供：
                        1. 室内外温差分析
                        2. \"内薄外极厚\"穿法建议（如里面穿单件长袖T恤，外面直接套极地羽绒服）
                        3. 绝对禁止穿的衣物（如厚重的高领毛衣）""",
                destinationRegion
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具indoorClimateOutfitTool执行完成");
        return result;
    }

    /**
     * 极端天气行李空间压缩建议工具
     *
     * @param city 城市名称
     * @param date 日期
     * @return String (如果预测需要重型保暖衣物，明确建议用户提前购买"免抽气真空压缩袋"，或者建议把最厚的外套直接穿在身上登机以节省行李额度喵)
     * @Description 根据目的地的天气预报判断用户是否需要携带极其臃肿的衣物（如长款羽绒服、大毛毯），并提供打包耗材和携带方案建议喵。
     */
    @Tool(name = "luggageSpaceOutfitTool", description = "根据目的地的天气预报判断用户是否需要携带极其臃肿的衣物（如长款羽绒服、大毛毯），并提供打包耗材和携带方案建议喵。")
    public String luggageSpaceOutfitTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "日期") String date) {
        log.info("开始进入方法luggageSpaceOutfitTool，参数为 city -> {}, date -> {}", city, date);

        String promptText = String.format(
                """
                        请根据城市【%s】在%s的天气预报，判断是否需要携带臃肿衣物：
                        
                        请提供：
                        1. 是否需要携带长款羽绒服、大毛毯等臃肿衣物
                        2. 打包耗材建议（如免抽气真空压缩袋）
                        3. 是否建议把最厚的外套直接穿在身上登机以节省行李额度""",
                city, date
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具luggageSpaceOutfitTool执行完成");
        return result;
    }

    /**
     * 季节景色与穿搭色彩反差工具
     *
     * @param city            城市名称
     * @param seasonOrScenery 季节或景色，如：雪景、红叶、沙漠
     * @return String (比如雪景强烈推荐大红色或亮黄色风衣；沙漠推荐纯白色或波西米亚长裙。避免与背景顺色导致拍照隐身喵)
     * @Description 这是一个注重"情绪价值"的高级工具！结合目的地的当前季节景色（如：秋季满山红叶、冬季白雪皑皑、夏季蔚蓝海边），推荐最容易拍出大片的衣物颜色搭配喵。
     */
    @Tool(name = "scenicColorOutfitTool", description = "这是一个注重\"情绪价值\"的高级工具！结合目的地的当前季节景色（如：秋季满山红叶、冬季白雪皑皑、夏季蔚蓝海边），推荐最容易拍出大片的衣物颜色搭配喵。")
    public String scenicColorOutfitTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "季节或景色") String seasonOrScenery) {
        log.info("开始进入方法scenicColorOutfitTool，参数为 city -> {}, seasonOrScenery -> {}", city, seasonOrScenery);

        String promptText = String.format(
                """
                        请根据城市【%s】的%s景色，推荐拍照穿搭配色：
                        
                        请提供：
                        1. 最容易拍出大片的衣物颜色搭配（如雪景强烈推荐大红色或亮黄色风衣）
                        2. 避免与背景顺色导致拍照隐身的提示""",
                city, seasonOrScenery
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具scenicColorOutfitTool执行完成");
        return result;
    }

    /**
     * 夜间降温与睡眠穿搭建议工具
     *
     * @param city  城市名称
     * @param month 月份
     * @return String (比如冬季去中国南方出差，工具会指出"夜间 5 度且无集中供暖，体感极冷，强烈建议携带加厚法兰绒睡衣"；去北方则建议带薄款棉质睡衣喵)
     * @Description 极具人文关怀的工具！查询目的地夜间的最低温度以及当地普遍的供暖情况，推荐应该带什么厚度的睡衣喵。
     */
    @Tool(name = "nightwearSuggestionTool", description = "极具人文关怀的工具！查询目的地夜间的最低温度以及当地普遍的供暖情况，推荐应该带什么厚度的睡衣喵。")
    public String nightwearSuggestionTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "月份") String month) {
        log.info("开始进入方法nightwearSuggestionTool，参数为 city -> {}, month -> {}", city, month);
        try {
            String adcode = getCityAdcode(city);
            if (adcode == null) return "抱歉喵，无法找到城市的编码喵~";
            JSONObject result = mapApiClient.weatherInfo(adcode);
            if (result == null) return "抱歉喵，天气查询失败喵~";

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s夜间睡衣建议喵！\n\n", city, month));

            JSONArray forecasts = result.getJSONArray("forecasts");
            if (forecasts != null && !forecasts.isEmpty()) {
                JSONObject f = forecasts.getJSONObject(0);
                JSONArray casts = f.getJSONArray("casts");
                if (casts != null && !casts.isEmpty()) {
                    JSONObject today = casts.getJSONObject(0);
                    int nightTemp = Integer.parseInt(today.getString("nighttemp"));
                    response.append(String.format("今晚最低温度：%d℃\n", nightTemp));
                    if (nightTemp < 5) {
                        response.append("寒冷！建议带加厚法兰绒睡衣或珊瑚绒睡衣，南方没暖气更要厚喵~\n");
                    } else if (nightTemp < 15) {
                        response.append("偏凉，建议带中厚棉质睡衣或薄绒睡衣喵~\n");
                    } else if (nightTemp < 22) {
                        response.append(String.format("适中，薄款棉质睡衣即可喵~\n"));
                    } else {
                        response.append("温暖，短袖睡衣或睡裙即可喵~\n");
                    }
                }
            }
            log.info("工具nightwearSuggestionTool执行完毕");
            return response.toString();
        } catch (Exception e) {
            log.error("工具nightwearSuggestionTool执行异常", e);
            return "抱歉喵，查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 解析风力等级字符串，支持范围值（如 "1-3"）和单值（如 "5"）
     *
     * @param windPower 风力字符串
     * @return 最大风力等级
     */
    private int parseWindPower(String windPower) {
        if (windPower == null || windPower.isEmpty()) {
            return 0;
        }

        try {
            // 如果是范围格式 "1-3"，取最大值
            if (windPower.contains("-")) {
                String[] parts = windPower.split("-");
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                return Math.max(min, max);
            } else {
                // 单值格式 "5"
                return Integer.parseInt(windPower.trim());
            }
        } catch (NumberFormatException e) {
            log.warn("无法解析风力等级: {}", windPower);
            return 0;
        }
    }

}