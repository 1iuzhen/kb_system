package com.kbsystem.backend.doc.service.impl;

import com.kbsystem.backend.common.exception.BizException;
import com.kbsystem.backend.doc.service.DocumentContentExtractService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 文档内容提取服务实现。
 */
@Service
public class DocumentContentExtractServiceImpl implements DocumentContentExtractService {

    /**
     * 支持的文件类型后缀。
     */
    private static final String EXT_MD = ".md";
    private static final String EXT_PDF = ".pdf";
    private static final String EXT_DOCX = ".docx";
    private static final Pattern MULTI_BLANK_LINE = Pattern.compile("\\n{3,}");

    /**
     * 从上传文件中提取 Markdown 文本。
     *
     * @param file 上传文件
     * @return Markdown 文本
     */
    @Override
    public String extractMarkdown(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new BizException(40021, "文件名不能为空");
        }
        try {
            return extractByExt(filename, file.getBytes());
        } catch (IOException exception) {
            throw new BizException(50021, "文件解析失败：" + exception.getMessage());
        }
    }

    /**
     * 从已落盘文件中提取文本。
     *
     * @param filePath 存储路径
     * @param originalFilename 原始文件名
     * @return 文本
     */
    @Override
    public String extractMarkdownFromStoredFile(String filePath, String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BizException(40025, "缺少原始文件名，无法解析");
        }
        try {
            byte[] bytes = Files.readAllBytes(Path.of(filePath));
            return extractByExt(originalFilename, bytes);
        } catch (IOException exception) {
            throw new BizException(50023, "读取已上传文件失败：" + exception.getMessage());
        }
    }

    /**
     * PDF 提取。
     *
     * @param file 上传文件
     * @return 文本内容
     * @throws IOException IO 异常
     */
    private String extractFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }

    /**
     * PDF 提取（按字节数组）。
     *
     * @param bytes 文件字节
     * @return 文本内容
     * @throws IOException IO 异常
     */
    private String extractFromPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    /**
     * DOCX 提取。
     *
     * @param file 上传文件
     * @return 文本内容
     * @throws IOException IO 异常
     */
    private String extractFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            // 优先使用官方提取器，覆盖段落、表格、页眉页脚等更多文本区域。
            String extractedText;
            try (XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                extractedText = extractor.getText();
            }
            if (extractedText != null && !extractedText.isBlank()) {
                return extractedText;
            }
            // 极端情况下再回退到段落扫描，避免提取器为空时完全丢内容。
            StringBuilder fallback = new StringBuilder();
            document.getParagraphs().forEach(paragraph -> {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    fallback.append(text).append("\n");
                }
            });
            return fallback.toString();
        }
    }

    /**
     * DOCX 提取（按字节数组）。
     *
     * @param bytes 文件字节
     * @return 文本内容
     * @throws IOException IO 异常
     */
    private String extractFromDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(bytes))) {
            String extractedText;
            try (XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                extractedText = extractor.getText();
            }
            if (extractedText != null && !extractedText.isBlank()) {
                return extractedText;
            }
            StringBuilder fallback = new StringBuilder();
            document.getParagraphs().forEach(paragraph -> {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    fallback.append(text).append("\n");
                }
            });
            return fallback.toString();
        }
    }

    /**
     * 根据扩展名分派解析逻辑。
     *
     * @param filename 文件名
     * @param bytes 文件字节
     * @return 提取文本
     * @throws IOException IO 异常
     */
    private String extractByExt(String filename, byte[] bytes) throws IOException {
        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        if (lowerFilename.endsWith(EXT_MD)) {
            return normalizeContent(new String(bytes, StandardCharsets.UTF_8));
        }
        if (lowerFilename.endsWith(EXT_PDF)) {
            return normalizeContent(extractFromPdf(bytes));
        }
        if (lowerFilename.endsWith(EXT_DOCX)) {
            return normalizeContent(extractFromDocx(bytes));
        }
        throw new BizException(40022, "仅支持 md/pdf/docx 文件");
    }

    /**
     * 文本清洗：
     * 1) 统一换行为 \n
     * 2) 删除每行行尾空白
     * 3) 连续空行压缩到最多 2 行
     *
     * @param raw 原始文本
     * @return 规范化文本
     */
    private String normalizeContent(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalizedNewLine = raw.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalizedNewLine.split("\n", -1);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            builder.append(stripTrailingWhitespace(lines[i]));
            if (i < lines.length - 1) {
                builder.append("\n");
            }
        }
        String compressedBlank = MULTI_BLANK_LINE.matcher(builder.toString()).replaceAll("\n\n");
        return compressedBlank.trim();
    }

    /**
     * 去掉行尾空白字符。
     *
     * @param line 行文本
     * @return 去尾空白后的行
     */
    private String stripTrailingWhitespace(String line) {
        int end = line.length();
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        return line.substring(0, end);
    }
}

