# RouteQueryTool 生成需求文档

## 🗺️ 红豆大人的 RouteQueryTool 原子能力库 (1~20)

### 1. 🚗 极速驾车路线规划与耗时测算工具 (driveRoutePlanningTool)

**大模型用的 @Description：** 最基础的导航工具喵！根据起点和终点，规划最快的自驾路线，并返回精准的物理距离、预计驾驶耗时以及预估的高速过路费。

**入参 (Input)：** String origin, String destination, String strategy (如：躲避拥堵、不走高速)

**出参 (Output)：** String (比如：“推荐走广深沿江高速，全程 120 公里，预计耗时 1.5 小时，过路费约 60 元喵”)

---

### 2. 🚇 公共交通与无缝换乘查询工具 (publicTransitRoutingTool)

**大模型用的 @Description：** 专为非自驾用户准备！综合地铁、公交、有轨电车，提供包含“步行距离”、“换乘次数”在内的最优公共交通组合方案喵。

**入参 (Input)：** String origin, String destination, String preference (如：少步行、少换乘)

**出参 (Output)：** String (详细换乘步骤，如：“先乘坐地铁 3 号线，在珠江新城站站内换乘 5 号线，出站后需步行 500 米，极其费腿喵”)

---

### 3. 🚶‍♂️ 步行/骑行绿道与卡路里消耗工具 (walkingAndCyclingRouteTool)

**大模型用的 @Description：** 针对 5 公里内的短途出行喵！避开机动车主干道，优先推荐绿道、公园穿行路线，并顺便计算骑行或步行将消耗的卡路里。

**入参 (Input)：** String origin, String destination, String mode (步行/骑行)

**出参 (Output)：** String (路线指引及消耗预估，如：“全程沿滨江步道骑行约 3 公里，风景绝佳，预计消耗 150 大卡喵”)

---

### 4. 🚦 实时路况与拥堵避坑预警工具 (liveTrafficCongestionTool)

**大模型用的 @Description：** 动态数据工具！查询指定道路或商圈当前的实时拥堵等级（红/黄/绿），以及前方是否有交通事故或施工封路喵。

**入参 (Input)：** String roadNameOrArea

**出参 (Output)：** String (路况播报，如：“天河路当前严重拥堵，平均车速仅 15km/h，强烈建议立刻改乘地铁喵！”)

---

### 5. 🦉 轨道交通首末班车探测工具 (subwayOperationTimeTool)

**大模型用的 @Description：** 深夜干饭人保命神器！查询特定地铁站/公交线路的末班车发车时间，防止用户在外面玩得太晚而流落街头喵！

**入参 (Input)：** String stationName, String lineName

**出参 (Output)：** String (如：“警告：地铁 3 号线往番禺广场方向的末班车将于 23:30 发车，您还剩 15 分钟，请立刻狂奔喵！”)

---

### 6. ⛽ 沿途 POI (兴趣点) 动态搜索工具 (poiAlongRouteTool)

**大模型用的 @Description：** 自驾游刚需！在已经规划好的长途路线上，动态搜索距离当前位置前方最近的服务区、加油站、公共厕所或充电桩喵！

**入参 (Input)：** String currentRouteId, String currentLatLng, String poiType (如：加油站、服务区)

**出参 (Output)：** String (比如：“前方 20 公里处有厚街服务区，配备 95 号汽油和 8 个国家电网快充桩喵”)

---

### 7. 🚖 网约车多平台比价与等候预估工具 (rideHailingEstimateTool)

**大模型用的 @Description：** 打车比价神器喵！查询从 A 到 B 打车时，快车、专车、豪华车的大致价格区间，以及当前区域呼叫网约车的平均排队等候时间。

**入参 (Input)：** String origin, String destination

**出参 (Output)：** String (如：“当前早高峰区域打车极难，预计排队 20 分钟，快车预估 45 元，建议加价呼叫专车喵”)

---

