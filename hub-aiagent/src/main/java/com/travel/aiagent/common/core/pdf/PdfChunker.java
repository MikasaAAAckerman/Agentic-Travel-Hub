package com.travel.aiagent.common.core.pdf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * PDF 文本分块器：把「行」按页累积成「块」。
 *
 * <p>策略（沿用旅行书籍知识库的实践经验）：
 * <ul>
 *   <li><b>按页切</b>——块不跨页，bbox 永远单页有效</li>
 *   <li><b>目标长度</b>——累积到 chunkSize（默认 800 字）即封块</li>
 *   <li><b>重叠</b>——下一块带上一块结尾的 overlap（默认 150 字），避免跨块信息断裂</li>
 *   <li><b>块 bbox</b>——块内所有行的坐标并集，供前端高亮</li>
 * </ul>
 */
@Slf4j
@Component
public class PdfChunker {

    /**
     * 目标块长度（字符数）
     */
    @Value("${pdf-rag.chunk-size:800}")
    private int chunkSize;

    /**
     * 相邻块的重叠长度（字符数）
     */
    @Value("${pdf-rag.chunk-overlap:150}")
    private int chunkOverlap;

    /**
     * 把抽取到的文本行切成块。
     *
     * @param lines 按页、按阅读顺序的文本行
     * @return 分块列表（块序号全局递增）
     */
    public List<PdfChunk> chunk(List<PdfTextLine> lines) {
        List<PdfChunk> chunks = new ArrayList<>();
        if (lines == null || lines.isEmpty()) {
            return chunks;
        }

        // 按页分组（LinkedHashMap 保持页序）
        Map<Integer, List<PdfTextLine>> byPage = new LinkedHashMap<>();
        for (PdfTextLine line : lines) {
            byPage.computeIfAbsent(line.page(), k -> new ArrayList<>()).add(line);
        }

        int index = 0;
        for (Map.Entry<Integer, List<PdfTextLine>> entry : byPage.entrySet()) {
            int page = entry.getKey();
            List<PdfTextLine> pageLines = entry.getValue();

            List<PdfTextLine> current = new ArrayList<>();
            int currentLen = 0;

            for (PdfTextLine line : pageLines) {
                current.add(line);
                currentLen += line.text().length();

                if (currentLen >= chunkSize) {
                    chunks.add(buildChunk(index++, page, current));
                    // 保留结尾若干行作为下一块的重叠部分
                    current = tailForOverlap(current, chunkOverlap);
                    currentLen = current.stream().mapToInt(l -> l.text().length()).sum();
                }
            }

            // 页内剩余行封块
            if (!current.isEmpty()) {
                chunks.add(buildChunk(index++, page, current));
            }
        }

        log.info("[PdfChunker] 分块完成 | lines={} | chunks={} | pages={}",
                lines.size(), chunks.size(), byPage.size());
        return chunks;
    }

    /**
     * 取列表尾部若干行，使其字符数达到 overlap（用于下一块开头）。
     */
    private List<PdfTextLine> tailForOverlap(List<PdfTextLine> lines, int overlapChars) {
        LinkedList<PdfTextLine> tail = new LinkedList<>();
        int len = 0;
        for (int i = lines.size() - 1; i >= 0 && len < overlapChars; i--) {
            PdfTextLine line = lines.get(i);
            tail.addFirst(line);
            len += line.text().length();
        }
        return new ArrayList<>(tail);
    }

    /**
     * 把若干行拼成一块，并计算并集 bbox。
     */
    private PdfChunk buildChunk(int index, int page, List<PdfTextLine> lines) {
        StringBuilder sb = new StringBuilder();
        float x0 = Float.MAX_VALUE;
        float y0 = Float.MAX_VALUE;
        float x1 = -Float.MAX_VALUE;
        float y1 = -Float.MAX_VALUE;

        for (PdfTextLine line : lines) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append(line.text());
            x0 = Math.min(x0, line.x0());
            y0 = Math.min(y0, line.y0());
            x1 = Math.max(x1, line.x1());
            y1 = Math.max(y1, line.y1());
        }

        return new PdfChunk(index, page, sb.toString(), x0, y0, x1, y1);
    }
}
