package com.travel.aiagent.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注字段在 SystemPrompt JSON 示例中的描述。
 *
 * <p>打在 PlanDetailVO 的字段上，{@code PromptBuilder} 自动生成
 * SystemPrompt 里的 JSON 格式示例，确保代码定义和 Prompt 描述永不漂移。
 *
 * <pre>{@code
 * @PromptField(value = "下一步动作：TOOL_CALL / FINISH / CLARIFY", example = "TOOL_CALL")
 * private String action;
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PromptField {

    /** 字段在 JSON 示例中的描述文字（给 LLM 看） */
    String value();

    /** 字段的示例值（可选，不填则使用空字符串占位） */
    String example() default "";
}
