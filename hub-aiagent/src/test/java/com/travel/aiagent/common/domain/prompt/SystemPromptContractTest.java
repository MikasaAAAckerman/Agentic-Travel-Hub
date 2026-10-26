package com.travel.aiagent.common.domain.prompt;

import com.travel.aiagent.common.domain.constraint.ConstraintDraftVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prompt 契约测试 —— 防止「实体改了、Prompt 没改」这类静默漂移。
 *
 * <p>这组用例是给一个真实 bug 上的锁：删掉 {@code ConstraintType} 之后，
 * 抽取 Prompt 仍然让模型输出 {@code type}/{@code slot}，而 {@code ConstraintDraftVO}
 * 只剩 {@code topic}。结果所有约束的 topic 都变成 general，被去重合并成一条 ——
 * 而且**编译不报错、测试全绿**，只有读 prompt 才发现。
 */
@DisplayName("SystemPrompt：抽取/回执契约与代码一致")
class SystemPromptContractTest {

    @Test
    @DisplayName("抽取规范必须覆盖 ConstraintDraftVO 的每个字段")
    void extractionSpecCoversEveryDraftField() {
        for (Field field : ConstraintDraftVO.class.getDeclaredFields()) {
            assertThat(SystemPrompt.CONSTRAINT_EXTRACTION_SPEC)
                    .as("字段 %s 没有写进抽取规范，模型不会输出它", field.getName())
                    .contains("\"" + field.getName() + "\"");
        }
    }

    @Test
    @DisplayName("抽取规范不得出现已删除的 type / slot 字段")
    void extractionSpecHasNoRemovedFields() {
        assertThat(SystemPrompt.CONSTRAINT_EXTRACTION_SPEC)
                .doesNotContain("\"type\"")
                .doesNotContain("\"slot\"");
    }

    @Test
    @DisplayName("抽取规范必须给出判据而不是场景枚举（否则模型只认例子里的类别）")
    void extractionSpecUsesCriteriaNotEnumeration() {
        assertThat(SystemPrompt.CONSTRAINT_EXTRACTION_SPEC)
                .contains("去掉这句话")
                .contains("会不会变");
    }

    @Test
    @DisplayName("回执规范必须说明三态 + usedAs + reason 必填")
    void receiptSpecCoversThreeStatesAndReason() {
        assertThat(SystemPrompt.CONSTRAINT_RECEIPT_REQUIREMENT)
                .contains("APPLIED")
                .contains("UNUSED")
                .contains("VIOLATED")
                .contains("usedAs")
                .contains("reason")
                .contains("每一条");
    }
}
