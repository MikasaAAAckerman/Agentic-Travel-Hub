package com.travel.aiagent.v3;

import java.util.function.Consumer;

/**
 * TravelSubAgent接口
 */
public interface ITravelGraphAgent {

    /**
     * 实现类名称
     */
    public String name();

    /**
     * 实现类的业务描述
     */
    public String description();

    /**
     * 执行方法，返回结果
     *
     * @param traceId 请求级链路追踪ID（透传，关联 llm_call_log 追踪）
     */
    public String execute(String userInput, String userId, String chatId, String traceId, Consumer<String> progress);

}
