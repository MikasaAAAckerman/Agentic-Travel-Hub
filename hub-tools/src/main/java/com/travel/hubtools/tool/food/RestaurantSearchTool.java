package com.travel.hubtools.tool.food;

import com.travel.hubtools.client.TavilyApiClient;
import com.travel.hubtools.tool.common.IAgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 餐厅美食工具（全部走 Tavily 搜索）
 */
@Slf4j
@Component
public class RestaurantSearchTool implements IAgentTool {

    @Autowired
    private TavilyApiClient tavilyApiClient;

    @Tool(name = "nearbyRestaurantQueryTool", description = "最基础的觅食工具喵！根据用户当前所在的商圈或地标，结合想吃的菜系，查询高分餐厅列表。")
    public String nearbyRestaurantQueryTool(@ToolParam(description = "位置") String location, @ToolParam(description = "菜系") String cuisineType, @ToolParam(description = "最低评分") double minRating, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法nearbyRestaurantQueryTool，参数为 location -> {},cuisineType -> {},minRating -> {},city -> {}", location, cuisineType, minRating, city);
        String query = String.format("%s %s %s 餐厅 推荐 评分", city, location, cuisineType);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "midnightSnackTool", description = "拯救深夜加班狗和夜猫子的工具喵！查询凌晨12点以后依然提供堂食的热门夜宵档。")
    public String midnightSnackTool(@ToolParam(description = "位置") String location, @ToolParam(description = "时间") String time, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法midnightSnackTool，参数为 location -> {}, time -> {}, city -> {}", location, time, city);
        String query = String.format("%s %s 深夜食堂 夜宵 24小时", city, location);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "authenticLocalFoodTool", description = "专治水土不服！查询本地人最爱的地道苍蝇馆子和非遗老字号。")
    public String authenticLocalFoodTool(@ToolParam(description = "城市") String city, @ToolParam(description = "想吃的当地特色") String localDish) {
        log.info("开始进入方法authenticLocalFoodTool，参数为 city -> {}, localDish -> {}", city, localDish);
        String query = String.format("%s 地道 %s 老字号 本地人推荐", city, localDish);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "businessBanquetTool", description = "请客吃饭不丢面子！查询环境优雅、有包厢、服务一流的高端中式酒楼。")
    public String businessBanquetTool(@ToolParam(description = "位置") String location, @ToolParam(description = "预算档次") String budgetLevel) {
        log.info("开始进入方法businessBanquetTool，参数为 location -> {}, budgetLevel -> {}", location, budgetLevel);
        String query = String.format("%s 商务宴请 包厢 中式酒楼 %s", location, budgetLevel);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "kidFriendlyDiningTool", description = "带娃吃饭不愁！查询有儿童餐、儿童乐园、宝宝椅的亲子友好餐厅。")
    public String kidFriendlyDiningTool(@ToolParam(description = "城市") String city, @ToolParam(description = "位置") String location) {
        log.info("开始进入方法kidFriendlyDiningTool，参数为 city -> {}, location -> {}", city, location);
        String query = String.format("%s %s 亲子餐厅 儿童餐 儿童乐园", city, location);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "petDiningTool", description = "毛孩子也能一起吃饭！查询允许携带宠物入座的餐厅。")
    public String petDiningTool(@ToolParam(description = "位置") String location, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法petDiningTool，参数为 location -> {}, city -> {}", location, city);
        String query = String.format("%s %s 宠物友好餐厅 携带宠物", city, location);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "rooftopScenicDiningTool", description = "约会神器！查询有露台、江景、海景或高空景观的浪漫餐厅。")
    public String rooftopScenicDiningTool(@ToolParam(description = "城市") String city, @ToolParam(description = "景观类型") String viewType) {
        log.info("开始进入方法rooftopScenicDiningTool，参数为 city -> {}, viewType -> {}", city, viewType);
        String query = String.format("%s %s 露台餐厅 景观餐厅 浪漫", city, viewType);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "largeGroupGatheringTool", description = "团建聚餐不愁！查询能容纳10人以上大桌、有包场服务的餐厅。")
    public String largeGroupGatheringTool(@ToolParam(description = "位置") String location, @ToolParam(description = "人数") int guestCount) {
        log.info("开始进入方法largeGroupGatheringTool，参数为 location -> {}, guestCount -> {}", location, guestCount);
        String query = String.format("%s 大桌 包场 团建 聚餐 %d人", location, guestCount);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "livePerformanceDiningTool", description = "边吃边看演出！查询有驻唱、乐队表演或脱口秀的餐厅。")
    public String livePerformanceDiningTool(@ToolParam(description = "城市") String city, @ToolParam(description = "表演类型") String performanceType) {
        log.info("开始进入方法livePerformanceDiningTool，参数为 city -> {}, performanceType -> {}", city, performanceType);
        String query = String.format("%s %s 驻唱 音乐餐厅 演出", city, performanceType);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "fitnessMacroDietTool", description = "健身党专属！查询有低卡、高蛋白、低碳水选项的餐厅。")
    public String fitnessMacroDietTool(@ToolParam(description = "城市") String city, @ToolParam(description = "饮食需求") String dietaryNeed) {
        log.info("开始进入方法fitnessMacroDietTool，参数为 city -> {}, dietaryNeed -> {}", city, dietaryNeed);
        String query = String.format("%s 健身餐 低卡 高蛋白 %s", city, dietaryNeed);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "diningParkingValetTool", description = "自驾吃饭不愁停车！查询餐厅是否有免费停车位或代客泊车。")
    public String diningParkingValetTool(@ToolParam(description = "餐厅名称") String restaurantName, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法diningParkingValetTool，参数为 restaurantName -> {}, city -> {}", restaurantName, city);
        String query = String.format("%s %s 停车位 代客泊车 免费停车", city, restaurantName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "brunchAndBakeryTool", description = "周末早午餐去哪吃？查询网红咖啡馆、面包店和烘焙店。")
    public String brunchAndBakeryTool(@ToolParam(description = "城市") String city, @ToolParam(description = "位置") String location) {
        log.info("开始进入方法brunchAndBakeryTool，参数为 city -> {}, location -> {}", city, location);
        String query = String.format("%s %s Brunch 咖啡馆 面包店 烘焙", city, location);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "nightMarketStallTool", description = "逛夜市吃小吃！查询夜市摊位、小吃街、美食节。")
    public String nightMarketStallTool(@ToolParam(description = "城市") String city, @ToolParam(description = "位置") String location) {
        log.info("开始进入方法nightMarketStallTool，参数为 city -> {}, location -> {}", city, location);
        String query = String.format("%s %s 夜市 小吃街 美食节", city, location);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "historicalThemedDiningTool", description = "穿越式用餐体验！查询有历史文化主题的餐厅。")
    public String historicalThemedDiningTool(@ToolParam(description = "城市") String city, @ToolParam(description = "主题") String theme) {
        log.info("开始进入方法historicalThemedDiningTool，参数为 city -> {}, theme -> {}", city, theme);
        String query = String.format("%s %s 主题餐厅 历史文化", city, theme);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "omakaseAndSeasonalTool", description = "吃货进阶！查询Omakase、季节限定菜单和时令食材。")
    public String omakaseAndSeasonalTool(@ToolParam(description = "城市") String city, @ToolParam(description = "菜系") String cuisineType) {
        log.info("开始进入方法omakaseAndSeasonalTool，参数为 city -> {}, cuisineType -> {}", city, cuisineType);
        String query = String.format("%s Omakase %s 季节限定 时令", city, cuisineType);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "postMealWalkabilityTool", description = "吃完饭散步消食！查询餐厅周边的公园、步道和适合散步的区域。")
    public String postMealWalkabilityTool(@ToolParam(description = "餐厅名称") String restaurantName, @ToolParam(description = "所在城市名") String city) {
        log.info("开始进入方法postMealWalkabilityTool，参数为 restaurantName -> {}, city -> {}", restaurantName, city);
        String query = String.format("%s %s 周边 公园 步道 散步", city, restaurantName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "dietaryRestrictionTool", description = "保命级别的工具！专门为有饮食禁忌的用户筛选安全餐厅。")
    public String dietaryRestrictionTool(@ToolParam(description = "位置") String location, @ToolParam(description = "禁忌列表") List<String> restrictions) {
        log.info("开始进入方法dietaryRestrictionTool，参数为 location -> {}, restrictions -> {}", location, restrictions);
        String query = String.format("%s %s 餐厅 饮食禁忌", location, String.join(" ", restrictions));
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "liveQueueAndReservationTool", description = "解决排队痛点的神器喵！查询热门餐厅的排队情况和预约方式。")
    public String liveQueueAndReservationTool(@ToolParam(description = "餐厅名称") String restaurantName) {
        log.info("开始进入方法liveQueueAndReservationTool，参数为 restaurantName -> {}", restaurantName);
        String query = String.format("%s 排队 预约 取号", restaurantName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "soloDiningFriendlyTool", description = "社恐友好餐厅查询！是否有一人座、扫码点餐等。")
    public String soloDiningFriendlyTool(@ToolParam(description = "位置") String location, @ToolParam(description = "用餐类型") String mealType) {
        log.info("开始进入方法soloDiningFriendlyTool，参数为 location -> {}, mealType -> {}", location, mealType);
        String query = String.format("%s %s 一人食 社恐友好 单人座", location, mealType);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "fineDiningEtiquetteTool", description = "高级餐厅礼仪查询！着装要求、开瓶费等。")
    public String fineDiningEtiquetteTool(@ToolParam(description = "餐厅名称") String restaurantName) {
        log.info("开始进入方法fineDiningEtiquetteTool，参数为 restaurantName -> {}", restaurantName);
        String query = String.format("%s 着装要求 开瓶费 最低消费 预约", restaurantName);
        return tavilyApiClient.searchAsText(query, 3);
    }
}
