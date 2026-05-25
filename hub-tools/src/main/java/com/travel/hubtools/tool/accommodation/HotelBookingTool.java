package com.travel.hubtools.tool.accommodation;

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

/**
 * 查房型与价格工具
 */
@Slf4j
@Component
public class HotelBookingTool implements IAgentTool {

    @Autowired
    private ChatModel dashScopeChatModel;

    @Autowired
    private MapApiClient mapApiClient;

    /**
     * 基础房态与实时报价检索工具
     *
     * @param location     城市或区域
     * @param checkInDate  入住日期
     * @param checkOutDate 离店日期
     * @param guestCount   人数
     * @return String (包含推荐酒店名称、可用房型及价格区间的精简列表)
     * @Description 最基础的酒店查询工具！根据用户指定的城市或地标、入住日期、离店日期和人数，查询满足条件的酒店列表、房型及实时基础报价喵。
     */
    @Tool(name = "hotelAvailabilityQueryTool", description = "最基础的酒店查询工具！根据用户指定的城市或地标、入住日期、离店日期和人数，查询满足条件的酒店列表、房型及实时基础报价喵。")
    public String hotelAvailabilityQueryTool(@ToolParam(description = "城市或区域") String location, @ToolParam(description = "入住日期") String checkInDate, @ToolParam(description = "离店日期") String checkOutDate, @ToolParam(description = "人数") int guestCount) {
        log.info("开始进入方法hotelAvailabilityQueryTool，参数为 location -> {}, checkInDate->{}，checkOutDate->{}，guestCount->{}", location, checkInDate, checkOutDate, guestCount);

        try {
            String city = location;
            // 尝试从位置提取城市（取前2字作为城市名尝试geocode）
            if (city.length() > 2) {
                city = location.substring(0, 2);
            }

            // 使用POI搜索酒店
            JSONObject result = mapApiClient.searchPOI(location + " 酒店", city, "100000", 10, 1);

            if (result == null) {
                return "抱歉喵，酒店查询失败，请稍后再试喵~";
            }

            JSONArray pois = result.getJSONArray("pois");
            if (pois == null || pois.isEmpty()) {
                return String.format("抱歉喵，在%s没有找到酒店喵~", location);
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%s】酒店查询结果喵！\n", location));
            response.append(String.format("入住：%s | 离店：%s | %d人\n\n", checkInDate, checkOutDate, guestCount));

            for (int i = 0; i < Math.min(pois.size(), 5); i++) {
                JSONObject poi = pois.getJSONObject(i);
                String name = poi.getString("name");
                String address = poi.getString("address");
                String tel = poi.getString("tel");

                response.append(String.format("%d. 【%s】\n", i + 1, name));
                if (address != null) response.append(String.format("   地址：%s\n", address));
                if (tel != null) response.append(String.format("   电话：%s\n", tel));

                JSONObject bizExt = poi.getJSONObject("biz_ext");
                if (bizExt != null) {
                    String rating = bizExt.getString("rating");
                    String cost = bizExt.getString("cost");
                    if (rating != null) response.append(String.format("   评分：%s 分\n", rating));
                    if (cost != null) response.append(String.format("   参考价：%s 元\n", cost));
                }
                response.append("\n");
            }

            response.append("以上为搜索结果，建议致电确认实时房态和价格喵~");
            log.info("工具hotelAvailabilityQueryTool执行完成");
            return response.toString();

        } catch (Exception e) {
            log.error("工具hotelAvailabilityQueryTool执行异常", e);
            return "抱歉喵，酒店查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 极客与远程办公设施查验工具
     *
     * @param hotelName 酒店名称
     * @return String (网络带宽实测数据评价、插座数量及办公区舒适度报告，绝对避开那种只能坐在床上敲键盘的烂酒店喵！)
     * @Description 专门为需要随时随地掏出电脑敲代码、处理高并发线上问题的硬核出差人士准备的工具喵！查询指定酒店是否提供百兆以上高速独立 Wi-Fi、符合人体工学的办公桌椅以及充足的不间断电源插座。
     */
    @Tool(name = "remoteWorkFacilityTool", description = "专门为需要随时随地掏出电脑敲代码、处理高并发线上问题的硬核出差人士准备的工具喵！查询指定酒店是否提供百兆以上高速独立 Wi-Fi、符合人体工学的办公桌椅以及充足的不间断电源插座。")
    public String remoteWorkFacilityTool(@ToolParam(description = "酒店名称") String hotelName) {
        log.info("开始进入方法remoteWorkFacilityTool，参数为 hotelName -> {}", hotelName);

        String promptText = String.format(
                """
                        请查询酒店【%s】的办公设施情况：
                        1. 网络带宽实测数据评价
                        2. 插座数量及位置
                        3. 办公区舒适度报告
                        
                        请提供详细评估，帮助程序员判断是否适合在此酒店工作。""",
                hotelName
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具remoteWorkFacilityTool执行完成");
        return result;
    }

    /**
     * 宠物友好与附加费查询工具
     *
     * @param hotelName 酒店名称
     * @param petType   宠物类型，如：小型犬、猫
     * @return String (宠物准入政策、收费标准及是否提供宠物窝/食盆)
     * @Description 查询特定酒店是否允许携带宠物（猫/狗），以及具体的体型限制、疫苗要求和额外的清洁押金/附加费喵。
     */
    @Tool(name = "petFriendlyPolicyTool", description = "查询特定酒店是否允许携带宠物（猫/狗），以及具体的体型限制、疫苗要求和额外的清洁押金/附加费喵。")
    public String petFriendlyPolicyTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "宠物类型，如：小型犬、猫") String petType) {
        log.info("开始进入方法petFriendlyPolicyTool，参数为 hotelName -> {}, petType -> {}", hotelName, petType);

        String promptText = String.format(
                """
                        请查询酒店【%s】的宠物政策：
                        宠物类型：%s
                        
                        请提供：
                        1. 宠物准入政策
                        2. 体型限制要求
                        3. 疫苗证明要求
                        4. 清洁押金或附加费标准
                        5. 是否提供宠物窝/食盆等设施""",
                hotelName, petType
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具petFriendlyPolicyTool执行完成");
        return result;
    }

    /**
     * 深夜到达与前台值守查询工具
     *
     * @param hotelName            酒店名称
     * @param estimatedArrivalTime 预计到达时间，如 "02:30 AM"
     * @return String (前台营业时间、深夜自助入住机或密码锁的获取流程)
     * @Description 针对航班延误或深夜到达的用户，查询酒店前台是否 24 小时值守、是否有最晚办理入住时间限制，以及深夜门禁策略喵。
     */
    @Tool(name = "lateCheckInPolicyTool", description = "针对航班延误或深夜到达的用户，查询酒店前台是否 24 小时值守、是否有最晚办理入住时间限制，以及深夜门禁策略喵。")
    public String lateCheckInPolicyTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "预计到达时间，如 \"02:30 AM\"") String estimatedArrivalTime) {
        log.info("开始进入方法lateCheckInPolicyTool，参数为 hotelName -> {}, estimatedArrivalTime -> {}", hotelName, estimatedArrivalTime);

        String promptText = String.format(
                """
                        请查询酒店【%s】的深夜入住政策：
                        预计到达时间：%s
                        
                        请提供：
                        1. 前台是否24小时值守
                        2. 最晚办理入住时间限制
                        3. 深夜门禁策略
                        4. 自助入住机或密码锁获取流程""",
                hotelName, estimatedArrivalTime
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具lateCheckInPolicyTool执行完成");
        return result;
    }

    /**
     * 停车泊位与新能源充电桩查询工具
     *
     * @param hotelName 酒店名称
     * @return String (车位配比、每日封顶收费、慢充/快充桩的具体配置)
     * @Description 自驾游用户的刚需工具！查询酒店是否有专属停车场、收费标准（是否对住客免费），以及极其重要的新能源汽车充电桩分布和空闲概况喵。
     */
    @Tool(name = "parkingAndEVChargingTool", description = "自驾游用户的刚需工具！查询酒店是否有专属停车场、收费标准（是否对住客免费），以及极其重要的新能源汽车充电桩分布和空闲概况喵。")
    public String parkingAndEVChargingTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "酒店所在城市名") String city) {
        log.info("开始进入方法parkingAndEVChargingTool，参数为 hotelName -> {}", hotelName);

        try {
            // 将酒店名称转换为经纬度
            String hotelCoord = mapApiClient.geocode(hotelName, city);

            if (hotelCoord == null) {
                return "抱歉喵，无法找到酒店的坐标信息，请检查酒店名称是否正确喵~";
            }

            // 搜索周边的停车场
            JSONObject parkingResult = mapApiClient.searchPOIAround("停车场", hotelCoord, 500, "150904");
            String parkingInfo = "";
            if (parkingResult != null && parkingResult.getJSONArray("pois") != null && !parkingResult.getJSONArray("pois").isEmpty()) {
                int count = parkingResult.getJSONArray("pois").size();
                JSONObject nearestParking = parkingResult.getJSONArray("pois").getJSONObject(0);
                String name = nearestParking.getString("name");
                String distance = nearestParking.getString("distance");
                parkingInfo = String.format("附近500米内有 %d 个停车场，最近的是%s（距离%s米）", count, name, distance);
            } else {
                parkingInfo = "附近500米内没有停车场";
            }

            // 搜索周边的充电桩
            JSONObject chargingResult = mapApiClient.searchPOIAround("充电桩", hotelCoord, 1000, "150905");
            String chargingInfo = "";
            if (chargingResult != null && chargingResult.getJSONArray("pois") != null && !chargingResult.getJSONArray("pois").isEmpty()) {
                int count = chargingResult.getJSONArray("pois").size();
                chargingInfo = String.format("方圆1公里内有 %d 个充电桩站点", count);
            } else {
                chargingInfo = "方圆1公里内没有充电桩";
            }

            String response = String.format("【%s】停车与充电设施喵！\n%s喵~\n%s喵~",
                    hotelName, parkingInfo, chargingInfo);

            log.info("工具parkingAndEVChargingTool执行完成");
            return response;

        } catch (Exception e) {
            log.error("工具parkingAndEVChargingTool执行异常", e);
            return "抱歉喵，查询停车和充电设施时出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 噪音指数与睡眠环境测评工具
     *
     * @param hotelName 酒店名称
     * @return String (隔音等级评估，以及建议房型如"强烈建议预订内向中庭的高层房间以避开临街噪音")
     * @Description 查询特定酒店的隔音评价和周边噪音源（如是否紧挨高架桥、火车站或夜店），并提供"如何向酒店申请安静房"的内部话术喵。
     */
    @Tool(name = "hotelNoiseLevelTool", description = "查询特定酒店的隔音评价和周边噪音源（如是否紧挨高架桥、火车站或夜店），并提供\"如何向酒店申请安静房\"的内部话术喵。")
    public String hotelNoiseLevelTool(@ToolParam(description = "酒店名称") String hotelName) {
        log.info("开始进入方法hotelNoiseLevelTool，参数为 hotelName -> {}", hotelName);

        String promptText = String.format(
                """
                        请评估酒店【%s】的噪音和睡眠环境：
                        
                        请提供：
                        1. 隔音等级评估
                        2. 周边噪音源分析（是否靠近高架桥、火车站、夜店等）
                        3. 建议房型（如内向中庭的高层房间）
                        4. 向酒店申请安静房的内部话术""",
                hotelName
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具hotelNoiseLevelTool执行完成");
        return result;
    }

    /**
     * 无障碍与适老设施查询工具
     *
     * @param hotelName 酒店名称
     * @return String (无障碍设施覆盖率，明确告知是否适合腿脚不便的老人或轮椅使用者)
     * @Description 极具人文关怀的查询工具。查询酒店是否配备轮椅坡道、电梯门宽是否达标、浴室内是否有安全扶手和紧急呼叫按钮喵。
     */
    @Tool(name = "accessibilityFacilityTool", description = "极具人文关怀的查询工具。查询酒店是否配备轮椅坡道、电梯门宽是否达标、浴室内是否有安全扶手和紧急呼叫按钮喵。")
    public String accessibilityFacilityTool(@ToolParam(description = "酒店名称") String hotelName) {
        log.info("开始进入方法accessibilityFacilityTool，参数为 hotelName -> {}", hotelName);

        String promptText = String.format(
                """
                        请查询酒店【%s】的无障碍设施：
                        
                        请提供：
                        1. 是否配备轮椅坡道
                        2. 电梯门宽是否达标
                        3. 浴室内是否有安全扶手和紧急呼叫按钮
                        4. 无障碍设施覆盖率
                        5. 是否适合腿脚不便的老人或轮椅使用者""",
                hotelName
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具accessibilityFacilityTool执行完成");
        return result;
    }

    /**
     * 亲子设施与加床政策查询工具
     *
     * @param hotelName 酒店名称
     * @param childAge  儿童年龄
     * @return String (加床费用标准、儿童早餐政策及是否有室内外儿童乐园)
     * @Description 查询酒店是否免费提供婴儿床（Crib）、儿童洗漱用品，以及超过特定年龄的儿童是否需要强制加床及加床费用喵。
     */
    @Tool(name = "familyFriendlyPolicyTool", description = "查询酒店是否免费提供婴儿床（Crib）、儿童洗漱用品，以及超过特定年龄的儿童是否需要强制加床及加床费用喵。")
    public String familyFriendlyPolicyTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "儿童年龄") int childAge) {
        log.info("开始进入方法familyFriendlyPolicyTool，参数为 hotelName -> {}, childAge -> {}", hotelName, childAge);

        String promptText = String.format(
                """
                        请查询酒店【%s】的亲子设施和加床政策：
                        儿童年龄：%d岁
                        
                        请提供：
                        1. 是否免费提供婴儿床（Crib）
                        2. 是否提供儿童洗漱用品
                        3. 该年龄儿童是否需要强制加床
                        4. 加床费用标准
                        5. 儿童早餐政策
                        6. 是否有室内外儿童乐园""",
                hotelName, childAge
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具familyFriendlyPolicyTool执行完成");
        return result;
    }

    /**
     * 押金与极限退改签政策工具
     *
     * @param hotelName   酒店名称
     * @param bookingDate 预订日期
     * @return String (精准的免费取消 Deadline 时间节点，以及押金退还周期)
     * @Description 查询酒店不同房型/价格档位的取消政策（如：不可取消、入住前 24 小时免费取消），以及线下需要冻结多少信用卡的预授权押金喵。
     */
    @Tool(name = "cancellationPolicyTool", description = "查询酒店不同房型/价格档位的取消政策（如：不可取消、入住前 24 小时免费取消），以及线下需要冻结多少信用卡的预授权押金喵。")
    public String cancellationPolicyTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "预订日期") String bookingDate) {
        log.info("开始进入方法cancellationPolicyTool，参数为 hotelName -> {}, bookingDate -> {}", hotelName, bookingDate);

        String promptText = String.format(
                """
                        请查询酒店【%s】的退改签政策和押金要求：
                        预订日期：%s
                        
                        请提供：
                        1. 不同房型的取消政策（不可取消、免费取消截止时间等）
                        2. 精准的免费取消 Deadline 时间节点
                        3. 需要冻结的信用卡预授权押金金额
                        4. 押金退还周期""",
                hotelName, bookingDate
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具cancellationPolicyTool执行完成");
        return result;
    }

    /**
     * 最后一公里交通与周边便利工具
     *
     * @param hotelName 酒店名称
     * @return String (比如："距离地铁 3 号线步行约 8 分钟，楼下 50 米即有全家便利店，极度便利喵")
     * @Description 查询酒店距离最近的地铁站/公交站的真实步行距离（非直线距离），以及方圆 500 米内是否有 24 小时便利店或药店喵。
     */
    @Tool(name = "hotelSurroundingsTool", description = "查询酒店距离最近的地铁站/公交站的真实步行距离（非直线距离），以及方圆 500 米内是否有 24 小时便利店或药店喵。")
    public String hotelSurroundingsTool(@ToolParam(description = "酒店名称") String hotelName,@ToolParam(description = "酒店所在城市名") String city) {
        log.info("开始进入方法hotelSurroundingsTool，参数为 hotelName -> {}", hotelName);

        try {
            // 将酒店名称转换为经纬度
            String hotelCoord = mapApiClient.geocode(hotelName, city);

            if (hotelCoord == null) {
                return "抱歉喵，无法找到酒店的坐标信息，请检查酒店名称是否正确喵~";
            }

            // 搜索周边的地铁站
            JSONObject metroResult = mapApiClient.searchPOIAround("地铁站", hotelCoord, 1000, "150500");
            String metroInfo = "";
            if (metroResult != null && metroResult.getJSONArray("pois") != null && !metroResult.getJSONArray("pois").isEmpty()) {
                JSONObject nearestMetro = metroResult.getJSONArray("pois").getJSONObject(0);
                String distance = nearestMetro.getString("distance");
                String name = nearestMetro.getString("name");
                metroInfo = String.format("距离最近的%s约 %s 米", name, distance);
            } else {
                metroInfo = "附近1公里内没有地铁站";
            }

            // 搜索周边的便利店
            JSONObject convenienceResult = mapApiClient.searchPOIAround("便利店", hotelCoord, 500, "060101");
            String convenienceInfo = "";
            if (convenienceResult != null && convenienceResult.getJSONArray("pois") != null && !convenienceResult.getJSONArray("pois").isEmpty()) {
                int count = convenienceResult.getJSONArray("pois").size();
                convenienceInfo = String.format("方圆500米内有 %d 家便利店", count);
            } else {
                convenienceInfo = "方圆500米内没有便利店";
            }

            // 搜索周边的药店
            JSONObject pharmacyResult = mapApiClient.searchPOIAround("药店", hotelCoord, 500, "090100");
            String pharmacyInfo = "";
            if (pharmacyResult != null && pharmacyResult.getJSONArray("pois") != null && !pharmacyResult.getJSONArray("pois").isEmpty()) {
                int count = pharmacyResult.getJSONArray("pois").size();
                pharmacyInfo = String.format("方圆500米内有 %d 家药店", count);
            } else {
                pharmacyInfo = "方圆500米内没有药店";
            }

            String response = String.format("【%s】周边便利性喵！\n%s喵~\n%s喵~\n%s喵~",
                    hotelName, metroInfo, convenienceInfo, pharmacyInfo);

            log.info("工具hotelSurroundingsTool执行完成");
            return response;

        } catch (Exception e) {
            log.error("工具hotelSurroundingsTool执行异常", e);
            return "抱歉喵，查询酒店周边信息时出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 提前入住与行李寄存查验工具
     *
     * @param hotelName   酒店名称
     * @param arrivalTime 到达时间，如 "08:00 AM"
     * @return String (提前入住的可能性预估，以及行李寄存处的安全级别与最长寄存时效喵)
     * @Description 专门应对早班机或早班高铁到达的场景！查询酒店是否允许早于下午两点免费提前入住，以及离店后是否提供带有监控的免费行李寄存服务喵。
     */
    @Tool(name = "earlyCheckInLuggageTool", description = "专门应对早班机或早班高铁到达的场景！查询酒店是否允许早于下午两点免费提前入住，以及离店后是否提供带有监控的免费行李寄存服务喵。")
    public String earlyCheckInLuggageTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "到达时间，如 \"08:00 AM\"") String arrivalTime) {
        log.info("开始进入方法earlyCheckInLuggageTool，参数为 hotelName -> {}, arrivalTime -> {}", hotelName, arrivalTime);

        String promptText = String.format(
                """
                        请查询酒店【%s】的提前入住和行李寄存政策：
                        到达时间：%s
                        
                        请提供：
                        1. 是否允许早于下午两点免费提前入住
                        2. 提前入住的可能性预估
                        3. 离店后是否提供免费行李寄存服务
                        4. 行李寄存处的安全级别（是否有监控）
                        5. 最长寄存时效""",
                hotelName, arrivalTime
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具earlyCheckInLuggageTool执行完成");
        return result;
    }

    /**
     * 早餐政策与打包服务甄别工具
     *
     * @param hotelName          酒店名称
     * @param needEarlyDeparture 是否需要早退
     * @return String (早餐供应时间段、自助餐丰富度评价，以及打包餐盒的申请流程喵)
     * @Description 查询房费是否包含双早、儿童早餐是否免费，以及极其关键的一点：如果用户需要凌晨赶飞机，酒店是否能提前准备便携式的早餐盒打包服务喵！
     */
    @Tool(name = "breakfastInclusionTool", description = "查询房费是否包含双早、儿童早餐是否免费，以及极其关键的一点：如果用户需要凌晨赶飞机，酒店是否能提前准备便携式的早餐盒打包服务喵！")
    public String breakfastInclusionTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "是否需要早退") boolean needEarlyDeparture) {
        log.info("开始进入方法breakfastInclusionTool，参数为 hotelName -> {}, needEarlyDeparture -> {}", hotelName, needEarlyDeparture);

        String promptText = String.format(
                """
                        请查询酒店【%s】的早餐政策：
                        是否需要早退：%b
                        
                        请提供：
                        1. 房费是否包含双早
                        2. 儿童早餐是否免费
                        3. 早餐供应时间段
                        4. 自助餐丰富度评价
                        5. 如需凌晨赶飞机，是否能提前准备便携式早餐盒打包服务
                        6. 打包餐盒的申请流程""",
                hotelName, needEarlyDeparture
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具breakfastInclusionTool执行完成");
        return result;
    }

    /**
     * 健身房与恒温泳池探测工具
     *
     * @param hotelName 酒店名称
     * @return String (健身房24小时开放情况、器械品牌，以及泳池的水温和必须佩戴泳帽的硬性规定喵)
     * @Description 为有着严格自律习惯的商旅人士准备的硬核工具！查询酒店健身房是否有深蹲架、跑步机数量，以及泳池是否为标准道、是否常年恒温喵。
     */
    @Tool(name = "gymPoolFacilityTool", description = "为有着严格自律习惯的商旅人士准备的硬核工具！查询酒店健身房是否有深蹲架、跑步机数量，以及泳池是否为标准道、是否常年恒温喵。")
    public String gymPoolFacilityTool(@ToolParam(description = "酒店名称") String hotelName) {
        log.info("开始进入方法gymPoolFacilityTool，参数为 hotelName -> {}", hotelName);

        String promptText = String.format(
                """
                        请查询酒店【%s】的健身和泳池设施：
                        
                        请提供：
                        1. 健身房24小时开放情况
                        2. 器械品牌及配置（是否有深蹲架、跑步机数量等）
                        3. 泳池是否为标准道
                        4. 泳池是否常年恒温及水温
                        5. 是否必须佩戴泳帽等硬性规定""",
                hotelName
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具gymPoolFacilityTool执行完成");
        return result;
    }

    /**
     * 自助洗衣与干洗服务检索工具
     *
     * @param hotelName 酒店名称
     * @param stayDays  入住天数
     * @return String (自助洗衣房拥挤程度预估、洗衣液是否免费提供，或高昂的干洗收费明细喵)
     * @Description 解决长途旅行换洗衣物痛点！查询酒店楼层内是否有免费的自助洗衣机和烘干机，或者是否提供次日达的商务衬衫干洗服务及计价标准喵。
     */
    @Tool(name = "laundryServiceTool", description = "解决长途旅行换洗衣物痛点！查询酒店楼层内是否有免费的自助洗衣机和烘干机，或者是否提供次日达的商务衬衫干洗服务及计价标准喵。")
    public String laundryServiceTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "入住天数") int stayDays) {
        log.info("开始进入方法laundryServiceTool，参数为 hotelName -> {}, stayDays -> {}", hotelName, stayDays);

        String promptText = String.format(
                """
                        请查询酒店【%s】的洗衣服务：
                        入住天数：%d天
                        
                        请提供：
                        1. 是否有免费的自助洗衣机和烘干机
                        2. 自助洗衣房拥挤程度预估
                        3. 洗衣液是否免费提供
                        4. 是否提供次日达的商务衬衫干洗服务
                        5. 干洗收费明细""",
                hotelName, stayDays
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具laundryServiceTool执行完成");
        return result;
    }

    /**
     * 景观房与楼层偏好匹配工具
     *
     * @param hotelName     酒店名称
     * @param preferredView 偏好景观，如：海景、高层
     * @return String (真实景观测评，如"所谓海景其实只能在阳台侧身看到一点点海，不建议加钱预订"，以及申请高层的成功率喵)
     * @Description 情绪价值拉满的工具！查询特定酒店的高级房型是否能看到江景、海景或地标建筑（如东方明珠），并识别"伪景观房"的雷区喵。
     */
    @Tool(name = "viewAndFloorPreferenceTool", description = "情绪价值拉满的工具！查询特定酒店的高级房型是否能看到江景、海景或地标建筑（如东方明珠），并识别\"伪景观房\"的雷区喵。")
    public String viewAndFloorPreferenceTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "偏好景观，如：海景、高层") String preferredView) {
        log.info("开始进入方法viewAndFloorPreferenceTool，参数为 hotelName -> {}, preferredView -> {}", hotelName, preferredView);

        String promptText = String.format(
                """
                        请评估酒店【%s】的景观房：
                        偏好景观：%s
                        
                        请提供：
                        1. 高级房型是否能真正看到%s
                        2. 真实景观测评（是否存在"伪景观房"）
                        3. 申请高层的成功率
                        4. 是否值得加钱预订景观房""",
                hotelName, preferredView, preferredView
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具viewAndFloorPreferenceTool执行完成");
        return result;
    }

    /**
     * 吸烟规定与违约金预警工具
     *
     * @param hotelName 酒店名称
     * @return String (禁烟红线提示，如"全季酒店全量禁烟，违规触发烟雾报警器将罚款 2000 元喵")
     * @Description 极度严肃的合规工具！明确查询酒店是否为全封闭无烟酒店，查询指定的楼层是否设立了吸烟室，以及如果在房间内违规抽烟会面临的巨额清洁罚款喵！
     */
    @Tool(name = "smokingPolicyTool", description = "极度严肃的合规工具！明确查询酒店是否为全封闭无烟酒店，查询指定的楼层是否设立了吸烟室，以及如果在房间内违规抽烟会面临的巨额清洁罚款喵！")
    public String smokingPolicyTool(@ToolParam(description = "酒店名称") String hotelName) {
        log.info("开始进入方法smokingPolicyTool，参数为 hotelName -> {}", hotelName);

        String promptText = String.format(
                """
                        请查询酒店【%s】的吸烟规定：
                        
                        请提供：
                        1. 是否为全封闭无烟酒店
                        2. 指定楼层是否设立吸烟室
                        3. 在房间内违规抽烟的清洁罚款金额
                        4. 禁烟红线提示""",
                hotelName
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具smokingPolicyTool执行完成");
        return result;
    }

    /**
     * 周边急救医疗与药店探查工具
     *
     * @param hotelName 酒店名称
     * @return String (急救资源的物理距离，以及酒店前台是否备有基础的创可贴、碘伏等急救箱喵)
     * @Description 旅行安全底线防线！查询酒店方圆两公里内是否有三甲医院的急诊科，以及 24 小时营业且能送药上门的外卖药店喵。
     */
    @Tool(name = "nearbyHospitalPharmacyTool", description = "旅行安全底线防线！查询酒店方圆两公里内是否有三甲医院的急诊科，以及 24 小时营业且能送药上门的外卖药店喵。")
    public String nearbyHospitalPharmacyTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "酒店所在城市名") String city) {
        log.info("开始进入方法nearbyHospitalPharmacyTool，参数为 hotelName -> {}, city -> {}", hotelName, city);

        try {
            String hotelCoord = mapApiClient.geocode(hotelName, city);

            if (hotelCoord == null) {
                return "抱歉喵，无法找到酒店的坐标信息，请检查酒店名称是否正确喵~";
            }

            // 搜索周边医院
            JSONObject hospitalResult = mapApiClient.searchPOIAround("医院", hotelCoord, 2000, "090200");
            String hospitalInfo = "";
            if (hospitalResult != null && hospitalResult.getJSONArray("pois") != null && !hospitalResult.getJSONArray("pois").isEmpty()) {
                JSONArray hospitals = hospitalResult.getJSONArray("pois");
                hospitalInfo = String.format("方圆2公里内有 %d 家医院", hospitals.size());
                JSONObject nearest = hospitals.getJSONObject(0);
                hospitalInfo += String.format("，最近的是%s（%s米）", nearest.getString("name"), nearest.getString("distance"));
            } else {
                hospitalInfo = "方圆2公里内没有找到医院";
            }

            // 搜索周边药店
            JSONObject pharmacyResult = mapApiClient.searchPOIAround("药店", hotelCoord, 500, "090100");
            String pharmacyInfo = "";
            if (pharmacyResult != null && pharmacyResult.getJSONArray("pois") != null && !pharmacyResult.getJSONArray("pois").isEmpty()) {
                int count = pharmacyResult.getJSONArray("pois").size();
                pharmacyInfo = String.format("方圆500米内有 %d 家药店", count);
            } else {
                pharmacyInfo = "方圆500米内没有药店";
            }

            String response = String.format("【%s】周边医疗保障喵！\n\n%s喵~\n%s喵~\n\n建议入住时咨询前台是否备有基础急救箱喵~",
                    hotelName, hospitalInfo, pharmacyInfo);

            log.info("工具nearbyHospitalPharmacyTool执行完成");
            return response;

        } catch (Exception e) {
            log.error("工具nearbyHospitalPharmacyTool执行异常", e);
            return "抱歉喵，医疗资源查询过程中出现错误：" + e.getMessage() + "喵~";
        }
    }

