package com.travel.hubtools.tool.accommodation;

import com.travel.hubtools.client.TavilyApiClient;
import com.travel.hubtools.tool.common.IAgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 酒店住宿工具（全部走 Tavily 搜索）
 */
@Slf4j
@Component
public class HotelBookingTool implements IAgentTool {

    @Autowired
    private TavilyApiClient tavilyApiClient;

    @Tool(name = "hotelAvailabilityQueryTool", description = "最基础的酒店查询工具！根据用户指定的城市或地标、入住日期、离店日期和人数，查询满足条件的酒店列表、房型及实时基础报价喵。")
    public String hotelAvailabilityQueryTool(@ToolParam(description = "城市或区域") String location, @ToolParam(description = "入住日期") String checkInDate, @ToolParam(description = "离店日期") String checkOutDate, @ToolParam(description = "人数") int guestCount) {
        log.info("开始进入方法hotelAvailabilityQueryTool，参数为 location -> {}, checkInDate->{}，checkOutDate->{}，guestCount->{}", location, checkInDate, checkOutDate, guestCount);
        String query = String.format("%s 酒店 %s到%s %d人 价格 房型", location, checkInDate, checkOutDate, guestCount);
        return tavilyApiClient.searchAsText(query, 5);
    }

    @Tool(name = "parkingAndEVChargingTool", description = "自驾游客的救星喵！查询酒店是否配备免费停车场、充电桩（快充/慢充），以及周边500米内的公共停车场和充电桩的实时空位。")
    public String parkingAndEVChargingTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法parkingAndEVChargingTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 停车场 充电桩 自驾", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "hotelSurroundingsTool", description = "差旅党刚需！查询酒店周边500米内的地铁站、便利店、药店的步行距离，以及最近的地铁线路名称。")
    public String hotelSurroundingsTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法hotelSurroundingsTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 地铁站 便利店 药店 周边", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "nearbyHospitalPharmacyTool", description = "健康安全保障工具！查询酒店周边3公里内的三甲医院和24小时药店，为有老人、小孩或慢性病患者的旅行团队提供紧急就医指南。")
    public String nearbyHospitalPharmacyTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法nearbyHospitalPharmacyTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 医院 药店 24小时", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "remoteWorkFacilityTool", description = "数字游民专属！查询酒店是否提供商务中心、会议室、高速Wi-Fi、静音办公区，以及大堂吧是否适合带笔记本久坐。")
    public String remoteWorkFacilityTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法remoteWorkFacilityTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 商务中心 会议室 WiFi 办公", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "petFriendlyPolicyTool", description = "毛孩子家长必看！查询酒店是否允许携带宠物入住、是否收取额外清洁费、是否有宠物专属楼层或户外遛狗区域。")
    public String petFriendlyPolicyTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法petFriendlyPolicyTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 宠物入住 携带宠物", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "lateCheckInPolicyTool", description = "深夜航班救星！查询酒店最晚入住时间、是否支持自助入住机或密码锁、以及凌晨到店是否会取消预订。")
    public String lateCheckInPolicyTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法lateCheckInPolicyTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 最晚入住 深夜入住 自助入住", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "hotelNoiseLevelTool", description = "浅睡眠患者福音！查询酒店是否临街、周边是否有施工工地或KTV、以及是否有提供静音房型。")
    public String hotelNoiseLevelTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法hotelNoiseLevelTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 噪音 静音 临街 施工", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "accessibilityFacilityTool", description = "无障碍出行保障！查询酒店是否有无障碍通道、轮椅友好房间、电梯、以及卫生间是否有扶手和紧急呼叫按钮。")
    public String accessibilityFacilityTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法accessibilityFacilityTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 无障碍 轮椅 电梯 无障碍通道", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "familyFriendlyPolicyTool", description = "带娃出行必备！查询酒店是否有亲子房、婴儿床、儿童泳池、儿童乐园、以及是否允许加床。")
    public String familyFriendlyPolicyTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法familyFriendlyPolicyTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 亲子房 婴儿床 儿童泳池 儿童乐园", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "cancellationPolicyTool", description = "预订前必看！查询酒店的免费取消截止时间、取消费用比例、以及不可抗力（如航班取消）下的退款政策。")
    public String cancellationPolicyTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法cancellationPolicyTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 取消政策 退款 免费取消", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "earlyCheckInLuggageTool", description = "早到星人福利！查询酒店是否支持免费提前入住、行李寄存服务、以及是否有公共淋浴间供早到旅客使用。")
    public String earlyCheckInLuggageTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法earlyCheckInLuggageTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 提前入住 行李寄存 淋浴", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "breakfastInclusionTool", description = "吃货必问！查询酒店是否包含早餐、早餐类型（自助/套餐）、用餐时间、以及是否有素食或无麸质选项。")
    public String breakfastInclusionTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法breakfastInclusionTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 早餐 自助早餐 含早", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "gymPoolFacilityTool", description = "健身党必备！查询酒店是否有健身房、游泳池、桑拿房，以及开放时间和是否需要额外收费。")
    public String gymPoolFacilityTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法gymPoolFacilityTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 健身房 游泳池 桑拿", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "laundryServiceTool", description = "长住旅客福音！查询酒店是否有自助洗衣房、洗衣服务、以及是否提供熨烫服务。")
    public String laundryServiceTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法laundryServiceTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 洗衣房 洗衣服务 熨烫", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "viewAndFloorPreferenceTool", description = "景观控必问！查询酒店是否有海景房、城景房、高楼层房间，以及如何在预订时指定房间偏好。")
    public String viewAndFloorPreferenceTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法viewAndFloorPreferenceTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 海景房 城景房 高楼层 景观", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "smokingPolicyTool", description = "烟民注意！查询酒店是否为无烟酒店、是否有吸烟楼层或户外吸烟区、以及在房间内吸烟的罚款金额。")
    public String smokingPolicyTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法smokingPolicyTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 吸烟 无烟 吸烟区 罚款", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "airportTransferTool", description = "转机党福音！查询酒店是否提供免费机场接送班车、班车时刻表、以及机场到酒店的打车费用估算。")
    public String airportTransferTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法airportTransferTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 机场接送 班车 免费接送", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "roomServiceMenuTool", description = "宅酒店必备！查询酒店是否有24小时送餐服务、菜单种类、以及是否支持外卖送至房间。")
    public String roomServiceMenuTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法roomServiceMenuTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 送餐服务 客房服务 外卖", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }

    @Tool(name = "securityAndPrivacyTool", description = "安全至上！查询酒店是否有24小时前台、监控系统、保险箱、以及是否需要门禁卡才能进入楼层。")
    public String securityAndPrivacyTool(@ToolParam(description = "酒店名称") String hotelName, @ToolParam(description = "所在城市") String city) {
        log.info("开始进入方法securityAndPrivacyTool，参数为 hotelName -> {}, city -> {}", hotelName, city);
        String query = String.format("%s %s 安全 监控 保险箱 门禁", city, hotelName);
        return tavilyApiClient.searchAsText(query, 3);
    }
}
