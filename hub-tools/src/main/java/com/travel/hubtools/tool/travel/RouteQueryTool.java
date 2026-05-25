package com.travel.hubtools.tool.travel;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.travel.hubtools.client.MapApiClient;
import com.travel.hubtools.tool.common.IAgentTool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 路线距离与耗时计算工具
 */
@Slf4j
@Component
public class RouteQueryTool implements IAgentTool {

    @Autowired
    private MapApiClient mapApiClient;

    @Autowired
    private ChatModel dashScopeChatModel;

    /**
     * 极速驾车路线规划与耗时测算工具
     *
     * @param origin      起点
     * @param destination 终点
     * @param strategy    策略，如：躲避拥堵、不走高速
     * @return String (比如：“推荐走广深沿江高速，全程 120 公里，预计耗时 1.5 小时，过路费约 60 元喵”)
     * @Description 最基础的导航工具喵！根据起点和终点，规划最快的自驾路线，并返回精准的物理距离、预计驾驶耗时以及预估的高速过路费。
     */
    @Tool(name = "driveRoutePlanningTool", description = "最基础的导航工具喵！根据起点和终点，规划最快的自驾路线，并返回精准的物理距离、预计驾驶耗时以及预估的高速过路费。")
    public String driveRoutePlanningTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination
            , @ToolParam(description = "策略") String strategy,@ToolParam(description = "起点所属城市") String originCity, @ToolParam(description = "目的地所属城市") String destinationCity) {
        log.info("开始进入方法driveRoutePlanningTool，参数为 origin -> {}, destination -> {}, strategy -> {}", origin, destination, strategy);

        try {
            // 将地址转换为经纬度
            String originCoord = mapApiClient.geocode(origin, originCity);
            String destCoord = mapApiClient.geocode(destination, destinationCity);

            if (originCoord == null || destCoord == null) {
                return "抱歉喵，无法找到起点或终点的坐标信息，请检查地址是否正确喵~";
            }

            // 根据策略选择对应的strategy值
            Integer strategyCode = parseStrategy(strategy);

            // 调用高德地图驾车路线规划API
            JSONObject result = mapApiClient.drivingRoutePlanning(originCoord, destCoord, strategyCode);

            if (result == null) {
                return "抱歉喵，路线规划失败，请稍后再试喵~";
            }

            // 解析结果
            JSONObject route = result.getJSONObject("route");
            JSONArray paths = route.getJSONArray("paths");

            if (paths == null || paths.isEmpty()) {
                return "抱歉喵，没有找到合适的路线喵~";
            }

            // 取第一条路线
            JSONObject firstPath = paths.getJSONObject(0);
            String distance = firstPath.getString("distance"); // 米
            String taxiCost = route.getString("taxi_cost"); // 出租车费用

            // 转换单位
            double distanceKm = Double.parseDouble(distance) / 1000.0;

            String response = String.format(
                    "推荐路线喵！全程 %.2f 公里",
                    distanceKm
            );

            if (taxiCost != null && !taxiCost.isEmpty()) {
                response += String.format("，打车费用约 %s 元", taxiCost);
            }

            response += "喵~";

            log.info("工具driveRoutePlanningTool执行完成");
            return response;

        } catch (Exception e) {
            log.error("工具driveRoutePlanningTool执行异常", e);
            return "抱歉喵，路线规划过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 解析策略字符串为高德地图的策略代码
     */
    private Integer parseStrategy(String strategy) {
        if (strategy == null || strategy.isEmpty()) {
            return 32; // 默认推荐
        }

        if (strategy.contains("速度优先")) return 0;
        if (strategy.contains("费用优先") || strategy.contains("少收费")) return 1;
        if (strategy.contains("躲避拥堵")) return 33;
        if (strategy.contains("高速优先")) return 34;
        if (strategy.contains("不走高速")) return 35;
        if (strategy.contains("大路优先")) return 37;

        return 32; // 默认推荐
    }

    /**
     * 公共交通与无缝换乘查询工具
     *
     * @param origin      起点
     * @param destination 终点
     * @param preference  偏好，如：少步行、少换乘
     * @return String (详细换乘步骤，如：“先乘坐地铁 3 号线，在珠江新城站站内换乘 5 号线，出站后需步行 500 米，极其费腿喵”)
     * @Description 专为非自驾用户准备！综合地铁、公交、有轨电车，提供包含“步行距离”、“换乘次数”在内的最优公共交通组合方案喵。
     */
    @Tool(name = "publicTransitRoutingTool", description = "专为非自驾用户准备！综合地铁、公交、有轨电车，提供包含\"步行距离\"、\"换乘次数\"在内的最优公共交通组合方案喵。")
    public String publicTransitRoutingTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination, @ToolParam(description = "偏好") String preference, @ToolParam(description = "城市") String city) {
        log.info("开始进入方法publicTransitRoutingTool，参数为 origin -> {}, destination -> {}, preference -> {}", origin, destination, preference);

        try {
            // 将地址转换为经纬度
            String originCoord = mapApiClient.geocode(origin, city);
            String destCoord = mapApiClient.geocode(destination, city);

            if (originCoord == null || destCoord == null) {
                return "抱歉喵，无法找到起点或终点的坐标信息，请检查地址是否正确喵~";
            }

            // 提取城市信息（从起点地址中）
//            String city = extractCity(origin);

            // 根据偏好选择策略
            Integer strategy = parseTransitPreference(preference);

            // 调用高德地图公交路线规划API
            JSONObject result = mapApiClient.publicTransitRoutePlanning(originCoord, destCoord, city, strategy);

            if (result == null) {
                return "抱歉喵，公交路线规划失败，请稍后再试喵~";
            }

            // 解析结果
            JSONObject route = result.getJSONObject("route");
            JSONArray transits = route.getJSONArray("transits");

            if (transits == null || transits.isEmpty()) {
                return "抱歉喵，没有找到合适的公交路线喵~";
            }

            // 取第一条路线
            JSONObject firstTransit = transits.getJSONObject(0);
            String distance = firstTransit.getString("distance"); // 米
            JSONArray segments = firstTransit.getJSONArray("segments");

            // 构建详细的换乘说明
            StringBuilder response = new StringBuilder();
            response.append("公交路线规划喵！\n");

            if (segments != null) {
                for (int i = 0; i < segments.size(); i++) {
                    JSONObject segment = segments.getJSONObject(i);
                    
                    // 处理步行段
                    JSONObject walking = segment.getJSONObject("walking");
                    if (walking != null) {
                        String distance_walk = walking.getString("distance");
                        String instruction = walking.getString("instruction");
                        response.append(String.format("   步行 %s 米", distance_walk));
                        if (instruction != null && !instruction.isEmpty()) {
                            response.append(String.format("（%s）", instruction));
                        }
                        response.append("\n");
                    }
                    
                    // 处理公交/地铁段
                    JSONObject bus = segment.getJSONObject("bus");
                    if (bus != null) {
                        JSONArray busLines = bus.getJSONArray("buslines");
                        if (busLines != null && !busLines.isEmpty()) {
                            for (int j = 0; j < busLines.size(); j++) {
                                JSONObject busLine = busLines.getJSONObject(j);
                                
                                // 获取线路信息
                                String lineName = busLine.getString("name");
                                String lineType = busLine.getString("type");
                                String lineDistance = busLine.getString("distance");
                                String viaNum = busLine.getString("via_num");
                                
                                // 获取起点站信息
                                JSONObject departureStop = busLine.getJSONObject("departure_stop");
                                String departureName = departureStop.getString("name");
                                JSONObject entrance = departureStop.getJSONObject("entrance");
                                String entranceInfo = "";
                                if (entrance != null) {
                                    entranceInfo = entrance.getString("name");
                                }
                                
                                // 获取终点站信息
                                JSONObject arrivalStop = busLine.getJSONObject("arrival_stop");
                                String arrivalName = arrivalStop.getString("name");
                                JSONObject exit = arrivalStop.getJSONObject("exit");
                                String exitInfo = "";
                                if (exit != null) {
                                    exitInfo = exit.getString("name");
                                }
                                
                                // 获取运营时间
                                String startTime = busLine.getString("station_start_time");
                                String endTime = busLine.getString("station_end_time");
                                String timeTips = busLine.getString("bus_time_tips");
                                
                                // 格式化输出
                                response.append(String.format("%d.%d. 乘坐 %s（%s）\n", i + 1, j + 1, lineName, lineType));
                                response.append(String.format("     从 %s", departureName));
                                if (!entranceInfo.isEmpty()) {
                                    response.append(String.format("（%s入口）", entranceInfo));
                                }
                                response.append(String.format(" 到 %s", arrivalName));
                                if (!exitInfo.isEmpty()) {
                                    response.append(String.format("（%s出口）", exitInfo));
                                }
                                response.append("\n");
                                
                                // 添加额外信息
                                double lineDistanceKm = Double.parseDouble(lineDistance) / 1000.0;
                                response.append(String.format("     距离：%.2f 公里，途经 %s 站\n", lineDistanceKm, viaNum));
                                
                                if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                                    // 格式化时间：0602 -> 06:02
                                    String startFormatted = startTime.substring(0, 2) + ":" + startTime.substring(2);
                                    String endFormatted = endTime.substring(0, 2) + ":" + endTime.substring(2);
                                    response.append(String.format("     运营时间：%s - %s\n", startFormatted, endFormatted));
                                }
                                
                                if (timeTips != null && !timeTips.isEmpty()) {
                                    response.append(String.format("     提示：%s\n", timeTips));
                                }
                                
                                // 获取途经站点
                                JSONArray viaStops = busLine.getJSONArray("via_stops");
                                if (viaStops != null && !viaStops.isEmpty()) {
                                    response.append("     途经站点：");
                                    for (int k = 0; k < viaStops.size(); k++) {
                                        JSONObject stop = viaStops.getJSONObject(k);
                                        response.append(stop.getString("name"));
                                        if (k < viaStops.size() - 1) {
                                            response.append(" → ");
                                        }
                                    }
                                    response.append("\n");
                                }
                            }
                        }
                    }
                    
                    // 添加分隔线
                    if (i < segments.size() - 1) {
                        response.append("   ↓ 换乘 ↓\n");
                    }
                }
            }

            double distanceKm = Double.parseDouble(distance) / 1000.0;
            response.append(String.format("\n总距离：%.2f 公里 喵~", distanceKm));

            log.info("工具publicTransitRoutingTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具publicTransitRoutingTool执行异常", e);
            return "抱歉喵，公交路线规划过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 从地址中提取城市信息
     */
    private String extractCity(String address) {
        // 简单实现：假设地址格式为“城市+具体地址”
        // 实际应用中可能需要更复杂的逻辑或使用地理编码返回的城市信息
        if (address.contains("北京")) return "北京";
        if (address.contains("上海")) return "上海";
        if (address.contains("广州")) return "广州";
        if (address.contains("深圳")) return "深圳";
        // 默认返回空，让API自动识别
        return "";
    }

    /**
     * 解析公交偏好为策略代码
     */
    private Integer parseTransitPreference(String preference) {
        if (preference == null || preference.isEmpty()) {
            return 0; // 最快捷
        }

        if (preference.contains("最经济") || preference.contains("省钱")) return 1;
        if (preference.contains("少换乘") || preference.contains("换乘")) return 2;
        if (preference.contains("少步行") || preference.contains("步行")) return 3;
        if (preference.contains("不乘地铁") || preference.contains("地铁")) return 4;

        return 0; // 默认最快捷
    }

    @Tool(name = "walkingAndCyclingRouteTool", description = "针对 5 公里内的短途出行喵！避开机动车主干道，优先推荐绿道、公园穿行路线，并顺便计算骑行或步行将消耗的卡路里。")
    public String walkingAndCyclingRouteTool(@ToolParam(description = "起点") String origin,
                                             @ToolParam(description = "目的地") String destination,
                                             @ToolParam(description = "模式") String mode,
                                             @ToolParam(description = "起点所属城市") String originCity,
                                             @ToolParam(description = "目的地所属城市") String destinationCity) {
        log.info("开始进入方法walkingAndCyclingRouteTool，参数为 origin -> {}, destination -> {}, mode -> {}", origin, destination, mode);

        try {
            // 将地址转换为经纬度
            String originCoord = mapApiClient.geocode(origin, originCity);
            String destCoord = mapApiClient.geocode(destination, destinationCity);

            if (originCoord == null || destCoord == null) {
                return "抱歉喵，无法找到起点或终点的坐标信息，请检查地址是否正确喵~";
            }

            JSONObject result;
            if ("骑行".equals(mode) || "cycling".equalsIgnoreCase(mode)) {
                result = mapApiClient.cyclingRoutePlanning(originCoord, destCoord);
            } else {
                result = mapApiClient.walkingRoutePlanning(originCoord, destCoord);
            }

            if (result == null) {
                return "抱歉喵，" + mode + "路线规划失败，请稍后再试喵~";
            }

            // 解析结果
            JSONObject route = result.getJSONObject("route");
            JSONArray paths = route.getJSONArray("paths");

            if (paths == null || paths.isEmpty()) {
                return "抱歉喵，没有找到合适的" + mode + "路线喵~";
            }

            // 取第一条路线
            JSONObject firstPath = paths.getJSONObject(0);
            String distance = firstPath.getString("distance"); // 米

            // 转换单位
            double distanceKm = Double.parseDouble(distance) / 1000.0;


            // 估算卡路里消耗（步行约50卡/公里，骑行约30卡/公里）
            double calories = "骑行".equals(mode) ? distanceKm * 30 : distanceKm * 50;

            String response = String.format(
                    "%s路线规划喵！全程 %.2f 公里，预计消耗 %.0f 卡路里喵~",
                    mode, distanceKm, calories
            );

            log.info("工具walkingAndCyclingRouteTool执行完成");
            return response;

        } catch (Exception e) {
            log.error("工具walkingAndCyclingRouteTool执行异常", e);
            return "抱歉喵，" + mode + "路线规划过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    @Tool(name = "liveTrafficCongestionTool", description = "动态数据工具！查询指定道路或商圈当前的实时拥堵等级（红/黄/绿），以及前方是否有交通事故或施工封路喵。")
    public String liveTrafficCongestionTool(@ToolParam(description = "道路或区域") String roadNameOrArea, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法liveTrafficCongestionTool，参数为 roadNameOrArea -> {}, city -> {}", roadNameOrArea, city);

        try {
            // 地理编码获取坐标
            String coord = mapApiClient.geocode(roadNameOrArea, city);

            if (coord == null) {
                return "抱歉喵，无法找到该位置的坐标信息，请检查地址是否正确喵~";
            }

            // 使用圆形区域交通态势查询周边路况
            JSONObject result = mapApiClient.trafficStatusCircle(coord, 2000);

            if (result == null) {
                return "抱歉喵，实时路况查询失败，请稍后再试喵~";
            }

            // 解析结果
            JSONObject trafficInfo = result.getJSONObject("trafficinfo");
            String description = trafficInfo != null ? trafficInfo.getString("description") : "暂无";

            JSONArray roads = result.getJSONArray("roads");
            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】实时路况喵！\n", roadNameOrArea));
            response.append(String.format("整体路况评估：%s\n\n", description));

            if (roads != null && !roads.isEmpty()) {
                for (int i = 0; i < Math.min(roads.size(), 5); i++) {
                    JSONObject road = roads.getJSONObject(i);
                    String name = road.getString("name");
                    String status = road.getString("status");
                    String speed = road.getString("speed");

                    String statusText = switch (status) {
                        case "0" -> "未知";
                        case "1" -> "畅通 🟢";
                        case "2" -> "缓行 🟡";
                        case "3" -> "拥堵 🔴";
                        case "4" -> "严重拥堵 🔴🔴";
                        default -> "未知";
                    };

                    response.append(String.format("%d. %s\n", i + 1, name));
                    response.append(String.format("   拥堵等级：%s\n", statusText));
                    if (speed != null && !speed.isEmpty()) {
                        response.append(String.format("   平均车速：%s km/h\n", speed));
                    }
                }
            }

            response.append("\n以上为实时路况数据喵~");
            log.info("工具liveTrafficCongestionTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具liveTrafficCongestionTool执行异常", e);
            return "抱歉喵，实时路况查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    @Tool(name = "subwayOperationTimeTool", description = "深夜干饭人保命神器！查询特定地铁站/公交线路的末班车发车时间，防止用户在外面玩得太晚而流落街头喵！")
    public String subwayOperationTimeTool(@ToolParam(description = "站点名称") String stationName, @ToolParam(description = "线路名称") String lineName) {
        log.info("开始进入方法subwayOperationTimeTool，参数为 stationName -> {}, lineName -> {}", stationName, lineName);

        String promptText = String.format(
                """
                        请查询%s站%s线路的运营时间：
                        
                        请提供：
                        1. 首班车发车时间
                        2. 末班车发车时间
                        3. 发车间隔""",
                stationName, lineName
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具subwayOperationTimeTool执行完成");
        return result;
    }

    @Tool(name = "poiAlongRouteTool", description = "自驾游刚需！在已经规划好的长途路线上，动态搜索距离当前位置前方最近的服务区、加油站、公共厕所或充电桩喵！")
    public String poiAlongRouteTool(@ToolParam(description = "路线ID") String currentRouteId, @ToolParam(description = "当前位置") String currentLatLng, @ToolParam(description = "POI类型") String poiType) {
        log.info("开始进入方法poiAlongRouteTool，参数为 currentRouteId -> {}, currentLatLng -> {}, poiType -> {}", currentRouteId, currentLatLng, poiType);

        String promptText = String.format(
                """
                        请在路线【%s】上搜索%s附近的%s：
                        当前位置：%s
                        
                        请提供：
                        1. 距离当前位置前方最近的POI
                        2. POI的详细信息（如加油站油品、充电桩数量等）""",
                currentRouteId, currentLatLng, poiType, currentLatLng
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具poiAlongRouteTool执行完成");
        return result;
    }

    @Tool(name = "rideHailingEstimateTool", description = "打车比价神器喵！查询从 A 到 B 打车时，快车、专车、豪华车的大致价格区间，以及当前区域呼叫网约车的平均排队等候时间。")
    public String rideHailingEstimateTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination, @ToolParam(description = "起点所属城市") String originCity, @ToolParam(description = "目的地所属城市") String destinationCity) {
        log.info("开始进入方法rideHailingEstimateTool，参数为 origin -> {}, destination -> {}", origin, destination);

        try {
            // 将地址转换为经纬度
            String originCoord = mapApiClient.geocode(origin, originCity);
            String destCoord = mapApiClient.geocode(destination, destinationCity);

            if (originCoord == null || destCoord == null) {
                return "抱歉喵，无法找到起点或终点的坐标信息，请检查地址是否正确喵~";
            }

            // 调用驾车路线规划获取出租车费用估算
            JSONObject result = mapApiClient.drivingRoutePlanning(originCoord, destCoord, 32);

            if (result == null) {
                return "抱歉喵，网约车估价失败，请稍后再试喵~";
            }

            JSONObject route = result.getJSONObject("route");
            String taxiCost = route != null ? route.getString("taxi_cost") : null;
            JSONArray paths = route != null ? route.getJSONArray("paths") : null;

            String distance = null;
            if (paths != null && !paths.isEmpty()) {
                JSONObject firstPath = paths.getJSONObject(0);
                distance = firstPath.getString("distance");
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("从【%s】到【%s】网约车估价喵！\n\n", origin, destination));

            if (distance != null) {
                double distanceKm = Double.parseDouble(distance) / 1000.0;
                response.append(String.format("行程距离：%.2f 公里\n", distanceKm));
            }

            if (taxiCost != null && !taxiCost.isEmpty()) {
                response.append(String.format("出租车预估价格：%s 元\n", taxiCost));
                // 基于出租车价格估算快车和专车
                double taxi = Double.parseDouble(taxiCost);
                response.append(String.format("快车预估价格：%.0f - %.0f 元\n", taxi * 0.8, taxi * 1.0));
                response.append(String.format("专车预估价格：%.0f - %.0f 元\n", taxi * 1.3, taxi * 1.8));
            } else {
                response.append("暂无法获取价格估算喵~\n");
            }

            response.append("实际价格以打车平台实时报价为准喵~");
            log.info("工具rideHailingEstimateTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具rideHailingEstimateTool执行异常", e);
            return "抱歉喵，网约车估价过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    @Tool(name = "scenicDriveRouteTool", description = "为了\"在路上\"的浪漫工具！主动避开枯燥的高速公路，专门规划途经海岸线、盘山公路或森林公园的绝美景观路线喵。")
    public String scenicDriveRouteTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination) {
        log.info("开始进入方法scenicDriveRouteTool，参数为 origin -> {}, destination -> {}", origin, destination);

        String promptText = String.format(
                """
                        请规划从%s到%s的景观路线：
                        
                        请提供：
                        1. 途经海岸线、盘山公路或森林公园的绝美景观路线
                        2. 车程增加时间
                        3. 景观特色描述""",
                origin, destination
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具scenicDriveRouteTool执行完成");
        return result;
    }

    @Tool(name = "largeVehicleRestrictionTool", description = "极其硬核的规则工具！专为驾驶房车或重型越野车的用户查询路线上的限高杆高度、桥梁限重，以及城市中心区对外地车牌的限行政策（如广州开四停四）喵！")
    public String largeVehicleRestrictionTool(@ToolParam(description = "路线规划") String routePlan, @ToolParam(description = "车辆类型") String vehicleType, @ToolParam(description = "车辆高度") double vehicleHeight) {
        log.info("开始进入方法largeVehicleRestrictionTool，参数为 routePlan -> {}, vehicleType -> {}, vehicleHeight -> {}", routePlan, vehicleType, vehicleHeight);

        String promptText = String.format(
                """
                        请查询路线【%s】对%s的限行限高政策：
                        车辆高度：%.2f米
                        
                        请提供：
                        1. 路线上的限高杆高度
                        2. 桥梁限重
                        3. 城市中心区对外地车牌的限行政策
                        4. 致命红线预警""",
                routePlan, vehicleType, vehicleHeight
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具largeVehicleRestrictionTool执行完成");
        return result;
    }

    @Tool(name = "multiStopRouteOptimizationTool", description = "算法级神仙工具！当用户在一天内需要打卡 5 个不同的景点时，此工具利用运筹学算法，自动计算出不走回头路的最优游玩顺序喵！")
    public String multiStopRouteOptimizationTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "途经点") List<String> waypoints) {
        log.info("开始进入方法multiStopRouteOptimizationTool，参数为 origin -> {}, waypoints -> {}", origin, waypoints);

        String promptText = String.format(
                """
                        请优化从%s出发，途经以下景点的最优路线：
                        途经点：%s
                        
                        请提供：
                        1. 重新排序后的打卡顺序
                        2. 总耗时
                        3. 可节省的时间百分比""",
                origin, waypoints.toString()
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具multiStopRouteOptimizationTool执行完成");
        return result;
    }

    @Tool(name = "sharedMobilityLocatorTool", description = "解决\"最后一公里\"痛点的神器喵！当用户下了地铁但距离目的地还有 2 公里时，查询该地铁口 100 米内是否有可用的美团/哈啰单车，或者是否处于\"共享电单车禁停区\"。")
    public String sharedMobilityLocatorTool(@ToolParam(description = "当前位置") String currentLocation, @ToolParam(description = "目的地") String destination) {
        log.info("开始进入方法sharedMobilityLocatorTool，参数为 currentLocation -> {}, destination -> {}", currentLocation, destination);

        String promptText = String.format(
                """
                        请查询从%s到%s的共享单车情况：
                        
                        请提供：
                        1. 当前位置附近是否有可用的共享单车
                        2. 骑行预计耗时
                        3. 目的地是否为电单车禁停区""",
                currentLocation, destination
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具sharedMobilityLocatorTool执行完成");
        return result;
    }

    @Tool(name = "ferryAndWaterwayTool", description = "专门处理跨江、跨海或岛屿旅游的水上交通工具喵！查询如鼓浪屿轮渡、珠江夜游船或大连至烟台滚装船的实时发船时刻表及抗风浪停航预警。")
    public String ferryAndWaterwayTool(@ToolParam(description = "出发码头") String departurePier, @ToolParam(description = "到达码头") String arrivalPier, @ToolParam(description = "日期") String date) {
        log.info("开始进入方法ferryAndWaterwayTool，参数为 departurePier -> {}, arrivalPier -> {}, date -> {}", departurePier, arrivalPier, date);

        String promptText = String.format(
                """
                        请查询从%s到%s的渡轮船次：
                        日期：%s
                        
                        请提供：
                        1. 实时发船时刻表
                        2. 票价
                        3. 抗风浪停航预警""",
                departurePier, arrivalPier, date
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具ferryAndWaterwayTool执行完成");
        return result;
    }

    @Tool(name = "cableCarAndShuttleTool", description = "为名山大川（如黄山、泰山、华山）量身定制！查询景区内部环保接驳车和高空索道的运营时间、当前排队耗时，并给出\"徒步与坐缆车\"的体力与时间性价比测算喵。")
    public String cableCarAndShuttleTool(@ToolParam(description = "景点") String scenicSpot, @ToolParam(description = "路段") String routeSegment) {
        log.info("开始进入方法cableCarAndShuttleTool，参数为 scenicSpot -> {}, routeSegment -> {}", scenicSpot, routeSegment);

        String promptText = String.format(
                """
                        请查询%s景区%s的交通情况：
                        
                        请提供：
                        1. 环保接驳车和高空索道的运营时间
                        2. 当前排队耗时
                        3. \"徒步与坐缆车\"的体力与时间性价比测算""",
                scenicSpot, routeSegment
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具cableCarAndShuttleTool执行完成");
        return result;
    }

    @Tool(name = "highwayTollAndETCTool", description = "极其硬核的省钱计算器！结合不同省份的\"节假日高速免费政策\"、\"ETC 九五折\"甚至\"夜间货车/特定路段差异化收费\"，精准计算过路费喵。")
    public String highwayTollAndETCTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination, @ToolParam(description = "车辆类型") String vehicleType, @ToolParam(description = "是否有ETC") boolean hasETC, @ToolParam(description = "起点所属城市") String originCity, @ToolParam(description = "目的地所属城市") String destinationCity) {
        log.info("开始进入方法highwayTollAndETCTool，参数为 origin -> {}, destination -> {}, vehicleType -> {}, hasETC -> {}", origin, destination, vehicleType, hasETC);

        try {
            // 将地址转换为经纬度
            String originCoord = mapApiClient.geocode(origin, originCity);
            String destCoord = mapApiClient.geocode(destination, destinationCity);

            if (originCoord == null || destCoord == null) {
                return "抱歉喵，无法找到起点或终点的坐标信息，请检查地址是否正确喵~";
            }

            // 使用高速优先策略计算过路费
            JSONObject result = mapApiClient.drivingRoutePlanning(originCoord, destCoord, 34);

            if (result == null) {
                return "抱歉喵，过路费计算失败，请稍后再试喵~";
            }

            JSONObject route = result.getJSONObject("route");
            JSONArray paths = route != null ? route.getJSONArray("paths") : null;

            if (paths == null || paths.isEmpty()) {
                return "抱歉喵，没有找到合适的路线喵~";
            }

            JSONObject firstPath = paths.getJSONObject(0);
            String distance = firstPath.getString("distance");
            String tollDistance = firstPath.getString("toll_distance");
            String tolls = firstPath.getString("tolls");

            double distanceKm = distance != null ? Double.parseDouble(distance) / 1000.0 : 0;
            double tollDistanceKm = tollDistance != null ? Double.parseDouble(tollDistance) / 1000.0 : 0;

            StringBuilder response = new StringBuilder();
            response.append(String.format("从【%s】到【%s】过路费测算喵！\n\n", origin, destination));
            response.append(String.format("全程距离：%.2f 公里\n", distanceKm));
            response.append(String.format("收费路段：%.2f 公里\n", tollDistanceKm));

            if (tolls != null && !tolls.isEmpty()) {
                double tollAmount = Double.parseDouble(tolls);
                response.append(String.format("原始过路费：%s 元\n", tolls));

                if (hasETC) {
                    double etcToll = tollAmount * 0.95;
                    response.append(String.format("ETC 95折后：%.2f 元\n", etcToll));
                }
            } else {
                response.append("该路线未查询到收费站信息喵~\n");
            }

            response.append(String.format("\n车辆类型：%s\n", vehicleType));
            response.append("提示：法定节假日七座及以下小型客车免费通行喵~\n");
            response.append("实际收费以收费站公示为准喵~");
            log.info("工具highwayTollAndETCTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具highwayTollAndETCTool执行异常", e);
            return "抱歉喵，过路费计算过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    @Tool(name = "trafficControlWarningTool", description = "自驾保命预警！实时查询公安交管部门的数据，判断前方高速是否因为大雪结冰、大雾低能见度或暴雨积水而采取了\"封闭收费站\"或\"警车带道\"的强制管制措施喵。")
    public String trafficControlWarningTool(@ToolParam(description = "道路名称") String roadName) {
        log.info("开始进入方法trafficControlWarningTool，参数为 roadName -> {}", roadName);

        String promptText = String.format(
                """
                        请查询【%s】的交通管制情况：
                        
                        请提供：
                        1. 是否因为恶劣天气采取强制管制措施
                        2. 管制类型（封闭收费站、警车带道等）
                        3. 致命红线预警""",
                roadName
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具trafficControlWarningTool执行完成");
        return result;
    }

    @Tool(name = "borderCrossingWaitTimeTool", description = "出境游或大湾区通行刚需！查询如深圳前往香港的各大口岸（罗湖、福田、深圳湾）当前的过关排队人数、通关耗时，并推荐最快的口岸喵。")
    public String borderCrossingWaitTimeTool(@ToolParam(description = "出发城市") String originCity, @ToolParam(description = "目的城市") String destinationCity, @ToolParam(description = "口岸") String checkPoint) {
        log.info("开始进入方法borderCrossingWaitTimeTool，参数为 originCity -> {}, destinationCity -> {}, checkPoint -> {}", originCity, destinationCity, checkPoint);

        String promptText = String.format(
                """
                        请查询从%s到%s通过%s口岸的过关情况：
                        
                        请提供：
                        1. 当前过关排队人数
                        2. 通关耗时
                        3. 是否推荐该口岸""",
                originCity, destinationCity, checkPoint
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具borderCrossingWaitTimeTool执行完成");
        return result;
    }

    @Tool(name = "transitPassCompatibilityTool", description = "拯救外地游客的利器喵！查询目的地的公交/地铁是否支持用户家乡的\"交通联合卡\"，或者是否能直接使用支付宝/微信的乘车码，避免在闸机口罚站下 APP 的尴尬。")
    public String transitPassCompatibilityTool(@ToolParam(description = "目的城市") String destinationCity) {
        log.info("开始进入方法transitPassCompatibilityTool，参数为 destinationCity -> {}", destinationCity);

        String promptText = String.format(
                """
                        请查询%s市的公共交通支付兼容性：
                        
                        请提供：
                        1. 是否支持\"交通联合卡\"
                        2. 是否能直接使用支付宝/微信的乘车码
                        3. 是否需要单独下载本地APP""",
                destinationCity
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具transitPassCompatibilityTool执行完成");
        return result;
    }

    @Tool(name = "roadsideAssistanceTool", description = "危机处理工具喵！当用户在偏远地区爆胎或抛锚时，提供距离最近的官方高速拖车电话、车险免费救援核销指南以及防天价拖车费的防坑话术！")
    public String roadsideAssistanceTool(@ToolParam(description = "位置坐标") String locationLatLng, @ToolParam(description = "紧急类型") String emergencyType) {
        log.info("开始进入方法roadsideAssistanceTool，参数为 locationLatLng -> {}, emergencyType -> {}", locationLatLng, emergencyType);

        String promptText = String.format(
                """
                        请提供%s位置的%s救援指南：
                        
                        请提供：
                        1. 距离最近的官方高速拖车电话
                        2. 车险免费救援核销指南
                        3. 防天价拖车费的防坑话术""",
                locationLatLng, emergencyType
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具roadsideAssistanceTool执行完成");
        return result;
    }

    @Tool(name = "stationInternalNavigationTool", description = "专门对付如\"重庆北站\"、\"北京南站\"这种结构极其复杂的巨型交通枢纽喵！测算从高铁出站口到地铁进站口的真实物理步行时间，防止换乘时间不足导致误车。")
    public String stationInternalNavigationTool(@ToolParam(description = "枢纽名称") String hubName, @ToolParam(description = "起始节点") String fromNode, @ToolParam(description = "目标节点") String toNode) {
        log.info("开始进入方法stationInternalNavigationTool，参数为 hubName -> {}, fromNode -> {}, toNode -> {}", hubName, fromNode, toNode);

        String promptText = String.format(
                """
                        请测算%s枢纽从%s到%s的步行导航：
                        
                        请提供：
                        1. 真实物理步行时间
                        2. 详细步行路线指引
                        3. 建议预留的换乘时间""",
                hubName, fromNode, toNode
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具stationInternalNavigationTool执行完成");
        return result;
    }

    @Tool(name = "urbanAirMobilityTool", description = "极其前沿的高端工具！查询如深圳、低空经济试点城市中，从 CBD 到机场的直升机\"飞的\"航线、票价（如单程 999 元）以及行李重量严格限制喵！")
    public String urbanAirMobilityTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination) {
        log.info("开始进入方法urbanAirMobilityTool，参数为 origin -> {}, destination -> {}", origin, destination);

        String promptText = String.format(
                """
                        请查询从%s到%s的直升机航线：
                        
                        请提供：
                        1. 航线信息
                        2. 票价
                        3. 行李重量严格限制""",
                origin, destination
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具urbanAirMobilityTool执行完成");
        return result;
    }
}