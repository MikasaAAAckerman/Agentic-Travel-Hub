package com.travel.aiagent.common.router;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

/**
 * 意图识别路由
 * 目的：只有旅游计划才进入ReAct对话，拦截普通闲聊
 */
@Slf4j
@Service
public class IntentRecognitionRouter {

    @Resource
    private ChatClient qwenChatClient;

    public String doIntentRecognition(String userInput) {
        log.info(" 开始对用户输入做意图识别 ");
        ChatResponse chatResponse = qwenChatClient.prompt()
                .system("""
                        你是一名及其专业的意图分析师，你的任务是将当前用户的输入判断是闲聊还是需要制定计划。
                        
                        【强制输出规范】
                        1. CHAT —— 如果用户只是在打招呼（如"你好"）、闲聊、赞美、或者问一些无需调用外部工具的通用百科知识。
                        2. PLAN —— 如果用户在要求查询天气、规划行程、比价、搜索航班等需要调用外部工具来完成的复杂任务。
                        """)
                .user(userInput)
                .call().chatResponse();
        log.info(" 获取意图识别结果 -> {}", JSON.toJSONString(chatResponse));
        String result = chatResponse.getResult().getOutput().getText();
        if (result.contains("PLAN")) {
            return "PLAN";
        } else {
            return "CHAT";
        }
    }


}
