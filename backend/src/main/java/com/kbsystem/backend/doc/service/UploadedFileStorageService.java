package com.kbsystem.backend.doc.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 上传文件存储服务。
 */
public interface UploadedFileStorageService {

    /**
     * 保存上传文件到本地目录（MVP 阶段）。
     *
     * @param workspaceId 知识库 ID
     * @param file 上传文件
     * @return 存储路径
     */
    String save(Long workspaceId, MultipartFile file);
}

