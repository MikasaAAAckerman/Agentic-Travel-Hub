package com.travel.hubtools.tool.food;

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
 * 周边高分餐厅检索工具
 */
@Slf4j
@Component
public class RestaurantSearchTool implements IAgentTool {

    @Autowired
    private ChatModel dashScopeChatModel;

    @Autowired
    private MapApiClient mapApiClient;

    /**
     * 基础附近美食与评分过滤工具
     *
     * @param location    当前位置
     * @param cuisineType 菜系，如：火锅、日料、粤菜
     * @param minRating   最低评分
     * @return String (推荐餐厅名称、人均消费、核心招牌菜及排队预警喵)
     * @Description 最基础的觅食工具喵！根据用户当前所在的商圈或地标，结合想吃的菜系，查询方圆 1 公里内的高分餐厅列表，并自动过滤掉评分低于 4.0 的避雷店！
     */
    @Tool(name = "nearbyRestaurantQueryTool", description = "最基础的觅食工具喵！根据用户当前所在的商圈或地标，结合想吃的菜系，查询方圆 1 公里内的高分餐厅列表，并自动过滤掉评分低于 4.0 的避雷店！")
    public String nearbyRestaurantQueryTool(@ToolParam(description = "位置") String location, @ToolParam(description = "菜系") String cuisineType
            , @ToolParam(description = "最低评分") double minRating, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法nearbyRestaurantQueryTool，参数为 location -> {},cuisineType -> {},minRating -> {},city -> {}", location, cuisineType, minRating, city);
        try {
            // 将位置转换为经纬度
            String coord = mapApiClient.geocode(location, city);

            if (coord == null) {
                return "抱歉喵，无法找到位置的坐标信息，请检查地址是否正确喵~";
            }

            // 使用高德地图POI周边搜索餐厅
            JSONObject result = mapApiClient.searchPOIAround(cuisineType + " 餐厅", coord, 1000, "050000");

            if (result == null) {
                return "抱歉喵，餐厅搜索失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            if (pois == null || pois.isEmpty()) {
                return String.format("抱歉喵，在%s附近1公里内没有找到%s餐厅喵~", location, cuisineType);
            }

            // 构建餐厅列表
            StringBuilder response = new StringBuilder();
            response.append(String.format("在【%s】附近找到 %d 家%s餐厅喵！\n\n", location, pois.size(), cuisineType));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                JSONObject poi = pois.getJSONObject(i);
                String name = poi.getString("name");
                String address = poi.getString("address");
                String tel = poi.getString("tel");
                String distance = poi.getString("distance");

                response.append(String.format("%d. 【%s】\n", i + 1, name));
                response.append(String.format("   距离：%s 米\n", distance));
                response.append(String.format("   地址：%s\n", address != null ? address : "未知"));

                if (tel != null && !tel.isEmpty()) {
                    response.append(String.format("   电话：%s\n", tel));
                }

                // 获取评分和人均消费
                JSONObject bizExt = poi.getJSONObject("biz_ext");
                if (bizExt != null) {
                    String rating = bizExt.getString("rating");
                    String cost = bizExt.getString("cost");
                    if (rating != null && !rating.isEmpty()) {
                        double ratingValue = Double.parseDouble(rating);
                        if (ratingValue >= minRating) {
                            response.append(String.format("   评分：%s 分（符合您的要求）\n", rating));
                        } else {
                            response.append(String.format("   评分：%s 分（低于您的要求%.1f分）\n", rating, minRating));
                        }
                    }
                    if (cost != null && !cost.isEmpty()) {
                        response.append(String.format("   人均消费：%s 元\n", cost));
                    }
                }

                response.append("\n");
            }

            response.append("需要更多详情可以告诉我喵~");

            log.info("工具nearbyRestaurantQueryTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具nearbyRestaurantQueryTool执行异常", e);
            return "抱歉喵，餐厅搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 严苛饮食禁忌与过敏原筛查工具
     *
     * @param location     位置
     * @param restrictions 禁忌列表，如：["无麸质", "花生过敏", "纯素食"]
     * @return String (符合要求的餐厅，并明确指出该餐厅是否有"防交叉感染"的后厨操作规范喵)
     * @Description 保命级别的工具！专门为有清真要求、纯素食主义（Vegan）、或者对花生、海鲜、麸质严重过敏的用户，精准筛选提供专属菜单或后厨完全隔离的绝对安全餐厅喵！
     */
    @Tool(name = "dietaryRestrictionTool", description = "保命级别的工具！专门为有清真要求、纯素食主义（Vegan）、或者对花生、海鲜、麸质严重过敏的用户，精准筛选提供专属菜单或后厨完全隔离的绝对安全餐厅喵！")
    public String dietaryRestrictionTool(@ToolParam(description = "位置") String location, @ToolParam(description = "禁忌列表") List<String> restrictions) {
        log.info("开始进入方法dietaryRestrictionTool，参数为 location -> {}, restrictions -> {}", location, restrictions);

        String promptText = String.format(
                """
                        请查询位置【%s】符合以下饮食禁忌的餐厅：
                        禁忌列表：%s
                        
                        请提供：
                        1. 符合要求的餐厅
                        2. 该餐厅是否有\"防交叉感染\"的后厨操作规范""",
                location, restrictions.toString()
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具dietaryRestrictionTool执行完成");
        return result;
    }

    /**
     * 实时等位与智能取号探测工具
     *
     * @param restaurantName 餐厅名称
     * @return String (如"当前小桌前方等待 45 桌，预计耗时 90 分钟，强烈建议立即调用线上取号接口"喵)
     * @Description 解决"肚子饿得咕咕叫还要排队两小时"痛点的神器喵！查询特定热门餐厅当前的实时排队桌数，以及是否支持线上提前取号或支付定金预订。
     */
    @Tool(name = "liveQueueAndReservationTool", description = "解决\"肚子饿得咕咕叫还要排队两小时\"痛点的神器喵！查询特定热门餐厅当前的实时排队桌数，以及是否支持线上提前取号或支付定金预订。")
    public String liveQueueAndReservationTool(@ToolParam(description = "餐厅名称") String restaurantName) {
        log.info("开始进入方法liveQueueAndReservationTool，参数为 restaurantName -> {}", restaurantName);

        String promptText = String.format(
                """
                        请查询餐厅【%s】的实时排队情况：
                        
                        请提供：
                        1. 当前实时排队桌数
                        2. 预计耗时
                        3. 是否支持线上提前取号或支付定金预订""",
                restaurantName
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具liveQueueAndReservationTool执行完成");
        return result;
    }

    /**
     * 社恐与一人食友好度评估工具
     *
     * @param location 位置
     * @param mealType 用餐类型，如：简餐、烤肉
     * @return String (社恐友好度评分，如"提供带隔板的单人拉面座，全程无接触上菜，社恐天堂喵")
     * @Description 情绪价值拉满喵！专门为独自旅行或重度社恐用户查询：餐厅是否提供"面壁单人座"、是否支持全程扫码点餐结账（无需和老服务员说话），以及是否足够安静。
     */
    @Tool(name = "soloDiningFriendlyTool", description = "情绪价值拉满喵！专门为独自旅行或重度社恐用户查询：餐厅是否提供\"面壁单人座\"、是否支持全程扫码点餐结账（无需和老服务员说话），以及是否足够安静。")
    public String soloDiningFriendlyTool(@ToolParam(description = "位置") String location, @ToolParam(description = "用餐类型") String mealType) {
        log.info("开始进入方法soloDiningFriendlyTool，参数为 location -> {}, mealType -> {}", location, mealType);

        String promptText = String.format(
                """
                        请评估位置【%s】的%s餐厅社恐友好度：
                        
                        请提供：
                        1. 是否提供\"面壁单人座\"
                        2. 是否支持全程扫码点餐结账
                        3. 环境是否足够安静
                        4. 社恐友好度评分""",
                location, mealType
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具soloDiningFriendlyTool执行完成");
        return result;
    }

    /**
     * 米其林与高端餐饮礼仪预警工具
     *
     * @param restaurantName 餐厅名称
     * @return String (比如："必须穿带领衬衫和包头鞋，严禁男士穿短裤拖鞋，需提前两周预约并支付 50% 定金喵")
     * @Description 高级避坑指南！查询特定的黑珍珠或米其林餐厅是否有严格的着装要求（Dress Code），以及是否有高昂的开瓶费、最低消费或强制的服务费比例喵。
     */
    @Tool(name = "fineDiningEtiquetteTool", description = "高级避坑指南！查询特定的黑珍珠或米其林餐厅是否有严格的着装要求（Dress Code），以及是否有高昂的开瓶费、最低消费或强制的服务费比例喵。")
    public String fineDiningEtiquetteTool(@ToolParam(description = "餐厅名称") String restaurantName) {
        log.info("开始进入方法fineDiningEtiquetteTool，参数为 restaurantName -> {}", restaurantName);

        String promptText = String.format(
                """
                        请查询餐厅【%s】的高端餐饮礼仪要求：
                        
                        请提供：
                        1. 是否有严格的着装要求（Dress Code）
                        2. 是否有高昂的开瓶费、最低消费或强制的服务费比例
                        3. 预约要求和定金政策""",
                restaurantName
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具fineDiningEtiquetteTool执行完成");
        return result;
    }

    /**
     * 深夜食堂与跨更供餐查询工具
     *
     * @param location 位置
     * @param time     时间，如 "02:00 AM"
     * @return String (深夜营业的餐厅列表、深夜专供菜单以及周边的治安安全提示喵)
     * @Description 拯救深夜加班狗和夜猫子的工具喵！查询指定区域在凌晨 12 点以后依然提供堂食的热门夜宵档、大排档或 24 小时快餐。
     */
    @Tool(name = "midnightSnackTool", description = "拯救深夜加班狗和夜猫子的工具喵！查询指定区域在凌晨 12 点以后依然提供堂食的热门夜宵档、大排档或 24 小时快餐。")
    public String midnightSnackTool(@ToolParam(description = "位置") String location, @ToolParam(description = "时间") String time, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法midnightSnackTool，参数为 location -> {}, time -> {}, city -> {}", location, time, city);

        try {
            // 使用高德POI搜索夜宵/深夜餐饮
            JSONObject result = mapApiClient.searchPOI("夜宵 深夜食堂", city, "050000", 10, 1);

            if (result == null) {
                return "抱歉喵，深夜食堂搜索失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            if (pois == null || pois.isEmpty()) {
                return String.format("抱歉喵，在%s没有找到深夜营业的餐厅喵~", location);
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("在【%s】深夜%s找到以下夜宵好去处喵！\n\n", location, time));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                JSONObject poi = pois.getJSONObject(i);
                response.append(formatPoiResult(i + 1, poi));
            }

            response.append("深夜出行请注意安全，建议结伴前往喵~");
            log.info("工具midnightSnackTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具midnightSnackTool执行异常", e);
            return "抱歉喵，深夜食堂搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 本地土著防坑与苍蝇馆子挖掘工具
     *
     * @param city      城市
     * @param localDish 特色小吃，如：豆汁儿、肠粉
     * @return String (避开商业街的隐秘小店定位、环境简陋预警，以及必须带现金的提示喵)
     * @Description 专门用来避开"专宰外地游客的网红美食街"的硬核工具喵！挖掘只存在于老旧小区里、环境一般但味道绝佳、只有本地老头老太去吃的正宗苍蝇馆子！
     */
    @Tool(name = "authenticLocalFoodTool", description = "专门用来避开\"专宰外地游客的网红美食街\"的硬核工具喵！挖掘只存在于老旧小区里、环境一般但味道绝佳、只有本地老头老太去吃的正宗苍蝇馆子！")
    public String authenticLocalFoodTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "特色小吃") String localDish) {
        log.info("开始进入方法authenticLocalFoodTool，参数为 city -> {}, localDish -> {}", city, localDish);

        try {
            // 搜索地道小吃店
            JSONObject result = mapApiClient.searchPOI(localDish + " 地道 老店", city, "050000", 10, 1);

            if (result == null) {
                return "抱歉喵，地道美食搜索失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            if (pois == null || pois.isEmpty()) {
                return String.format("抱歉喵，在%s没有找到%s的地道美食喵~", city, localDish);
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s地道美食挖掘结果喵！\n\n", city, localDish));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                JSONObject poi = pois.getJSONObject(i);
                response.append(formatPoiResult(i + 1, poi));
            }

            response.append("这些都是本地人爱去的口碑老店，避开网红街的套路喵~\n");
            response.append("温馨提示：苍蝇馆子环境简陋但味道一流，建议自备纸巾，部分小店可能只收现金喵~");
            log.info("工具authenticLocalFoodTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具authenticLocalFoodTool执行异常", e);
            return "抱歉喵，地道美食搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 商务宴请与高密闭性包厢检索工具
     *
     * @param location    位置
     * @param guestCount  人数
     * @param needInvoice 是否需要发票
     * @return String (包厢最低消费标准、是否有专车接送通道，以及发票开具的便利度喵)
     * @Description 为出差谈几个亿大项目的用户准备喵！严格筛选提供绝对隔音包厢、有专属服务员、方便泊车且能顺利开具增值税专用发票的高端宴请酒楼。
     */
    @Tool(name = "businessBanquetTool", description = "为出差谈几个亿大项目的用户准备喵！严格筛选提供绝对隔音包厢、有专属服务员、方便泊车且能顺利开具增值税专用发票的高端宴请酒楼。")
    public String businessBanquetTool(@ToolParam(description = "位置") String location, @ToolParam(description = "人数") int guestCount, @ToolParam(description = "是否需要发票") boolean needInvoice) {
        log.info("开始进入方法businessBanquetTool，参数为 location -> {}, guestCount -> {}, needInvoice -> {}", location, guestCount, needInvoice);

        try {
            JSONObject result = mapApiClient.searchPOI("商务宴请 包厢 中式酒楼", location, "050000", 10, 1);

            if (result == null || result.getJSONArray("pois") == null || result.getJSONArray("pois").isEmpty()) {
                return String.format("抱歉喵，在%s没有找到适合商务宴请的餐厅喵~", location);
            }

            JSONArray pois = result.getJSONArray("pois");
            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】商务宴请餐厅（%d人）喵！\n\n", location, guestCount));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                JSONObject poi = pois.getJSONObject(i);
                response.append(formatPoiResult(i + 1, poi));
            }

            response.append(needInvoice ? "建议提前致电确认包厢最低消费和发票开具事宜喵~" : "建议提前致电确认包厢最低消费喵~");
            log.info("工具businessBanquetTool执行完毕");
            return response.toString();

        } catch (Exception e) {
            log.error("工具businessBanquetTool执行异常", e);
            return "抱歉喵，商务宴请搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 宝妈喘息与硬核亲子餐厅查询工具
     *
     * @param location 位置
     * @param kidAge   儿童年龄
     * @return String (比如：\"不仅提供儿童餐，还有 50 平方米的海洋球池和免费看护小姐姐，家长可以安心干饭喵！\")
     * @Description 带娃出行的救星喵！查询餐厅是否提供符合安全标准的宝宝椅、无盐无糖的儿童特供餐，以及最重要的——是否有带专人看护的室内儿童淘气堡！
     */
    @Tool(name = "kidFriendlyDiningTool", description = "带娃出行的救星喵！查询餐厅是否提供符合安全标准的宝宝椅、无盐无糖的儿童特供餐，以及最重要的——是否有带专人看护的室内儿童淘气堡！")
    public String kidFriendlyDiningTool(@ToolParam(description = "位置") String location, @ToolParam(description = "儿童年龄") int kidAge, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法kidFriendlyDiningTool，参数为 location -> {}, kidAge -> {}, city -> {}", location, kidAge, city);

        try {
            JSONObject result = mapApiClient.searchPOI("亲子餐厅 儿童餐 亲子乐园", city, "050000", 10, 1);

            if (result == null) {
                return "抱歉喵，亲子餐厅搜索失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            if (pois == null || pois.isEmpty()) {
                return String.format("抱歉喵，在%s附近没有找到适合%d岁儿童的亲子餐厅喵~", location, kidAge);
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】附近亲子餐厅（适合%d岁儿童）喵！\n\n", location, kidAge));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                JSONObject poi = pois.getJSONObject(i);
                response.append(formatPoiResult(i + 1, poi));
            }

            response.append("建议提前致电确认是否提供宝宝椅、儿童餐及游乐设施喵~");
            log.info("工具kidFriendlyDiningTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具kidFriendlyDiningTool执行异常", e);
            return "抱歉喵，亲子餐厅搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 宠物共餐与毛孩子友好菜单工具
     *
     * @param location 位置
     * @return String (宠物入内规则：是只能坐外摆区还是可以进室内，以及宠物特供菜单的价格喵)
     * @Description 带着主子去干饭喵！查询不仅允许宠物入内，甚至专门为猫狗提供\"无添加宠物特供披萨/牛排\"的硬核宠物友好餐厅。
     */
    @Tool(name = "petDiningTool", description = "带着主子去干饭喵！查询不仅允许宠物入内，甚至专门为猫狗提供\"无添加宠物特供披萨/牛排\"的硬核宠物友好餐厅。")
    public String petDiningTool(@ToolParam(description = "位置") String location, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法petDiningTool，参数为 location -> {}, city -> {}", location, city);

        try {
            String coord = mapApiClient.geocode(location, city);

            if (coord == null) {
                return "抱歉喵，无法找到位置的坐标信息，请检查地址是否正确喵~";
            }

            JSONObject result = mapApiClient.searchPOIAround("宠物友好 餐厅", coord, 3000, "050000");

            if (result == null) {
                return "抱歉喵，宠物友好餐厅搜索失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            if (pois == null || pois.isEmpty()) {
                return String.format("抱歉喵，在%s附近没有找到宠物友好餐厅喵~", location);
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】附近宠物友好餐厅喵！\n\n", location));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                JSONObject poi = pois.getJSONObject(i);
                String distance = poi.getString("distance");
                response.append(formatPoiResult(i + 1, poi));
                response.append(String.format("   距离：%s 米\n", distance));
            }

            response.append("建议提前致电确认宠物入内政策（外摆区/室内）及是否提供宠物餐喵~");
            log.info("工具petDiningTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具petDiningTool执行异常", e);
            return "抱歉喵，宠物友好餐厅搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 高空景观与露台餐厅查验工具
     *
     * @param city     城市
     * @param viewType 景观类型，如：外滩江景、高空夜景
     * @return String (绝佳景观餐厅列表，并附带"靠窗位需提前 30 天预订且需支付 500 元定金"的防坑提示喵)
     * @Description 情绪价值与约会必杀工具！专门筛选那些位于摩天大楼顶层、或者拥有绝佳江景/海景露台的浪漫餐厅，并评估其靠窗座位的预订难度喵。
     */
    @Tool(name = "rooftopScenicDiningTool", description = "情绪价值与约会必杀工具！专门筛选那些位于摩天大楼顶层、或者拥有绝佳江景/海景露台的浪漫餐厅，并评估其靠窗座位的预订难度喵。")
    public String rooftopScenicDiningTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "景观类型") String viewType) {
        log.info("开始进入方法rooftopScenicDiningTool，参数为 city -> {}, viewType -> {}", city, viewType);

        try {
            String keywords = viewType + " 景观餐厅 露台 高空";
            JSONObject result = mapApiClient.searchPOI(keywords, city, "050000", 10, 1);

            if (result == null) {
                return "抱歉喵，景观餐厅搜索失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            if (pois == null || pois.isEmpty()) {
                return String.format("抱歉喵，在%s没有找到%s景观餐厅喵~", city, viewType);
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s景观餐厅喵！\n\n", city, viewType));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                JSONObject poi = pois.getJSONObject(i);
                response.append(formatPoiResult(i + 1, poi));
            }

            response.append("景观位通常需要提前预订，靠窗座位建议提前1-2周预约喵~");
            log.info("工具rooftopScenicDiningTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具rooftopScenicDiningTool执行异常", e);
            return "抱歉喵，景观餐厅搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 超大多人聚餐与团建适配工具
     *
     * @param location  位置
     * @param groupSize 人数，如：20
     * @return String (提供大圆桌的餐厅、包场最低消费额度以及团建专属套餐报价喵)
     * @Description 拯救 HR 和团建组织者的神器！专门查询餐厅是否提供能容纳 15 人以上的连坐大长桌或超大圆桌包厢，以及是否支持极其复杂的 AA 制分账和开具集体发票喵。
     */
    @Tool(name = "largeGroupGatheringTool", description = "拯救 HR 和团建组织者的神器！专门查询餐厅是否提供能容纳 15 人以上的连坐大长桌或超大圆桌包厢，以及是否支持极其复杂的 AA 制分账和开具集体发票喵。")
    public String largeGroupGatheringTool(@ToolParam(description = "位置") String location, @ToolParam(description = "人数") int groupSize) {
        log.info("开始进入方法largeGroupGatheringTool，参数为 location -> {}, groupSize -> {}", location, groupSize);

        try {
            JSONObject result = mapApiClient.searchPOI("大桌 包场 团建 聚餐", location, "050000", 10, 1);

            if (result == null || result.getJSONArray("pois") == null || result.getJSONArray("pois").isEmpty()) {
                return String.format("抱歉喵，在%s没有找到适合%d人聚餐的餐厅喵~", location, groupSize);
            }

            JSONArray pois = result.getJSONArray("pois");
            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%d人聚餐餐厅喵！\n\n", location, groupSize));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                response.append(formatPoiResult(i + 1, pois.getJSONObject(i)));
            }

            response.append("建议提前致电确认包厢容量、最低消费和包场政策喵~");
            log.info("工具largeGroupGatheringTool执行完毕");
            return response.toString();

        } catch (Exception e) {
            log.error("工具largeGroupGatheringTool执行异常", e);
            return "抱歉喵，聚餐搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 驻唱与演艺主题餐饮探测工具
     *
     * @param location        位置
     * @param performanceType 演出类型，如：爵士乐、脱口秀
     * @return String (演出时刻表、演出期间的噪音等级评估，以及是否需要额外收取门票/卡座费喵)
     * @Description 查询特定餐厅或 Livehouse 在用户就餐时段是否有乐队驻唱、脱口秀表演或特色民族舞蹈，并提供演出具体的时间表喵。
     */
    @Tool(name = "livePerformanceDiningTool", description = "查询特定餐厅或 Livehouse 在用户就餐时段是否有乐队驻唱、脱口秀表演或特色民族舞蹈，并提供演出具体的时间表喵。")
    public String livePerformanceDiningTool(@ToolParam(description = "位置") String location, @ToolParam(description = "演出类型") String performanceType) {
        log.info("开始进入方法livePerformanceDiningTool，参数为 location -> {}, performanceType -> {}", location, performanceType);

        try {
            JSONObject result = mapApiClient.searchPOI(performanceType + " 驻唱 音乐餐厅", location, "050000", 10, 1);
            if (result == null || result.getJSONArray("pois") == null || result.getJSONArray("pois").isEmpty()) {
                return String.format("抱歉喵，在%s没有找到%s表演的餐厅喵~", location, performanceType);
            }
            JSONArray pois = result.getJSONArray("pois");
            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s表演餐厅喵！\n\n", location, performanceType));
            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                response.append(formatPoiResult(i + 1, pois.getJSONObject(i)));
            }
            response.append("演出时间和门票政策请致电确认喵~");
            log.info("工具livePerformanceDiningTool执行完毕");
            return response.toString();
        } catch (Exception e) {
            log.error("工具livePerformanceDiningTool执行异常", e);
            return "抱歉喵，搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 严格减脂与卡路里透明餐饮工具
     */
    @Tool(name = "fitnessMacroDietTool", description = "健身狂魔的福音！精准检索那些提供详细卡路里标识（Macros）、支持把沙拉酱换成油醋汁分开装，或者专门提供水煮鸡胸肉/低GI主食的硬核轻食餐厅喵。")
    public String fitnessMacroDietTool(@ToolParam(description = "位置") String location, @ToolParam(description = "饮食目标") String dietGoal, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法fitnessMacroDietTool，参数为 location -> {}, dietGoal -> {}, city -> {}", location, dietGoal, city);
        try {
            String keywords = dietGoal.contains("减脂") ? "轻食 沙拉 健康餐 低卡" : "高蛋白 增肌餐 健身餐";
            JSONObject result = mapApiClient.searchPOI(keywords, city, "050000", 10, 1);
            if (result == null || result.getJSONArray("pois") == null || result.getJSONArray("pois").isEmpty()) {
                return String.format("抱歉喵，在%s没有找到%s餐厅喵~", location, dietGoal);
            }
            JSONArray pois = result.getJSONArray("pois");
            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s餐厅喵！\n\n", location, dietGoal));
            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                response.append(formatPoiResult(i + 1, pois.getJSONObject(i)));
            }
            response.append("卡路里和Macros信息请到店查看菜单标注喵~");
            log.info("工具fitnessMacroDietTool执行完毕");
            return response.toString();
        } catch (Exception e) {
            log.error("工具fitnessMacroDietTool执行异常", e);
            return "抱歉喵，搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 泊车难度与代客泊车检索工具
     *
     * @param restaurantName 餐厅名称
     * @return String (泊车指南，如"门口交警贴条极严，强烈建议停在马路对面的商场，或者使用其 50 元/次的代客泊车服务喵")
     * @Description 自驾干饭人的刚需！查询位于拥堵市中心的餐厅是否提供专属免费车位、地下车库入口的寻找难度，以及是否提供高规格的代客泊车（Valet）服务喵。
     */
    @Tool(name = "diningParkingValetTool", description = "自驾干饭人的刚需！查询位于拥堵市中心的餐厅是否提供专属免费车位、地下车库入口的寻找难度，以及是否提供高规格的代客泊车（Valet）服务喵。")
    public String diningParkingValetTool(@ToolParam(description = "餐厅名称") String restaurantName, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法diningParkingValetTool，参数为 restaurantName -> {}, city -> {}", restaurantName, city);

        try {
            String coord = mapApiClient.geocode(restaurantName, city);

            if (coord == null) {
                return "抱歉喵，无法找到餐厅的坐标信息，请检查餐厅名称是否正确喵~";
            }

            // 搜索周边停车场
            JSONObject result = mapApiClient.searchPOIAround("停车场", coord, 500, "150904");

            if (result == null) {
                return "抱歉喵，停车信息查询失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】周边停车指南喵！\n\n", restaurantName));

            if (pois != null && !pois.isEmpty()) {
                response.append(String.format("附近500米内有%d个停车场：\n", pois.size()));
                for (int i = 0; i < Math.min(pois.size(), 3); i++) {
                    JSONObject poi = pois.getJSONObject(i);
                    String name = poi.getString("name");
                    String distance = poi.getString("distance");
                    String address = poi.getString("address");
                    response.append(String.format("%d. %s（距离%s米）\n", i + 1, name, distance));
                    if (address != null && !address.isEmpty()) {
                        response.append(String.format("   地址：%s\n", address));
                    }
                }
            } else {
                response.append("附近500米内未找到停车场喵~\n");
            }

            response.append("市中心停车位紧张，建议优先考虑代客泊车或公共交通出行喵~");
            log.info("工具diningParkingValetTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具diningParkingValetTool执行异常", e);
            return "抱歉喵，停车信息查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 早午餐与独立烘焙咖啡馆挖掘工具
     *
     * @param location 位置
     * @return String (环境慵懒且有手冲咖啡加持的 Brunch 店，以及它们最招牌的当季面包喵)
     * @Description 专为周末睡到自然醒的慵懒打工人准备！挖掘不仅提供高质量 Brunch（如班尼迪克蛋），还拥有自家烘焙咖啡豆和手工酵母面包的独立小众咖啡馆喵。
     */
    @Tool(name = "brunchAndBakeryTool", description = "专为周末睡到自然醒的慵懒打工人准备！挖掘不仅提供高质量 Brunch（如班尼迪克蛋），还拥有自家烘焙咖啡豆和手工酵母面包的独立小众咖啡馆喵。")
    public String brunchAndBakeryTool(@ToolParam(description = "位置") String location, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法brunchAndBakeryTool，参数为 location -> {}, city -> {}", location, city);

        try {
            // 搜索 Brunch 咖啡馆和面包店
            JSONObject result = mapApiClient.searchPOI("Brunch 咖啡馆 面包店 烘焙", city, "050200", 10, 1);

            if (result == null) {
                return "抱歉喵，Brunch和咖啡馆搜索失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            if (pois == null || pois.isEmpty()) {
                return String.format("抱歉喵，在%s没有找到Brunch和咖啡馆喵~", location);
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】Brunch & 精品咖啡馆喵！\n\n", location));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                JSONObject poi = pois.getJSONObject(i);
                response.append(formatPoiResult(i + 1, poi));
            }

            response.append("慵懒的周末从一顿Brunch开始，搭配手冲咖啡和手工面包，完美喵~");
            log.info("工具brunchAndBakeryTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具brunchAndBakeryTool执行异常", e);
            return "抱歉喵，Brunch搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 夜市摊位与流动小吃定位工具
     *
     * @param city       城市
     * @param marketName 夜市名称
     * @return String (当晚夜市的必吃摊位排雷、摊主出摊概率，以及"必须自备纸巾和零钱"的提示喵)
     * @Description 地气十足的工具！这不仅能搜到固定餐厅，还能结合当地城管的规定和社交媒体情报，预测那些极其好吃但居无定所的"神出鬼没流动小吃摊"的当晚出摊位置喵！
     */
    @Tool(name = "nightMarketStallTool", description = "地气十足的工具！这不仅能搜到固定餐厅，还能结合当地城管的规定和社交媒体情报，预测那些极其好吃但居无定所的\"神出鬼没流动小吃摊\"的当晚出摊位置喵！")
    public String nightMarketStallTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "夜市名称") String marketName) {
        log.info("开始进入方法nightMarketStallTool，参数为 city -> {}, marketName -> {}", city, marketName);

        try {
            String keywords = (marketName != null && !marketName.isEmpty()) ? marketName : "夜市";
            JSONObject result = mapApiClient.searchPOI(keywords, city, "050000", 10, 1);
            if (result == null || result.getJSONArray("pois") == null || result.getJSONArray("pois").isEmpty()) {
                return String.format("抱歉喵，在%s没有找到夜市喵~", city);
            }
            JSONArray pois = result.getJSONArray("pois");
            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】夜市美食喵！\n\n", city));
            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                response.append(formatPoiResult(i + 1, pois.getJSONObject(i)));
            }
            response.append("夜市摊位可能有变动，建议自备纸巾和零钱喵~");
            log.info("工具nightMarketStallTool执行完毕");
            return response.toString();
        } catch (Exception e) {
            log.error("工具nightMarketStallTool执行异常", e);
            return "抱歉喵，夜市搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 历史建筑与沉浸式主题餐厅工具
     */
    @Tool(name = "historicalThemedDiningTool", description = "查询那些开设在百年老洋房、古镇四合院，或者具有极强二次元属性（如女仆咖啡厅、动漫联名快闪店）的沉浸式餐厅，提供打破次元壁的用餐体验喵。")
    public String historicalThemedDiningTool(@ToolParam(description = "位置") String location, @ToolParam(description = "主题") String theme, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法historicalThemedDiningTool，参数为 location -> {}, theme -> {}, city -> {}", location, theme, city);
        try {
            JSONObject result = mapApiClient.searchPOI(theme + " 主题餐厅", city, "050000", 10, 1);
            if (result == null || result.getJSONArray("pois") == null || result.getJSONArray("pois").isEmpty()) {
                return String.format("抱歉喵，在%s没有找到%s主题餐厅喵~", location, theme);
            }
            JSONArray pois = result.getJSONArray("pois");
            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s主题餐厅喵！\n\n", location, theme));
            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                response.append(formatPoiResult(i + 1, pois.getJSONObject(i)));
            }
            response.append("沉浸式餐厅可能有预约和角色扮演规则，建议提前了解喵~");
            log.info("工具historicalThemedDiningTool执行完毕");
            return response.toString();
        } catch (Exception e) {
            log.error("工具historicalThemedDiningTool执行异常", e);
            return "抱歉喵，搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 无菜单料理与当季限定食材探测工具
     */
    @Tool(name = "omakaseAndSeasonalTool", description = "土豪与老饕专属！查询高端日料 Omakase 或创意法餐当前的\"当季限定菜单\"（如：秋季的蓝鳍金枪鱼、春季的白芦笋），并评估其食材新鲜度溢价喵。")
    public String omakaseAndSeasonalTool(@ToolParam(description = "城市名称") String city, @ToolParam(description = "菜系风格") String cuisineStyle) {
        log.info("开始进入方法omakaseAndSeasonalTool，参数为 city -> {}, cuisineStyle -> {}", city, cuisineStyle);
        try {
            String keywords = cuisineStyle + " 高端料理 Omakase";
            JSONObject result = mapApiClient.searchPOI(keywords, city, "050000", 10, 1);
            if (result == null || result.getJSONArray("pois") == null || result.getJSONArray("pois").isEmpty()) {
                return String.format("抱歉喵，在%s没有找到%s高端料理喵~", city, cuisineStyle);
            }
            JSONArray pois = result.getJSONArray("pois");
            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】%s高端料理喵！\n\n", city, cuisineStyle));
            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                response.append(formatPoiResult(i + 1, pois.getJSONObject(i)));
            }
            response.append("高端料理通常需提前预约，当季菜单请致电咨询喵~");
            log.info("工具omakaseAndSeasonalTool执行完毕");
            return response.toString();
        } catch (Exception e) {
            log.error("工具omakaseAndSeasonalTool执行异常", e);
            return "抱歉喵，搜索过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 饭后消食与周边散步路线匹配工具
     *
     * @param restaurantName 餐厅名称
     * @return String (比如："吃完这家惠灵顿牛排，出门右转步行 200 米就是无敌江景漫步道，约会成功率直线上升喵！")
     * @Description 跨域整合的神级工具！在推荐完大餐后，顺便查询餐厅出门后是否有适合牵手散步的林荫小道、滨江步道或城市公园，为完美的约会画上句号喵。
     */
    @Tool(name = "postMealWalkabilityTool", description = "跨域整合的神级工具！在推荐完大餐后，顺便查询餐厅出门后是否有适合牵手散步的林荫小道、滨江步道或城市公园，为完美的约会画上句号喵。")
    public String postMealWalkabilityTool(@ToolParam(description = "餐厅名称") String restaurantName, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法postMealWalkabilityTool，参数为 restaurantName -> {}, city -> {}", restaurantName, city);

        try {
            String coord = mapApiClient.geocode(restaurantName, city);

            if (coord == null) {
                return "抱歉喵，无法找到餐厅的坐标信息，请检查餐厅名称是否正确喵~";
            }

            // 搜索周边公园、景点、滨江步道
            JSONObject result = mapApiClient.searchPOIAround("公园 景区 步道", coord, 1000, "110000");

            if (result == null) {
                return "抱歉喵，周边散步路线查询失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            StringBuilder response = new StringBuilder();
            response.append(String.format("吃完【%s】去哪散步喵？\n\n", restaurantName));

            if (pois != null && !pois.isEmpty()) {
                response.append("附近适合散步的好去处：\n");
                for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                    JSONObject poi = pois.getJSONObject(i);
                    String name = poi.getString("name");
                    String distance = poi.getString("distance");
                    String type = poi.getString("type");
                    response.append(String.format("%d. %s（%s米）\n", i + 1, name, distance));
                    if (type != null && !type.isEmpty()) {
                        response.append(String.format("   类型：%s\n", type));
                    }
                }
            } else {
                response.append("附近1公里内未找到公园或散步道喵~\n");
            }

            response.append("吃完饭散散步，约会成功率直线上升喵~");
            log.info("工具postMealWalkabilityTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具postMealWalkabilityTool执行异常", e);
            return "抱歉喵，散步路线查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 格式化POI结果为统一输出格式
     */
    private String formatPoiResult(int index, JSONObject poi) {
        StringBuilder sb = new StringBuilder();
        String name = poi.getString("name");
        String address = poi.getString("address");
        String tel = poi.getString("tel");

        sb.append(String.format("%d. 【%s】\n", index, name));
        if (address != null && !address.isEmpty()) {
            sb.append(String.format("   地址：%s\n", address));
        }
        if (tel != null && !tel.isEmpty()) {
            sb.append(String.format("   电话：%s\n", tel));
        }

        JSONObject bizExt = poi.getJSONObject("biz_ext");
        if (bizExt != null) {
            String rating = bizExt.getString("rating");
            String cost = bizExt.getString("cost");
            if (rating != null && !rating.isEmpty()) {
                sb.append(String.format("   评分：%s 分\n", rating));
            }
            if (cost != null && !cost.isEmpty()) {
                sb.append(String.format("   人均消费：%s 元\n", cost));
            }
        }
        sb.append("\n");
        return sb.toString();
    }
}