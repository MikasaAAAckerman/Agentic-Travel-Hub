package com.travel.aiagent.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TokenEstimator：真实 usage 拿不到时的兜底估算")
class TokenEstimatorTest {

    @Test
    @DisplayName("空值 -> 0")
    void blankIsZero() {
        assertThat(TokenEstimator.estimate((String) null)).isZero();
        assertThat(TokenEstimator.estimate("")).isZero();
    }

    @Test
    @DisplayName("纯中文：一字一 token")
    void chineseCountsPerChar() {
        assertThat(TokenEstimator.estimate("你好世界")).isEqualTo(4);
    }

    @Test
    @DisplayName("纯英文：四字符一 token，向上取整")
    void englishCountsPerFourChars() {
        assertThat(TokenEstimator.estimate("hello world")).isEqualTo(3);   // 11 / 4 -> 3
        assertThat(TokenEstimator.estimate("abcd")).isEqualTo(1);
        assertThat(TokenEstimator.estimate("abcde")).isEqualTo(2);
    }

    @Test
    @DisplayName("中英混排分别计数后相加")
    void mixedTextSumsBoth() {
        // 2 个汉字 + 8 个英文字符(2 token)
        assertThat(TokenEstimator.estimate("你好abcdefgh")).isEqualTo(4);
    }

    @Test
    @DisplayName("多段文本合计，null 段按 0")
    void multiplePartsAreSummed() {
        assertThat(TokenEstimator.estimate("你好", null, "abcd")).isEqualTo(3);
    }
}
