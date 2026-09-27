package com.travel.aiagent.common.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PDF 文档元数据（pdf_document 表）。
 *
 * <p>状态流转：PARSING（入库中）→ READY（可检索）/ FAILED（解析失败）
 */
@Data
@Builder
public class PdfDocument {

    /** 解析中 */
    public static final String STATUS_PARSING = "PARSING";
    /** 可检索 */
    public static final String STATUS_READY = "READY";
    /** 解析失败 */
    public static final String STATUS_FAILED = "FAILED";

    private String id;
    private String fileName;
    private String filePath;
    private Integer pageCount;
    private Integer chunkCount;
    private Integer imageCount;
    private String status;
    private String errorMsg;
    private LocalDateTime createdAt;
}
