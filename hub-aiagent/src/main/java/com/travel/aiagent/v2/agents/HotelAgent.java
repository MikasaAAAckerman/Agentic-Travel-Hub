package com.travel.aiagent.v2.agents;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 酒店住宿领域 Agent。
 */
@Slf4j
@Component("hotelAgent")
public class HotelAgent extends BaseTravelAgent {

    @Override
    public String name() { return "HotelAgent"; }

    @Override
    public String description() { return "酒店住宿专家：酒店搜索、停车充电桩、周边设施、取消政策、早餐/洗衣/接送机服务"; }


}
