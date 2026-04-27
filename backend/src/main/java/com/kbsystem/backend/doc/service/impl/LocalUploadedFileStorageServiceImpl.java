package com.kbsystem.backend.doc.service.impl;

import com.kbsystem.backend.common.exception.BizException;
import com.kbsystem.backend.doc.service.UploadedFileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 本地上传文件存储服务实现。
 */
@Service
public class LocalUploadedFileStorageServiceImpl implements UploadedFileStorageService {

    /**
     * 本地文件根目录。
     */
    @Value("${app.upload.local-dir:uploads}")
    private String uploadLocalDir;

    /**
     * 保存上传文件到本地目录。
     *
     * @param workspaceId 知识库 ID
     * @param file 上传文件
     * @return 存储路径
     */
    @Override
    public String save(Long workspaceId, MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new BizException(40023, "上传文件名不能为空");
        }
        try {
            Path workspacePath = Path.of(uploadLocalDir, String.valueOf(workspaceId));
            Files.createDirectories(workspacePath);
            String prefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            Path target = workspacePath.resolve(prefix + "_" + filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new BizException(50022, "保存上传文件失败：" + exception.getMessage());
        }
    }
}

