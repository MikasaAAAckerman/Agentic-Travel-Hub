package com.travel.hubtools.tool.travel;

import com.travel.hubtools.client.TavilyApiClient;
import com.travel.hubtools.tool.common.IAgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 路线距离与耗时计算工具（全部走 Tavily 搜索）
 */
@Slf4j
@Component
public class RouteQueryTool implements IAgentTool {

    @Autowired
    private TavilyApiClient tavilyApiClient;

    @Tool(name = "driveRoutePlanningTool", description = "最基础的导航工具喵！根据起点和终点，规划最快的自驾路线，并返回精准的物理距离、预计驾驶耗时以及预估的高速过路费。")
    public String driveRoutePlanningTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination, @ToolParam(description = "策略") String strategy) {
        log.info("开始进入方法driveRoutePlanningTool，参数为 origin -> {}, destination -> {}, strategy -> {}", origin, destination, strategy);
        String query = String.format("%s到%s 自驾路线 距离 时间 过路费", origin, destination);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "publicTransitRoutingTool", description = "专为非自驾用户准备！综合地铁、公交、有轨电车，提供包含\"步行距离\"、\"换乘次数\"在内的最优公共交通组合方案喵。")
    public String publicTransitRoutingTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination, @ToolParam(description = "偏好") String preference) {
        log.info("开始进入方法publicTransitRoutingTool，参数为 origin -> {}, destination -> {}, preference -> {}", origin, destination, preference);
        String query = String.format("%s到%s 公交地铁 换乘 步行距离", origin, destination);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "walkingAndCyclingRouteTool", description = "针对 5 公里内的短途出行喵！避开机动车主干道，优先推荐绿道、公园穿行路线，并顺便计算骑行或步行将消耗的卡路里。")
    public String walkingAndCyclingRouteTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination, @ToolParam(description = "模式") String mode) {
        log.info("开始进入方法walkingAndCyclingRouteTool，参数为 origin -> {}, destination -> {}, mode -> {}", origin, destination, mode);
        String query = String.format("%s到%s %s 步行 骑行 路线", origin, destination, mode);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "liveTrafficCongestionTool", description = "动态数据工具！查询指定道路或商圈当前的实时拥堵等级（红/黄/绿），以及前方是否有交通事故或施工封路喵。")
    public String liveTrafficCongestionTool(@ToolParam(description = "道路或区域") String roadNameOrArea, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法liveTrafficCongestionTool，参数为 roadNameOrArea -> {}, city -> {}", roadNameOrArea, city);
        String query = String.format("%s %s 实时路况 拥堵 事故", city, roadNameOrArea);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "subwayOperationTimeTool", description = "深夜干饭人保命神器！查询特定地铁站/公交线路的末班车发车时间，防止用户在外面玩得太晚而流落街头喵！")
    public String subwayOperationTimeTool(@ToolParam(description = "站点名称") String stationName, @ToolParam(description = "线路名称") String lineName) {
        log.info("开始进入方法subwayOperationTimeTool，参数为 stationName -> {}, lineName -> {}", stationName, lineName);
        String query = String.format("%s %s 运营时间 首班车 末班车", stationName, lineName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "poiAlongRouteTool", description = "自驾游刚需！在已经规划好的长途路线上，动态搜索距离当前位置前方最近的服务区、加油站、公共厕所或充电桩喵！")
    public String poiAlongRouteTool(@ToolParam(description = "路线ID") String currentRouteId, @ToolParam(description = "当前位置") String currentLatLng, @ToolParam(description = "POI类型") String poiType) {
        log.info("开始进入方法poiAlongRouteTool，参数为 currentRouteId -> {}, currentLatLng -> {}, poiType -> {}", currentRouteId, currentLatLng, poiType);
        String query = String.format("路线 %s 附近 %s", currentRouteId, poiType);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "rideHailingEstimateTool", description = "打车比价神器喵！查询从 A 到 B 打车时，快车、专车、豪华车的大致价格区间，以及当前区域呼叫网约车的平均排队等候时间。")
    public String rideHailingEstimateTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination) {
        log.info("开始进入方法rideHailingEstimateTool，参数为 origin -> {}, destination -> {}", origin, destination);
        String query = String.format("%s到%s 打车 价格 快车 专车", origin, destination);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "scenicDriveRouteTool", description = "为了\"在路上\"的浪漫工具！主动避开枯燥的高速公路，专门规划途经海岸线、盘山公路或森林公园的绝美景观路线喵。")
    public String scenicDriveRouteTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination) {
        log.info("开始进入方法scenicDriveRouteTool，参数为 origin -> {}, destination -> {}", origin, destination);
        String query = String.format("%s到%s 景观路线 自驾 海岸线 盘山公路", origin, destination);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "largeVehicleRestrictionTool", description = "极其硬核的规则工具！专为驾驶房车或重型越野车的用户查询路线上的限高杆高度、桥梁限重，以及城市中心区对外地车牌的限行政策（如广州开四停四）喵！")
    public String largeVehicleRestrictionTool(@ToolParam(description = "路线规划") String routePlan, @ToolParam(description = "车辆类型") String vehicleType, @ToolParam(description = "车辆高度") double vehicleHeight) {
        log.info("开始进入方法largeVehicleRestrictionTool，参数为 routePlan -> {}, vehicleType -> {}, vehicleHeight -> {}", routePlan, vehicleType, vehicleHeight);
        String query = String.format("%s %s 限高 限重 限行政策", routePlan, vehicleType);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "multiStopRouteOptimizationTool", description = "算法级神仙工具！当用户在一天内需要打卡 5 个不同的景点时，此工具利用运筹学算法，自动计算出不走回头路的最优游玩顺序喵！")
    public String multiStopRouteOptimizationTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "途经点") List<String> waypoints) {
        log.info("开始进入方法multiStopRouteOptimizationTool，参数为 origin -> {}, waypoints -> {}", origin, waypoints);
        String query = String.format("%s出发 多景点路线优化 %s", origin, String.join(" ", waypoints));
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "sharedMobilityLocatorTool", description = "解决\"最后一公里\"痛点的神器喵！当用户下了地铁但距离目的地还有 2 公里时，查询该地铁口 100 米内是否有可用的美团/哈啰单车，或者是否处于\"共享电单车禁停区\"。")
    public String sharedMobilityLocatorTool(@ToolParam(description = "当前位置") String currentLocation, @ToolParam(description = "目的地") String destination) {
        log.info("开始进入方法sharedMobilityLocatorTool，参数为 currentLocation -> {}, destination -> {}", currentLocation, destination);
        String query = String.format("%s附近 共享单车 禁停区", currentLocation);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "ferryAndWaterwayTool", description = "专门处理跨江、跨海或岛屿旅游的水上交通工具喵！查询如鼓浪屿轮渡、珠江夜游船或大连至烟台滚装船的实时发船时刻表及抗风浪停航预警。")
    public String ferryAndWaterwayTool(@ToolParam(description = "出发码头") String departurePier, @ToolParam(description = "到达码头") String arrivalPier, @ToolParam(description = "日期") String date) {
        log.info("开始进入方法ferryAndWaterwayTool，参数为 departurePier -> {}, arrivalPier -> {}, date -> {}", departurePier, arrivalPier, date);
        String query = String.format("%s到%s 渡轮 船次 时刻表 %s", departurePier, arrivalPier, date);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "cableCarAndShuttleTool", description = "为名山大川（如黄山、泰山、华山）量身定制！查询景区内部环保接驳车和高空索道的运营时间、当前排队耗时，并给出\"徒步与坐缆车\"的体力与时间性价比测算喵。")
    public String cableCarAndShuttleTool(@ToolParam(description = "景点") String scenicSpot, @ToolParam(description = "路段") String routeSegment) {
        log.info("开始进入方法cableCarAndShuttleTool，参数为 scenicSpot -> {}, routeSegment -> {}", scenicSpot, routeSegment);
        String query = String.format("%s %s 索道 接驳车 运营时间", scenicSpot, routeSegment);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "highwayTollAndETCTool", description = "极其硬核的省钱计算器！结合不同省份的\"节假日高速免费政策\"、\"ETC 九五折\"甚至\"夜间货车/特定路段差异化收费\"，精准计算过路费喵。")
    public String highwayTollAndETCTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination, @ToolParam(description = "车辆类型") String vehicleType, @ToolParam(description = "是否有ETC") boolean hasETC) {
        log.info("开始进入方法highwayTollAndETCTool，参数为 origin -> {}, destination -> {}, vehicleType -> {}, hasETC -> {}", origin, destination, vehicleType, hasETC);
        String query = String.format("%s到%s 高速过路费 ETC %s", origin, destination, vehicleType);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "trafficControlWarningTool", description = "自驾保命预警！实时查询公安交管部门的数据，判断前方高速是否因为大雪结冰、大雾低能见度或暴雨积水而采取了\"封闭收费站\"或\"警车带道\"的强制管制措施喵。")
    public String trafficControlWarningTool(@ToolParam(description = "道路名称") String roadName) {
        log.info("开始进入方法trafficControlWarningTool，参数为 roadName -> {}", roadName);
        String query = String.format("%s 交通管制 封闭收费站 恶劣天气", roadName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "borderCrossingWaitTimeTool", description = "出境游或大湾区通行刚需！查询如深圳前往香港的各大口岸（罗湖、福田、深圳湾）当前的过关排队人数、通关耗时，并推荐最快的口岸喵。")
    public String borderCrossingWaitTimeTool(@ToolParam(description = "出发城市") String originCity, @ToolParam(description = "目的城市") String destinationCity, @ToolParam(description = "口岸") String checkPoint) {
        log.info("开始进入方法borderCrossingWaitTimeTool，参数为 originCity -> {}, destinationCity -> {}, checkPoint -> {}", originCity, destinationCity, checkPoint);
        String query = String.format("%s到%s %s口岸 通关 排队", originCity, destinationCity, checkPoint);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "transitPassCompatibilityTool", description = "拯救外地游客的利器喵！查询目的地的公交/地铁是否支持用户家乡的\"交通联合卡\"，或者是否能直接使用支付宝/微信的乘车码，避免在闸机口罚站下 APP 的尴尬。")
    public String transitPassCompatibilityTool(@ToolParam(description = "目的城市") String destinationCity) {
        log.info("开始进入方法transitPassCompatibilityTool，参数为 destinationCity -> {}", destinationCity);
        String query = String.format("%s 公交地铁 交通联合卡 乘车码 支付方式", destinationCity);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "roadsideAssistanceTool", description = "危机处理工具喵！当用户在偏远地区爆胎或抛锚时，提供距离最近的官方高速拖车电话、车险免费救援核销指南以及防天价拖车费的防坑话术！")
    public String roadsideAssistanceTool(@ToolParam(description = "位置坐标") String locationLatLng, @ToolParam(description = "紧急类型") String emergencyType) {
        log.info("开始进入方法roadsideAssistanceTool，参数为 locationLatLng -> {}, emergencyType -> {}", locationLatLng, emergencyType);
        String query = String.format("%s %s 救援 拖车电话 车险救援", locationLatLng, emergencyType);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "stationInternalNavigationTool", description = "专门对付如\"重庆北站\"、\"北京南站\"这种结构极其复杂的巨型交通枢纽喵！测算从高铁出站口到地铁进站口的真实物理步行时间，防止换乘时间不足导致误车。")
    public String stationInternalNavigationTool(@ToolParam(description = "枢纽名称") String hubName, @ToolParam(description = "起始节点") String fromNode, @ToolParam(description = "目标节点") String toNode) {
        log.info("开始进入方法stationInternalNavigationTool，参数为 hubName -> {}, fromNode -> {}, toNode -> {}", hubName, fromNode, toNode);
        String query = String.format("%s %s到%s 步行导航 换乘时间", hubName, fromNode, toNode);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "urbanAirMobilityTool", description = "极其前沿的高端工具！查询如深圳、低空经济试点城市中，从 CBD 到机场的直升机\"飞的\"航线、票价（如单程 999 元）以及行李重量严格限制喵！")
    public String urbanAirMobilityTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination) {
        log.info("开始进入方法urbanAirMobilityTool，参数为 origin -> {}, destination -> {}", origin, destination);
        String query = String.format("%s到%s 直升机 低空飞行 航线 票价", origin, destination);
        return tavilyApiClient.searchAsText(query, 3);
    }
}
