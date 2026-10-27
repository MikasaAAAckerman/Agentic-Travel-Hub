package com.travel.aiagent.common.utils;

/**
 * Token 估算 —— 只在拿不到真实 usage 时兜底。
 *
 * <p>为什么要估算：Spring AI 的 {@code .entity(Class)} 只返回反序列化后的对象，
 * <b>拿不到 {@code ChatResponse.metadata.usage}</b>；而把 5 处方 planner 调用全改成
 * {@code .chatResponse()} 会连带丢掉框架注入的"输出格式指令"，风险大于收益。
 *
 * <p>于是约定：<b>能拿到真实 usage 就传真实值，拿不到就用本类估算</b>。
 * 估算只服务于"要不要压缩"这类阈值判断，不用于计费。
 *
 * <p>算法：CJK 字符按 1 token 计，其余字符按 4 字符 1 token 计（向上取整）。
 * 中文在多数分词器里约 1 字 1 token，英文约 4 字符 1 token，这个粗粒度够用。
 */
public final class TokenEstimator {

    private TokenEstimator() {
    }

    public static int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjk = 0;
        int other = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCjk(c)) {
                cjk++;
            } else {
                other++;
            }
        }
        return cjk + (other + 3) / 4;
    }

    /** 多段文本合计 */
    public static int estimate(String... parts) {
        if (parts == null || parts.length == 0) {
            return 0;
        }
        int total = 0;
        for (String p : parts) {
            total += estimate(p);
        }
        return total;
    }

    private static boolean isCjk(char c) {
        // 基本汉字 + 扩展A + 兼容汉字 + 全角标点 + 假名/谚文
        return (c >= 0x4E00 && c <= 0x9FFF)
                || (c >= 0x3400 && c <= 0x4DBF)
                || (c >= 0xF900 && c <= 0xFAFF)
                || (c >= 0x3000 && c <= 0x303F)
                || (c >= 0xFF00 && c <= 0xFFEF)
                || (c >= 0x3040 && c <= 0x30FF)
                || (c >= 0xAC00 && c <= 0xD7AF);
    }
}
