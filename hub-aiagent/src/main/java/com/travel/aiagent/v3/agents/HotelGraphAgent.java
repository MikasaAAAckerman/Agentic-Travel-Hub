package com.travel.aiagent.v3.agents;

import com.travel.aiagent.v3.graph.SubAgentGraph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HotelGraphAgent extends BaseTravelGraphAgent {

    public HotelGraphAgent(SubAgentGraph subAgentGraph) { super(subAgentGraph); }

    @Override
    public String name() { return "HotelAgent"; }

    @Override
    public String description() { return "酒店住宿专家：酒店搜索、停车充电桩、周边设施、取消政策、早餐/洗衣/接送机服务"; }

    @Override
    protected String domainInstruction() {
        return "推荐酒店前必须核对最晚入住时间是否晚于抵达时间，否则该候选不可用；"
                + "若用户提出了行动能力方面的要求，候选酒店必须具备无障碍通行条件（电梯、无台阶入口等）。";
    }
}