    /**
     * 接送机与穿梭巴士预订工具
     *
     * @param hotelName 酒店名称
     * @param terminal  航站楼/高铁站
     * @return String (穿梭巴士的上车地点指引、发车频次，以及如果错过免费班车的打车成本预估喵)
     * @Description 查询偏远度假村或机场周边酒店是否提供免费的接送机穿梭巴士，以及巴士的发车时刻表和提前预约要求喵。
     */
    @Tool(name = "airportTransferTool", description = "查询偏远度假村或机场周边酒店是否提供免费的接送机穿梭巴士，以及巴士的发车时刻表和提前预约要求喵。")
    public String airportTransferTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "航站楼/高铁站") String terminal) {
        log.info("开始进入方法airportTransferTool，参数为 hotelName -> {}, terminal -> {}", hotelName, terminal);

        String promptText = String.format(
                """
                        请查询酒店【%s】的接送机服务：
                        航站楼/高铁站：%s
                        
                        请提供：
                        1. 是否提供免费接送机穿梭巴士
                        2. 巴士发车时刻表
                        3. 上车地点指引
                        4. 是否需要提前预约及预约要求
                        5. 如果错过免费班车的打车成本预估""",
                hotelName, terminal
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具airportTransferTool执行完成");
        return result;
    }

    /**
     * 客房送餐与深夜菜单查验工具
     *
     * @param hotelName 酒店名称
     * @return String (酒店外卖机器人的运行情况、外卖柜位置，以及客房服务菜单的性价比评估喵)
     * @Description 查询酒店是否提供 24 小时的客房送餐服务（In-Room Dining），以及外卖小哥是否被允许直接将夜宵送至客房门口喵。
     */
    @Tool(name = "roomServiceMenuTool", description = "查询酒店是否提供 24 小时的客房送餐服务（In-Room Dining），以及外卖小哥是否被允许直接将夜宵送至客房门口喵。")
    public String roomServiceMenuTool(@ToolParam(description = "酒店名称") String hotelName) {
        log.info("开始进入方法roomServiceMenuTool，参数为 hotelName -> {}", hotelName);

        String promptText = String.format(
                """
                        请查询酒店【%s】的客房送餐服务：
                        
                        请提供：
                        1. 是否提供24小时客房送餐服务
                        2. 酒店外卖机器人的运行情况
                        3. 外卖柜位置
                        4. 外卖小哥是否被允许直接送至客房门口
                        5. 客房服务菜单的性价比评估""",
                hotelName
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具roomServiceMenuTool执行完成");
        return result;
    }

    /**
     * 安保巡逻与电梯梯控查询工具
     *
     * @param hotelName 酒店名称
     * @return String (梯控严格等级、走廊监控死角评估，以及前台对访客登记的严格程度喵)
     * @Description 为单身独居出行的女性用户提供极强的安全保障！查询酒店是否实行严格的刷卡梯控（只能到所在楼层），以及是否有全天候的安保人员巡视喵！
     */
    @Tool(name = "securityAndPrivacyTool", description = "为单身独居出行的女性用户提供极强的安全保障！查询酒店是否实行严格的刷卡梯控（只能到所在楼层），以及是否有全天候的安保人员巡视喵！")
    public String securityAndPrivacyTool(@ToolParam(description = "酒店名称") String hotelName) {
        log.info("开始进入方法securityAndPrivacyTool，参数为 hotelName -> {}", hotelName);

        String promptText = String.format(
                """
                        请查询酒店【%s】的安保和隐私保护措施：
                        
                        请提供：
                        1. 是否实行严格的刷卡梯控（只能到所在楼层）
                        2. 梯控严格等级
                        3. 是否有全天候安保人员巡视
                        4. 走廊监控死角评估
                        5. 前台对访客登记的严格程度""",
                hotelName
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具securityAndPrivacyTool执行完成");
        return result;
    }
}