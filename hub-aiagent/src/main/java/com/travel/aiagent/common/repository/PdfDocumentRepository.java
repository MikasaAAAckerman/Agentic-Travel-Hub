package com.travel.aiagent.common.repository;

import com.travel.aiagent.common.domain.PdfDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * PDF 文档元数据 DAO（pdf_document 表）。
 *
 * <p>用 JdbcTemplate 参数化查询（与项目现有 DAO 风格一致，不引 MyBatis）。
 */
@Slf4j
@Repository
public class PdfDocumentRepository {

    private final JdbcTemplate jdbcTemplate;

    public PdfDocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<PdfDocument> ROW_MAPPER = (ResultSet rs, int rowNum) -> PdfDocument.builder()
            .id(rs.getString("id"))
            .fileName(rs.getString("file_name"))
            .filePath(rs.getString("file_path"))
            .pageCount(rs.getInt("page_count"))
            .chunkCount(rs.getInt("chunk_count"))
            .imageCount(rs.getInt("image_count"))
            .status(rs.getString("status"))
            .errorMsg(rs.getString("error_msg"))
            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
            .build();

    private static java.time.LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    public void insert(PdfDocument doc) {
        jdbcTemplate.update("""
                INSERT INTO pdf_document (id, file_name, file_path, status)
                VALUES (?, ?, ?, ?)
                """, doc.getId(), doc.getFileName(), doc.getFilePath(), doc.getStatus());
    }

    /**
     * 解析完成后回填统计信息并置终态。
     */
    public void updateResult(String id, int pageCount, int chunkCount, int imageCount, String status) {
        jdbcTemplate.update("""
                UPDATE pdf_document
                SET page_count = ?, chunk_count = ?, image_count = ?, status = ?
                WHERE id = ?
                """, pageCount, chunkCount, imageCount, status, id);
    }

    public void updateFailed(String id, String errorMsg) {
        jdbcTemplate.update("""
                UPDATE pdf_document SET status = ?, error_msg = ? WHERE id = ?
                """, PdfDocument.STATUS_FAILED, errorMsg, id);
    }

    public List<PdfDocument> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM pdf_document ORDER BY created_at DESC", ROW_MAPPER);
    }

    public PdfDocument findById(String id) {
        List<PdfDocument> list = jdbcTemplate.query(
                "SELECT * FROM pdf_document WHERE id = ?", ROW_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM pdf_document WHERE id = ?", id);
    }
}
