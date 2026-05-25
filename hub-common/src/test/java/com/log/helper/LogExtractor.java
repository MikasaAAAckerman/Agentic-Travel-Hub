package com.log.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志提炼工具——把 travel-hub.log 的关键节点抽成时间线。
 *
 * <p>输出格式：HH:mm:ss 时间戳 + 缩进层级 + 事件描述
 */
public class LogExtractor {

    // ─── 时间提取 ───
    private static final Pattern TIME = Pattern.compile("T(\\d{2}:\\d{2}:\\d{2})");

    // ─── 匹配的日志模式（只保留这些） ───
    private static final Pattern ORCH_PLANNER_START = Pattern.compile("Orchestrator planner 节点 \\| (第\\d+轮调度)");
    private static final Pattern ORCH_PLANNER_DONE  = Pattern.compile("Orchestrator planner → ACTION=(\\w+)(?:, SUB_AGENT_NAME=(\\w+))?");
    private static final Pattern SUB_PLANNER_START  = Pattern.compile("\\[V3-Sub] (\\w+) planner 节点 \\| (第\\d+轮)");
    private static final Pattern SUB_PLANNER_DONE   = Pattern.compile("\\[V3-Sub] (\\w+) planner → action=(\\w+)");
    private static final Pattern SUB_WORKER_ENTER   = Pattern.compile("\\[V3-Sub] (\\w+) 进入 worker 节点");
    private static final Pattern TOOL_START         = Pattern.compile("开始进入方法(\\w+)，参数为 (.+)");
    private static final Pattern TOOL_DONE          = Pattern.compile("工具(\\w+)执行完成");
    private static final Pattern SUB_FINISH         = Pattern.compile("\\[V3-Sub] finish 节点");
    private static final Pattern AGENT_DONE         = Pattern.compile("\\[V3] (\\w+) → 完成");

    // ─── 要跳过的行（包含这些关键字的整行删掉） ───
    private static final List<String> SKIP_CONTAINS = List.of(
            "[Planner] DeepSeek 开始任务规划",
            "userMessage =",
            "任务规划完成 \\| result =",
            "Worker] 构建 RAG Worker Prompt",
            "Worker] doWorkWithRag启动",
            "Worker] RAG检索完成",
            "成功从知识库文档中切割出",
            "\\[V3-Sub] (\\w+) 开始执行",
            "\\[V3] finish 节点",
            "\\[V3] Orchestrator planner 节点 \\| 第1轮调度后立刻 CLARIFY" // keep first round always
    );

    // ─── 输出 ───
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String INDENT = "       ";

    private static String currentSubAgent = "";

    public static void main(String[] args) throws IOException {
        Path logFile = Paths.get("logs/travel-hub.log");
        if (!Files.exists(logFile)) {
            System.out.println("日志文件不存在：" + logFile.toAbsolutePath());
            return;
        }

        List<String> lines = Files.readAllLines(logFile);
        StringBuilder out = new StringBuilder();

        for (String line : lines) {
            // 跳过噪音行
            if (SKIP_CONTAINS.stream().anyMatch(k -> line.contains(k) && !k.contains("第1轮"))) {
                continue;
            }

            String time = extractTime(line);
            if (time == null) continue;

            // 跳过框架日志 + debug
            if (line.contains("MethodToolCallback")
                    || line.contains("DefaultToolCallingManager")
                    || line.contains("DefaultToolCallResultConverter")
                    || line.contains("DEBUG ")) {
                continue;
            }

            // ─── Orchestrator Planner 开始 ───
            Matcher m = ORCH_PLANNER_START.matcher(line);
            if (m.find()) {
                out.append(time).append("：调度Agent ").append(m.group(1)).append(" 开始规划任务\n");
                continue;
            }

            // ─── Orchestrator Planner 完成 ───
            m = ORCH_PLANNER_DONE.matcher(line);
            if (m.find()) {
                String action = m.group(1);
                if ("SUB_AGENT_CALL".equals(action)) {
                    String name = m.group(2);
                    out.append(time).append("：调度Agent 规划完成，开始 call ").append(name).append("\n\n");
                    out.append("-- ").append(name).append(" 开始 --\n\n");
                    currentSubAgent = name;
                } else if ("FINISH".equals(action) || "CLARIFY".equals(action)) {
                    out.append(time).append("：调度Agent 规划完成，action=").append(action).append("\n");
                }
                continue;
            }

            // ─── Sub Planner 开始 ───
            m = SUB_PLANNER_START.matcher(line);
            if (m.find()) {
                out.append(time).append("：").append(INDENT).append(m.group(1)).append(" 开始规划任务（").append(m.group(2)).append("）\n");
                continue;
            }

            // ─── Sub Planner 完成 ───
            m = SUB_PLANNER_DONE.matcher(line);
            if (m.find()) {
                out.append(time).append("：").append(INDENT).append(m.group(1)).append(" 规划完成，action=").append(m.group(2)).append("\n");
                continue;
            }

            // ─── Sub Worker 进入 ───
            m = SUB_WORKER_ENTER.matcher(line);
            if (m.find()) {
                out.append(time).append("：").append(INDENT).append(m.group(1)).append(" 触发 worker 开始执行工具\n");
                continue;
            }

            // ─── Tool 开始 ───
            m = TOOL_START.matcher(line);
            if (m.find()) {
                out.append(time).append("：").append(INDENT).append(INDENT).append("🔧 ").append(m.group(1))
                        .append("(").append(truncate(m.group(2), 80)).append(")\n");
                continue;
            }

            // ─── Tool 完成 ───
            m = TOOL_DONE.matcher(line);
            if (m.find()) {
                out.append(time).append("：").append(INDENT).append(INDENT).append("✅ ").append(m.group(1)).append(" 完成\n");
                continue;
            }

            // ─── Sub finish ───
            if (SUB_FINISH.matcher(line).find()) {
                out.append(time).append("：").append(INDENT).append("finish 节点\n");
                continue;
            }

            // ─── Agent 完成 ───
            m = AGENT_DONE.matcher(line);
            if (m.find()) {
                out.append("\n-- ").append(m.group(1)).append(" 结束 --\n\n");
                currentSubAgent = "";
                continue;
            }
        }

        // 写文件
        String filename = "logs/timeline_" + LocalDateTime.now().format(FILE_TS) + ".txt";
        Files.writeString(Paths.get(filename), out.toString());
        System.out.println("时间线已生成：" + filename);
        System.out.println("共 " + out.toString().lines().count() + " 行事件");
    }

    private static String extractTime(String line) {
        Matcher m = TIME.matcher(line);
        return m.find() ? m.group(1) : null;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
