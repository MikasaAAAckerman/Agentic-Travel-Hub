package com.travel.aiagent.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理SpringAI接受到的Document的工具类
 */
@Slf4j
public class SpringAIDocumentUtils {

    public static final Pattern TOOL_NAME_PATTERN = Pattern.compile("\\[TOOL_BEAN_NAME:\\s*([a-zA-Z0-9_]+)\\]");

    public static List<String> getToolBeanList(List<Document> documentList, Pattern pattern) {
        // 准备一个空篮子，用来装抠出来的工具名字
        List<String> targetToolNames = new ArrayList<>();
        // 防御性编程：确保知识库真的返回了东西
        if (documentList != null && !documentList.isEmpty()) {
            // 遍历猎犬咬回来的每一块肉（Document）
            for (Document doc : documentList) {
                // doc.getContent() 就是你刚才 Debug 看到的那一大串包含中文和暗号的字符串！
                String content = doc.getFormattedContent();
                // 💥 步骤 2：放出正则匹配器，对着这段文本进行全身扫描
                Matcher matcher = pattern.matcher(content);
                // 💥 步骤 3：如果找到了符合 [TOOL_BEAN_NAME: xxx] 格式的暗号
                if (matcher.find()) {
                    // matcher.group(1) 极其精准地提取出第一个括号 () 里的内容，也就是 beanName！
                    // 顺手加个 .trim() 去掉可能存在的首尾空格，绝对的安全防御喵！
                    String beanName = matcher.group(1).trim();
                    targetToolNames.add(beanName);
                    log.info("🎯 成功从知识库文档中切割出工具暗号: {}", beanName);
                }
            }
        }
        return targetToolNames;
    }

}
