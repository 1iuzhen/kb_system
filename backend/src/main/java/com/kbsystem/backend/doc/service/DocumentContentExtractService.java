package com.kbsystem.backend.doc.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文档内容提取服务。
 */
public interface DocumentContentExtractService {

    /**
     * 从上传文件中提取 Markdown 文本内容。
     *
     * @param file 上传文件
     * @return Markdown 文本
     */
    String extractMarkdown(MultipartFile file);

    /**
     * 从已存储的文件路径中提取 Markdown 文本内容。
     *
     * @param filePath 存储路径
     * @param originalFilename 原始文件名（用于识别后缀）
     * @return Markdown 文本
     */
    String extractMarkdownFromStoredFile(String filePath, String originalFilename);
}

