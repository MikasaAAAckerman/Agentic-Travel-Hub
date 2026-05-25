# WeatherOutfitTool 生成需求文档

## 🌤️ 天气与穿搭建议工具集

### 1. ☀️ 基础实时气温与穿衣指数工具 (realTimeOutfitTool)

**大模型用的 @Description：** 这是一个根据指定城市的当前实时气温、体感温度，提供最基础穿衣层数和衣物厚度建议的工具喵。用于回答"今天北京冷不冷，该穿几件"的问题。

**入参 (Input)：** String city

**出参 (Output)：** String (包含气温、风力及对应的如"建议穿长袖加薄外套"等基础穿衣指数)

---

### 2. 🕶️ 紫外线防御穿搭工具 (uvIndexProtectionTool)

**大模型用的 @Description：** 专门用于查询特定地区紫外线（UV）指数，并据此推荐防晒衣、墨镜、遮阳伞等物理防晒装备的工具喵。当用户问及"去三亚要带什么防晒装备"时调用。

**入参 (Input)：** String city, String date

**出参 (Output)：** String (紫外线等级，以及对应的防晒装备建议)

---

### 3. 🧅 昼夜温差叠穿策略工具 (tempDifferenceLayeringTool)

**大模型用的 @Description：** 查询目的地当日最高温与最低温的差值，并提供"洋葱式叠穿法"等具体搭配建议的工具喵。用于解决早晚冷、中午热的穿衣尴尬。

**入参 (Input)：** String city, String date

**出参 (Output)：** String (温差数值，以及早中晚的衣物增减建议)

---

### 4. ☔ 降水概率与雨具鞋履推荐工具 (rainGearSuggestionTool)

**大模型用的 @Description：** 查询目的地的降水概率和降水量，并精确推荐是否需要携带雨伞、雨衣，以及是否应该穿着防水鞋/防滑鞋的工具喵。

**入参 (Input)：** String city, String date

**出参 (Output)：** String (降水概率，以及针对性的防水衣物和鞋类建议)

---

### 5. 🧳 商旅多日打包清单生成工具 (businessTravelPackingTool)

**大模型用的 @Description：** 这是一个极其强大的工具！根据用户出差或旅行的起始日期、结束日期和目的地，综合那几天的天气趋势，生成一份包含内衣、外套、正装数量的极简打包清单喵。

**入参 (Input)：** String city, String startDate, String endDate, String purpose

**出参 (Output)：** String (按天数和天气汇总的行李箱衣物装箱清单)

---

### 6. ⛈️ 极端灾害天气防御穿搭工具 (extremeWeatherOutfitTool)

**大模型用的 @Description：** 查询目的地是否有台风、暴雪、冰雹或沙尘暴等极端天气预警，并提供极其严肃的防御性穿搭或避险建议喵。当用户计划前往可能发生自然灾害的地区时必须调用！

**入参 (Input)：** String city, String date

**出参 (Output)：** String (预警级别及对应的硬核防护装备，如冲锋衣、防风镜、防滑雪地靴，或直接强烈建议"取消行程待在室内")

---

### 7. 💧 湿度与体感面料推荐工具 (humidityFabricSuggestionTool)

**大模型用的 @Description：** 专门查询当地空气相对湿度，并针对"高温高湿（闷热）"或"低温极燥（易起静电）"的特殊环境，推荐最舒适衣物面料的工具喵。

**入参 (Input)：** String city

**出参 (Output)：** String (比如去广州出差遇到90%湿度的回南天，工具会明确返回"极度闷热潮湿，强烈建议穿着速干、透气的冰丝或棉麻材质，多带备用内衣"；如果是去北方干冷地区，则提示防静电穿搭)

---

### 8. 💨 风力风寒指数穿搭工具 (windChillOutfitTool)

**大模型用的 @Description：** 专门查询当地风速大小和风寒效应，给出防风保暖细节建议的工具喵。当用户问"海边风大不大，穿裙子会不会走光"或"要不要带伞"时极其有用！

**入参 (Input)：** String city, String date

**出参 (Output)：** String (如遇7级大风，返回"风力极大，建议穿抗风夹克、戴防风帽，绝对禁止穿宽松短裙，且打伞会被吹翻，建议穿雨衣")

---

### 9. 🏔️ 户外运动专业气象穿搭工具 (outdoorSportsOutfitTool)

**大模型用的 @Description：** 根据当日天气和紫外线情况，为准备在目的地进行徒步、登山、滑雪等特定户外运动的用户，提供极其专业的运动装备建议喵。

**入参 (Input)：** String city, String sportType

**出参 (Output)：** String (返回专业的"三层穿衣法"指南：排汗层、保暖层、防风外层的具体材质要求及装备清单)

---

### 10. 😷 花粉过敏与空气质量防护工具 (aqiPollenProtectionTool)

**大模型用的 @Description：** 查询目的地的空气质量指数（AQI）或当季花粉浓度，给出健康防护性质的穿搭建议喵。用于保护用户的呼吸道和皮肤。

**入参 (Input)：** String city, String month

**出参 (Output)：** String (AQI数值/花粉指数，以及明确建议是否需要佩戴N95口罩、全包围护目镜，或建议穿长袖长裤以隔离过敏原)

---

### 11. 👕 晾晒指数与换洗频次工具 (laundryDryingIndexTool)

