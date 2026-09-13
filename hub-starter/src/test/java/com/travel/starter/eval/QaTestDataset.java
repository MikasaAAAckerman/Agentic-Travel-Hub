package com.travel.starter.eval;

import com.travel.aiagent.common.domain.eval.QaTestCase;

import java.util.List;

/**
 * 评测数据集：标准问答对
 *
 * 用于 RAG 系统的质量评测，覆盖不同难度和分类
 */
public class QaTestDataset {

    /**
     * 获取评测数据集
     *
     * @return 标准问答对列表
     */
    public static List<QaTestCase> getTestCases() {
        return List.of(
                // ==================== 景点查询 ====================
                QaTestCase.builder()
                        .id("qa_001")
                        .question("三亚有什么好玩的景点？")
                        .expectedToolBeans(List.of("AttractionAndEntertainmentTool"))
                        .expectedDocIds(List.of("subculturePoiRadarTool_v1.0"))
                        .groundTruth("三亚有亚龙湾、天涯海角、南山寺、蜈支洲岛等著名景点")
                        .difficulty("easy")
                        .category("景点查询")
                        .build(),

                QaTestCase.builder()
                        .id("qa_002")
                        .question("广州有什么小众的二次元打卡地？")
                        .expectedToolBeans(List.of("AttractionAndEntertainmentTool"))
                        .expectedDocIds(List.of("subculturePoiRadarTool_v1.0"))
                        .groundTruth("广州有动漫星城、地王广场等二次元聚集地")
                        .difficulty("medium")
                        .category("景点查询")
                        .build(),

                // ==================== 交通规划 ====================
                QaTestCase.builder()
                        .id("qa_003")
                        .question("从北京到三亚怎么走最便宜？")
                        .expectedToolBeans(List.of("FlightTicketTool"))
                        .expectedDocIds(List.of("flightAvailabilityAndPriceTool_v1.0"))
                        .groundTruth("最便宜的方式是乘坐火车或特价机票")
                        .difficulty("easy")
                        .category("交通规划")
                        .build(),

                QaTestCase.builder()
                        .id("qa_004")
                        .question("三亚有哪些航班可以直飞？")
                        .expectedToolBeans(List.of("FlightTicketTool"))
                        .expectedDocIds(List.of("flightAvailabilityAndPriceTool_v1.0"))
                        .groundTruth("三亚凤凰机场有多个城市的直飞航班")
                        .difficulty("medium")
                        .category("交通规划")
                        .build(),

                // ==================== 酒店预订 ====================
                QaTestCase.builder()
                        .id("qa_005")
                        .question("三亚亚龙湾有什么好的酒店推荐？")
                        .expectedToolBeans(List.of("HotelBookingTool"))
                        .expectedDocIds(List.of("hotelSearchTool_v1.0"))
                        .groundTruth("亚龙湾有万豪、希尔顿、喜来登等高端酒店")
                        .difficulty("easy")
                        .category("酒店预订")
                        .build(),

                QaTestCase.builder()
                        .id("qa_006")
                        .question("三亚有没有带私人沙滩的酒店？")
                        .expectedToolBeans(List.of("HotelBookingTool"))
                        .expectedDocIds(List.of("hotelSearchTool_v1.0"))
                        .groundTruth("亚龙湾和海棠湾有多家酒店配有私人沙滩")
                        .difficulty("medium")
                        .category("酒店预订")
                        .build(),

                // ==================== 天气查询 ====================
                QaTestCase.builder()
                        .id("qa_007")
                        .question("三亚明天天气怎么样？")
                        .expectedToolBeans(List.of("WeatherOutfitTool"))
                        .expectedDocIds(List.of("weatherQueryTool_v1.0"))
                        .groundTruth("需要查询实时天气信息")
                        .difficulty("easy")
                        .category("天气查询")
                        .build(),

                QaTestCase.builder()
                        .id("qa_008")
                        .question("三亚6月份去需要带什么衣服？")
                        .expectedToolBeans(List.of("WeatherOutfitTool"))
                        .expectedDocIds(List.of("weatherQueryTool_v1.0"))
                        .groundTruth("6月是夏季，建议带短袖、短裤、防晒霜")
                        .difficulty("medium")
                        .category("天气查询")
                        .build(),

                // ==================== 美食推荐 ====================
                QaTestCase.builder()
                        .id("qa_009")
                        .question("三亚有什么好吃的海鲜餐厅？")
                        .expectedToolBeans(List.of("RestaurantSearchTool"))
                        .expectedDocIds(List.of("restaurantSearchTool_v1.0"))
                        .groundTruth("三亚有第一市场、春园海鲜广场等海鲜聚集地")
                        .difficulty("easy")
                        .category("美食推荐")
                        .build(),

                QaTestCase.builder()
                        .id("qa_010")
                        .question("三亚有什么特色的本地美食？")
                        .expectedToolBeans(List.of("RestaurantSearchTool"))
                        .expectedDocIds(List.of("restaurantSearchTool_v1.0"))
                        .groundTruth("三亚有文昌鸡、加积鸭、和乐蟹、东山羊等海南四大名菜")
                        .difficulty("medium")
                        .category("美食推荐")
                        .build(),

                // ==================== 复杂问题 ====================
                QaTestCase.builder()
                        .id("qa_011")
                        .question("三亚明天适合去海边吗？")
                        .expectedToolBeans(List.of("WeatherOutfitTool", "AttractionAndEntertainmentTool"))
                        .expectedDocIds(List.of("weatherQueryTool_v1.0", "subculturePoiRadarTool_v1.0"))
                        .groundTruth("需要先查询天气，再判断是否适合海边活动")
                        .difficulty("hard")
                        .category("综合查询")
                        .build(),

                QaTestCase.builder()
                        .id("qa_012")
                        .question("帮我规划一个三亚3天2晚的行程")
                        .expectedToolBeans(List.of("AttractionAndEntertainmentTool", "HotelBookingTool", "RestaurantSearchTool"))
                        .expectedDocIds(List.of("subculturePoiRadarTool_v1.0", "hotelSearchTool_v1.0", "restaurantSearchTool_v1.0"))
                        .groundTruth("需要综合景点、酒店、美食等多方面信息")
                        .difficulty("hard")
                        .category("行程规划")
                        .build()
        );
    }
}
