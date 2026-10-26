package com.travel.aiagent.common.core.constraint;

import com.travel.aiagent.common.domain.constraint.ConstraintSet;
import com.travel.aiagent.common.domain.constraint.GlobalConstraint;

import java.util.List;

/**
 * 把只读快照渲染成 Prompt 中的独立区块。
 *
 * <p>不要把约束塞进 ReAct 上下文的 JSON blob 里 —— 模型对 JSON 嵌套中的规则注意力很低。
 * 约束必须是 System Prompt 里一块显眼的、人读的文本。
 *
 * <p>渲染是<b>内容盲</b>的：不分类、不加领域标签，只给出 id + 强度 + 原文描述。
 */
public final class ConstraintRenderer {

    public static final String HEADER =
            "【必须遵守的全局约束】（上游调度下发，只读：不得修改、不得省略，且必须逐条回执）";

    private ConstraintRenderer() {
    }

    /**
     * @return 渲染后的区块；无约束时返回空串（调用方据此决定是否拼接）
     */
    public static String render(ConstraintSet set) {
        if (set == null || set.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (GlobalConstraint c : set.hardItems()) {
            sb.append("- [").append(c.modality()).append("] ")
                    .append(c.text())
                    .append("（id=").append(c.id()).append("）\n");
        }
        List<GlobalConstraint> prefers = set.preferItems();
        if (!prefers.isEmpty()) {
            sb.append("- [PREFER] 其余偏好：");
            for (int i = 0; i < prefers.size(); i++) {
                if (i > 0) {
                    sb.append('、');
                }
                sb.append(prefers.get(i).text());
            }
            sb.append("（偏好项不要求逐条回执）\n");
        }
        sb.append("约束集：revision=").append(set.revision())
                .append("，digest=").append(set.digest());
        return sb.toString();
    }

    /**
     * 渲染本领域的静态执行规则（纯 Prompt 文本，不是约束数据）。
     *
     * <p>这些规则由各 Agent 用代码声明，属于"我自己怎么干活"，
     * 不参与对账、不进真源 —— 所以它就是一个字符串，不需要领域模型。
     */
    public static String renderDomainInstruction(String instruction) {
        if (instruction == null || instruction.isBlank()) {
            return "";
        }
        return "【本领域执行规则】（只对本 Agent 生效，不参与对账）\n- " + instruction.trim();
    }
}
