package com.travel.aiagent.common.core.constraint;

import com.travel.aiagent.common.domain.constraint.ConstraintDraftVO;
import com.travel.aiagent.common.domain.constraint.ConstraintSet;
import com.travel.aiagent.common.domain.constraint.GlobalConstraint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 约束的<b>唯一写入口</b>（Single Source of Truth 的落点）。
 *
 * <p>它只做三件事，全部是"生产"而不是"校验"：
 * <ol>
 *   <li>归一化：交给 {@link ConstraintNormalizer} 做证据反查、来源/模态降级、同 topic 去重</li>
 *   <li>版本化：内容变了才 bump revision，跨会话保持单调</li>
 *   <li>缓存：按会话保存上一版，供多轮继承</li>
 * </ol>
 *
 * <p><b>为什么不和 {@link PlanConstraintChecker} 合并：</b>方向相反。
 * Store 是"从无到有"（要有 id、有版本、有状态），Checker 是"从有到判"（无状态、纯函数、可重放）。
 * 合并会让"写"藏进一个叫 check 的名字下面，单一写者就失效了。
 *
 * <p>注意它<b>不调用任何模型</b> —— 纯计算 + 缓存，可单测、可秒回。
 *
 * <p><b>已知缺口：</b>缓存只在进程内，重启即失忆（revision 从头开始）；
 * 且只增不减，超过 {@value #MAX_CACHE} 个会话后按遍历顺序随机淘汰。
 * 真源持久化（一张表）是明确待办。
 */
@Slf4j
@Service
public class ConstraintStore {

    private static final int MAX_CACHE = 500;

    private final ConstraintNormalizer normalizer;

    /** key = userId:chatId。P0 用进程内缓存，后续可换 Redis / DB */
    private final Map<String, ConstraintSet> cache = new ConcurrentHashMap<>();

    public ConstraintStore(ConstraintNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    /**
     * 用 Planner 本轮吐出的约束草稿合并出新的约束集。
     *
     * <p>草稿为空时保留上一版 —— 一次抽取抖动不该清空已确认的真源。
     *
     * @param userInput 本轮用户输入，用于 span 反查
     * @param drafts    Planner 输出的约束草稿（可为空）
     */
    public ConstraintSet merge(String userId, String chatId, String userInput, List<ConstraintDraftVO> drafts) {
        String key = key(userId, chatId);
        ConstraintSet previous = cache.get(key);

        ConstraintSet result = normalizer.normalize(drafts, userInput, previous);
        cache.put(key, result);
        evictIfNeeded();

        if (!result.isEmpty()) {
            boolean changed = previous == null || !previous.revision().equals(result.revision());
            log.info("[SSoT] 约束集{} | key={} | revision={} | digest={} | items={}",
                    changed ? "更新" : "沿用", key,
                    result.revision(), result.digest(), result.items().size());
        }
        return result;
    }

    /** 供日志/调试用：把约束渲染成一行行文本 */
    public String describe(ConstraintSet set) {
        if (set == null || set.isEmpty()) {
            return "（无约束）";
        }
        StringBuilder sb = new StringBuilder();
        for (GlobalConstraint c : set.items()) {
            sb.append(c.id()).append(' ')
                    .append(c.topic()).append(' ')
                    .append(c.modality()).append(' ')
                    .append(c.provenance()).append(' ')
                    .append(c.evidenceCheck()).append(" 证据=")
                    .append(c.evidence()).append(' ')
                    .append(c.text()).append('\n');
        }
        return sb.toString();
    }

    private void evictIfNeeded() {
        if (cache.size() > MAX_CACHE) {
            cache.keySet().stream().findFirst().ifPresent(cache::remove);
        }
    }

    private String key(String userId, String chatId) {
        return (userId == null ? "" : userId) + ":" + (chatId == null ? "" : chatId);
    }
}
