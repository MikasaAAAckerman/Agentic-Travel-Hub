package com.travel.aiagent.common.core.pdf;

/**
 * PDF 文本分块（检索的最小单元）。
 *
 * <p>坐标约定同 {@link PdfTextLine}：PDF 用户空间（原点左下，单位 point）。
 * bbox 是块内所有行的并集包围盒——前端据此在原 PDF 上画高亮矩形。
 *
 * <p>⚠️ 分块不跨页：每块只属于一页，保证 bbox 单页有效、引用能精确定位。
 *
 * @param index   块序号（文档内递增）
 * @param page    页码（从 1 开始）
 * @param content 块文本
 * @param x0      左边界
 * @param y0      下边界
 * @param x1      右边界
 * @param y1      上边界
 */
public record PdfChunk(int index, int page, String content, float x0, float y0, float x1, float y1) {
}
