package com.travel.aiagent.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    @Bean("parallelToolExecutor") // 👑 给你的发动机取个霸气的名字喵！
    public Executor parallelToolExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 1. 核心线程数：由于工具调用大部分时间在等网络 IO，可以设置得大一些喵！
        // 建议值：CPU 核心数 * 2 
        executor.setCorePoolSize(16);
        
        // 2. 最大线程数：极端情况下能顶上去的线程数喵！
        executor.setMaxPoolSize(64);
        
        // 3. 队列容量：不要设置得太大，否则任务全挤在队列里，延迟会爆炸喵！
        executor.setQueueCapacity(100);
        
        // 4. 线程名称前缀：极其重要！出 Bug 查日志时一眼就能看到是本小姐的线程在跑喵！
        executor.setThreadNamePrefix("AI-Tool-Parallel-");
        
        // 5. 拒绝策略：如果任务实在太多，让调用者线程自己跑（CallerRunsPolicy），保证任务不丢失喵！
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 6. 优雅关闭：确保你的 Spring 停机时，没跑完的工具不会被粗暴掐断喵！
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
}