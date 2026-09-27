package com.travel.aiagent.common.core.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * PDF 页面渲染器：把 PDF 页渲染成 PNG 图片。
 *
 * <p>用途（多模态 RAG 的两条链路）：
 * <ul>
 *   <li>入库：页面 PNG → 多模态向量模型（qwen3-vl-embedding）→ 图片向量库</li>
 *   <li>问答：命中页 PNG → 视觉模型（deepseek-flash）看图作答</li>
 * </ul>
 */
@Slf4j
@Component
public class PdfPageRenderer {

    /**
     * 渲染 DPI（150 兼顾清晰度与体积；多模态 embedding 对图片尺寸通常有限制）
     */
    @Value("${pdf-rag.page-dpi:150}")
    private float dpi;

    /**
     * 渲染指定页为 PNG 字节。
     *
     * @param document  PDF 文档
     * @param pageIndex 页索引（从 0 开始）
     */
    public byte[] renderPageToPng(PDDocument document, int pageIndex) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        BufferedImage image = renderer.renderImageWithDPI(pageIndex, dpi);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        byte[] png = out.toByteArray();

        log.debug("[PdfRender] 页面渲染完成 | page={} | dpi={} | bytes={}",
                pageIndex + 1, dpi, png.length);
        return png;
    }
}
