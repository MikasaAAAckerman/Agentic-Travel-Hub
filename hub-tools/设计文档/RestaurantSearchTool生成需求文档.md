# RestaurantSearchTool 生成需求文档

## 🍽️ RestaurantSearchTool 原子能力库 (1~20)

### 1. 🍜 基础附近美食与评分过滤工具 (nearbyRestaurantQueryTool)

**大模型用的 @Description：** 最基础的觅食工具喵！根据用户当前所在的商圈或地标，结合想吃的菜系，查询方圆 1 公里内的高分餐厅列表，并自动过滤掉评分低于 4.0 的避雷店！

**入参 (Input)：** String location (当前位置), String cuisineType (菜系，如：火锅、日料、粤菜), double minRating (最低评分)

**出参 (Output)：** String (推荐餐厅名称、人均消费、核心招牌菜及排队预警喵)

---

### 2. 🚫 严苛饮食禁忌与过敏原筛查工具 (dietaryRestrictionTool)

**大模型用的 @Description：** 保命级别的工具！专门为有清真要求、纯素食主义（Vegan）、或者对花生、海鲜、麸质严重过敏的用户，精准筛选提供专属菜单或后厨完全隔离的绝对安全餐厅喵！

**入参 (Input)：** String location, List<String> restrictions (禁忌列表，如：["无麸质", "花生过敏", "纯素食"])

**出参 (Output)：** String (符合要求的餐厅，并明确指出该餐厅是否有"防交叉感染"的后厨操作规范喵)

---

### 3. ⏱️ 实时等位与智能取号探测工具 (liveQueueAndReservationTool)

**大模型用的 @Description：** 解决"肚子饿得咕咕叫还要排队两小时"痛点的神器喵！查询特定热门餐厅当前的实时排队桌数，以及是否支持线上提前取号或支付定金预订。

**入参 (Input)：** String restaurantName

**出参 (Output)：** String (如"当前小桌前方等待 45 桌，预计耗时 90 分钟，强烈建议立即调用线上取号接口"喵)

---

### 4. 🧍‍♂️ 社恐与一人食友好度评估工具 (soloDiningFriendlyTool)

**大模型用的 @Description：** 情绪价值拉满喵！专门为独自旅行或重度社恐用户查询：餐厅是否提供"面壁单人座"、是否支持全程扫码点餐结账（无需和老服务员说话），以及是否足够安静。

**入参 (Input)：** String location, String mealType (如：简餐、烤肉)

**出参 (Output)：** String (社恐友好度评分，如"提供带隔板的单人拉面座，全程无接触上菜，社恐天堂喵")

---

### 5. 👔 米其林与高端餐饮礼仪预警工具 (fineDiningEtiquetteTool)

**大模型用的 @Description：** 高级避坑指南！查询特定的黑珍珠或米其林餐厅是否有严格的着装要求（Dress Code），以及是否有高昂的开瓶费、最低消费或强制的服务费比例喵。

**入参 (Input)：** String restaurantName

**出参 (Output)：** String (比如："必须穿带领衬衫和包头鞋，严禁男士穿短裤拖鞋，需提前两周预约并支付 50% 定金喵")

---

### 6. 🦉 深夜食堂与跨更供餐查询工具 (midnightSnackTool)

**大模型用的 @Description：** 拯救深夜加班狗和夜猫子的工具喵！查询指定区域在凌晨 12 点以后依然提供堂食的热门夜宵档、大排档或 24 小时快餐。

**入参 (Input)：** String location, String time (如 "02:00 AM")

**出参 (Output)：** String (深夜营业的餐厅列表、深夜专供菜单以及周边的治安安全提示喵)

---

### 7. 🗺️ 本地土著防坑与苍蝇馆子挖掘工具 (authenticLocalFoodTool)

**大模型用的 @Description：** 专门用来避开"专宰外地游客的网红美食街"的硬核工具喵！挖掘只存在于老旧小区里、环境一般但味道绝佳、只有本地老头老太去吃的正宗苍蝇馆子！

**入参 (Input)：** String city, String localDish (特色小吃，如：豆汁儿、肠粉)

**出参 (Output)：** String (避开商业街的隐秘小店定位、环境简陋预警，以及必须带现金的提示喵)

---

### 8. 🥂 商务宴请与高密闭性包厢检索工具 (businessBanquetTool)

**大模型用的 @Description：** 为出差谈几个亿大项目的用户准备喵！严格筛选提供绝对隔音包厢、有专属服务员、方便泊车且能顺利开具增值税专用发票的高端宴请酒楼。

**入参 (Input)：** String location, int guestCount, boolean needInvoice

**出参 (Output)：** String (包厢最低消费标准、是否有专车接送通道，以及发票开具的便利度喵)

---

### 9. 🍼 宝妈喘息与硬核亲子餐厅查询工具 (kidFriendlyDiningTool)

**大模型用的 @Description：** 带娃出行的救星喵！查询餐厅是否提供符合安全标准的宝宝椅、无盐无糖的儿童特供餐，以及最重要的——是否有带专人看护的室内儿童淘气堡！

**入参 (Input)：** String location, int kidAge

**出参 (Output)：** String (比如："不仅提供儿童餐，还有 50 平方米的海洋球池和免费看护小姐姐，家长可以安心干饭喵！")

---

### 10. 🐶 宠物共餐与毛孩子友好菜单工具 (petDiningTool)

**大模型用的 @Description：** 带着主子去干饭喵！查询不仅允许宠物入内，甚至专门为猫狗提供"无添加宠物特供披萨/牛排"的硬核宠物友好餐厅。

**入参 (Input)：** String location

