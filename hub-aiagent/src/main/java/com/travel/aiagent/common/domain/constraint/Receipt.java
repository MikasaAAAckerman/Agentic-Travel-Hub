package com.travel.aiagent.common.domain.constraint;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 上行回执 —— 只走状态，不走内容。
 *
 * <p>内容仍然走 conclusion 字符串（给人看、给 Orchestrator 组织最终答案）；
 * 回执只负责让程序对账。一个字段不要干两件事。
 *
 * <p>只承载裁决（verdicts）。曾经的 facts / proposals 两个列表因为没有任何生产者、
 * 永远是空数组，已删除 —— 真源里不留空壳。
 *
 * <p><b>为什么跨边界要走 {@link #toJson()} / {@link #parse(String)} 而不是直接塞对象：</b>
 * 图框架在子图输出合并回父图时会做一次隐式序列化，top-level 类型能保住，
 * 但 {@code List<Verdict>} 的元素会被还原成 {@code LinkedHashMap}，
 * 审计器遍历时直接 ClassCastException。字符串是唯一不会被"加工"的载体。
 *
 * @param revision 回显下行时的版本，不一致即过期回执，直接丢弃
 * @param digest   回显下行时的摘要，不一致即链路被截断/篡改，fail fast
 */
public record Receipt(
        String revision,
        String digest,
        List<Verdict> verdicts
) {

    public Receipt {
        revision = revision == null ? "" : revision;
        digest = digest == null ? "" : digest;
        verdicts = verdicts == null ? List.of() : List.copyOf(verdicts);
    }

    public static Receipt empty() {
        return new Receipt("", "", List.of());
    }

    public static Receipt ofVerdicts(String revision, String digest, List<Verdict> verdicts) {
        return new Receipt(revision, digest, verdicts);
    }

    public boolean isBlank() {
        return verdicts.isEmpty();
    }

    public String toJson() {
        JSONObject root = new JSONObject();
        root.put("revision", revision);
        root.put("digest", digest);
        JSONArray array = new JSONArray();
        for (Verdict v : verdicts) {
            JSONObject item = new JSONObject();
            item.put("constraintId", v.constraintId());
            item.put("status", v.status().name());
            item.put("usedAs", v.usedAs().name());
            item.put("reason", v.reason());
            array.add(item);
        }
        root.put("verdicts", array);
        return root.toJSONString();
    }

    /**
     * 从 JSON 字符串还原。解析失败一律视为空回执 ——
     * 让上层审计器报 MISSING_RECEIPT，而不是抛异常打挂整轮对话。
     */
    public static Receipt parse(String json) {
        if (json == null || json.isBlank()) {
            return empty();
        }
        try {
            JSONObject root = JSON.parseObject(json);
            List<Verdict> verdicts = new ArrayList<>();
            JSONArray array = root.getJSONArray("verdicts");
            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    verdicts.add(new Verdict(
                            item.getString("constraintId"),
                            VerdictStatus.fromCode(item.getString("status")),
                            Modality.fromCode(item.getString("usedAs")),
                            item.getString("reason")));
                }
            }
            return new Receipt(root.getString("revision"), root.getString("digest"), verdicts);
        } catch (Exception e) {
            return empty();
        }
    }
}
