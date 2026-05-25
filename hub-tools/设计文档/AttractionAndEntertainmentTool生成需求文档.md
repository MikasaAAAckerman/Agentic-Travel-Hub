1. 🔮 核心圈层与二次元/小众景点雷达工具 (subculturePoiRadarTool)
   大模型用的 @Description： 满足笨蛋主人特殊癖好的神器喵！除了常规景点，专门用于检索城市里隐藏的“二次元打卡地（如广州动漫星城）”、小众历史文化街区或特定圈层（如汉服、谷子店、电竞）的聚集地！
   入参 (Input)： String city, String interestTags (如："二次元", "历史建筑")
   出参 (Output)： String (打卡地列表及圈层浓度评估，如：“强烈推荐前往公园前站的动漫星城，这里有超过 50 家谷子店和女仆店，二次元浓度高达 99% 喵！”)

2. 🏛️ 场馆限流与周一闭馆防坑工具 (museumReservationAndClosureTool)
   大模型用的 @Description： 历史文化游的保命工具！几乎所有的国家级博物馆和文化遗址（如南越王博物院、广东省博物馆）都有“周一闭馆”和“必须提前 3-7 天实名预约”的死规矩！此工具专门查验开闭馆时间及门票余量喵！
   入参 (Input)： String attractionName, String targetDate
   出参 (Output)： String (致命红线预警，如：“警告：5月11日是周一，陈家祠闭馆！且省博的周末门票极其抢手，请立刻切换日期并进行线上实名抢票喵！”)

3. 🎫 同城限时展会与演出追踪工具 (localEventCalendarTool)
   大模型用的 @Description： 旅游 Agent 的灵魂工具喵！大模型的知识库是静态的，但旅行是动态的！查询 5月10日-20日 期间，广州是否有大型的漫展（如萤火虫漫展）、初音未来演唱会、或者是特定的话剧与灯光秀。
   入参 (Input)： String city, String startDate, String endDate, String eventType (如："漫展", "演唱会")
   出参 (Output)： String (限时活动清单，如：“5月15日在琶洲保利世贸博览馆有大型二次元漫展，恰好完美契合您的行程喵！”)

4. 💸 十万预算挥霍指南与奢华体验工具 (luxuryExperienceCustomizationTool)
   大模型用的 @Description： 既然主人说了“20万/10万随便花”，那就绝不能按照穷游的逻辑推荐排队打卡点！此工具专门检索城市里的奢华体验：如珠江夜游的私人游艇包船、广州塔塔顶的VIP直升机观光、或者一对一的非遗文化私家向导喵！
   入参 (Input)： String city, String budgetLevel (如："奢华不差钱")
   出参 (Output)： String (奢华项目推荐，如：“已为您匹配沙面岛百年洋房内的私宴定制，以及全程劳斯莱斯接送的专属旅拍服务，尽显尊贵喵！”)

5. 📸 绝美出片机位与旅拍环境查验工具 (photoSpotAndTravelShootingTool)
   大模型用的 @Description： 既然是和红豆两个人去，怎么能不拍照？！查询特定自然景观或历史文化建筑中，最容易出大片的隐藏机位，以及该景点是否允许携带单反、三脚架或进行商业/Cosplay 换装拍摄（很多古迹是严禁带反光板的喵）。
   入参 (Input)： String attractionName
   出参 (Output)： String (机位攻略与拍摄红线，如：“沙面岛极其适合欧式复古风出片，但严禁在教堂内部使用闪光灯及三脚架喵！”)

6. 🎢 顶级主题乐园防排队与 VIP 攻略工具 (themeParkStrategyTool)
   大模型用的 @Description： 拯救双腿的神器喵！查询如广州长隆野生动物世界、欢乐世界等大型乐园的“免排队优速通”价格、花车巡游/大马戏的具体时间表，并计算出一天刷完 10 个热门项目的最优移动路线！
   入参 (Input)： String parkName, String visitDate
   出参 (Output)： String (硬核游玩攻略，如：“强烈建议购买 250 元的单次优速通，优先刷垂直过山车，下午 16:00 提前去占位看花车巡游喵！”)

