# 高德地图API集成设计文档

## 一、集成概述

本次集成将高德地图Web服务API全面接入Agentic-Travel-Hub项目，为旅行规划提供真实的地理位置数据支持。

## 二、核心组件

### 1. MapApiClient（HTTP客户端层）
**位置**: `hub-tools/src/main/java/com/travel/hubtools/client/MapApiClient.java`

**职责**: 
- 封装高德地图Web服务API的HTTP调用
- 统一处理请求参数拼接和响应解析
- 提供地理编码、路径规划、POI搜索等基础能力

**已实现的API接口**:
- ✅ 地理编码 (geocode) - 地址转经纬度
- ✅ 逆地理编码 (reverseGeocode) - 经纬度转地址
- ✅ 驾车路线规划 (drivingRoutePlanning)
- ✅ 步行路线规划 (walkingRoutePlanning)
- ✅ 骑行路线规划 (cyclingRoutePlanning)
- ✅ 公交路线规划 (publicTransitRoutePlanning)
- ✅ POI关键字搜索 (searchPOI)
- ✅ POI周边搜索 (searchPOIAround)
- ✅ 实时路况查询 (trafficStatus)
- ✅ 天气查询 (weatherInfo)

### 2. RouteQueryTool（路线规划工具层）
**位置**: `hub-tools/src/main/java/com/travel/hubtools/tool/travel/RouteQueryTool.java`

**职责**:
- 将大模型的意图转换为具体的高德API调用
- 处理地址到坐标的自动转换
- 格式化返回结果为大模型友好的自然语言

**已对接的工具方法**:
- ✅ driveRoutePlanningTool - 驾车路线规划
- ✅ publicTransitRoutingTool - 公共交通换乘
- ✅ walkingAndCyclingRouteTool - 步行/骑行路线

### 3. AmapPOISearchTool（地点搜索工具层）
**位置**: `hub-tools/src/main/java/com/travel/hubtools/tool/travel/AmapPOISearchTool.java`

**职责**:
- 提供地点搜索功能
- 支持关键字搜索和周边搜索
- 集成天气查询功能

**已对接的工具方法**:
- ✅ poiKeywordSearchTool - 地点关键字搜索
- ✅ poiAroundSearchTool - 周边地点搜索
- ✅ weatherQueryTool - 天气查询

## 三、技术实现细节

### 1. API Key管理
**配置位置**: `hub-starter/src/main/resources/application-dev.yml`

```yaml
amap:
  api:
    key: xxxxxxxxxxxxxxxxxxx
```

**注入方式**: 通过@Value注解在MapApiClient中注入

### 2. 坐标系统
- 高德地图使用 **GCJ-02坐标系**（火星坐标系）
- 所有经纬度格式统一为：**经度,纬度**（如：116.480881,39.989410）

### 3. 错误处理机制
- 所有API调用均采用try-catch包裹
- 失败时返回友好的错误提示（带"喵~"语气词）
- 详细错误日志记录到log文件

### 4. 策略映射
**驾车策略映射表**:
| 用户描述 | 高德策略代码 | 说明 |
|---------|------------|------|
| 速度优先 | 0 | 只返回一条最快路线 |
| 费用优先/少收费 | 1 | 不走收费路段 |
| 躲避拥堵 | 33 | 避开拥堵路段 |
| 高速优先 | 34 | 优先走高速 |
| 不走高速 | 35 | 完全避开高速 |
| 大路优先 | 37 | 优先走主干道 |
| 默认/空 | 32 | 高德推荐策略 |

**公交策略映射表**:
| 用户描述 | 高德策略代码 | 说明 |
|---------|------------|------|
| 最快捷/默认 | 0 | 时间最短 |
| 最经济/省钱 | 1 | 费用最少 |
| 少换乘 | 2 | 换乘次数最少 |
| 少步行 | 3 | 步行距离最短 |
| 不乘地铁 | 4 | 避免地铁 |

## 四、数据流示例

### 场景1：用户请求"从北京故宫到天坛公园怎么走"

