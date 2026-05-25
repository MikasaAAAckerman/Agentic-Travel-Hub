package com.travel.hubtools.tool.entertainment;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.travel.hubtools.client.MapApiClient;
import com.travel.hubtools.tool.common.IAgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 景点与娱乐活动查询工具
 */
@Slf4j
@Component
public class AttractionAndEntertainmentTool implements IAgentTool {

    @Autowired
    private ChatModel dashScopeChatModel;

    @Autowired
    private MapApiClient mapApiClient;

    /**
     * 核心圈层与二次元/小众景点雷达工具
     *
     * @param city         城市
     * @param interestTags 兴趣标签，如："二次元", "历史建筑"
     * @return String (打卡地列表及圈层浓度评估)
     * @Description 满足笨蛋主人特殊癖好的神器喵！除了常规景点，专门用于检索城市里隐藏的"二次元打卡地（如广州动漫星城）"、小众历史文化街区或特定圈层（如汉服、谷子店、电竞）的聚集地！
     */
    @Tool(name = "subculturePoiRadarTool", description = "满足笨蛋主人特殊癖好的神器喵！除了常规景点，专门用于检索城市里隐藏的\"二次元打卡地（如广州动漫星城）\"、小众历史文化街区或特定圈层（如汉服、谷子店、电竞）的聚集地！")
    public String subculturePoiRadarTool(@ToolParam(description = "城市") String city, @ToolParam(description = "兴趣标签，如：\"二次元\", \"历史建筑\"") List<String> interestTags) {
        log.info("开始进入方法subculturePoiRadarTool，参数为 city -> {}, interestTags -> {}", city, interestTags);

        try {
            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】圈层景点雷达扫描结果喵！\n\n", city));

            for (String tag : interestTags) {
                JSONObject result = mapApiClient.searchPOI(tag, city, null, 10, 1);

                if (result != null && result.getJSONArray("pois") != null) {
                    JSONArray pois = result.getJSONArray("pois");
                    response.append(String.format("🎯 【%s】相关地点（共%d个）：\n", tag, pois.size()));

                    for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                        JSONObject poi = pois.getJSONObject(i);
                        String name = poi.getString("name");
                        String address = poi.getString("address");

                        response.append(String.format("  %d. %s\n", i + 1, name));
                        if (address != null && !address.isEmpty()) {
                            response.append(String.format("     地址：%s\n", address));
                        }
                    }
                    response.append("\n");
                }
            }

            response.append("需要更详细的圈层浓度评估可以告诉我喵~");

            log.info("工具subculturePoiRadarTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具subculturePoiRadarTool执行异常", e);
            return "抱歉喵，圈层景点搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 场馆限流与周一闭馆防坑工具
     *
     * @param attractionName 景点名称
     * @param targetDate     目标日期，格式：yyyy-MM-dd
     * @return String (致命红线预警)
     * @Description 历史文化游的保命工具！几乎所有的国家级博物馆和文化遗址都有"周一闭馆"和"必须提前 3-7 天实名预约"的死规矩！此工具专门查验开闭馆时间及门票余量喵！
     */
    @Tool(name = "museumReservationAndClosureTool", description = "历史文化游的保命工具！几乎所有的国家级博物馆和文化遗址都有\"周一闭馆\"和\"必须提前 3-7 天实名预约\"的死规矩！此工具专门查验开闭馆时间及门票余量喵！")
    public String museumReservationAndClosureTool(@ToolParam(description = "景点名称") String attractionName, @ToolParam(description = "目标日期，格式：yyyy-MM-dd") String targetDate) {
        log.info("开始进入方法museumReservationAndClosureTool，参数为 attractionName -> {}, targetDate -> {}", attractionName, targetDate);

        String promptText = String.format(
                """
                        请查询景点【%s】在%s的开闭馆情况及预约要求：
                        
                        请提供：
                        1. 该日期是否闭馆（特别注意周一闭馆规则）
                        2. 是否需要提前预约及提前天数
                        3. 门票余量情况
                        4. 致命红线预警""",
                attractionName, targetDate
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具museumReservationAndClosureTool执行完成");
        return result;
    }

    /**
     * 同城限时展会与演出追踪工具
     *
     * @param city      城市
     * @param startDate 开始日期，格式：yyyy-MM-dd
     * @param endDate   结束日期，格式：yyyy-MM-dd
     * @param eventType 活动类型，如："漫展", "演唱会"
     * @return String (限时活动清单)
     * @Description 旅游 Agent 的灵魂工具喵！查询指定期间，城市是否有大型的漫展、演唱会、话剧与灯光秀。
     */
    @Tool(name = "localEventCalendarTool", description = "旅游 Agent 的灵魂工具喵！查询指定期间，城市是否有大型的漫展、演唱会、话剧与灯光秀。")
    public String localEventCalendarTool(@ToolParam(description = "城市") String city, @ToolParam(description = "开始日期，格式：yyyy-MM-dd") String startDate, @ToolParam(description = "结束日期，格式：yyyy-MM-dd") String endDate, @ToolParam(description = "活动类型，如：\"漫展\", \"演唱会\"") String eventType) {
        log.info("开始进入方法localEventCalendarTool，参数为 city -> {}, startDate -> {}, endDate -> {}, eventType -> {}", city, startDate, endDate, eventType);

        String promptText = String.format(
                """
                        请查询%s在%s至%s期间的%s活动：
                        
                        请提供：
                        1. 活动名称及时间
                        2. 活动地点
                        3. 票务信息
                        4. 是否推荐参加""",
                city, startDate, endDate, eventType
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具localEventCalendarTool执行完成");
        return result;
    }

    /**
     * 十万预算挥霍指南与奢华体验工具
     *
     * @param city        城市
     * @param budgetLevel 预算等级，如："奢华不差钱"
     * @return String (奢华项目推荐)
     * @Description 既然主人说了"20万/10万随便花"，那就绝不能按照穷游的逻辑推荐排队打卡点！此工具专门检索城市里的奢华体验喵！
     */
    @Tool(name = "luxuryExperienceCustomizationTool", description = "既然主人说了\"20万/10万随便花\"，那就绝不能按照穷游的逻辑推荐排队打卡点！此工具专门检索城市里的奢华体验喵！")
    public String luxuryExperienceCustomizationTool(@ToolParam(description = "城市") String city, @ToolParam(description = "预算等级，如：\"奢华不差钱\"") String budgetLevel) {
        log.info("开始进入方法luxuryExperienceCustomizationTool，参数为 city -> {}, budgetLevel -> {}", city, budgetLevel);

        String promptText = String.format(
                """
                        请为%s推荐%s预算等级的奢华体验项目：
                        
                        请提供：
                        1. 奢华体验项目列表（如私人游艇、直升机观光等）
                        2. 价格区间
                        3. 预订方式
                        4. 独特性说明""",
                city, budgetLevel
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具luxuryExperienceCustomizationTool执行完成");
        return result;
    }

    /**
     * 绝美出片机位与旅拍环境查验工具
     *
     * @param attractionName 景点名称
     * @return String (机位攻略与拍摄红线)
     * @Description 既然是和红豆两个人去，怎么能不拍照？！查询特定自然景观或历史文化建筑中，最容易出大片的隐藏机位喵！
     */
    @Tool(name = "photoSpotAndTravelShootingTool", description = "既然是和红豆两个人去，怎么能不拍照？！查询特定自然景观或历史文化建筑中，最容易出大片的隐藏机位喵！")
    public String photoSpotAndTravelShootingTool(@ToolParam(description = "景点名称") String attractionName) {
        log.info("开始进入方法photoSpotAndTravelShootingTool，参数为 attractionName -> {}", attractionName);

        String promptText = String.format(
                """
                        请评估景点【%s】的拍照条件：
                        
                        请提供：
                        1. 最佳拍照机位推荐
                        2. 是否允许携带单反、三脚架
                        3. 是否允许商业/Cosplay换装拍摄
                        4. 拍摄红线提示（如禁用闪光灯等）
                        5. 最佳拍摄时间段""",
                attractionName
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具photoSpotAndTravelShootingTool执行完成");
        return result;
    }

    /**
     * 顶级主题乐园防排队与 VIP 攻略工具
     *
     * @param parkName   乐园名称
     * @param visitDate  游玩日期，格式：yyyy-MM-dd
     * @return String (硬核游玩攻略)
     * @Description 拯救双腿的神器喵！查询大型乐园的"免排队优速通"价格、表演时间表，并计算出一天刷完热门项目的最优移动路线！
     */
    @Tool(name = "themeParkStrategyTool", description = "拯救双腿的神器喵！查询大型乐园的\"免排队优速通\"价格、表演时间表，并计算出一天刷完热门项目的最优移动路线！")
    public String themeParkStrategyTool(@ToolParam(description = "乐园名称") String parkName, @ToolParam(description = "游玩日期，格式：yyyy-MM-dd") String visitDate) {
        log.info("开始进入方法themeParkStrategyTool，参数为 parkName -> {}, visitDate -> {}", parkName, visitDate);

        String promptText = String.format(
                """
                        请为%s在%s制定游玩攻略：
                        
                        请提供：
                        1. 优速通/VIP票价格及购买建议
                        2. 花车巡游/表演时间表
                        3. 一天刷完热门项目的最优路线
                        4. 避坑提示""",
                parkName, visitDate
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具themeParkStrategyTool执行完成");
        return result;
    }

    /**
     * 沉浸式夜生活与微醺清吧雷达工具
     *
     * @param location 位置
     * @param vibeType 氛围类型，如："安静微醺", "Livehouse"
     * @return String (酒吧坐标及酒单人均)
     * @Description 为成年人的浪漫夜晚量身定制喵！精准探测城市里隐藏在暗巷中的 Speakeasy、Livehouse，以及适合情侣安静微醺的高空露台 Bar喵！
     */
    @Tool(name = "nightlifeAndBarRadarTool", description = "为成年人的浪漫夜晚量身定制喵！精准探测城市里隐藏在暗巷中的 Speakeasy、Livehouse，以及适合情侣安静微醺的高空露台 Bar喵！")
    public String nightlifeAndBarRadarTool(@ToolParam(description = "位置") String location, @ToolParam(description = "氛围类型，如：\"安静微醺\", \"Livehouse\"") String vibeType, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法nightlifeAndBarRadarTool，参数为 location -> {}, vibeType -> {}, city -> {}", location, vibeType, city);

        try {
            String coord = mapApiClient.geocode(location, city);

            if (coord == null) {
                return "抱歉喵，无法找到位置的坐标信息，请检查地址是否正确喵~";
            }

            // 搜索周边酒吧和夜生活场所
            String keywords = vibeType + " 酒吧";
            JSONObject result = mapApiClient.searchPOIAround(keywords, coord, 2000, "080300");

            if (result == null) {
                return "抱歉喵，酒吧搜索失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            if (pois == null || pois.isEmpty()) {
                return String.format("抱歉喵，在%s附近没有找到%s风格的酒吧喵~", location, vibeType);
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】附近%s风酒吧/夜生活场所喵！\n\n", location, vibeType));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                JSONObject poi = pois.getJSONObject(i);
                String name = poi.getString("name");
                String address = poi.getString("address");
                String distance = poi.getString("distance");

                response.append(String.format("%d. 【%s】\n", i + 1, name));
                response.append(String.format("   距离：%s 米\n", distance));
                if (address != null) response.append(String.format("   地址：%s\n", address));

                JSONObject bizExt = poi.getJSONObject("biz_ext");
                if (bizExt != null) {
                    String rating = bizExt.getString("rating");
                    String cost = bizExt.getString("cost");
                    if (rating != null) response.append(String.format("   评分：%s 分\n", rating));
                    if (cost != null) response.append(String.format("   人均：%s 元\n", cost));
                }
                response.append("\n");
            }

            response.append("建议提前查看营业时间，部分Speakeasy需要预约喵~\n");
            response.append("微醺虽好，可不要贪杯喵~");
            log.info("工具nightlifeAndBarRadarTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具nightlifeAndBarRadarTool执行异常", e);
            return "抱歉喵，酒吧搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 天然温泉与高端水疗避雷工具
     *
     * @param city    城市
     * @param spaType 水疗类型，如："真温泉", "泰式SPA"
     * @return String (温泉真伪鉴定报告及私汤推荐)
     * @Description 帮你极其冷酷地鉴别哪些是"真正含氡元素的天然真温泉"，哪些是"靠锅炉烧热的假温泉"喵！
     */
    @Tool(name = "hotSpringAndSpaTool", description = "帮你极其冷酷地鉴别哪些是\"真正含氡元素的天然真温泉\"，哪些是\"靠锅炉烧热的假温泉\"喵！")
    public String hotSpringAndSpaTool(@ToolParam(description = "城市") String city, @ToolParam(description = "水疗类型，如：\"真温泉\", \"泰式SPA\"") String spaType) {
        log.info("开始进入方法hotSpringAndSpaTool，参数为 city -> {}, spaType -> {}", city, spaType);

        String promptText = String.format(
                """
                        请查询%s的%s场所：
                        
                        请提供：
                        1. 温泉真伪鉴定（是否天然真温泉）
                        2. 推荐场所及特色
                        3. 私汤预订难度
                        4. 价格区间""",
                city, spaType
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具hotSpringAndSpaTool执行完成");
        return result;
    }

    /**
     * 寺庙祈福与上香玄学指南工具
     *
     * @param templeName 寺庙名称
     * @param prayGoal   祈福目标，如："求财", "求姻缘"
     * @return String (玄学攻略)
     * @Description 极其硬核的玄学向导喵！不仅查询寺庙的开放时间，更明确指出哪个殿求财最灵、哪个殿求姻缘最准喵！
     */
    @Tool(name = "templeAndPrayingGuideTool", description = "极其硬核的玄学向导喵！不仅查询寺庙的开放时间，更明确指出哪个殿求财最灵、哪个殿求姻缘最准喵！")
    public String templeAndPrayingGuideTool(@ToolParam(description = "寺庙名称") String templeName, @ToolParam(description = "祈福目标，如：\"求财\", \"求姻缘\"") String prayGoal) {
        log.info("开始进入方法templeAndPrayingGuideTool，参数为 templeName -> {}, prayGoal -> {}", templeName, prayGoal);

        String promptText = String.format(
                """
                        请查询寺庙【%s】的%s祈福指南：
                        
                        请提供：
                        1. 哪个殿%s最灵验
                        2. 开放时间
                        3. 祈福礼仪注意事项（如左手敬香、不踩门槛等）
                        4. 最佳祈福时间段""",
                templeName, prayGoal, prayGoal
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具templeAndPrayingGuideTool执行完成");
        return result;
    }

    /**
     * 硬核户外徒步与野奢露营探测工具
     *
     * @param region           地区
     * @param difficultyLevel  难度等级，如："新手小白", "野外求生"
     * @return String (户外路线分析)
     * @Description 远离城市喧嚣的自然工具喵！查询周边未被商业化过度开发的野外徒步路线、爬升海拔数据，以及提供拎包入住的"Glamping 野奢露营地"价目表。
     */
    @Tool(name = "hikingAndCampingTrailTool", description = "远离城市喧嚣的自然工具喵！查询周边未被商业化过度开发的野外徒步路线、爬升海拔数据，以及提供拎包入住的\"Glamping 野奢露营地\"价目表。")
    public String hikingAndCampingTrailTool(@ToolParam(description = "地区") String region, @ToolParam(description = "难度等级，如：\"新手小白\", \"野外求生\"") String difficultyLevel) {
        log.info("开始进入方法hikingAndCampingTrailTool，参数为 region -> {}, difficultyLevel -> {}", region, difficultyLevel);

        String promptText = String.format(
                """
                        请查询%s的%s难度等级的户外徒步路线和露营地：
                        
                        请提供：
                        1. 推荐徒步路线及爬升海拔数据
                        2. 路线难度评估
                        3. 野奢露营地推荐及价目表
                        4. 装备建议""",
                region, difficultyLevel
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具hikingAndCampingTrailTool执行完成");
        return result;
    }

    /**
     * 历史遗迹与博物馆金牌向导工具
     *
     * @param attractionName 景点名称
     * @return String (讲解服务指南)
     * @Description 拒绝"走马观花"的深度游神器喵！查询特定的历史文化景点是否提供官方的高质量 AR 导览器租借，或者是否能预约到国家级的金牌人工讲解员。
     */
    @Tool(name = "historicalRelicAudioGuideTool", description = "拒绝\"走马观花\"的深度游神器喵！查询特定的历史文化景点是否提供官方的高质量 AR 导览器租借，或者是否能预约到国家级的金牌人工讲解员。")
    public String historicalRelicAudioGuideTool(@ToolParam(description = "景点名称") String attractionName) {
        log.info("开始进入方法historicalRelicAudioGuideTool，参数为 attractionName -> {}", attractionName);

        String promptText = String.format(
                """
                        请查询景点【%s】的讲解服务：
                        
                        请提供：
                        1. 是否提供 AR 导览器租借及价格
                        2. 是否能预约金牌人工讲解员及费用
                        3. 讲解时长和内容特色
                        4. 预订方式和建议""",
                attractionName
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具historicalRelicAudioGuideTool执行完成");
        return result;
    }

    /**
     * 密室逃脱与实景剧本杀评价工具
     *
     * @param location 位置
     * @param theme    主题类型，如："微恐解谜", "情感沉浸"
     * @return String (硬核评测)
     * @Description 年轻人社交娱乐的探测雷达喵！查询评分最高的大型机械密室或 NPC 飙戏剧本杀，明确标出"恐怖指数"、"烧脑指数"以及是否适合两人拼车游玩。
     */
    @Tool(name = "escapeRoomAndScriptMurderTool", description = "年轻人社交娱乐的探测雷达喵！查询评分最高的大型机械密室或 NPC 飙戏剧本杀，明确标出\"恐怖指数\"、\"烧脑指数\"以及是否适合两人拼车游玩。")
    public String escapeRoomAndScriptMurderTool(@ToolParam(description = "位置") String location, @ToolParam(description = "主题类型，如：\"微恐解谜\", \"情感沉浸\"") String theme) {
        log.info("开始进入方法escapeRoomAndScriptMurderTool，参数为 location -> {}, theme -> {}", location, theme);

        String promptText = String.format(
                """
                        请查询%s的%s主题密室/剧本杀：
                        
                        请提供：
                        1. 推荐场所名称及评分
                        2. 恐怖指数和烧脑指数评级
                        3. 是否适合两人游玩
                        4. NPC互动强度和游戏时长""",
                location, theme
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具escapeRoomAndScriptMurderTool执行完成");
        return result;
    }

    /**
     * 本地民俗庙会与跳蚤市场探测工具
     *
     * @param city 城市
     * @param date 日期，格式：yyyy-MM-dd
     * @return String (集市情报)
     * @Description 感受最地道市井气息的工具喵！追踪那些只在特定日子才开市的非遗民俗集市、古董文玩跳蚤市场、或者充满文艺气息的文创后备箱集市。
     */
    @Tool(name = "localMarketAndBazaarTool", description = "感受最地道市井气息的工具喵！追踪那些只在特定日子才开市的非遗民俗集市、古董文玩跳蚤市场、或者充满文艺气息的文创后备箱集市。")
    public String localMarketAndBazaarTool(@ToolParam(description = "城市") String city, @ToolParam(description = "日期，格式：yyyy-MM-dd") String date) {
        log.info("开始进入方法localMarketAndBazaarTool，参数为 city -> {}, date -> {}", city, date);

        try {
            // 搜索集市和市场
            JSONObject result = mapApiClient.searchPOI("集市 市场 跳蚤市场 文创集市", city, null, 10, 1);

            if (result == null) {
                return "抱歉喵，集市搜索失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            if (pois == null || pois.isEmpty()) {
                return String.format("抱歉喵，在%s没有找到集市和市场喵~", city);
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s特色集市喵！\n\n", city, date));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                JSONObject poi = pois.getJSONObject(i);
                String name = poi.getString("name");
                String address = poi.getString("address");
                String type = poi.getString("type");

                response.append(String.format("%d. 【%s】\n", i + 1, name));
                if (address != null) response.append(String.format("   地址：%s\n", address));
                if (type != null) response.append(String.format("   类型：%s\n", type));
                response.append("\n");
            }

            response.append("部分集市仅在特定日期开放，建议提前电话确认喵~\n");
            response.append("逛集市记得带现金和购物袋喵~");
            log.info("工具localMarketAndBazaarTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具localMarketAndBazaarTool执行异常", e);
            return "抱歉喵，集市搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 动物园喂食互动与剧场时刻表工具
     *
     * @param zooName    动物园名称
     * @param animalType 动物类型，如："大熊猫", "白鲸"
     * @return String (互动时间表)
     * @Description 专攻长隆野生动物世界或正佳极地海洋世界的精细化工具喵！精准抓取每天大熊猫进食、白鲸表演、或者给长颈鹿喂树叶的极其精确的时间窗口！
     */
    @Tool(name = "zooAndAquariumFeedingTimeTool", description = "专攻长隆野生动物世界或正佳极地海洋世界的精细化工具喵！精准抓取每天大熊猫进食、白鲸表演、或者给长颈鹿喂树叶的极其精确的时间窗口！")
    public String zooAndAquariumFeedingTimeTool(@ToolParam(description = "动物园名称") String zooName, @ToolParam(description = "动物类型，如：\"大熊猫\", \"白鲸\"") String animalType) {
        log.info("开始进入方法zooAndAquariumFeedingTimeTool，参数为 zooName -> {}, animalType -> {}", zooName, animalType);

        String promptText = String.format(
                """
                        请查询%s的%s互动和表演时间：
                        
                        请提供：
                        1. 喂食/表演的具体时间段
                        2. 最佳观赏位置建议
                        3. 是否需要额外付费
                        4. 注意事项和Tips""",
                zooName, animalType
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具zooAndAquariumFeedingTimeTool执行完成");
        return result;
    }

    /**
     * 城市制高点与观光游船票务工具
     *
     * @param attractionName 景点名称
     * @param ticketType     票种类型，如："摩天轮", "VIP露天甲板"
     * @return String (票务预警)
     * @Description 专门查询那些能把城市全貌尽收眼底的特殊交通工具喵！例如广州塔的摩天轮、白云山的高空索道、或者珠江夜游的VIP露天甲板票的余票及最佳观赏时段。
     */
    @Tool(name = "cityViewCableCarAndFerryTool", description = "专门查询那些能把城市全貌尽收眼底的特殊交通工具喵！例如广州塔的摩天轮、白云山的高空索道、或者珠江夜游的VIP露天甲板票的余票及最佳观赏时段。")
    public String cityViewCableCarAndFerryTool(@ToolParam(description = "景点名称") String attractionName, @ToolParam(description = "票种类型，如：\"摩天轮\", \"VIP露天甲板\"") String ticketType, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法cityViewCableCarAndFerryTool，参数为 attractionName -> {}, ticketType -> {}, city -> {}", attractionName, ticketType, city);

        try {
            // 搜索景点 + 票种信息
            String keywords = attractionName + " " + ticketType;
            JSONObject result = mapApiClient.searchPOI(keywords, city, null, 10, 1);

            if (result == null) {
                return "抱歉喵，景点票务查询失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            if (pois == null || pois.isEmpty()) {
                return String.format("抱歉喵，没有找到%s的%s票务信息喵~", attractionName, ticketType);
            }

            JSONObject poi = pois.getJSONObject(0);
            String name = poi.getString("name");
            String address = poi.getString("address");
            String tel = poi.getString("tel");

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s票务信息喵！\n\n", attractionName, ticketType));
            response.append(String.format("名称：%s\n", name));
            if (address != null) response.append(String.format("地址：%s\n", address));
            if (tel != null && !tel.isEmpty()) {
                response.append(String.format("咨询电话：%s\n", tel));
            }

            // 获取评分和价格
            JSONObject bizExt = poi.getJSONObject("biz_ext");
            if (bizExt != null) {
                String rating = bizExt.getString("rating");
                String cost = bizExt.getString("cost");
                if (rating != null) response.append(String.format("评分：%s 分\n", rating));
                if (cost != null) response.append(String.format("参考价：%s 元\n", cost));
            }

            response.append("\n建议提前致电确认余票和最佳观赏时段喵~\n");
            response.append("热门景点建议工作日前往，避开节假日人流高峰喵~");
            log.info("工具cityViewCableCarAndFerryTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具cityViewCableCarAndFerryTool执行异常", e);
            return "抱歉喵，票务查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 顶级电竞馆与 VR 主题乐园检索工具
     *
     * @param city                 城市
     * @param facilityRequirement 设施要求，如："4090显卡", "大空间VR"
     * @return String (硬件测评及坐标)
     * @Description 游戏党绝对不容错过的赛博庇护所喵！查询配备 4090 显卡的高端电竞酒店、或者是提供全向跑步机的超大型 VR 沉浸式体验馆。
     */
    @Tool(name = "vrAndEsportsArenaTool", description = "游戏党绝对不容错过的赛博庇护所喵！查询配备 4090 显卡的高端电竞酒店、或者是提供全向跑步机的超大型 VR 沉浸式体验馆。")
    public String vrAndEsportsArenaTool(@ToolParam(description = "城市") String city, @ToolParam(description = "设施要求，如：\"4090显卡\", \"大空间VR\"") String facilityRequirement) {
        log.info("开始进入方法vrAndEsportsArenaTool，参数为 city -> {}, facilityRequirement -> {}", city, facilityRequirement);

        String promptText = String.format(
                """
                        请查询%s的%s电竞馆/VR体验馆：
                        
                        请提供：
                        1. 推荐场所名称及配置详情
                        2. 价格及包间类型
                        3. 是否适合双人游玩
                        4. 地址和预订方式""",
                city, facilityRequirement
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具vrAndEsportsArenaTool执行完成");
        return result;
    }

    /**
     * 极速卡丁车与极限运动挑战工具
     *
     * @param location   位置
     * @param sportsType 运动类型，如："卡丁车", "蹦极"
     * @return String (极限运动情报)
     * @Description 为寻求刺激的成年人准备的肾上腺素飙升工具喵！查询市区的室内专业卡丁车赛道、或者周边的蹦极、高空跳伞基地及签署免责声明的要求。
     */
    @Tool(name = "extremeSportsChallengeTool", description = "为寻求刺激的成年人准备的肾上腺素飙升工具喵！查询市区的室内专业卡丁车赛道、或者周边的蹦极、高空跳伞基地及签署免责声明的要求。")
    public String extremeSportsChallengeTool(@ToolParam(description = "位置") String location, @ToolParam(description = "运动类型，如：\"卡丁车\", \"蹦极\"") String sportsType) {
        log.info("开始进入方法extremeSportsChallengeTool，参数为 location -> {}, sportsType -> {}", location, sportsType);

        String promptText = String.format(
                """
                        请查询%s的%s场所：
                        
                        请提供：
                        1. 推荐场所名称及特色
                        2. 安全要求和免责声明
                        3. 价格区间
                        4. 是否适合双人竞技/体验""",
                location, sportsType
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具extremeSportsChallengeTool执行完成");
        return result;
    }

    /**
     * 非遗手作与双人 DIY 体验工坊工具
     *
     * @param city          城市
     * @param workshopType  工坊类型，如："陶艺", "银饰"
     * @return String (工坊推荐)
     * @Description 提升情侣感情的完美助攻工具喵！查询那些提供双人制陶（拉胚）、银饰锻打、或者是广彩瓷器绘制的文艺手作工坊，并评估是否能当天带走成品。
     */
    @Tool(name = "culturalWorkshopAndDIYTool", description = "提升情侣感情的完美助攻工具喵！查询那些提供双人制陶（拉胚）、银饰锻打、或者是广彩瓷器绘制的文艺手作工坊，并评估是否能当天带走成品。")
    public String culturalWorkshopAndDIYTool(@ToolParam(description = "城市") String city, @ToolParam(description = "工坊类型，如：\"陶艺\", \"银饰\"") String workshopType) {
        log.info("开始进入方法culturalWorkshopAndDIYTool，参数为 city -> {}, workshopType -> {}", city, workshopType);

        String promptText = String.format(
                """
                        请查询%s的%s手作工坊：
                        
                        请提供：
                        1. 推荐工坊名称及特色
                        2. 双人体验价格和时长
                        3. 是否能当天带走成品
                        4. 是否需要提前预约""",
                city, workshopType
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具culturalWorkshopAndDIYTool执行完成");
        return result;
    }

    /**
     * 巨幕影院与冷门话剧排期查询工具
     *
     * @param city       城市
     * @param screenType 影厅类型，如："IMAX", "话剧"
     * @return String (排片表及最佳选座)
     * @Description 查验极致视听盛宴的工具喵！筛选出真正拥有二代激光 IMAX 或杜比影院（Dolby Cinema）的顶级影厅，或者是查询广州大剧院近期上演的先锋话剧及音乐剧排期。
     */
    @Tool(name = "cinemaAndTheaterShowtimeTool", description = "查验极致视听盛宴的工具喵！筛选出真正拥有二代激光 IMAX 或杜比影院（Dolby Cinema）的顶级影厅，或者是查询广州大剧院近期上演的先锋话剧及音乐剧排期。")
    public String cinemaAndTheaterShowtimeTool(@ToolParam(description = "城市") String city, @ToolParam(description = "影厅类型，如：\"IMAX\", \"话剧\"") String screenType) {
        log.info("开始进入方法cinemaAndTheaterShowtimeTool，参数为 city -> {}, screenType -> {}", city, screenType);

        String promptText = String.format(
                """
                        请查询%s的%s排期信息：
                        
                        请提供：
                        1. 近期排片/演出时间表
                        2. 推荐场次和最佳选座
                        3. 票价区间
                        4. 购票渠道和建议""",
                city, screenType
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具cinemaAndTheaterShowtimeTool执行完成");
        return result;
    }

    /**
     * 季节性灯光秀与烟花庆典捕捉工具
     *
     * @param city 城市
     * @param date 日期，格式：yyyy-MM-dd
     * @return String (庆典情报及机位)
     * @Description 跨越信息差的顶级浪漫探测器喵！捕捉那些转瞬即逝的城市灯光秀、节假日江边烟花燃放的具体时间、以及能完美避开人挤人的神仙观赏机位！
     */
    @Tool(name = "festivalAndLightShowTool", description = "跨越信息差的顶级浪漫探测器喵！捕捉那些转瞬即逝的城市灯光秀、节假日江边烟花燃放的具体时间、以及能完美避开人挤人的神仙观赏机位！")
    public String festivalAndLightShowTool(@ToolParam(description = "城市") String city, @ToolParam(description = "日期，格式：yyyy-MM-dd") String date) {
        log.info("开始进入方法festivalAndLightShowTool，参数为 city -> {}, date -> {}", city, date);

        String promptText = String.format(
                """
                        请查询%s在%s的灯光秀和烟花庆典：
                        
                        请提供：
                        1. 活动时间和地点
                        2. 最佳观赏机位推荐（避开人流）
                        3. 是否需要门票或预约
                        4. 交通和注意事项""",
                city, date
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具festivalAndLightShowTool执行完成");
        return result;
    }
}
