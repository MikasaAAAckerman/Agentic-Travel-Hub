package com.travel.aiagent.common.core.pdf;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 带坐标的 PDF 文本抽取器。
 *
 * <p>继承 {@link PDFTextStripper} 并覆写 {@link #writeString(String, List)}——
 * 标准实现只吐文本，这里额外收集每个文本块的包围盒（bbox），
 * 供「引用 → 悬浮 → 原 PDF 高亮」的溯源链路使用。
 *
 * <p>坐标转换：{@code TextPosition.getYDirAdj()} 以【页顶】为原点，
 * 这里转成 PDF 用户空间（原点左下），与 pdf.js 保持一致（见 {@link PdfTextLine}）。
 */
public class CoordinateAwareTextStripper extends PDFTextStripper {

    private final List<PdfTextLine> lines = new ArrayList<>();

    public CoordinateAwareTextStripper() throws IOException {
        super();
        // 按位置排序输出，保证阅读顺序（多栏排版也尽量正确）
        setSortByPosition(true);
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        if (text == null || text.isBlank() || textPositions == null || textPositions.isEmpty()) {
            return;
        }

        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minYFromTop = Float.MAX_VALUE;
        float maxYFromTop = -Float.MAX_VALUE;
        float pageHeight = 0f;

        for (TextPosition tp : textPositions) {
            float x = tp.getXDirAdj();
            float yFromTop = tp.getYDirAdj();
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x + tp.getWidthDirAdj());
            minYFromTop = Math.min(minYFromTop, yFromTop);
            maxYFromTop = Math.max(maxYFromTop, yFromTop + tp.getHeightDir());
            pageHeight = tp.getPageHeight();
        }

        // 页顶原点 → PDF 用户空间（原点左下）：y = 页高 - yFromTop
        float y0 = pageHeight - maxYFromTop;   // 视觉下边界 → PDF 小 y
        float y1 = pageHeight - minYFromTop;   // 视觉上边界 → PDF 大 y

        lines.add(new PdfTextLine(getCurrentPageNo(), text, minX, y0, maxX, y1));
    }

    /**
     * @return 抽取到的全部文本行（按页、按阅读顺序）
     */
    public List<PdfTextLine> getLines() {
        return lines;
    }
}