### 8. 🌄 景观公路与自驾游路线定制工具 (scenicDriveRouteTool)

**大模型用的 @Description：** 为了“在路上”的浪漫工具！主动避开枯燥的高速公路，专门规划途经海岸线、盘山公路或森林公园的绝美景观路线喵。

**入参 (Input)：** String origin, String destination

**出参 (Output)：** String (如：“为您切换为沿海公路模式，车程将增加 40 分钟，但右侧全程可观赏绝美海景，适合打开车窗播放音乐喵”)

---

### 9. 🚚 大型车辆/房车限行与限高规避工具 (largeVehicleRestrictionTool)

**大模型用的 @Description：** 极其硬核的规则工具！专为驾驶房车或重型越野车的用户查询路线上的限高杆高度、桥梁限重，以及城市中心区对外地车牌的限行政策（如广州开四停四）喵！

**入参 (Input)：** String routePlan, String vehicleType (如：外地牌照、房车), double vehicleHeight

**出参 (Output)：** String (致命红线预警，如：“路线前方有 2.5 米限高杆，您的房车无法通过，已为您重新规划绕行路线喵！”)

---

### 10. 📍 TSP 多目的地旅行商最优路径工具 (multiStopRouteOptimizationTool)

**大模型用的 @Description：** 算法级神仙工具！当用户在一天内需要打卡 5 个不同的景点时，此工具利用运筹学算法，自动计算出不走回头路的最优游玩顺序喵！

**入参 (Input)：** String origin, List<String> waypoints (途经点列表)

**出参 (Output)：** String (重新排序后的打卡顺序及总耗时，如：“已为您优化路线，建议先去沙面，顺路去上下九，最后去广州塔看夜景，可节省 40% 通勤时间喵！”)

---

### 11. 🚲 共享单车与电单车分布探测工具 (sharedMobilityLocatorTool)

**大模型用的 @Description：** 解决“最后一公里”痛点的神器喵！当用户下了地铁但距离目的地还有 2 公里时，查询该地铁口 100 米内是否有可用的美团/哈啰单车，或者是否处于“共享电单车禁停区”。

**入参 (Input)：** String currentLocation, String destination

**出参 (Output)：** String (比如：“出站口 A 有大量共享单车，骑行约 8 分钟可达；注意目的地为电单车禁停区，强行停车将扣除 20 元调度费喵！”)

---

### 12. 🚢 水上客运与观光渡轮时刻工具 (ferryAndWaterwayTool)

**大模型用的 @Description：** 专门处理跨江、跨海或岛屿旅游的水上交通工具喵！查询如鼓浪屿轮渡、珠江夜游船或大连至烟台滚装船的实时发船时刻表及抗风浪停航预警。

**入参 (Input)：** String departurePier, String arrivalPier, String date

**出参 (Output)：** String (船次信息及预警，如：“受台风外围影响，今日前往涠洲岛的客滚船全部停航，请立即调整行程喵！”)

---

### 13. 🚠 景区索道与接驳车排队测算工具 (cableCarAndShuttleTool)

**大模型用的 @Description：** 为名山大川（如黄山、泰山、华山）量身定制！查询景区内部环保接驳车和高空索道的运营时间、当前排队耗时，并给出“徒步与坐缆车”的体力与时间性价比测算喵。

**入参 (Input)：** String scenicSpot, String routeSegment (如：云谷索道)

**出参 (Output)：** String (如：“当前云谷索道排队预计 120 分钟，若选择步行上山约需 3 小时，建议购买 VIP 快速通道票喵！”)

---

### 14. 💳 高速差异化收费与 ETC 测算工具 (highwayTollAndETCTool)

**大模型用的 @Description：** 极其硬核的省钱计算器！结合不同省份的“节假日高速免费政策”、“ETC 九五折”甚至“夜间货车/特定路段差异化收费”，精准计算过路费喵。

