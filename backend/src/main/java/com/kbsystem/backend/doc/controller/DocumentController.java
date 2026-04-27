package com.kbsystem.backend.doc.controller;

import com.kbsystem.backend.audit.annotation.AuditLog;
import com.kbsystem.backend.common.Result;
import com.kbsystem.backend.doc.model.*;
import com.kbsystem.backend.doc.service.DocumentService;
import com.kbsystem.backend.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档控制器。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceId}/documents")
@Tag(name = "文档模块")
public class DocumentController {

    /**
     * 文档服务。
     */
    private final DocumentService documentService;

    /**
     * 创建文档。
     *
     * @param workspaceId 知识库 ID
     * @param request     请求体
     * @return 文档信息
     */
    @PostMapping
    @Operation(summary = "创建文档")
    public Result<DocumentVO> createDocument(@PathVariable Long workspaceId, @Valid @RequestBody CreateDocumentRequest request) {
        return Result.ok(documentService.createDocument(UserContext.getUserId(), workspaceId, request));
    }

    /**
     * 上传并解析文档文件。
     *
     * @param workspaceId 知识库 ID
     * @param file 上传文件
     * @return 文档信息
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传并解析文档文件")
    @AuditLog(action = "upload", targetType = "document")
    public Result<DocumentVO> uploadDocument(@PathVariable Long workspaceId, @RequestPart("file") MultipartFile file) {
        return Result.ok(documentService.uploadAndParseDocument(UserContext.getUserId(), workspaceId, file));
    }

    /**
     * 重试解析上传文档。
     *
     * @param workspaceId 知识库 ID
     * @param documentId 文档 ID
     * @return 文档信息
     */
    @PostMapping("/{documentId}/retry-parse")
    @Operation(summary = "重试解析文档")
    @AuditLog(action = "retry_parse", targetType = "document", targetIdArgIndex = 1)
    public Result<DocumentVO> retryParse(@PathVariable Long workspaceId, @PathVariable Long documentId) {
        return Result.ok(documentService.retryParseDocument(UserContext.getUserId(), workspaceId, documentId));
    }

    /**
     * 手动提取文档向量。
     *
     * @param workspaceId 知识库 ID
     * @param documentId 文档 ID
     * @return 文档信息
     */
    @PostMapping("/{documentId}/extract-vector")
    @Operation(summary = "提取文档到向量知识库")
    @AuditLog(action = "extract_vector", targetType = "document", targetIdArgIndex = 1)
    public Result<DocumentVO> extractVector(@PathVariable Long workspaceId, @PathVariable Long documentId) {
        return Result.ok(documentService.extractVector(UserContext.getUserId(), workspaceId, documentId));
    }

    /**
     * 删除文档。
     *
     * @param workspaceId 知识库 ID
     * @param documentId 文档 ID
     * @return 空结果
     */
    @DeleteMapping("/{documentId}")
    @Operation(summary = "删除文档")
    @AuditLog(action = "delete", targetType = "document", targetIdArgIndex = 1)
    public Result<Void> deleteDocument(@PathVariable Long workspaceId, @PathVariable Long documentId) {
        documentService.deleteDocument(UserContext.getUserId(), workspaceId, documentId);
        return Result.ok(null);
    }

    /**
     * 兼容式删除文档接口（使用 POST，避免部分环境拦截 DELETE）。
     *
     * @param workspaceId 知识库 ID
     * @param documentId 文档 ID
     * @return 空结果
     */
    @PostMapping("/{documentId}/delete")
    @Operation(summary = "删除文档(兼容POST)")
    @AuditLog(action = "delete", targetType = "document", targetIdArgIndex = 1)
    public Result<Void> deleteDocumentByPost(@PathVariable Long workspaceId, @PathVariable Long documentId) {
        documentService.deleteDocument(UserContext.getUserId(), workspaceId, documentId);
        return Result.ok(null);
    }

    /**
     * 查询文档列表。
     *
     * @param workspaceId 知识库 ID
     * @return 文档列表
     */
    @GetMapping
    @Operation(summary = "查询文档列表")
    public Result<List<DocumentVO>> listDocuments(@PathVariable Long workspaceId) {
        return Result.ok(documentService.listDocuments(UserContext.getUserId(), workspaceId));
    }

    /**
     * 查询文档详情。
     *
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @return 文档详情
     */
    @GetMapping("/{documentId}")
    @Operation(summary = "查询文档详情")
    public Result<DocumentDetailVO> getDocument(@PathVariable Long workspaceId, @PathVariable Long documentId) {
        return Result.ok(documentService.getDocumentDetail(UserContext.getUserId(), workspaceId, documentId));
    }

    /**
     * 保存文档并产生新版本。
     *
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @param request     请求体
     * @return 新版本信息
     */
    @PostMapping("/{documentId}/save")
    @Operation(summary = "保存文档")
    @AuditLog(action = "save", targetType = "document", targetIdArgIndex = 1)
    public Result<DocumentVersionVO> saveDocument(@PathVariable Long workspaceId,
                                                  @PathVariable Long documentId,
                                                  @Valid @RequestBody SaveDocumentRequest request) {
        return Result.ok(documentService.saveDocument(UserContext.getUserId(), workspaceId, documentId, request));
    }

    /**
     * 查询版本列表。
     *
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @return 版本列表
     */
    @GetMapping("/{documentId}/versions")
    @Operation(summary = "查询文档版本列表")
    public Result<List<DocumentVersionVO>> listVersions(@PathVariable Long workspaceId, @PathVariable Long documentId) {
        return Result.ok(documentService.listVersions(UserContext.getUserId(), workspaceId, documentId));
    }

    /**
     * 查询指定版本详情。
     *
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @param versionNo   版本号
     * @return 版本详情
     */
    @GetMapping("/{documentId}/versions/{versionNo}")
    @Operation(summary = "查询指定文档版本详情")
    public Result<DocumentVersionDetailVO> getVersionDetail(@PathVariable Long workspaceId,
                                                            @PathVariable Long documentId,
                                                            @PathVariable Integer versionNo) {
        return Result.ok(documentService.getVersionDetail(UserContext.getUserId(), workspaceId, documentId, versionNo));
    }

    /**
     * 回滚到指定版本。
     *
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @param versionNo   目标版本号
     * @return 新版本信息
     */
    @PostMapping("/{documentId}/rollback/{versionNo}")
    @Operation(summary = "回滚文档版本")
    @AuditLog(action = "rollback", targetType = "document", targetIdArgIndex = 1)
    public Result<DocumentVersionVO> rollback(@PathVariable Long workspaceId,
                                              @PathVariable Long documentId,
                                              @PathVariable Integer versionNo) {
        return Result.ok(documentService.rollbackVersion(UserContext.getUserId(), workspaceId, documentId, versionNo));
    }
}