**出参 (Output)：** String (宠物入内规则：是只能坐外摆区还是可以进室内，以及宠物特供菜单的价格喵)

---

### 11. 🌆 高空景观与露台餐厅查验工具 (rooftopScenicDiningTool)

**大模型用的 @Description：** 情绪价值与约会必杀工具！专门筛选那些位于摩天大楼顶层、或者拥有绝佳江景/海景露台的浪漫餐厅，并评估其靠窗座位的预订难度喵。

**入参 (Input)：** String city, String viewType (如：外滩江景、高空夜景)

**出参 (Output)：** String (绝佳景观餐厅列表，并附带"靠窗位需提前 30 天预订且需支付 500 元定金"的防坑提示喵)

---

### 12. 👨‍👩‍👧‍👦 超大多人聚餐与团建适配工具 (largeGroupGatheringTool)

**大模型用的 @Description：** 拯救 HR 和团建组织者的神器！专门查询餐厅是否提供能容纳 15 人以上的连坐大长桌或超大圆桌包厢，以及是否支持极其复杂的 AA 制分账和开具集体发票喵。

**入参 (Input)：** String location, int groupSize (人数，如：20)

**出参 (Output)：** String (提供大圆桌的餐厅、包场最低消费额度以及团建专属套餐报价喵)

---

### 13. 🎸 驻唱与演艺主题餐饮探测工具 (livePerformanceDiningTool)

**大模型用的 @Description：** 查询特定餐厅或 Livehouse 在用户就餐时段是否有乐队驻唱、脱口秀表演或特色民族舞蹈，并提供演出具体的时间表喵。

**入参 (Input)：** String location, String performanceType (如：爵士乐、脱口秀)

**出参 (Output)：** String (演出时刻表、演出期间的噪音等级评估，以及是否需要额外收取门票/卡座费喵)

---

### 14. 🥗 严格减脂与卡路里透明餐饮工具 (fitnessMacroDietTool)

**大模型用的 @Description：** 健身狂魔的福音！精准检索那些提供详细卡路里标识（Macros）、支持把沙拉酱换成油醋汁分开装，或者专门提供水煮鸡胸肉/低GI主食的硬核轻食餐厅喵。

**入参 (Input)：** String location, String dietGoal (如：严格减脂、增肌)

**出参 (Output)：** String (卡路里透明的轻食店列表，以及该店是否提供纯粗粮替换选项喵)

---

### 15. 🅿️ 泊车难度与代客泊车检索工具 (diningParkingValetTool)

**大模型用的 @Description：** 自驾干饭人的刚需！查询位于拥堵市中心的餐厅是否提供专属免费车位、地下车库入口的寻找难度，以及是否提供高规格的代客泊车（Valet）服务喵。

**入参 (Input)：** String restaurantName

**出参 (Output)：** String (泊车指南，如"门口交警贴条极严，强烈建议停在马路对面的商场，或者使用其 50 元/次的代客泊车服务喵")

---

### 16. 🥐 早午餐与独立烘焙咖啡馆挖掘工具 (brunchAndBakeryTool)

**大模型用的 @Description：** 专为周末睡到自然醒的慵懒打工人准备！挖掘不仅提供高质量 Brunch（如班尼迪克蛋），还拥有自家烘焙咖啡豆和手工酵母面包的独立小众咖啡馆喵。

**入参 (Input)：** String location

**出参 (Output)：** String (环境慵懒且有手冲咖啡加持的 Brunch 店，以及它们最招牌的当季面包喵)

---

### 17. 🍢 夜市摊位与流动小吃定位工具 (nightMarketStallTool)

**大模型用的 @Description：** 地气十足的工具！这不仅能搜到固定餐厅，还能结合当地城管的规定和社交媒体情报，预测那些极其好吃但居无定所的"神出鬼没流动小吃摊"的当晚出摊位置喵！

**入参 (Input)：** String city, String marketName (夜市名称)

**出参 (Output)：** String (当晚夜市的必吃摊位排雷、摊主出摊概率，以及"必须自备纸巾和零钱"的提示喵)

---

### 18. 👘 历史建筑与沉浸式主题餐厅工具 (historicalThemedDiningTool)

**大模型用的 @Description：** 查询那些开设在百年老洋房、古镇四合院，或者具有极强二次元属性（如女仆咖啡厅、动漫联名快闪店）的沉浸式餐厅，提供打破次元壁的用餐体验喵。

**入参 (Input)：** String location, String theme (如：老洋房、女仆咖啡)

**出参 (Output)：** String (极具特色的打卡地，以及是否需要遵守特定的"角色扮演互动规则"喵)

---

### 19. 🍣 无菜单料理与当季限定食材探测工具 (omakaseAndSeasonalTool)

**大模型用的 @Description：** 土豪与老饕专属！查询高端日料 Omakase 或创意法餐当前的"当季限定菜单"（如：秋季的蓝鳍金枪鱼、春季的白芦笋），并评估其食材新鲜度溢价喵。

**入参 (Input)：** String city, String cuisineStyle

**出参 (Output)：** String (当季隐藏菜单揭秘、主厨口碑，以及提前三个月预订的疯狂排期表喵)

---

### 20. 🚶‍♂️ 饭后消食与周边散步路线匹配工具 (postMealWalkabilityTool)

**大模型用的 @Description：** 跨域整合的神级工具！在推荐完大餐后，顺便查询餐厅出门后是否有适合牵手散步的林荫小道、滨江步道或城市公园，为完美的约会画上句号喵。

**入参 (Input)：** String restaurantName

**出参 (Output)：** String (比如："吃完这家惠灵顿牛排，出门右转步行 200 米就是无敌江景漫步道，约会成功率直线上升喵！")

---