**大模型用的 @Description：** 根据目的地的湿度和天气情况，评估衣物自然晾干的难度，推荐换洗衣物的携带数量。比如用户要去广州出差遇到极其潮湿的回南天，此工具会强烈建议额外携带双倍的内衣和袜子喵！

**入参 (Input)：** String city, int tripDays

**出参 (Output)：** String (晾晒指数，如"极难晾干"，及内衣袜子的额外携带数量建议)

---

### 12. 🕌 宗教与文化着装禁忌工具 (culturalDressCodeTool)

**大模型用的 @Description：** 查询目的地或特定景点是否有严格的宗教或文化着装要求，避免用户因穿着短裤、吊带或未戴头巾而被拒绝进入特定场所喵。

**入参 (Input)：** String city, String attractionName

**出参 (Output)：** String (着装红线，如"必须遮挡肩膀和膝盖，严禁紧身衣物，需准备头巾")

---

### 13. 💦 出汗指数与备用衣物推荐工具 (sweatIndexOutfitTool)

**大模型用的 @Description：** 综合当地气温、湿度和用户的活动强度，预测用户的出汗量，并给出是否需要随身携带备用替换衣物或止汗剂的建议喵。

**入参 (Input)：** String city, String activityLevel

**出参 (Output)：** String (出汗预测及备用吸汗T恤、毛巾的携带建议)

---

### 14. 🦟 蚊虫活跃度与防护穿搭工具 (bugActivityProtectionTool)

**大模型用的 @Description：** 专门查询目的地（尤其是野外、热带或多水域地区）当季的蚊虫活跃指数，提供物理防蚊防虫的穿衣策略喵。

**入参 (Input)：** String city, String environment

**出参 (Output)：** String (蚊虫预警级别及对应的穿搭，如"强烈建议穿着浅色长袖长裤，避免暴露脚踝，并携带强效驱蚊液")

---

### 15. 👔 极端温差下的商务正装生存工具 (businessSuitSurvivalTool)

**大模型用的 @Description：** 专门为需要穿厚重西装或礼服参加会议，但室外温度极高（或极低）的用户，提供"通勤与会场切换"的穿衣与携带策略喵。

**入参 (Input)：** String city, String eventType

**出参 (Output)：** String (例如："室外35度极热，建议通勤穿便装，将西服外套装入防皱衣袋，到达冷气充足的会场后再换上以免汗湿")

---

### 16. 👟 地形与降水联合鞋履推荐工具 (terrainFootwearTool)

**大模型用的 @Description：** 这是一个结合当地降水情况和景点具体地形，提供极其专业的鞋子建议的工具喵。当用户问"下雨天去爬黄山穿什么"或者"去欧洲老城区石板路逛街穿什么"时调用！

**入参 (Input)：** String city, String terrain, String weatherCondition

**出参 (Output)：** String (比如："雨天+山地"绝对推荐防滑登山靴，严禁穿平底鞋；"晴天+欧洲石板路"强烈推荐厚底运动鞋，严禁高跟鞋以免卡跟喵)

---

### 17. 🏠 室内外极端温差对策工具 (indoorClimateOutfitTool)

**大模型用的 @Description：** 专门解决某些地区"室外冻死，室内热死"的穿衣痛点喵！比如冬季去日本北海道或中国东北，室外零下20度，室内暖气零上25度。

**入参 (Input)：** String destinationRegion

**出参 (Output)：** String (强烈建议采用"内薄外极厚"穿法，如里面穿单件长袖T恤，外面直接套极地羽绒服，方便进入室内秒脱，绝对禁止穿厚重的高领毛衣喵)

---

### 18. 🎒 极端天气行李空间压缩建议工具 (luggageSpaceOutfitTool)

**大模型用的 @Description：** 根据目的地的天气预报判断用户是否需要携带极其臃肿的衣物（如长款羽绒服、大毛毯），并提供打包耗材和携带方案建议喵。

**入参 (Input)：** String city, String date

**出参 (Output)：** String (如果预测需要重型保暖衣物，明确建议用户提前购买"免抽气真空压缩袋"，或者建议把最厚的外套直接穿在身上登机以节省行李额度喵)

---

### 19. 📸 季节景色与穿搭色彩反差工具 (scenicColorOutfitTool)

**大模型用的 @Description：** 这是一个注重"情绪价值"的高级工具！结合目的地的当前季节景色（如：秋季满山红叶、冬季白雪皑皑、夏季蔚蓝海边），推荐最容易拍出大片的衣物颜色搭配喵。

**入参 (Input)：** String city, String seasonOrScenery

**出参 (Output)：** String (比如雪景强烈推荐大红色或亮黄色风衣；沙漠推荐纯白色或波西米亚长裙。避免与背景顺色导致拍照隐身喵)

---

### 20. 🌙 夜间降温与睡眠穿搭建议工具 (nightwearSuggestionTool)

**大模型用的 @Description：** 极具人文关怀的工具！查询目的地夜间的最低温度以及当地普遍的供暖情况，推荐应该带什么厚度的睡衣喵。

**入参 (Input)：** String city, String month

**出参 (Output)：** String (比如冬季去中国南方出差，工具会指出"夜间 5 度且无集中供暖，体感极冷，强烈建议携带加厚法兰绒睡衣"；去北方则建议带薄款棉质睡衣喵)

---
