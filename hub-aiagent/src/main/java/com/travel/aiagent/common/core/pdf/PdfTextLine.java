package com.travel.aiagent.common.core.pdf;

/**
 * PDF 里的一行文本 + 它的位置。
 *
 * <p>⚠️ 坐标约定：<b>PDF 用户空间</b>——原点在【页面左下角】，y 轴向上，单位 point（1/72 英寸）。
 * 这是 pdf.js {@code viewport.convertToViewportRectangle()} 期望的坐标系，
 * 前端拿到 bbox 后直接换算即可，不需要翻转 y。
 *
 * @param page 页码（从 1 开始）
 * @param text 行文本
 * @param x0   左边界
 * @param y0   下边界
 * @param x1   右边界
 * @param y1   上边界
 */
public record PdfTextLine(int page, String text, float x0, float y0, float x1, float y1) {
}
