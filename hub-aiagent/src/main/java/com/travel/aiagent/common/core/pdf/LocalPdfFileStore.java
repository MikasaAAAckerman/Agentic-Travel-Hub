package com.travel.aiagent.common.core.pdf;

import com.travel.common.constant.BizException;
import com.travel.common.constant.ServiceResponseTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * PDF 文件存储——本地磁盘实现。
 *
 * <p>目录结构：{@code {user.dir}/{storeDir}/{docId}/}
 * <pre>
 *   original.pdf
 *   pages/page-1.png, page-2.png ...
 * </pre>
 */
@Slf4j
@Component
public class LocalPdfFileStore implements PdfFileStore {

    @Value("${pdf-rag.store-dir:pdf-store}")
    private String storeDir;

    @Override
    public String saveOriginal(String docId, String fileName, byte[] content) {
        try {
            Path dir = docDir(docId);
            Files.createDirectories(dir);
            Path target = dir.resolve("original.pdf");
            Files.write(target, content);
            log.info("[PdfStore] 原件已保存 | docId={} | fileName={} | bytes={}",
                    docId, fileName, content.length);
            return target.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("[PdfStore] 保存原件失败 | docId={}", docId, e);
            throw new BizException(ServiceResponseTypeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public String savePageImage(String docId, int page, byte[] png) {
        try {
            Path pagesDir = docDir(docId).resolve("pages");
            Files.createDirectories(pagesDir);
            Path target = pagesDir.resolve("page-" + page + ".png");
            Files.write(target, png);
            return target.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("[PdfStore] 保存页面图失败 | docId={} | page={}", docId, page, e);
            throw new BizException(ServiceResponseTypeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public byte[] readOriginal(String docId) {
        return readBytes(docDir(docId).resolve("original.pdf"), docId, 0);
    }

    @Override
    public byte[] readPageImage(String docId, int page) {
        return readBytes(docDir(docId).resolve("pages").resolve("page-" + page + ".png"), docId, page);
    }

    @Override
    public void delete(String docId) {
        try {
            Path dir = docDir(docId);
            if (!Files.exists(dir)) {
                return;
            }
            // 递归删除目录（原件 + 页面图）
            try (var walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        log.warn("[PdfStore] 删除文件失败 | path={}", p, e);
                    }
                });
            }
            log.info("[PdfStore] 文档文件已删除 | docId={}", docId);
        } catch (IOException e) {
            log.error("[PdfStore] 删除文档目录失败 | docId={}", docId, e);
        }
    }

    private Path docDir(String docId) {
        return Paths.get(System.getProperty("user.dir"), storeDir, docId);
    }

    private byte[] readBytes(Path path, String docId, int page) {
        try {
            if (!Files.exists(path)) {
                log.warn("[PdfStore] 文件不存在 | docId={} | page={} | path={}", docId, page, path);
                throw new BizException(ServiceResponseTypeEnum.SYSTEM_ERROR);
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("[PdfStore] 读取失败 | docId={} | path={}", docId, path, e);
            throw new BizException(ServiceResponseTypeEnum.SYSTEM_ERROR);
        }
    }
}
