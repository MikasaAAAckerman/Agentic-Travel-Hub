# FlightTicketTool 生成需求文档

## ✈️ FlightTicketTool 原子能力库 (1~20)

### 1. 🛫 基础航线与舱位比价工具 (flightAvailabilityAndPriceTool)

**大模型用的 @Description：** 最核心的航班搜索库喵！根据出发地、目的地（支持城市或具体机场三字码）、日期和舱位等级（经济/商务/头等），查询直飞或转机的航班列表及实时基础票价。

**入参 (Input)：** String origin, String destination, String date, String cabinClass

**出参 (Output)：** String (包含航班号、起降时间、航司名称及各个舱位余票和价格的精简列表喵)

---

### 2. 🧳 廉航鉴别与行李额度透视工具 (baggageAllowanceTool)

**大模型用的 @Description：** 极其重要的避坑工具喵！专门识别春秋、亚航等低成本航空（LCC），并精准查询该航班的免费手提行李尺寸限制、是否有免费托运额度，以及逾重行李的极其昂贵的罚款标准！

**入参 (Input)：** String flightNumber, String cabinClass

**出参 (Output)：** String (硬核提示，如："警告：此为廉航，无免费托运，手提仅限 7kg 且查体积极严，强烈建议提前线上购买行李额喵！")

---

### 3. ⏱️ 历史准点率与延误气象预警工具 (flightPunctualityTool)

**大模型用的 @Description：** 拯救商务出差党的保命神器喵！结合该航班过去 30 天的历史准点率，以及起降地当天的气象预报（如雷暴、大雾），预测航班取消或严重延误的概率。

**入参 (Input)：** String flightNumber, String date

**出参 (Output)：** String (准点率数据及预警，如"历史准点率仅 45%，且到达地有雷雨预警，极高概率延误，建议改签高铁喵")

---

### 4. 💺 机型解析与座位舒适度探测工具 (aircraftSeatComfortTool)

**大模型用的 @Description：** 查询执飞该航班的具体机型（如 A350、B737），并提供座椅间距（Pitch）、是否为大宽体机、是否有个人娱乐系统（PTV），以及避雷靠近洗手间的"异味座位"喵。

**入参 (Input)：** String flightNumber, String date

**出参 (Output)：** String (硬件测评报告，如"空客 A350 宽体机，座椅间距宽敞，强烈推荐加钱选紧急出口排喵")

---

### 5. 🛂 跨国转机签证与行李直挂查验工具 (transitVisaAndBaggageTool)

**大模型用的 @Description：** 国际航班的生死线喵！查询在第三国转机时，是否需要提前办理极其麻烦的过境签证（Transit Visa），以及托运行李能否直挂目的地（无需中途捞行李重新安检）。

**入参 (Input)：** String flightNumber, String transitAirportCode, String userNationality (用户国籍)

**出参 (Output)：** String (致命红线提示，如"警告：在日本成田机场隔夜转机需提取行李，且无日本签证将被拒绝登机喵！")

---

### 6. 📶 万米高空 Wi-Fi 与电源插座探测工具 (inFlightConnectivityTool)

**大模型用的 @Description：** 专为需要在万米高空依然要苦逼改 Bug 的程序员准备喵！查询该航班是否提供机上 Wi-Fi（及收费标准）、座位底部是否有 220V 电源插座或 USB 接口。

**入参 (Input)：** String flightNumber

**出参 (Output)：** String (网络与供电报告，如"全程提供付费局域网，可收发微信，但无独立电源插座，请自备两万毫安充电宝喵")

---

### 7. 🍱 特殊餐食预订与清真素食查验工具 (specialMealRequestTool)

**大模型用的 @Description：** 查询该航班的餐食标准（是正餐、简餐还是只有一瓶矿泉水），并提供如何提前 48 小时申请儿童餐、低脂餐、穆斯林餐或全素食的入口信息喵。

**入参 (Input)：** String flightNumber, String mealPreference

**出参 (Output)：** String (供餐情况，如"此航班仅提供小零食，若需无麸质餐食必须提前 48 小时致电航司人工客服喵")

---

### 8. 🌙 红眼航班预警与时差恢复策略工具 (redEyeJetLagTool)

**大模型用的 @Description：** 识别由于跨越多个时区或深夜起飞的"红眼航班（Red-eye Flight）"，并为用户提供褪黑素服用建议、眼罩耳塞携带提醒及到达后的倒时差方案喵。

**入参 (Input)：** String flightNumber

**出参 (Output)：** String (疲劳预警，如"凌晨 2 点起飞，落地为当地早上 8 点，极度消耗体力，建议登机即睡，切勿饮用机上咖啡喵")

---

### 9. 🐶 宠物进客舱与有氧舱预订查询工具 (aviationPetPolicyTool)

**大模型用的 @Description：** 带毛孩子上天的必备工具喵！查询航司是否允许小型宠物带入客舱（放座位底下），或者底舱是否有温控有氧舱，以及极其繁琐的检疫证明要求！

**入参 (Input)：** String airlineCode, String petType

**出参 (Output)：** String (如"海航支持带猫进客舱，但每个航班限 2 只，需提前 7 天申请并准备《动物检疫合格证明》喵")

---

### 10. 📉 票价波动趋势与抄底建议工具 (flightPriceTrendTool)

**大模型用的 @Description：** 帮你省钱的神器喵！对比当前票价与历史同期均价，判断现在是否为买票的最佳时机（抄底），或者建议"再等等，可能会放特价票"。

**入参 (Input)：** String origin, String destination, String targetDate

**出参 (Output)：** String (价格趋势分析，如"当前票价处于近三个月历史最高点，且离起飞还有 40 天，建议设置降价提醒，暂时观望喵")

---