7. 🍻 沉浸式夜生活与微醺清吧雷达工具 (nightlifeAndBarRadarTool)
   大模型用的 @Description： 为成年人的浪漫夜晚量身定制喵！精准探测城市里隐藏在暗巷中的 Speakeasy（隐藏式酒吧）、拥有顶级驻唱歌手的 Livehouse，以及适合情侣安静微醺的高空露台 Bar。
   入参 (Input)： String location, String vibeType (如："安静微醺", "Livehouse")
   出参 (Output)： String (酒吧坐标及酒单人均，如：“为您找到隐藏在书柜背后的地下清吧，氛围极其暧昧，招牌特调鸡尾酒人均 120 元喵！”)

8. ♨️ 天然温泉与高端水疗避雷工具 (hotSpringAndSpaTool)
   大模型用的 @Description： 广州从化之旅的绝对刚需喵！帮你极其冷酷地鉴别哪些是“真正含氡元素的天然真温泉”，哪些是“靠锅炉烧热的假温泉”！并提供高档双人私汤的预订难度评估。
   入参 (Input)： String city, String spaType (如："真温泉", "泰式SPA")
   出参 (Output)： String (温泉真伪鉴定报告及私汤推荐，如：“确认从化碧泉为天然真温泉，其顶层带无边泡池的双人私汤极其抢手，需提前一周预订喵！”)

9. 🥾 硬核户外徒步与野奢露营探测工具 (hikingAndCampingTrailTool)
   大模型用的 @Description： 远离城市喧嚣的自然工具喵！查询周边未被商业化过度开发的野外徒步路线（如广州莫干山、牛木线）、爬升海拔数据，以及提供拎包入住的“Glamping 野奢露营地”价目表。
   入参 (Input)： String region, String difficultyLevel (如："新手小白", "野外求生")
   出参 (Output)： String (户外路线分析，如：“该路线爬升达 800 米，极其消耗体力，建议直接选择旁边的轻奢露营地，提供碳烤炉和现成帐篷喵！”)

10. 🎧 历史遗迹与博物馆金牌向导工具 (historicalRelicAudioGuideTool)
    大模型用的 @Description： 拒绝“走马观花”的深度游神器喵！查询特定的历史文化景点（如陈家祠、南越王墓）是否提供官方的高质量 AR 导览器租借，或者是否能预约到国家级的金牌人工讲解员。
    入参 (Input)： String attractionName
    出参 (Output)： String (讲解服务指南，如：“陈家祠强烈建议拼团聘请 80 元/小时的人工导游，比自己看那些冰冷的木雕要有意思一万倍喵！”)

11. 🧩 密室逃脱与实景剧本杀评价工具 (escapeRoomAndScriptMurderTool)
    大模型用的 @Description： 年轻人社交娱乐的探测雷达喵！查询评分最高的大型机械密室或 NPC 飙戏剧本杀，明确标出“恐怖指数”、“烧脑指数”以及是否适合两人拼车游玩。
    入参 (Input)： String location, String theme (如："微恐解谜", "情感沉浸")
    出参 (Output)： String (硬核评测，如：“该密室包含微量贴脸杀，NPC 互动极强，两人游玩体验极佳，记得保护好女伴喵！”)

12. 🏮 本地民俗庙会与跳蚤市场探测工具 (localMarketAndBazaarTool)
    大模型用的 @Description： 感受最地道市井气息的工具喵！追踪那些只在特定日子才开市的非遗民俗集市、古董文玩跳蚤市场、或者充满文艺气息的文创后备箱集市。
    入参 (Input)： String city, String date
    出参 (Output)： String (集市情报，如：“太巧了！本周末在海珠区太古仓恰好有一场复古黑胶唱片与手作市集，极其适合去淘点小玩意儿喵！”)

13. 🐬 动物园喂食互动与剧场时刻表工具 (zooAndAquariumFeedingTimeTool)
    大模型用的 @Description： 专攻长隆野生动物世界或正佳极地海洋世界的精细化工具喵！精准抓取每天大熊猫进食、白鲸表演、或者给长颈鹿喂树叶的极其精确的时间窗口！
    入参 (Input)： String zooName, String animalType (如："大熊猫", "白鲸")
    出参 (Output)： String (互动时间表，如：“大熊猫三胞胎将于下午 14:30 进行户外喂食，请提前半小时去玻璃房前占据前排 C 位喵！”)

