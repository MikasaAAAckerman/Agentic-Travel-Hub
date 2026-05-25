package com.travel.hubtools.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 专门对接高德地图的 HTTP Client
 * 提供地理编码、路径规划、POI搜索等核心功能
 */
@Slf4j
@Component
public class MapApiClient {

    private static Map<String, String> cityCodeMap = new HashMap<>();

    @Value("${spring.ai.mcp.client.stdio.connections.amap-maps-server.env.AMAP_MAPS_API_KEY}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // 高德地图Web服务API基础URL
    private static final String AMAP_BASE_URL = "https://restapi.amap.com";

    /**
     * 地理编码：将结构化地址转换为经纬度坐标
     *
     * @param address 结构化地址（如：北京市朝阳区阜通东大街6号）
     * @param city    查询城市（可选，如：北京）
     * @return 经纬度坐标字符串（格式：经度,纬度）
     */
    public String geocode(String address, String city) {
        log.info("[MapApiClient] 开始地理编码 | address={}, city={}", address, city);
        if (StringUtils.isEmpty(city)) {
            return "城市为null，无法继续查询";
        }
        if (cityCodeMap.get(city) != null) {
            log.info(" city -> {}，命中缓存 ", city);
            return cityCodeMap.get(city);
        }
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v3/geocode/geo")
                    .queryParam("key", apiKey)
                    .queryParam("address", address)
                    .queryParam("city", city)
                    .queryParam("output", "JSON")
                    .encode() // 💥 绝杀第一步：强制开启 UTF-8 的 URL 编码！把中文变成 %E5...
                    .build()
                    .toUri(); // 💥 绝杀第二步：生成真正的 java.net.URI 对象，而不是 String！

            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                JSONArray geocodes = result.getJSONArray("geocodes");
                if (geocodes != null && !geocodes.isEmpty()) {
                    String location = geocodes.getJSONObject(0).getString("location");
                    log.info("[MapApiClient] 地理编码成功 | location={}", location);
                    log.info("[MapApiClient] 返回结果: {}", JSON.toJSONString(result));
                    cityCodeMap.put(city, geocodes.getJSONObject(0).getString("citycode"));
                    return location;
                }
            }

            log.warn("[MapApiClient] 地理编码失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] 地理编码异常", e);
            return null;
        }
    }

    /**
     * 逆地理编码：将经纬度转换为详细地址
     *
     * @param location 经纬度坐标（格式：经度,纬度）
     * @return 详细地址信息
     */
    public String reverseGeocode(String location) {
        log.info("[MapApiClient] 开始逆地理编码 | location={}", location);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v3/geocode/regeo")
                    .queryParam("key", apiKey)
                    .queryParam("location", location)
                    .queryParam("output", "JSON")
                    .queryParam("extensions", "base")
                    .encode()
                    .build()
                    .toUri();

            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                JSONObject regeocode = result.getJSONObject("regeocode");
                String formattedAddress = regeocode.getString("formatted_address");
                log.info("[MapApiClient] 逆地理编码成功 | address={}", formattedAddress);
                log.info("[MapApiClient] 返回结果: {}", JSON.toJSONString(result));
                return formattedAddress;
            }

            log.warn("[MapApiClient] 逆地理编码失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] 逆地理编码异常", e);
            return null;
        }
    }

    /**
     * 驾车路线规划
     *
     * @param origin      起点经纬度（格式：经度,纬度）
     * @param destination 终点经纬度（格式：经度,纬度）
     * @param strategy    策略（0-速度优先, 1-费用优先, 32-默认推荐, 33-躲避拥堵, 34-高速优先, 35-不走高速）
     * @return 路线规划结果JSON对象
     */
    public JSONObject drivingRoutePlanning(String origin, String destination, Integer strategy) {
        log.info("[MapApiClient] 开始驾车路线规划 | origin={}, destination={}, strategy={}",
                origin, destination, strategy);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v5/direction/driving")
                    .queryParam("key", apiKey)
                    .queryParam("origin", origin)
                    .queryParam("destination", destination)
                    .queryParam("strategy", strategy != null ? strategy : 32)
                    .queryParam("output", "json")
                    .encode()
                    .build()
                    .toUri();

            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                log.info("[MapApiClient] 驾车路线规划成功 -> {}", JSON.toJSONString(result));
                log.info("[MapApiClient] 返回结果: {}", JSON.toJSONString(result));
                return result;
            }

            log.warn("[MapApiClient] 驾车路线规划失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] 驾车路线规划异常", e);
            return null;
        }
    }

    /**
     * 步行路线规划
     *
     * @param origin      起点经纬度（格式：经度,纬度）
     * @param destination 终点经纬度（格式：经度,纬度）
     * @return 路线规划结果JSON对象
     */
    public JSONObject walkingRoutePlanning(String origin, String destination) {
        log.info("[MapApiClient] 开始步行路线规划 | origin={}, destination={}", origin, destination);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v5/direction/walking")
                    .queryParam("key", apiKey)
                    .queryParam("origin", origin)
                    .queryParam("destination", destination)
                    .queryParam("output", "json")
                    .encode()
                    .build()
                    .toUri();

            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                log.info("[MapApiClient] 步行路线规划成功");
                log.info("[MapApiClient] 返回结果: {}", JSON.toJSONString(result));
                return result;
            }

            log.warn("[MapApiClient] 步行路线规划失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] 步行路线规划异常", e);
            return null;
        }
    }

    /**
     * 骑行路线规划
     *
     * @param origin      起点经纬度（格式：经度,纬度）
     * @param destination 终点经纬度（格式：经度,纬度）
     * @return 路线规划结果JSON对象
     */
    public JSONObject cyclingRoutePlanning(String origin, String destination) {
        log.info("[MapApiClient] 开始骑行路线规划 | origin={}, destination={}", origin, destination);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v5/direction/bicycling")
                    .queryParam("key", apiKey)
                    .queryParam("origin", origin)
                    .queryParam("destination", destination)
                    .queryParam("output", "json")
                    .encode()
                    .build()
                    .toUri();

            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                log.info("[MapApiClient] 骑行路线规划成功");
                log.info("[MapApiClient] 返回结果: {}", JSON.toJSONString(result));
                return result;
            }

            log.warn("[MapApiClient] 骑行路线规划失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] 骑行路线规划异常", e);
            return null;
        }
    }

    /**
     * 公交路线规划
     *
     * @param origin      起点经纬度（格式：经度,纬度）
     * @param destination 终点经纬度（格式：经度,纬度）
     * @param city        城市名称或adcode
     * @param strategy    策略（0-最快捷, 1-最经济, 2-最少换乘, 3-最少步行, 4-不乘地铁）
     * @return 路线规划结果JSON对象
     */
    public JSONObject publicTransitRoutePlanning(String origin, String destination, String city, Integer strategy) {
        log.info("[MapApiClient] 开始公交路线规划 | origin={}, destination={}, city={}, strategy={}",
                origin, destination, city, strategy);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v5/direction/transit/integrated")
                    .queryParam("key", apiKey)
                    .queryParam("origin", origin)
                    .queryParam("destination", destination)
                    .queryParam("city1", cityCodeMap.get(city))
                    .queryParam("city2", cityCodeMap.get(city))
                    .queryParam("strategy", strategy != null ? strategy : 0)
                    .queryParam("output", "json")
                    .encode()
                    .build()
                    .toUri();

            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                log.info("[MapApiClient] 公交路线规划成功");
                log.info("[MapApiClient] 返回结果: {}", JSON.toJSONString(result));
                return result;
            }

            log.warn("[MapApiClient] 公交路线规划失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] 公交路线规划异常", e);
            return null;
        }
    }

    /**
     * POI关键字搜索
     *
     * @param keywords 搜索关键词（如：肯德基、朝阳公园）
     * @param city     查询城市（可选）
     * @param types    POI类型（可选，如：餐饮服务|住宿服务）
     * @param offset   每页记录数（建议不超过25）
     * @param page     当前页数
     * @return POI搜索结果JSON对象
     */
    public JSONObject searchPOI(String keywords, String city, String types, Integer offset, Integer page) {
        log.info("[MapApiClient] 开始POI搜索 | keywords={}, city={}, types={}", keywords, city, types);

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v3/place/text")
                    .queryParam("key", apiKey)
                    .queryParam("keywords", keywords)
                    .queryParam("offset", offset != null ? offset : 20)
                    .queryParam("page", page != null ? page : 1)
                    .queryParam("output", "json")
                    .queryParam("extensions", "all");

            if (city != null && !city.isEmpty()) {
                builder.queryParam("city", city);
            }
            if (types != null && !types.isEmpty()) {
                builder.queryParam("types", types);
            }

            URI uri = builder.encode().build().toUri();
            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                log.info("[MapApiClient] POI搜索成功 | count={}", result.getString("count"));
                log.info("[MapApiClient] 返回结果: {}", JSON.toJSONString(result));
                return result;
            }

            log.warn("[MapApiClient] POI搜索失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] POI搜索异常", e);
            return null;
        }
    }

    /**
     * POI周边搜索
     *
     * @param keywords 搜索关键词
     * @param location 中心点经纬度（格式：经度,纬度）
     * @param radius   搜索半径（米，最大3000）
     * @param types    POI类型
     * @return POI搜索结果JSON对象
     */
    public JSONObject searchPOIAround(String keywords, String location, Integer radius, String types) {
        log.info("[MapApiClient] 开始POI周边搜索 | keywords={}, location={}, radius={}",
                keywords, location, radius);

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v3/place/around")
                    .queryParam("key", apiKey)
                    .queryParam("keywords", keywords)
                    .queryParam("location", location)
                    .queryParam("radius", radius != null ? radius : 1000)
                    .queryParam("output", "json")
                    .queryParam("extensions", "all");

            if (types != null && !types.isEmpty()) {
                builder.queryParam("types", types);
            }

            URI uri = builder.encode().build().toUri();
            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                log.info("[MapApiClient] POI周边搜索成功 | count={}", result.getString("count"));
                log.info("[MapApiClient] 返回结果: {}", JSON.toJSONString(result));
                return result;
            }

            log.warn("[MapApiClient] POI周边搜索失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] POI周边搜索异常", e);
            return null;
        }
    }

    /**
     * 实时路况查询
     *
     * @param city 城市adcode
     * @return 路况信息JSON对象
     */
    public JSONObject trafficStatus(String city) {
        log.info("[MapApiClient] 开始实时路况查询 | city={}", city);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v3/traffic/status/city")
                    .queryParam("key", apiKey)
                    .queryParam("city", city)
                    .queryParam("output", "json")
                    .encode()
                    .build()
                    .toUri();

            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                log.info("[MapApiClient] 实时路况查询成功");
                log.info("[MapApiClient] 返回结果: {}", JSON.toJSONString(result));
                return result;
            }

            log.warn("[MapApiClient] 实时路况查询失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] 实时路况查询异常", e);
            return null;
        }
    }

    /**
     * 天气查询
     *
     * @param city 城市adcode
     * @return 天气信息JSON对象
     */
    public JSONObject weatherInfo(String city) {
        log.info("[MapApiClient] 开始天气查询 | city={}", city);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v3/weather/weatherInfo")
                    .queryParam("key", apiKey)
                    .queryParam("city", city)
                    .queryParam("output", "json")
                    .queryParam("extensions", "all")
                    .encode()
                    .build()
                    .toUri();

            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                log.info("[MapApiClient] 天气查询成功");
                log.info("[MapApiClient] 返回结果: {}", JSON.toJSONString(result));
                return result;
            }

            log.warn("[MapApiClient] 天气查询失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] 天气查询异常", e);
            return null;
        }
    }

    /**
     * POI详情搜索
     *
     * @param poiId POI ID（从searchPOI结果中获取）
     * @return POI详情JSON对象
     */
    public JSONObject searchPOIDetail(String poiId) {
        log.info("[MapApiClient] 开始POI详情搜索 | poiId={}", poiId);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v3/place/detail")
                    .queryParam("key", apiKey)
                    .queryParam("id", poiId)
                    .queryParam("output", "json")
                    .encode()
                    .build()
                    .toUri();

            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                log.info("[MapApiClient] POI详情搜索成功");
                return result;
            }

            log.warn("[MapApiClient] POI详情搜索失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] POI详情搜索异常", e);
            return null;
        }
    }

    /**
     * 搜索关键词输入提示（自动补全）
     *
     * @param keywords 搜索关键词
     * @param city     查询城市（可选）
     * @return 输入提示结果JSON对象
     */
    public JSONObject inputTips(String keywords, String city) {
        log.info("[MapApiClient] 开始输入提示 | keywords={}, city={}", keywords, city);

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v3/assistant/inputtips")
                    .queryParam("key", apiKey)
                    .queryParam("keywords", keywords)
                    .queryParam("output", "json");

            if (city != null && !city.isEmpty()) {
                builder.queryParam("city", city);
            }

            URI uri = builder.encode().build().toUri();
            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                log.info("[MapApiClient] 输入提示成功 | count={}", result.getString("count"));
                return result;
            }

            log.warn("[MapApiClient] 输入提示失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] 输入提示异常", e);
            return null;
        }
    }

    /**
     * 行政区划查询
     *
     * @param keywords 查询关键词（如：北京、朝阳区）
     * @param subdistrict 显示下级行政区级数（0-3）
     * @return 行政区划结果JSON对象
     */
    public JSONObject districtQuery(String keywords, Integer subdistrict) {
        log.info("[MapApiClient] 开始行政区划查询 | keywords={}, subdistrict={}", keywords, subdistrict);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v3/config/district")
                    .queryParam("key", apiKey)
                    .queryParam("keywords", keywords)
                    .queryParam("subdistrict", subdistrict != null ? subdistrict : 0)
                    .queryParam("output", "json")
                    .queryParam("extensions", "base")
                    .encode()
                    .build()
                    .toUri();

            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                log.info("[MapApiClient] 行政区划查询成功");
                return result;
            }

            log.warn("[MapApiClient] 行政区划查询失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] 行政区划查询异常", e);
            return null;
        }
    }

    /**
     * 圆形区域交通态势查询
     *
     * @param location 中心点经纬度（格式：经度,纬度）
     * @param radius   搜索半径（米，最大5000）
     * @return 交通态势JSON对象
     */
    public JSONObject trafficStatusCircle(String location, Integer radius) {
        log.info("[MapApiClient] 开始圆形区域交通态势查询 | location={}, radius={}", location, radius);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(AMAP_BASE_URL + "/v3/traffic/status/circle")
                    .queryParam("key", apiKey)
                    .queryParam("location", location)
                    .queryParam("radius", radius != null ? radius : 1000)
                    .queryParam("output", "json")
                    .queryParam("extensions", "all")
                    .encode()
                    .build()
                    .toUri();

            log.info("[MapApiClient] 请求URI: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JSONObject result = JSON.parseObject(response.getBody());

            if ("1".equals(result.getString("status"))) {
                log.info("[MapApiClient] 圆形区域交通态势查询成功");
                return result;
            }

            log.warn("[MapApiClient] 圆形区域交通态势查询失败 | info={}", result.getString("info"));
            return null;

        } catch (Exception e) {
            log.error("[MapApiClient] 圆形区域交通态势查询异常", e);
            return null;
        }
    }
}
