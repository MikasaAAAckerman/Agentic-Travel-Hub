package com.travel.hubtools.tool.travel;

import com.travel.hubtools.tool.common.IAgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 机票余票查询工具
 */
@Slf4j
@Component
public class FlightTicketTool implements IAgentTool {

    @Autowired
    private ChatModel dashScopeChatModel;

    /**
     * 基础航线与舱位比价工具
     *
     * @param origin      出发地
     * @param destination 目的地
     * @param date        日期
     * @param cabinClass  舱位等级，如：经济、商务、头等
     * @return String (包含航班号、起降时间、航司名称及各个舱位余票和价格的精简列表喵)
     * @Description 最核心的航班搜索库喵！根据出发地、目的地（支持城市或具体机场三字码）、日期和舱位等级（经济/商务/头等），查询直飞或转机的航班列表及实时基础票价。
     */
    @Tool(name = "flightAvailabilityAndPriceTool", description = "最核心的航班搜索库喵！根据出发地、目的地（支持城市或具体机场三字码）、日期和舱位等级（经济/商务/头等），查询直飞或转机的航班列表及实时基础票价。")
    public String flightAvailabilityAndPriceTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination, @ToolParam(description = "日期") String date, @ToolParam(description = "舱位等级") String cabinClass) {
        log.info("开始进入方法flightAvailabilityAndPriceTool，参数为 origin -> {}, destination -> {}, date -> {}, cabinClass -> {}", origin, destination, date, cabinClass);

        String promptText = String.format(
                """
                        请查询从%s到%s的航班信息：
                        日期：%s
                        舱位等级：%s
                        
                        请提供：
                        1. 航班号
                        2. 起降时间
                        3. 航司名称
                        4. 各个舱位余票和价格""",
                origin, destination, date, cabinClass
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具flightAvailabilityAndPriceTool执行完成");
        return result;
    }

    /**
     * 廉航鉴别与行李额度透视工具
     *
     * @param flightNumber 航班号
     * @param cabinClass   舱位等级
     * @return String (硬核提示，如："警告：此为廉航，无免费托运，手提仅限 7kg 且查体积极严，强烈建议提前线上购买行李额喵！")
     * @Description 极其重要的避坑工具喵！专门识别春秋、亚航等低成本航空（LCC），并精准查询该航班的免费手提行李尺寸限制、是否有免费托运额度，以及逾重行李的极其昂贵的罚款标准！
     */
    @Tool(name = "baggageAllowanceTool", description = "极其重要的避坑工具喵！专门识别春秋、亚航等低成本航空（LCC），并精准查询该航班的免费手提行李尺寸限制、是否有免费托运额度，以及逾重行李的极其昂贵的罚款标准！")
    public String baggageAllowanceTool(@ToolParam(description = "航班号") String flightNumber, @ToolParam(description = "舱位等级") String cabinClass) {
        log.info("开始进入方法baggageAllowanceTool，参数为 flightNumber -> {}, cabinClass -> {}", flightNumber, cabinClass);

        String promptText = String.format(
                """
                        请查询航班【%s】在%s舱位的行李政策：
                        
                        请提供：
                        1. 是否为廉航
                        2. 免费手提行李尺寸限制
                        3. 是否有免费托运额度
                        4. 逾重行李的罚款标准""",
                flightNumber, cabinClass
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具baggageAllowanceTool执行完成");
        return result;
    }

    @Tool(name = "flightPunctualityTool", description = "拯救商务出差党的保命神器喵！结合该航班过去 30 天的历史准点率，以及起降地当天的气象预报（如雷暴、大雾），预测航班取消或严重延误的概率。")
    public String flightPunctualityTool(@ToolParam(description = "航班号") String flightNumber, @ToolParam(description = "日期") String date) {
        log.info("开始进入方法flightPunctualityTool，参数为 flightNumber -> {}, date -> {}", flightNumber, date);

        String promptText = String.format(
                """
                        请查询航班【%s】在%s的准点率和延误预警：
                        
                        请提供：
                        1. 过去30天的历史准点率
                        2. 起降地当天的气象预报
                        3. 航班取消或严重延误的概率预测""",
                flightNumber, date
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具flightPunctualityTool执行完成");
        return result;
    }

    @Tool(name = "aircraftSeatComfortTool", description = "查询执飞该航班的具体机型（如 A350、B737），并提供座椅间距（Pitch）、是否为大宽体机、是否有个人娱乐系统（PTV），以及避雷靠近洗手间的\"异味座位\"喵。")
    public String aircraftSeatComfortTool(@ToolParam(description = "航班号") String flightNumber, @ToolParam(description = "日期") String date) {
        log.info("开始进入方法aircraftSeatComfortTool，参数为 flightNumber -> {}, date -> {}", flightNumber, date);

        String promptText = String.format(
                """
                        请查询航班【%s】在%s的机型和座位信息：
                        
                        请提供：
                        1. 具体机型（如A350、B737）
                        2. 座椅间距（Pitch）
                        3. 是否为大宽体机
                        4. 是否有个人娱乐系统（PTV）
                        5. 需要避雷的座位（如靠近洗手间）""",
                flightNumber, date
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具aircraftSeatComfortTool执行完成");
        return result;
    }

    @Tool(name = "transitVisaAndBaggageTool", description = "国际航班的生死线喵！查询在第三国转机时，是否需要提前办理极其麻烦的过境签证（Transit Visa），以及托运行李能否直挂目的地（无需中途捞行李重新安检）。")
    public String transitVisaAndBaggageTool(@ToolParam(description = "航班号") String flightNumber, @ToolParam(description = "转机机场代码") String transitAirportCode, @ToolParam(description = "用户国籍") String userNationality) {
        log.info("开始进入方法transitVisaAndBaggageTool，参数为 flightNumber -> {}, transitAirportCode -> {}, userNationality -> {}", flightNumber, transitAirportCode, userNationality);

        String promptText = String.format(
                """
                        请查询航班【%s】在%s转机的签证和行李政策：
                        用户国籍：%s
                        
                        请提供：
                        1. 是否需要过境签证（Transit Visa）
                        2. 托运行李能否直挂目的地
                        3. 致命红线提示""",
                flightNumber, transitAirportCode, userNationality
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具transitVisaAndBaggageTool执行完成");
        return result;
    }

    @Tool(name = "inFlightConnectivityTool", description = "专为需要在万米高空依然要苦逼改 Bug 的程序员准备喵！查询该航班是否提供机上 Wi-Fi（及收费标准）、座位底部是否有 220V 电源插座或 USB 接口。")
    public String inFlightConnectivityTool(@ToolParam(description = "航班号") String flightNumber) {
        log.info("开始进入方法inFlightConnectivityTool，参数为 flightNumber -> {}", flightNumber);

        String promptText = String.format(
                """
                        请查询航班【%s】的机上网络和电源情况：
                        
                        请提供：
                        1. 是否提供机上Wi-Fi及收费标准
                        2. 座位底部是否有220V电源插座或USB接口""",
                flightNumber
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具inFlightConnectivityTool执行完成");
        return result;
    }

    @Tool(name = "specialMealRequestTool", description = "查询该航班的餐食标准（是正餐、简餐还是只有一瓶矿泉水），并提供如何提前 48 小时申请儿童餐、低脂餐、穆斯林餐或全素食的入口信息喵。")
    public String specialMealRequestTool(@ToolParam(description = "航班号") String flightNumber, @ToolParam(description = "餐食偏好") String mealPreference) {
        log.info("开始进入方法specialMealRequestTool，参数为 flightNumber -> {}, mealPreference -> {}", flightNumber, mealPreference);

        String promptText = String.format(
                """
                        请查询航班【%s】的餐食情况：
                        餐食偏好：%s
                        
                        请提供：
                        1. 餐食标准（正餐、简餐或仅矿泉水）
                        2. 如何提前48小时申请特殊餐食""",
                flightNumber, mealPreference
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具specialMealRequestTool执行完成");
        return result;
    }

    @Tool(name = "redEyeJetLagTool", description = "识别由于跨越多个时区或深夜起飞的\"红眼航班（Red-eye Flight）\"，并为用户提供褪黑素服用建议、眼罩耳塞携带提醒及到达后的倒时差方案喵。")
    public String redEyeJetLagTool(@ToolParam(description = "航班号") String flightNumber) {
        log.info("开始进入方法redEyeJetLagTool，参数为 flightNumber -> {}", flightNumber);

        String promptText = String.format(
                """
                        请分析航班【%s】的红眼航班情况和时差建议：
                        
                        请提供：
                        1. 是否为红眼航班
                        2. 褪黑素服用建议
                        3. 眼罩耳塞携带提醒
                        4. 到达后的倒时差方案""",
                flightNumber
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具redEyeJetLagTool执行完成");
        return result;
    }

    @Tool(name = "aviationPetPolicyTool", description = "带毛孩子上天的必备工具喵！查询航司是否允许小型宠物带入客舱（放座位底下），或者底舱是否有温控有氧舱，以及极其繁琐的检疫证明要求！")
    public String aviationPetPolicyTool(@ToolParam(description = "航司代码") String airlineCode, @ToolParam(description = "宠物类型") String petType) {
        log.info("开始进入方法aviationPetPolicyTool，参数为 airlineCode -> {}, petType -> {}", airlineCode, petType);

        String promptText = String.format(
                """
                        请查询航司【%s】的%s宠物政策：
                        
                        请提供：
                        1. 是否允许小型宠物带入客舱
                        2. 底舱是否有温控有氧舱
                        3. 检疫证明要求""",
                airlineCode, petType
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具aviationPetPolicyTool执行完成");
        return result;
    }

    @Tool(name = "flightPriceTrendTool", description = "帮你省钱的神器喵！对比当前票价与历史同期均价，判断现在是否为买票的最佳时机（抄底），或者建议\"再等等，可能会放特价票\"。")
    public String flightPriceTrendTool(@ToolParam(description = "起点") String origin, @ToolParam(description = "目的地") String destination, @ToolParam(description = "目标日期") String targetDate) {
        log.info("开始进入方法flightPriceTrendTool，参数为 origin -> {}, destination -> {}, targetDate -> {}", origin, destination, targetDate);

        String promptText = String.format(
                """
                        请分析从%s到%s在%s的票价趋势：
                        
                        请提供：
                        1. 当前票价与历史同期均价对比
                        2. 现在是否为买票的最佳时机
                        3. 价格趋势分析""",
                origin, destination, targetDate
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具flightPriceTrendTool执行完成");
        return result;
    }

    @Tool(name = "seatSelectionPreferenceTool", description = "极其精细的选座工具喵！不仅查询剩余座位，还能根据用户\"想看风景（靠窗）\"、\"尿频需要随时起立（过道）\"或\"坚决不坐中间夹心饼干位\"的偏好，自动寻找并锁定最优座位！")
    public String seatSelectionPreferenceTool(@ToolParam(description = "航班号") String flightNumber, @ToolParam(description = "偏好") String preference) {
        log.info("开始进入方法seatSelectionPreferenceTool，参数为 flightNumber -> {}, preference -> {}", flightNumber, preference);

        String promptText = String.format(
                """
                        请查询航班【%s】的座位情况，偏好：%s
                        
                        请提供：
                        1. 推荐座位的排号
                        2. 选该座位是否需要额外支付\"选座费\"""",
                flightNumber, preference
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具seatSelectionPreferenceTool执行完成");
        return result;
    }

    @Tool(name = "infantBassinetAndFamilyTool", description = "带人类幼崽出行的救命工具喵！专门查询航班是否提供可挂在舱壁上的免费婴儿摇篮（Bassinet），以及是否能为带两名以上儿童的家庭强制分配\"连号座位\"。")
    public String infantBassinetAndFamilyTool(@ToolParam(description = "航班号") String flightNumber, @ToolParam(description = "婴儿数量") int infantCount) {
        log.info("开始进入方法infantBassinetAndFamilyTool，参数为 flightNumber -> {}, infantCount -> {}", flightNumber, infantCount);

        String promptText = String.format(
                """
                        请查询航班【%s】的婴儿和家庭座位政策：
                        婴儿数量：%d
                        
                        请提供：
                        1. 是否提供免费婴儿摇篮（Bassinet）
                        2. 摇篮剩余数量和申请截止时间
                        3. 是否能强制分配\"连号座位\"""",
                flightNumber, infantCount
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具infantBassinetAndFamilyTool执行完成");
        return result;
    }

    @Tool(name = "turbulenceForecastTool", description = "结合高空急流与气象雷达数据，预测该航线途经区域（如著名的孟加拉湾、赤道辐合带）是否会有重度颠簸，为严重晕机的用户提供避雷或备药建议喵！")
    public String turbulenceForecastTool(@ToolParam(description = "航班号") String flightNumber, @ToolParam(description = "日期") String date) {
        log.info("开始进入方法turbulenceForecastTool，参数为 flightNumber -> {}, date -> {}", flightNumber, date);

        String promptText = String.format(
                """
                        请预测航班【%s】在%s的颠簸情况：
                        
                        请提供：
                        1. 颠簸指数预测
                        2. 途经区域是否有重度颠簸
                        3. 晕机防护建议""",
                flightNumber, date
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具turbulenceForecastTool执行完成");
        return result;
    }

    @Tool(name = "refundAndChangeFeeTool", description = "极其复杂但也最省钱的工具喵！解析航司极其变态的\"阶梯退改签规则\"（如起飞前 7 天、前 48 小时、前 4 小时的不同扣费比例），帮用户算出此时退票要损失多少钱。")
    public String refundAndChangeFeeTool(@ToolParam(description = "航班号") String flightNumber, @ToolParam(description = "舱位代码") String bookingClass) {
        log.info("开始进入方法refundAndChangeFeeTool，参数为 flightNumber -> {}, bookingClass -> {}", flightNumber, bookingClass);

        String promptText = String.format(
                """
                        请查询航班【%s】在%s舱位的退改签政策：
                        
                        请提供：
                        1. \"阶梯退改签规则\"
                        2. 精准的退票手续费金额
                        3. 是否为特价机票及退款规则""",
                flightNumber, bookingClass
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具refundAndChangeFeeTool执行完成");
        return result;
    }

    @Tool(name = "securityWaitTimeTool", description = "防误机神器喵！根据出发机场当天的客流量（如是否为节假日早高峰），预测安检队伍的排队时长，并提示是否可以通过购买 CIP 服务走快速通道。")
    public String securityWaitTimeTool(@ToolParam(description = "出发机场") String departureAirport, @ToolParam(description = "航班时间") String flightTime) {
        log.info("开始进入方法securityWaitTimeTool，参数为 departureAirport -> {}, flightTime -> {}", departureAirport, flightTime);

        String promptText = String.format(
                """
                        请预测%s机场在%s的安检排队情况：
                        
                        请提供：
                        1. 预计安检排队时长
                        2. 是否可以通过购买CIP服务走快速通道
                        3. 建议到达机场的提前量""",
                departureAirport, flightTime
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具securityWaitTimeTool执行完成");
        return result;
    }

    @Tool(name = "airportLoungeAccessTool", description = "高端羊毛党专属喵！查询用户持有的信用卡或龙腾出行/Priority Pass 卡，在特定的航站楼能免费进入哪个贵宾休息室，以及休息室是否提供热食。")
    public String airportLoungeAccessTool(@ToolParam(description = "机场代码") String airportCode, @ToolParam(description = "航站楼/高铁站") String terminal, @ToolParam(description = "会员类型") String membershipType) {
        log.info("开始进入方法airportLoungeAccessTool，参数为 airportCode -> {}, terminal -> {}, membershipType -> {}", airportCode, terminal, membershipType);

        String promptText = String.format(
                """
                        请查询%s机场%s航站楼的贵宾室权益：
                        会员类型：%s
                        
                        请提供：
                        1. 可用贵宾室的位置指引
                        2. 休息室是否提供热食
                        3. 限带人数""",
                airportCode, terminal, membershipType
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具airportLoungeAccessTool执行完成");
        return result;
    }

    @Tool(name = "wheelchairAndMedicalTool", description = "极具人文关怀的医疗协助工具喵！为腿脚不便或刚做完手术的用户，查询如何提前申请机场内的免费轮椅接送，以及机上是否配备医用氧气瓶。")
    public String wheelchairAndMedicalTool(@ToolParam(description = "航班号") String flightNumber, @ToolParam(description = "医疗需求") String medicalNeed) {
        log.info("开始进入方法wheelchairAndMedicalTool，参数为 flightNumber -> {}, medicalNeed -> {}", flightNumber, medicalNeed);

        String promptText = String.format(
                """
                        请查询航班【%s】的无障碍服务：
                        医疗需求：%s
                        
                        请提供：
                        1. 如何提前申请机场内的免费轮椅接送
                        2. 机上是否配备医用氧气瓶
                        3. 相关申请要求和流程""",
                flightNumber, medicalNeed
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具wheelchairAndMedicalTool执行完成");
        return result;
    }

    @Tool(name = "oversizedSportsEquipmentTool", description = "滑雪、高尔夫和冲浪爱好者的硬核工具喵！查询航司对雪板、高尔夫球包等异型行李的收费标准，有些航司将其算作普通行李额，有些则要收天价超规费！")
    public String oversizedSportsEquipmentTool(@ToolParam(description = "航司代码") String airlineCode, @ToolParam(description = "器材类型") String equipmentType) {
        log.info("开始进入方法oversizedSportsEquipmentTool，参数为 airlineCode -> {}, equipmentType -> {}", airlineCode, equipmentType);

        String promptText = String.format(
                """
                        请查询航司【%s】的%s托运政策：
                        
                        请提供：
                        1. 异型行李托运政策
                        2. 收费标准
                        3. 包装要求""",
                airlineCode, equipmentType
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具oversizedSportsEquipmentTool执行完成");
        return result;
    }

    @Tool(name = "onlineCheckInTool", description = "查询该航班是否支持提前 24 小时进行线上值机（Web Check-in），并在无托运行李的情况下直接生成电子二维码登机牌，跳过柜台排队喵。")
    public String onlineCheckInTool(@ToolParam(description = "航班号") String flightNumber) {
        log.info("开始进入方法onlineCheckInTool，参数为 flightNumber -> {}", flightNumber);

        String promptText = String.format(
                """
                        请查询航班【%s】的在线值机政策：
                        
                        请提供：
                        1. 是否支持提前24小时线上值机
                        2. 是否支持电子登机牌
                        3. 是否有必须人工柜台值机的限制""",
                flightNumber
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具onlineCheckInTool执行完成");
        return result;
    }

    @Tool(name = "carbonEmissionAndGreenFlightTool", description = "为注重环保或有企业 ESG 要求的差旅人士提供喵！计算该航线的碳排放量，并推荐使用了可持续航空燃料（SAF）或碳排放较低的\"绿色航班\"。")
    public String carbonEmissionAndGreenFlightTool(@ToolParam(description = "航班号") String flightNumber) {
        log.info("开始进入方法carbonEmissionAndGreenFlightTool，参数为 flightNumber -> {}", flightNumber);

        String promptText = String.format(
                """
                        请计算航班【%s】的碳排放情况：
                        
                        请提供：
                        1. 人均碳排放千克数
                        2. 是否使用了可持续航空燃料（SAF）
                        3. 是否属于环保之选""",
                flightNumber
        );

        String result = dashScopeChatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
        log.info("工具carbonEmissionAndGreenFlightTool执行完成");
        return result;
    }
}