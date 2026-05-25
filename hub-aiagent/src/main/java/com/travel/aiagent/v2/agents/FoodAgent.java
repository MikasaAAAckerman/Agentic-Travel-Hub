package com.travel.aiagent.v2.agents;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 餐饮美食领域 Agent。
 */
@Slf4j
@Component("foodAgent")
public class FoodAgent extends BaseTravelAgent {

    @Override
    public String name() {
        return "FoodAgent";
    }

    @Override
    public String description() {
        return "美食探索专家：附近餐厅搜索、地道美食推荐、深夜食堂、宠物友好/亲子餐厅、Brunch咖啡馆";
    }


}