### 11. 💺 选座博弈与靠窗/过道偏好锁定工具 (seatSelectionPreferenceTool)

**大模型用的 @Description：** 极其精细的选座工具喵！不仅查询剩余座位，还能根据用户"想看风景（靠窗）"、"尿频需要随时起立（过道）"或"坚决不坐中间夹心饼干位"的偏好，自动寻找并锁定最优座位！

**入参 (Input)：** String flightNumber, String preference (如：靠窗、过道、前排)

**出参 (Output)：** String (推荐座位的排号，以及选该座位是否需要额外支付"选座费"的明确提示喵)

---

### 12. 🍼 婴儿摇篮与家庭连座查验工具 (infantBassinetAndFamilyTool)

**大模型用的 @Description：** 带人类幼崽出行的救命工具喵！专门查询航班是否提供可挂在舱壁上的免费婴儿摇篮（Bassinet），以及是否能为带两名以上儿童的家庭强制分配"连号座位"。

**入参 (Input)：** String flightNumber, int infantCount

**出参 (Output)：** String (摇篮剩余数量、申请截止时间，以及"未满 2 周岁婴儿票无独立座位"的硬性规则提醒喵)

---

### 13. 😷 气流颠簸预警与晕机防护工具 (turbulenceForecastTool)

**大模型用的 @Description：** 结合高空急流与气象雷达数据，预测该航线途经区域（如著名的孟加拉湾、赤道辐合带）是否会有重度颠簸，为严重晕机的用户提供避雷或备药建议喵！

**入参 (Input)：** String flightNumber, String date

**出参 (Output)：** String (颠簸指数预测，如"预计途经雷暴区，将有 30 分钟中度颠簸，强烈建议提前服用晕机药并选择机翼附近最平稳的座位喵")

---

### 14. 💰 退改签手续费与阶梯扣费计算工具 (refundAndChangeFeeTool)

**大模型用的 @Description：** 极其复杂但也最省钱的工具喵！解析航司极其变态的"阶梯退改签规则"（如起飞前 7 天、前 48 小时、前 4 小时的不同扣费比例），帮用户算出此时退票要损失多少钱。

**入参 (Input)：** String flightNumber, String bookingClass (子舱位代码，如 Y, B, M, X)

**出参 (Output)：** String (精准的退票手续费金额，以及"如果是特价机票则仅退机建燃油费"的扎心警告喵)

---

### 15. 🛃 机场安检排队耗时与快速通道预测工具 (securityWaitTimeTool)

**大模型用的 @Description：** 防误机神器喵！根据出发机场当天的客流量（如是否为节假日早高峰），预测安检队伍的排队时长，并提示是否可以通过购买 CIP 服务走快速通道。

**入参 (Input)：** String departureAirport, String flightTime

**出参 (Output)：** String (建议到达机场的提前量，如"当前虹桥 T2 安检爆满，预计耗时 45 分钟，强烈建议至少提前 2.5 小时到达机场喵")

---

### 16. ☕ 机场贵宾室与龙腾/PP卡权益核销工具 (airportLoungeAccessTool)

**大模型用的 @Description：** 高端羊毛党专属喵！查询用户持有的信用卡或龙腾出行/Priority Pass 卡，在特定的航站楼能免费进入哪个贵宾休息室，以及休息室是否提供热食。

**入参 (Input)：** String airportCode, String terminal, String membershipType (如：龙腾卡、白金信用卡)

**出参 (Output)：** String (可用贵宾室的位置指引，如"T3 航站楼安检后左转 V1 休息室可用，全天供应热食面条，限带 1 人喵")

---

### 17. ♿ 轮椅申请与机上无障碍服务查验工具 (wheelchairAndMedicalTool)

**大模型用的 @Description：** 极具人文关怀的医疗协助工具喵！为腿脚不便或刚做完手术的用户，查询如何提前申请机场内的免费轮椅接送，以及机上是否配备医用氧气瓶。

**入参 (Input)：** String flightNumber, String medicalNeed (如：轮椅、机上吸氧)

**出参 (Output)：** String (如"WCHC 级别轮椅需提前 48 小时致电航司申请，乘务员将协助上下机，自带制氧机需提供电池合规证明喵")

---

### 18. 🎿 超大超重运动器械托运查询工具 (oversizedSportsEquipmentTool)

**大模型用的 @Description：** 滑雪、高尔夫和冲浪爱好者的硬核工具喵！查询航司对雪板、高尔夫球包等异型行李的收费标准，有些航司将其算作普通行李额，有些则要收天价超规费！

**入参 (Input)：** String airlineCode, String equipmentType (如：滑雪板、自行车)

**出参 (Output)：** String (异型行李托运政策，如"国航国内线滑雪板免费算作一件普通行李，但包装长度不得超过 203 厘米喵")

---

### 19. 📄 电子登机牌与自助值机开放探测工具 (onlineCheckInTool)

**大模型用的 @Description：** 查询该航班是否支持提前 24 小时进行线上值机（Web Check-in），并在无托运行李的情况下直接生成电子二维码登机牌，跳过柜台排队喵。

**入参 (Input)：** String flightNumber

**出参 (Output)：** String (值机开放时间，以及"此国际航班因需查验签证，必须进行人工柜台值机，不支持电子登机牌"的限制提示喵)

---

### 20. 🌍 碳排放计算与绿色飞行筛选工具 (carbonEmissionAndGreenFlightTool)

**大模型用的 @Description：** 为注重环保或有企业 ESG 要求的差旅人士提供喵！计算该航线的碳排放量，并推荐使用了可持续航空燃料（SAF）或碳排放较低的"绿色航班"。

**入参 (Input)：** String flightNumber

**出参 (Output)：** String (人均碳排放千克数，以及"该航班碳排放低于同航线平均值 15%，属于环保之选喵")

---
