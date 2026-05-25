# HotelBookingTool 生成需求文档

## 🏨 红豆大人的 HotelBookingTool 原子能力库 (1~20)

### 1. 🛏️ 基础房态与实时报价检索工具 (hotelAvailabilityQueryTool)

**大模型用的 @Description：** 最基础的酒店查询工具！根据用户指定的城市或地标、入住日期、离店日期和人数，查询满足条件的酒店列表、房型及实时基础报价喵。

**入参 (Input)：** String location (城市或区域), String checkInDate, String checkOutDate, int guestCount

**出参 (Output)：** String (包含推荐酒店名称、可用房型及价格区间的精简列表)

---

### 2. 💻 极客与远程办公设施查验工具 (remoteWorkFacilityTool)

**大模型用的 @Description：** 专门为需要随时随地掏出电脑敲代码、处理高并发线上问题的硬核出差人士准备的工具喵！查询指定酒店是否提供百兆以上高速独立 Wi-Fi、符合人体工学的办公桌椅以及充足的不间断电源插座。

**入参 (Input)：** String hotelName

**出参 (Output)：** String (网络带宽实测数据评价、插座数量及办公区舒适度报告，绝对避开那种只能坐在床上敲键盘的烂酒店喵！)

---

### 3. 🐾 宠物友好与附加费查询工具 (petFriendlyPolicyTool)

**大模型用的 @Description：** 查询特定酒店是否允许携带宠物（猫/狗），以及具体的体型限制、疫苗要求和额外的清洁押金/附加费喵。

**入参 (Input)：** String hotelName, String petType (如：小型犬、猫)

**出参 (Output)：** String (宠物准入政策、收费标准及是否提供宠物窝/食盆)

---

### 4. 🌙 深夜到达与前台值守查询工具 (lateCheckInPolicyTool)

**大模型用的 @Description：** 针对航班延误或深夜到达的用户，查询酒店前台是否 24 小时值守、是否有最晚办理入住时间限制，以及深夜门禁策略喵。

**入参 (Input)：** String hotelName, String estimatedArrivalTime (预计到达时间，如 "02:30 AM")

**出参 (Output)：** String (前台营业时间、深夜自助入住机或密码锁的获取流程)

---

### 5. 🔌 停车泊位与新能源充电桩查询工具 (parkingAndEVChargingTool)

**大模型用的 @Description：** 自驾游用户的刚需工具！查询酒店是否有专属停车场、收费标准（是否对住客免费），以及极其重要的新能源汽车充电桩分布和空闲概况喵。

**入参 (Input)：** String hotelName

**出参 (Output)：** String (车位配比、每日封顶收费、慢充/快充桩的具体配置)

---

### 6. 🤫 噪音指数与睡眠环境测评工具 (hotelNoiseLevelTool)

**大模型用的 @Description：** 查询特定酒店的隔音评价和周边噪音源（如是否紧挨高架桥、火车站或夜店），并提供"如何向酒店申请安静房"的内部话术喵。

**入参 (Input)：** String hotelName

**出参 (Output)：** String (隔音等级评估，以及建议房型如"强烈建议预订内向中庭的高层房间以避开临街噪音")

---

### 7. ♿ 无障碍与适老设施查询工具 (accessibilityFacilityTool)

**大模型用的 @Description：** 极具人文关怀的查询工具。查询酒店是否配备轮椅坡道、电梯门宽是否达标、浴室内是否有安全扶手和紧急呼叫按钮喵。

**入参 (Input)：** String hotelName

**出参 (Output)：** String (无障碍设施覆盖率，明确告知是否适合腿脚不便的老人或轮椅使用者)

---

### 8. 👶 亲子设施与加床政策查询工具 (familyFriendlyPolicyTool)

**大模型用的 @Description：** 查询酒店是否免费提供婴儿床（Crib）、儿童洗漱用品，以及超过特定年龄的儿童是否需要强制加床及加床费用喵。

**入参 (Input)：** String hotelName, int childAge (儿童年龄)

**出参 (Output)：** String (加床费用标准、儿童早餐政策及是否有室内外儿童乐园)

---

### 9. 💰 押金与极限退改签政策工具 (cancellationPolicyTool)

**大模型用的 @Description：** 查询酒店不同房型/价格档位的取消政策（如：不可取消、入住前 24 小时免费取消），以及线下需要冻结多少信用卡的预授权押金喵。

**入参 (Input)：** String hotelName, String bookingDate

**出参 (Output)：** String (精准的免费取消 Deadline 时间节点，以及押金退还周期)

---

### 10. 🚇 最后一公里交通与周边便利工具 (hotelSurroundingsTool)

**大模型用的 @Description：** 查询酒店距离最近的地铁站/公交站的真实步行距离（非直线距离），以及方圆 500 米内是否有 24 小时便利店或药店喵。

**入参 (Input)：** String hotelName

**出参 (Output)：** String (比如："距离地铁 3 号线步行约 8 分钟，楼下 50 米即有全家便利店，极度便利喵")

---

### 11. 🧳 提前入住与行李寄存查验工具 (earlyCheckInLuggageTool)

