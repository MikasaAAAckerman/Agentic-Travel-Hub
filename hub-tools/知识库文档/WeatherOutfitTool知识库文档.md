# 旅游大模型原子工具名录

功能：这是一个根据指定城市的当前实时气温、体感温度，提供最基础穿衣层数和衣物厚度建议的工具喵。用于回答"今天北京冷不冷，该穿几件"的问题。
[TOOL_BEAN_NAME: realTimeOutfitTool]

功能：专门用于查询特定地区紫外线（UV）指数，并据此推荐防晒衣、墨镜、遮阳伞等物理防晒装备的工具喵。当用户问及"去三亚要带什么防晒装备"时调用。
[TOOL_BEAN_NAME: uvIndexProtectionTool]

功能：查询目的地当日最高温与最低温的差值，并提供"洋葱式叠穿法"等具体搭配建议的工具喵。用于解决早晚冷、中午热的穿衣尴尬。
[TOOL_BEAN_NAME: tempDifferenceLayeringTool]

功能：查询目的地的降水概率和降水量，并精确推荐是否需要携带雨伞、雨衣，以及是否应该穿着防水鞋/防滑鞋的工具喵。
[TOOL_BEAN_NAME: rainGearSuggestionTool]

功能：这是一个极其强大的工具！根据用户出差或旅行的起始日期、结束日期和目的地，综合那几天的天气趋势，生成一份包含内衣、外套、正装数量的极简打包清单喵。
[TOOL_BEAN_NAME: businessTravelPackingTool]

功能：查询目的地是否有台风、暴雪、冰雹或沙尘暴等极端天气预警，并提供极其严肃的防御性穿搭或避险建议喵。当用户计划前往可能发生自然灾害的地区时必须调用！
[TOOL_BEAN_NAME: extremeWeatherOutfitTool]

功能：专门查询当地空气相对湿度，并针对"高温高湿（闷热）"或"低温极燥（易起静电）"的特殊环境，推荐最舒适衣物面料的工具喵。
[TOOL_BEAN_NAME: humidityFabricSuggestionTool]

功能：专门查询当地风速大小和风寒效应，给出防风保暖细节建议的工具喵。当用户问"海边风大不大，穿裙子会不会走光"或"要不要带伞"时极其有用！
[TOOL_BEAN_NAME: windChillOutfitTool]

功能：根据当日天气和紫外线情况，为准备在目的地进行徒步、登山、滑雪等特定户外运动的用户，提供极其专业的运动装备建议喵。
[TOOL_BEAN_NAME: outdoorSportsOutfitTool]

功能：查询目的地的空气质量指数（AQI）或当季花粉浓度，给出健康防护性质的穿搭建议喵。用于保护用户的呼吸道和皮肤。
[TOOL_BEAN_NAME: aqiPollenProtectionTool]

功能：根据目的地的湿度和天气情况，评估衣物自然晾干的难度，推荐换洗衣物的携带数量。比如用户要去广州出差遇到极其潮湿的回南天，此工具会强烈建议额外携带双倍的内衣和袜子喵！
[TOOL_BEAN_NAME: laundryDryingIndexTool]

功能：查询目的地或特定景点是否有严格的宗教或文化着装要求，避免用户因穿着短裤、吊带或未戴头巾而被拒绝进入特定场所喵。
[TOOL_BEAN_NAME: culturalDressCodeTool]

功能：综合当地气温、湿度和用户的活动强度，预测用户的出汗量，并给出是否需要随身携带备用替换衣物或止汗剂的建议喵。
[TOOL_BEAN_NAME: sweatIndexOutfitTool]

功能：专门查询目的地（尤其是野外、热带或多水域地区）当季的蚊虫活跃指数，提供物理防蚊防虫的穿衣策略喵。
[TOOL_BEAN_NAME: bugActivityProtectionTool]

功能：专门为需要穿厚重西装或礼服参加会议，但室外温度极高（或极低）的用户，提供"通勤与会场切换"的穿衣与携带策略喵。
[TOOL_BEAN_NAME: businessSuitSurvivalTool]

功能：这是一个结合当地降水情况和景点具体地形，提供极其专业的鞋子建议的工具喵。当用户问"下雨天去爬黄山穿什么"或者"去欧洲老城区石板路逛街穿什么"时调用！
[TOOL_BEAN_NAME: terrainFootwearTool]

功能：专门解决某些地区"室外冻死，室内热死"的穿衣痛点喵！比如冬季去日本北海道或中国东北，室外零下20度，室内暖气零上25度。
[TOOL_BEAN_NAME: indoorClimateOutfitTool]

功能：根据目的地的天气预报判断用户是否需要携带极其臃肿的衣物（如长款羽绒服、大毛毯），并提供打包耗材和携带方案建议喵。
[TOOL_BEAN_NAME: luggageSpaceOutfitTool]

功能：这是一个注重"情绪价值"的高级工具！结合目的地的当前季节景色（如：秋季满山红叶、冬季白雪皑皑、夏季蔚蓝海边），推荐最容易拍出大片的衣物颜色搭配喵。
[TOOL_BEAN_NAME: scenicColorOutfitTool]

功能：极具人文关怀的工具！查询目的地夜间的最低温度以及当地普遍的供暖情况，推荐应该带什么厚度的睡衣喵。
[TOOL_BEAN_NAME: nightwearSuggestionTool]
