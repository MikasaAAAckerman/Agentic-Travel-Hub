package com.travel.aiagent.common.core.pdf;

/**
 * PDF 文件存储抽象。
 *
 * <p>当前实现：本地磁盘（{@link LocalPdfFileStore}）。
 * 未来要上对象存储（OSS/MinIO）时，只需新增一个实现类，
 * 上层 {@code PdfIngestService} / 问答链路零改动——这就是抽象出接口的意义。
 *
 * <p>目录约定（本地实现）：
 * <pre>
 *   {storeDir}/{docId}/original.pdf      原文件（前端 pdf.js 加载）
 *   {storeDir}/{docId}/pages/page-1.png  页面图片（多模态向量化 / 看图作答）
 * </pre>
 */
public interface PdfFileStore {

    /**
     * 保存 PDF 原件。
     *
     * @return 存储路径（写进 pdf_document.file_path）
     */
    String saveOriginal(String docId, String fileName, byte[] content);

    /**
     * 保存页面图片。
     *
     * @param page 页码（从 1 开始）
     * @return 存储路径
     */
    String savePageImage(String docId, int page, byte[] png);

    /**
     * 读取 PDF 原件（前端 pdf.js 加载用）。
     */
    byte[] readOriginal(String docId);

    /**
     * 读取页面图片（看图作答用）。
     *
     * @param page 页码（从 1 开始）
     */
    byte[] readPageImage(String docId, int page);

    /**
     * 删除该文档的全部文件（原件 + 页面图）。
     */
    void delete(String docId);
}
