package com.log.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 日志瘦身工具——删掉 Spring AI 框架噪音行，只留业务日志。
 *
 * <p>用法：直接 run main()，原地覆盖 logs/travel-hub.log。
 */
public class LogCleaner {

    /** 包含这些关键字的行直接删掉 */
    private static final List<String> FILTER_KEYWORDS = List.of(
            "MethodToolCallback",
            "DefaultToolCallingManager",
            "DefaultToolCallResultConverter"
    );

    public static void main(String[] args) throws IOException {
        Path logFile = Paths.get("logs/travel-hub.log");
        if (!Files.exists(logFile)) {
            System.out.println("❌ 日志文件不存在：" + logFile.toAbsolutePath());
            return;
        }

        List<String> allLines = Files.readAllLines(logFile);
        int before = allLines.size();

        List<String> filtered = allLines.stream()
                .filter(line -> FILTER_KEYWORDS.stream().noneMatch(line::contains))
                .toList();

        int after = filtered.size();
        Files.write(logFile, filtered);

        System.out.printf("✅ 日志瘦身完成 | %d行 → %d行（删了%d行框架噪音）\n",
                before, after, before - after);
    }
}
