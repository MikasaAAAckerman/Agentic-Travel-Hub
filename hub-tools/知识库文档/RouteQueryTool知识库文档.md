# 旅游大模型原子工具名录

功能：最基础的导航工具喵！根据起点和终点，规划最快的自驾路线，并返回精准的物理距离、预计驾驶耗时以及预估的高速过路费。
[TOOL_BEAN_NAME: driveRoutePlanningTool]

功能：专为非自驾用户准备！综合地铁、公交、有轨电车，提供包含"步行距离"、"换乘次数"在内的最优公共交通组合方案喵。
[TOOL_BEAN_NAME: publicTransitRoutingTool]

功能：针对 5 公里内的短途出行喵！避开机动车主干道，优先推荐绿道、公园穿行路线，并顺便计算骑行或步行将消耗的卡路里。
[TOOL_BEAN_NAME: walkingAndCyclingRouteTool]

功能：动态数据工具！查询指定道路或商圈当前的实时拥堵等级（红/黄/绿），以及前方是否有交通事故或施工封路喵。
[TOOL_BEAN_NAME: liveTrafficCongestionTool]

功能：深夜干饭人保命神器！查询特定地铁站/公交线路的末班车发车时间，防止用户在外面玩得太晚而流落街头喵！
[TOOL_BEAN_NAME: subwayOperationTimeTool]

功能：自驾游刚需！在已经规划好的长途路线上，动态搜索距离当前位置前方最近的服务区、加油站、公共厕所或充电桩喵！
[TOOL_BEAN_NAME: poiAlongRouteTool]

功能：打车比价神器喵！查询从 A 到 B 打车时，快车、专车、豪华车的大致价格区间，以及当前区域呼叫网约车的平均排队等候时间。
[TOOL_BEAN_NAME: rideHailingEstimateTool]

功能：为了"在路上"的浪漫工具！主动避开枯燥的高速公路，专门规划途经海岸线、盘山公路或森林公园的绝美景观路线喵。
[TOOL_BEAN_NAME: scenicDriveRouteTool]

功能：极其硬核的规则工具！专为驾驶房车或重型越野车的用户查询路线上的限高杆高度、桥梁限重，以及城市中心区对外地车牌的限行政策（如广州开四停四）喵！
[TOOL_BEAN_NAME: largeVehicleRestrictionTool]

功能：算法级神仙工具！当用户在一天内需要打卡 5 个不同的景点时，此工具利用运筹学算法，自动计算出不走回头路的最优游玩顺序喵！
[TOOL_BEAN_NAME: multiStopRouteOptimizationTool]

功能：解决"最后一公里"痛点的神器喵！当用户下了地铁但距离目的地还有 2 公里时，查询该地铁口 100 米内是否有可用的美团/哈啰单车，或者是否处于"共享电单车禁停区"。
[TOOL_BEAN_NAME: sharedMobilityLocatorTool]

功能：专门处理跨江、跨海或岛屿旅游的水上交通工具喵！查询如鼓浪屿轮渡、珠江夜游船或大连至烟台滚装船的实时发船时刻表及抗风浪停航预警。
[TOOL_BEAN_NAME: ferryAndWaterwayTool]

功能：为名山大川（如黄山、泰山、华山）量身定制！查询景区内部环保接驳车和高空索道的运营时间、当前排队耗时，并给出"徒步与坐缆车"的体力与时间性价比测算喵。
[TOOL_BEAN_NAME: cableCarAndShuttleTool]

功能：极其硬核的省钱计算器！结合不同省份的"节假日高速免费政策"、"ETC 九五折"甚至"夜间货车/特定路段差异化收费"，精准计算过路费喵。
[TOOL_BEAN_NAME: highwayTollAndETCTool]

功能：自驾保命预警！实时查询公安交管部门的数据，判断前方高速是否因为大雪结冰、大雾低能见度或暴雨积水而采取了"封闭收费站"或"警车带道"的强制管制措施喵。
[TOOL_BEAN_NAME: trafficControlWarningTool]

功能：出境游或大湾区通行刚需！查询如深圳前往香港的各大口岸（罗湖、福田、深圳湾）当前的过关排队人数、通关耗时，并推荐最快的口岸喵。
[TOOL_BEAN_NAME: borderCrossingWaitTimeTool]

功能：拯救外地游客的利器喵！查询目的地的公交/地铁是否支持用户家乡的"交通联合卡"，或者是否能直接使用支付宝/微信的乘车码，避免在闸机口罚站下 APP 的尴尬。
[TOOL_BEAN_NAME: transitPassCompatibilityTool]

功能：危机处理工具喵！当用户在偏远地区爆胎或抛锚时，提供距离最近的官方高速拖车电话、车险免费救援核销指南以及防天价拖车费的防坑话术！
[TOOL_BEAN_NAME: roadsideAssistanceTool]

功能：专门对付如"重庆北站"、"北京南站"这种结构极其复杂的巨型交通枢纽喵！测算从高铁出站口到地铁进站口的真实物理步行时间，防止换乘时间不足导致误车。
[TOOL_BEAN_NAME: stationInternalNavigationTool]

功能：极其前沿的高端工具！查询如深圳、低空经济试点城市中，从 CBD 到机场的直升机"飞的"航线、票价（如单程 999 元）以及行李重量严格限制喵！
[TOOL_BEAN_NAME: urbanAirMobilityTool]
