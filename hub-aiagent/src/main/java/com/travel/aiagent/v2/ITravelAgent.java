package com.travel.aiagent.v2;

import java.util.function.Consumer;

/**
 * TravelSubAgent接口
 */
public interface ITravelAgent {

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
     */
    public String execute(String userInput, String chatMemory, Consumer<String> progress);

}
