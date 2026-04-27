package com.kbsystem.backend.doc.service;

import com.kbsystem.backend.doc.model.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档服务接口。
 */
public interface DocumentService {

    /**
     * 创建文档。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param request     请求参数
     * @return 文档信息
     */
    DocumentVO createDocument(Long userId, Long workspaceId, CreateDocumentRequest request);

    /**
     * 查询知识库下文档列表。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @return 文档列表
     */
    List<DocumentVO> listDocuments(Long userId, Long workspaceId);

    /**
     * 查询文档详情。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @return 文档详情
     */
    DocumentDetailVO getDocumentDetail(Long userId, Long workspaceId, Long documentId);

    /**
     * 保存文档并生成新版本。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @param request     请求参数
     * @return 新版本信息
     */
    DocumentVersionVO saveDocument(Long userId, Long workspaceId, Long documentId, SaveDocumentRequest request);

    /**
     * 查询文档版本列表。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @return 版本列表
     */
    List<DocumentVersionVO> listVersions(Long userId, Long workspaceId, Long documentId);

    /**
     * 查询指定版本详情。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @param versionNo   版本号
     * @return 版本详情
     */
    DocumentVersionDetailVO getVersionDetail(Long userId, Long workspaceId, Long documentId, Integer versionNo);

    /**
     * 回滚到指定版本（会生成一个新版本）。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @param versionNo   目标版本号
     * @return 新版本信息
     */
    DocumentVersionVO rollbackVersion(Long userId, Long workspaceId, Long documentId, Integer versionNo);

    /**
     * 上传并解析文档（md/pdf/docx）。
     *
     * @param userId 当前用户 ID
     * @param workspaceId 知识库 ID
     * @param file 上传文件
     * @return 文档信息
     */
    DocumentVO uploadAndParseDocument(Long userId, Long workspaceId, MultipartFile file);

    /**
     * 重试解析已上传文件。
     *
     * @param userId 当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId 文档 ID
     * @return 文档信息
     */
    DocumentVO retryParseDocument(Long userId, Long workspaceId, Long documentId);

    /**
     * 手动提取文档向量（若已有旧向量先删除再重建）。
     *
     * @param userId 当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId 文档 ID
     * @return 文档信息
     */
    DocumentVO extractVector(Long userId, Long workspaceId, Long documentId);

    /**
     * 删除文档（同时删除版本与向量分块）。
     *
     * @param userId 当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId 文档 ID
     */
    void deleteDocument(Long userId, Long workspaceId, Long documentId);
}

