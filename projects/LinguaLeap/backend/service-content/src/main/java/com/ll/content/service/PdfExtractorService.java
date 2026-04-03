package com.ll.content.service;

import com.ll.common.exception.BizException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class PdfExtractorService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractorService.class);
    private static final int MAX_PAGES = 50;

    public String extractText(Path pdfPath) {
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            int pageCount = doc.getNumberOfPages();
            if (pageCount > MAX_PAGES) {
                log.warn("PDF 页数过多 ({}), 仅提取前 {} 页", pageCount, MAX_PAGES);
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(Math.min(pageCount, MAX_PAGES));
            String text = stripper.getText(doc);

            if (text == null || text.isBlank()) {
                throw new BizException("PDF 中未提取到文本内容，可能是扫描版 PDF");
            }

            log.info("PDF 文本提取完成: {} 页, {} 字符", Math.min(pageCount, MAX_PAGES), text.length());
            return text.trim();
        } catch (BizException e) {
            throw e;
        } catch (IOException e) {
            log.error("PDF 解析失败", e);
            throw new BizException("PDF 文件解析失败，请确认文件格式正确");
        }
    }
}