**入参 (Input)：** String routePlan, String vehicleType, boolean hasETC, String travelTime

**出参 (Output)：** String (精准计费，如：“由于您是纯电绿牌且持有 ETC，在广东沿江高速可享受专属折扣，预估过路费仅 42 元喵！”)

---

### 15. ⚠️ 恶劣天气交通管制与封路探测工具 (trafficControlWarningTool)

**大模型用的 @Description：** 自驾保命预警！实时查询公安交管部门的数据，判断前方高速是否因为大雪结冰、大雾低能见度或暴雨积水而采取了“封闭收费站”或“警车带道”的强制管制措施喵。

**入参 (Input)：** String roadName (如：京港澳高速湖南段)

**出参 (Output)：** String (致命红线预警，如：“警告：前方路段因大雪结冰实施特级交通管制，所有车辆就近分流下高速，请立即寻找附近酒店喵！”)

---

### 16. 🛂 跨境检查站与过关耗时测算工具 (borderCrossingWaitTimeTool)

**大模型用的 @Description：** 出境游或大湾区通行刚需！查询如深圳前往香港的各大口岸（罗湖、福田、深圳湾）当前的过关排队人数、通关耗时，并推荐最快的口岸喵。

**入参 (Input)：** String originCity, String destinationCity, String checkPoint

**出参 (Output)：** String (通关情报，如：“当前罗湖口岸人流激增，预计过关耗时 90 分钟，强烈建议改走高铁西九龙站一地两检喵！”)

---

### 17. 🎫 乘车码异地互联互通查询工具 (transitPassCompatibilityTool)

**大模型用的 @Description：** 拯救外地游客的利器喵！查询目的地的公交/地铁是否支持用户家乡的“交通联合卡”，或者是否能直接使用支付宝/微信的乘车码，避免在闸机口罚站下 APP 的尴尬。

**入参 (Input)：** String destinationCity

**出参 (Output)：** String (支付指南，如：“成都市地铁全面支持微信/支付宝乘车码直刷，无需单独下载天府通 APP，极度丝滑喵！”)

---

### 18. 🆘 道路救援与拖车呼叫指引工具 (roadsideAssistanceTool)

**大模型用的 @Description：** 危机处理工具喵！当用户在偏远地区爆胎或抛锚时，提供距离最近的官方高速拖车电话、车险免费救援核销指南以及防天价拖车费的防坑话术！

**入参 (Input)：** String locationLatLng, String emergencyType (如：爆胎、没电、抛锚)

**出参 (Output)：** String (救援指南，如：“切勿答应路边野生拖车！请立即拨打 12122 高速交警电话，并将三角警示牌放置车后 150 米处喵！”)

---

### 19. 🏢 超大枢纽站内 3D 步行导航测算工具 (stationInternalNavigationTool)

**大模型用的 @Description：** 专门对付如“重庆北站”、“北京南站”这种结构极其复杂的巨型交通枢纽喵！测算从高铁出站口到地铁进站口的真实物理步行时间，防止换乘时间不足导致误车。

**入参 (Input)：** String hubName, String fromNode (如：高铁到达层), String toNode (如：地铁 2 号线入口)

**出参 (Output)：** String (如：“从重庆北站高铁到达层前往地铁 10 号线，需步行穿过极长的地下通道，实测耗时约 12 分钟，请预留充足换乘时间喵！”)

---

### 20. 🚁 城市低空经济与飞的/直升机预订工具 (urbanAirMobilityTool)

**大模型用的 @Description：** 极其前沿的高端工具！查询如深圳、低空经济试点城市中，从 CBD 到机场的直升机“飞的”航线、票价（如单程 999 元）以及行李重量严格限制喵！

**入参 (Input)：** String origin, String destination

**出参 (Output)：** String (如："为您查到东部通航的直升机航线，从福田大中华楼顶至宝安机场仅需 15 分钟，票价 998 元，每人限带 5kg 手提行李喵！")

---