```
用户输入 
  ↓
IntentRecognitionRouter (识别为PLAN)
  ↓
DualCoreReactEngine (Planner分析需要路线规划)
  ↓
QwenWorkerService (选择driveRoutePlanningTool)
  ↓
RouteQueryTool.driveRoutePlanningTool()
  ↓
MapApiClient.geocode("北京故宫") → 116.397499,39.908722
MapApiClient.geocode("天坛公园") → 116.410000,39.880000
  ↓
MapApiClient.drivingRoutePlanning(origin, destination, 32)
  ↓
高德API返回JSON → 解析距离、耗时、费用
  ↓
格式化输出："推荐路线喵！全程 8.50 公里，预计耗时 0.50 小时（约 30 分钟）喵~"
  ↓
返回给Planner → 继续下一步决策或FINISH
```

### 场景2：用户请求"帮我找附近的肯德基"

```
用户输入
  ↓
IntentRecognitionRouter (识别为PLAN)
  ↓
DualCoreReactEngine (Planner分析需要POI搜索)
  ↓
QwenWorkerService (选择poiAroundSearchTool)
  ↓
AmapPOISearchTool.poiAroundSearchTool()
  ↓
MapApiClient.geocode(当前位置) → 获取坐标
  ↓
MapApiClient.searchPOIAround("肯德基", coord, 1000)
  ↓
高德API返回JSON → 解析POI列表
  ↓
格式化输出："附近找到 5 个相关地点喵！1. 【肯德基(三里屯店)】距离：350 米..."
  ↓
返回给Planner
```

## 五、API调用限制与优化

### 1. 配额限制
- 个人开发者认证：每日30万次调用
- QPS限制：5次/秒
- 超出限制会返回错误码

### 2. 优化建议
- ✅ 已实现：地址缓存（可进一步优化为Redis缓存）
- ✅ 已实现：合理的超时控制（通过ResilienceConfig）
- ⚠️ 待优化：高频POI搜索结果缓存
- ⚠️ 待优化：批量坐标转换优化

## 六、测试验证

### 单元测试建议
```java
@Test
public void testDriveRoutePlanning() {
    String result = routeQueryTool.driveRoutePlanningTool(
        "北京故宫", 
        "天坛公园", 
        "躲避拥堵"
    );
    assertNotNull(result);
    assertTrue(result.contains("公里"));
    assertTrue(result.contains("喵"));
}
```

### 集成测试场景
1. 地址解析准确性测试
2. 不同策略路线对比测试
3. POI搜索结果相关性测试
4. 异常情况处理测试（无效地址、网络异常等）

## 七、后续扩展计划

### 短期优化（1-2周）
- [ ] 添加坐标缓存机制（减少重复geocode调用）
- [ ] 完善城市adcode映射表（支持更多城市）
- [ ] 增加路线详情展示（分段导航指令）

### 中期扩展（1个月）
- [ ] 集成高德地图静态图API（生成路线示意图）
- [ ] 支持多途经点路线规划
- [ ] 添加货车路线规划（考虑限高限重）

### 长期愿景（3个月）
- [ ] 实时交通事件推送（事故、施工）
- [ ] AR导航集成
- [ ] 离线地图支持

## 八、注意事项

### 1. 坐标系问题
⚠️ **重要**: 高德使用GCJ-02坐标系，与百度BD-09、GPS WGS-84不同
- 如需与其他地图混用，必须进行坐标转换
- 本项目暂不涉及跨平台坐标转换

### 2. API Key安全
- ✅ 已配置在application-dev.yml（开发环境）
- ⚠️ 生产环境建议使用环境变量或配置中心
- ⚠️ 不要将Key提交到公共代码仓库

### 3. 商业化授权
- 非商业用途：免费使用
- 商业用途：需向高德申请商用授权
- 详见：https://lbs.amap.com/agreement/

## 九、参考文档

- 高德地图开放平台：https://lbs.amap.com/
- Web服务API文档：https://lbs.amap.com/api/webservice/guide/api/
- POI分类编码表：https://lbs.amap.com/api/webservice/download
- 城市编码表：https://lbs.amap.com/api/webservice/download

---

**文档版本**: v1.0  
**更新时间**: 2026-05-08  
**作者**: Agentic-Travel-Hub Team