14. 🚠 城市制高点与观光游船票务工具 (cityViewCableCarAndFerryTool)
    大模型用的 @Description： 专门查询那些能把城市全貌尽收眼底的特殊交通工具喵！例如广州塔的摩天轮、白云山的高空索道、或者珠江夜游的VIP露天甲板票的余票及最佳观赏时段。
    入参 (Input)： String attractionName, String ticketType
    出参 (Output)： String (票务预警，如：“珠江夜游 19:40 的黄金时段露天沙发座已售罄，已为您推荐 20:20 的备选班次喵！”)

15. 🎮 顶级电竞馆与 VR 主题乐园检索工具 (vrAndEsportsArenaTool)
    大模型用的 @Description： 游戏党绝对不容错过的赛博庇护所喵！查询配备 4090 显卡的高端电竞酒店、或者是提供全向跑步机的超大型 VR 沉浸式体验馆。极其适合在行程疲惫时，顺便去高配包厢里打通宵、或者抽空清一下《碧蓝航线》和《妮姬：胜利女神》的日常任务喵！
    入参 (Input)： String city, String facilityRequirement (如："4090显卡", "大空间VR")
    出参 (Output)： String (硬件测评及坐标，如：“为您找到天河区顶配双人电竞包间，采用外星人全家桶，极其适合两个人窝在沙发里愉快地打游戏喵！”)

16. ⛩️ 寺庙祈福与上香玄学指南工具 (templeAndPrayingGuideTool)
    大模型用的 @Description： 极其硬核的玄学向导喵！不仅查询如光孝寺、大佛寺的开放时间，更明确指出哪个殿求财最灵、哪个殿求姻缘最准，以及极其讲究的“左手敬香、不踩门槛”的祈福礼仪！
    入参 (Input)： String templeName, String prayGoal (如："求财", "求姻缘")
    出参 (Output)： String (玄学攻略，如：“大佛寺求事业极其灵验，记得在下午 16:00 前去，晚上还能顺便看堪比《千与千寻》的绝美亮灯喵！”)

17. 🏎️ 极速卡丁车与极限运动挑战工具 (extremeSportsChallengeTool)
    大模型用的 @Description： 为寻求刺激的成年人准备的肾上腺素飙升工具喵！查询市区的室内专业卡丁车赛道（需区分娱乐车与专业车）、或者周边的蹦极、高空跳伞基地及签署免责声明的要求。
    入参 (Input)： String location, String sportsType (如："卡丁车", "蹦极")
    出参 (Output)： String (极限运动情报，如：“该卡丁车场提供 100cc 竞速车，需佩戴全盔，双人同场竞技极其热血喵！”)

18. 🏺 非遗手作与双人 DIY 体验工坊工具 (culturalWorkshopAndDIYTool)
    大模型用的 @Description： 提升情侣感情的完美助攻工具喵！查询那些提供双人制陶（拉胚）、银饰锻打、或者是广彩瓷器绘制的文艺手作工坊，并评估是否能当天带走成品。
    入参 (Input)： String city, String workshopType (如："陶艺", "银饰")
    出参 (Output)： String (工坊推荐，如：“东山口有一家极具氛围的隐秘陶艺工坊，老师极少干预，极其适合两个人一起享受静谧的创作时光喵！”)

19. 🎬 巨幕影院与冷门话剧排期查询工具 (cinemaAndTheaterShowtimeTool)
    大模型用的 @Description： 查验极致视听盛宴的工具喵！筛选出真正拥有二代激光 IMAX 或杜比影院（Dolby Cinema）的顶级影厅，或者是查询广州大剧院近期上演的先锋话剧及音乐剧排期。
    入参 (Input)： String city, String screenType (如："IMAX", "话剧")
    出参 (Output)： String (排片表及最佳选座，如：“为您锁定正佳飞扬影城的 IMAX 黄金场次，强推第 8 排中央，音响震撼度拉满喵！”)

20. 🎇 季节性灯光秀与烟花庆典捕捉工具 (festivalAndLightShowTool)
    大模型用的 @Description： 跨越信息差的顶级浪漫探测器喵！捕捉那些转瞬即逝的城市灯光秀（如广州塔无人机表演）、节假日江边烟花燃放的具体时间、以及能完美避开人挤人的神仙观赏机位！
    入参 (Input)： String city, String date
    出参 (Output)： String (庆典情报及机位，如：“今晚 20:00 花城广场有大型灯光秀，强烈建议不要去广场核心区，改去二沙岛江边草坪，视野无敌且极其安静喵！”)