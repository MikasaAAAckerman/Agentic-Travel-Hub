# 旅游大模型原子工具名录

功能：最核心的航班搜索库喵！根据出发地、目的地（支持城市或具体机场三字码）、日期和舱位等级（经济/商务/头等），查询直飞或转机的航班列表及实时基础票价。
[TOOL_BEAN_NAME: flightAvailabilityAndPriceTool]

功能：极其重要的避坑工具喵！专门识别春秋、亚航等低成本航空（LCC），并精准查询该航班的免费手提行李尺寸限制、是否有免费托运额度，以及逾重行李的极其昂贵的罚款标准！
[TOOL_BEAN_NAME: baggageAllowanceTool]

功能：拯救商务出差党的保命神器喵！结合该航班过去 30 天的历史准点率，以及起降地当天的气象预报（如雷暴、大雾），预测航班取消或严重延误的概率。
[TOOL_BEAN_NAME: flightPunctualityTool]

功能：查询执飞该航班的具体机型（如 A350、B737），并提供座椅间距（Pitch）、是否为大宽体机、是否有个人娱乐系统（PTV），以及避雷靠近洗手间的"异味座位"喵。
[TOOL_BEAN_NAME: aircraftSeatComfortTool]

功能：国际航班的生死线喵！查询在第三国转机时，是否需要提前办理极其麻烦的过境签证（Transit Visa），以及托运行李能否直挂目的地（无需中途捞行李重新安检）。
[TOOL_BEAN_NAME: transitVisaAndBaggageTool]

功能：专为需要在万米高空依然要苦逼改 Bug 的程序员准备喵！查询该航班是否提供机上 Wi-Fi（及收费标准）、座位底部是否有 220V 电源插座或 USB 接口。
[TOOL_BEAN_NAME: inFlightConnectivityTool]

功能：查询该航班的餐食标准（是正餐、简餐还是只有一瓶矿泉水），并提供如何提前 48 小时申请儿童餐、低脂餐、穆斯林餐或全素食的入口信息喵。
[TOOL_BEAN_NAME: specialMealRequestTool]

功能：识别由于跨越多个时区或深夜起飞的"红眼航班（Red-eye Flight）"，并为用户提供褪黑素服用建议、眼罩耳塞携带提醒及到达后的倒时差方案喵。
[TOOL_BEAN_NAME: redEyeJetLagTool]

功能：带毛孩子上天的必备工具喵！查询航司是否允许小型宠物带入客舱（放座位底下），或者底舱是否有温控有氧舱，以及极其繁琐的检疫证明要求！
[TOOL_BEAN_NAME: aviationPetPolicyTool]

功能：帮你省钱的神器喵！对比当前票价与历史同期均价，判断现在是否为买票的最佳时机（抄底），或者建议"再等等，可能会放特价票"。
[TOOL_BEAN_NAME: flightPriceTrendTool]

功能：极其精细的选座工具喵！不仅查询剩余座位，还能根据用户"想看风景（靠窗）"、"尿频需要随时起立（过道）"或"坚决不坐中间夹心饼干位"的偏好，自动寻找并锁定最优座位！
[TOOL_BEAN_NAME: seatSelectionPreferenceTool]

功能：带人类幼崽出行的救命工具喵！专门查询航班是否提供可挂在舱壁上的免费婴儿摇篮（Bassinet），以及是否能为带两名以上儿童的家庭强制分配"连号座位"。
[TOOL_BEAN_NAME: infantBassinetAndFamilyTool]

功能：结合高空急流与气象雷达数据，预测该航线途经区域（如著名的孟加拉湾、赤道辐合带）是否会有重度颠簸，为严重晕机的用户提供避雷或备药建议喵！
[TOOL_BEAN_NAME: turbulenceForecastTool]

功能：极其复杂但也最省钱的工具喵！解析航司极其变态的"阶梯退改签规则"（如起飞前 7 天、前 48 小时、前 4 小时的不同扣费比例），帮用户算出此时退票要损失多少钱。
[TOOL_BEAN_NAME: refundAndChangeFeeTool]

功能：防误机神器喵！根据出发机场当天的客流量（如是否为节假日早高峰），预测安检队伍的排队时长，并提示是否可以通过购买 CIP 服务走快速通道。
[TOOL_BEAN_NAME: securityWaitTimeTool]

功能：高端羊毛党专属喵！查询用户持有的信用卡或龙腾出行/Priority Pass 卡，在特定的航站楼能免费进入哪个贵宾休息室，以及休息室是否提供热食。
[TOOL_BEAN_NAME: airportLoungeAccessTool]

功能：极具人文关怀的医疗协助工具喵！为腿脚不便或刚做完手术的用户，查询如何提前申请机场内的免费轮椅接送，以及机上是否配备医用氧气瓶。
[TOOL_BEAN_NAME: wheelchairAndMedicalTool]

功能：滑雪、高尔夫和冲浪爱好者的硬核工具喵！查询航司对雪板、高尔夫球包等异型行李的收费标准，有些航司将其算作普通行李额，有些则要收天价超规费！
[TOOL_BEAN_NAME: oversizedSportsEquipmentTool]

功能：查询该航班是否支持提前 24 小时进行线上值机（Web Check-in），并在无托运行李的情况下直接生成电子二维码登机牌，跳过柜台排队喵。
[TOOL_BEAN_NAME: onlineCheckInTool]

功能：为注重环保或有企业 ESG 要求的差旅人士提供喵！计算该航线的碳排放量，并推荐使用了可持续航空燃料（SAF）或碳排放较低的"绿色航班"。
[TOOL_BEAN_NAME: carbonEmissionAndGreenFlightTool]
