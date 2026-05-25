package com.travel.aiagent.v2.agents;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 出行路线领域 Agent。
 *
 * <p>持有工具：RouteQueryTool + AmapPOISearchTool（路线相关部分）
 */
@Slf4j
@Component("routeAgent")
public class RouteAgent extends BaseTravelAgent {

    @Override
    public String name() {
        return "RouteAgent";
    }

    @Override
    public String description() {
        return "出行路线专家：驾车/公交/步行路线规划、实时路况、网约车估价、过路费计算";
    }


}