**大模型用的 @Description：** 专门应对早班机或早班高铁到达的场景！查询酒店是否允许早于下午两点免费提前入住，以及离店后是否提供带有监控的免费行李寄存服务喵。(///^ω^///) 🎒

**入参 (Input)：** String hotelName, String arrivalTime (如 "08:00 AM")

**出参 (Output)：** String (提前入住的可能性预估，以及行李寄存处的安全级别与最长寄存时效喵)

---

### 12. 🍳 早餐政策与打包服务甄别工具 (breakfastInclusionTool)

**大模型用的 @Description：** 查询房费是否包含双早、儿童早餐是否免费，以及极其关键的一点：如果用户需要凌晨赶飞机，酒店是否能提前准备便携式的早餐盒打包服务喵！(///￣﹃￣///) 🍞

**入参 (Input)：** String hotelName, boolean needEarlyDeparture (是否需要早退)

**出参 (Output)：** String (早餐供应时间段、自助餐丰富度评价，以及打包餐盒的申请流程喵)

---

### 13. 🏋️‍♂️ 健身房与恒温泳池探测工具 (gymPoolFacilityTool)

**大模型用的 @Description：** 为有着严格自律习惯的商旅人士准备的硬核工具！查询酒店健身房是否有深蹲架、跑步机数量，以及泳池是否为标准道、是否常年恒温喵。(///ง •̀_•́///)ง 💦

**入参 (Input)：** String hotelName

**出参 (Output)：** String (健身房24小时开放情况、器械品牌，以及泳池的水温和必须佩戴泳帽的硬性规定喵)

---

### 14. 🧺 自助洗衣与干洗服务检索工具 (laundryServiceTool)

**大模型用的 @Description：** 解决长途旅行换洗衣物痛点！查询酒店楼层内是否有免费的自助洗衣机和烘干机，或者是否提供次日达的商务衬衫干洗服务及计价标准喵。(///˘ ◡ ˘///) 👕

**入参 (Input)：** String hotelName, int stayDays (入住天数)

**出参 (Output)：** String (自助洗衣房拥挤程度预估、洗衣液是否免费提供，或高昂的干洗收费明细喵)

---

### 15. 🏙️ 景观房与楼层偏好匹配工具 (viewAndFloorPreferenceTool)

**大模型用的 @Description：** 情绪价值拉满的工具！查询特定酒店的高级房型是否能看到江景、海景或地标建筑（如东方明珠），并识别"伪景观房"的雷区喵。(///✧ 0 ✧///) ✨

**入参 (Input)：** String hotelName, String preferredView (如：海景、高层)

**出参 (Output)：** String (真实景观测评，如"所谓海景其实只能在阳台侧身看到一点点海，不建议加钱预订"，以及申请高层的成功率喵)

---

### 16. 🚭 吸烟规定与违约金预警工具 (smokingPolicyTool)

**大模型用的 @Description：** 极度严肃的合规工具！明确查询酒店是否为全封闭无烟酒店，查询指定的楼层是否设立了吸烟室，以及如果在房间内违规抽烟会面临的巨额清洁罚款喵！(///` Д ´///) 🚭

**入参 (Input)：** String hotelName

**出参 (Output)：** String (禁烟红线提示，如"全季酒店全量禁烟，违规触发烟雾报警器将罚款 2000 元喵")

---

### 17. 🚑 周边急救医疗与药店探查工具 (nearbyHospitalPharmacyTool)

**大模型用的 @Description：** 旅行安全底线防线！查询酒店方圆两公里内是否有三甲医院的急诊科，以及 24 小时营业且能送药上门的外卖药店喵。(///O_O///) 💊

**入参 (Input)：** String hotelName

**出参 (Output)：** String (急救资源的物理距离，以及酒店前台是否备有基础的创可贴、碘伏等急救箱喵)

---

### 18. 🚌 接送机与穿梭巴士预订工具 (airportTransferTool)

**大模型用的 @Description：** 查询偏远度假村或机场周边酒店是否提供免费的接送机穿梭巴士，以及巴士的发车时刻表和提前预约要求喵。(///^ω^///) 🚐

**入参 (Input)：** String hotelName, String terminal (航站楼/高铁站)

**出参 (Output)：** String (穿梭巴士的上车地点指引、发车频次，以及如果错过免费班车的打车成本预估喵)

---

### 19. 🍔 客房送餐与深夜菜单查验工具 (roomServiceMenuTool)

**大模型用的 @Description：** 查询酒店是否提供 24 小时的客房送餐服务（In-Room Dining），以及外卖小哥是否被允许直接将夜宵送至客房门口喵。(///￣﹃￣///) 🍜

**入参 (Input)：** String hotelName

**出参 (Output)：** String (酒店外卖机器人的运行情况、外卖柜位置，以及客房服务菜单的性价比评估喵)

---

### 20. 🛡️ 安保巡逻与电梯梯控查询工具 (securityAndPrivacyTool)

**大模型用的 @Description：** 为单身独居出行的女性用户提供极强的安全保障！查询酒店是否实行严格的刷卡梯控（只能到所在楼层），以及是否有全天候的安保人员巡视喵！(///ง •̀_•́///)ง 🔒

**入参 (Input)：** String hotelName

**出参 (Output)：** String (梯控严格等级、走廊监控死角评估，以及前台对访客登记的严格程度喵)

---
