package com.travel.aiagent.common.utils;

import com.travel.aiagent.common.annotation.PromptField;

import java.lang.reflect.Field;

/**
 * 根据标注了 {@link PromptField} 的类，自动生成 SystemPrompt 中的 JSON 格式示例。
 *
 * <p>用法：改 PlanDetailVO 字段 → Prompt 自动同步，忘改注解编译期无感知但运行时
 * JSON 示例与实体一致。
 */
public final class PromptBuilder {

    private PromptBuilder() {}

    /**
     * 生成 JSON 格式示例字符串，供 SystemPrompt 注入。
     *
     * @param clazz 标注了 @PromptField 的类（如 PlanDetailVO.class）
     * @param indent 每行缩进空格数（填 6 或 8 跟 SystemPrompt 文本块对齐）
     */
    public static String buildJsonExample(Class<?> clazz, int indent) {
        String pad = " ".repeat(indent);
        StringBuilder sb = new StringBuilder("{\n");

        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            PromptField pf = f.getAnnotation(PromptField.class);
            if (pf == null) continue;

            String example = pf.example().isEmpty() ? "..." : pf.example();
            sb.append(pad).append(String.format("\"%s\": \"%s\"", f.getName(), pf.value()));

            // 最后一个不加逗号
            if (i < fields.length - 1) {
                boolean hasNextAnnotated = false;
                for (int j = i + 1; j < fields.length; j++) {
                    if (fields[j].getAnnotation(PromptField.class) != null) {
                        hasNextAnnotated = true;
                        break;
                    }
                }
                if (hasNextAnnotated) sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(pad.substring(0, Math.max(0, indent - 2))).append("}");
        return sb.toString();
    }

    /** 默认 6 空格缩进（配合 SystemPrompt 的文本块） */
    public static String buildJsonExample(Class<?> clazz) {
        return buildJsonExample(clazz, 6);
    }
}
