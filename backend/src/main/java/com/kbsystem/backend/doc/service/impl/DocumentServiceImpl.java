package com.kbsystem.backend.doc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kbsystem.backend.ai.service.EmbeddingService;
import com.kbsystem.backend.common.exception.BizException;
import com.kbsystem.backend.doc.entity.DocumentEntity;
import com.kbsystem.backend.doc.entity.DocumentVersionEntity;
import com.kbsystem.backend.doc.mapper.ChunkMapper;
import com.kbsystem.backend.doc.mapper.DocumentMapper;
import com.kbsystem.backend.doc.mapper.DocumentVersionMapper;
import com.kbsystem.backend.doc.model.*;
import com.kbsystem.backend.doc.service.DocumentContentExtractService;
import com.kbsystem.backend.doc.service.DocumentService;
import com.kbsystem.backend.doc.service.UploadedFileStorageService;
import com.kbsystem.backend.security.UserContext;
import com.kbsystem.backend.workspace.entity.WorkspaceEntity;
import com.kbsystem.backend.workspace.entity.WorkspaceMemberEntity;
import com.kbsystem.backend.workspace.mapper.WorkspaceMapper;
import com.kbsystem.backend.workspace.mapper.WorkspaceMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档服务实现。
 */
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    /**
     * 上传文件最大字节数（默认 10MB）。
     */
    @Value("${app.upload.max-size-bytes:10485760}")
    private long maxUploadSizeBytes;

    /**
     * 允许的上传扩展名（逗号分隔，不含点）。
     */
    @Value("${app.upload.allowed-exts:md,pdf,docx}")
    private String allowedUploadExts;

    /**
     * embedding 维度。
     */
    @Value("${app.ai.embedding-dim:1536}")
    private int embeddingDim;

    /**
     * 文档 mapper。
     */
    private final DocumentMapper documentMapper;

    /**
     * 文档版本 mapper。
     */
    private final DocumentVersionMapper documentVersionMapper;

    /**
     * 向量分块 mapper。
     */
    private final ChunkMapper chunkMapper;

    /**
     * 知识库 mapper。
     */
    private final WorkspaceMapper workspaceMapper;

    /**
     * 成员 mapper。
     */
    private final WorkspaceMemberMapper workspaceMemberMapper;

    /**
     * 文件解析服务。
     */
    private final DocumentContentExtractService documentContentExtractService;

    /**
     * 本地存储服务（后续可替换为 MinIO）。
     */
    private final UploadedFileStorageService uploadedFileStorageService;

    /**
     * 向量化服务（真实 embedding）。
     */
    private final EmbeddingService embeddingService;

    /**
     * 创建文档并初始化版本 1。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param request     请求参数
     * @return 文档信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO createDocument(Long userId, Long workspaceId, CreateDocumentRequest request) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner", "editor"));

        DocumentEntity entity = new DocumentEntity();
        entity.setWorkspaceId(workspaceId);
        entity.setTitle(ensureUniqueTitle(workspaceId, request.getTitle()));
        entity.setStatus("draft");
        entity.setIndexStatus("not_indexed");
        entity.setIndexedVersionNo(0);
        entity.setIndexErrorMsg(null);
        entity.setLatestVersionNo(1);
        documentMapper.insert(entity);

        DocumentVersionEntity version = new DocumentVersionEntity();
        version.setDocumentId(entity.getId());
        version.setVersionNo(1);
        version.setTitleSnapshot(entity.getTitle());
        version.setContent(request.getContent() == null ? "" : request.getContent());
        version.setStatus("draft");
        version.setSaveUserId(userId);
        version.setSaveUsername(UserContext.getUsername());
        documentVersionMapper.insert(version);
        return toDocumentVO(entity);
    }

    /**
     * 查询文档列表。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @return 文档列表
     */
    @Override
    public List<DocumentVO> listDocuments(Long userId, Long workspaceId) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner", "editor", "viewer"));
        return documentMapper.selectList(new LambdaQueryWrapper<DocumentEntity>()
                        .eq(DocumentEntity::getWorkspaceId, workspaceId))
                .stream()
                .sorted(Comparator.comparing(DocumentEntity::getId))
                .map(this::toDocumentVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询文档详情（返回最新版本内容）。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @return 文档详情
     */
    @Override
    public DocumentDetailVO getDocumentDetail(Long userId, Long workspaceId, Long documentId) {
        requireWorkspace(workspaceId);
        String role = requireRole(userId, workspaceId, Set.of("owner", "editor", "viewer"));
        DocumentEntity document = requireDocument(workspaceId, documentId);
        // 解析失败或刚上传时可能还没有版本（latestVersionNo=0），此时返回空内容但保留状态信息。
        if (document.getLatestVersionNo() == null || document.getLatestVersionNo() <= 0) {
            return new DocumentDetailVO(
                    document.getId(),
                    document.getWorkspaceId(),
                    document.getTitle(),
                    "",
                    0,
                    document.getIndexStatus(),
                    document.getIndexedVersionNo(),
                    document.getIndexErrorMsg(),
                    document.getStatus(),
                    document.getParseErrorMsg(),
                    role
            );
        }
        DocumentVersionEntity latest = requireVersionByNo(document.getId(), document.getLatestVersionNo());
        return new DocumentDetailVO(document.getId(), document.getWorkspaceId(), document.getTitle(),
                latest.getContent(), document.getLatestVersionNo(), document.getIndexStatus(), document.getIndexedVersionNo(),
                document.getIndexErrorMsg(), document.getStatus(), document.getParseErrorMsg(), role);
    }

    /**
     * 保存文档内容并生成新版本。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @param request     请求参数
     * @return 新版本信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVersionVO saveDocument(Long userId, Long workspaceId, Long documentId, SaveDocumentRequest request) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner", "editor"));
        DocumentEntity document = requireDocument(workspaceId, documentId);
        // 乐观锁语义：前端提交的 baseVersion 必须与当前最新版本一致，防止并发覆盖。
        if (!request.getBaseVersion().equals(document.getLatestVersionNo())) {
            throw new BizException(40901, "文档版本冲突，请刷新后重试");
        }

        int newVersionNo = document.getLatestVersionNo() + 1;
        DocumentVersionEntity newVersion = new DocumentVersionEntity();
        newVersion.setDocumentId(document.getId());
        newVersion.setVersionNo(newVersionNo);
        newVersion.setTitleSnapshot(request.getTitle());
        newVersion.setContent(request.getContent());
        newVersion.setStatus("draft");
        newVersion.setSaveUserId(userId);
        newVersion.setSaveUsername(UserContext.getUsername());
        documentVersionMapper.insert(newVersion);

        document.setTitle(request.getTitle());
        document.setLatestVersionNo(newVersionNo);
        document.setStatus("draft");
        document.setIndexStatus("not_indexed");
        document.setIndexErrorMsg(null);
        documentMapper.updateById(document);
        return toVersionVO(newVersion);
    }

    /**
     * 查询版本历史。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @return 历史列表
     */
    @Override
    public List<DocumentVersionVO> listVersions(Long userId, Long workspaceId, Long documentId) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner", "editor", "viewer"));
        requireDocument(workspaceId, documentId);
        return documentVersionMapper.selectList(new LambdaQueryWrapper<DocumentVersionEntity>()
                        .eq(DocumentVersionEntity::getDocumentId, documentId))
                .stream()
                .sorted((a, b) -> Integer.compare(b.getVersionNo(), a.getVersionNo()))
                .map(this::toVersionVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询指定版本详情。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @param versionNo   版本号
     * @return 版本详情
     */
    @Override
    public DocumentVersionDetailVO getVersionDetail(Long userId, Long workspaceId, Long documentId, Integer versionNo) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner", "editor", "viewer"));
        requireDocument(workspaceId, documentId);
        DocumentVersionEntity version = requireVersionByNo(documentId, versionNo);
        return new DocumentVersionDetailVO(version.getId(), version.getDocumentId(), version.getVersionNo(),
                version.getStatus(), version.getTitleSnapshot(), version.getContent(), version.getSaveUsername(), version.getCreateTime());
    }

    /**
     * 回滚到指定版本。
     * 回滚不会删除后续版本，而是复制目标版本内容创建一个新版本。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @param versionNo   目标版本号
     * @return 新版本信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVersionVO rollbackVersion(Long userId, Long workspaceId, Long documentId, Integer versionNo) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner", "editor"));
        DocumentEntity document = requireDocument(workspaceId, documentId);
        DocumentVersionEntity target = requireVersionByNo(document.getId(), versionNo);

        int newVersionNo = document.getLatestVersionNo() + 1;
        DocumentVersionEntity newVersion = new DocumentVersionEntity();
        newVersion.setDocumentId(document.getId());
        newVersion.setVersionNo(newVersionNo);
        newVersion.setTitleSnapshot(document.getTitle());
        newVersion.setContent(target.getContent());
        newVersion.setStatus("draft");
        newVersion.setSaveUserId(userId);
        newVersion.setSaveUsername(UserContext.getUsername());
        documentVersionMapper.insert(newVersion);

        document.setLatestVersionNo(newVersionNo);
        document.setIndexStatus("not_indexed");
        document.setIndexErrorMsg(null);
        documentMapper.updateById(document);
        return toVersionVO(newVersion);
    }

    /**
     * 上传并解析文档。
     * 流程：校验权限 -> 保存原文件 -> 解析文本 -> 生成文档与版本。
     *
     * @param userId 当前用户 ID
     * @param workspaceId 知识库 ID
     * @param file 上传文件
     * @return 文档信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO uploadAndParseDocument(Long userId, Long workspaceId, MultipartFile file) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner", "editor"));
        validateUploadFile(file);
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new BizException(40024, "上传文件名不能为空");
        }
        // 上传文件先落盘（MVP 阶段），后续可切换到 MinIO。
        String sourceFilePath = uploadedFileStorageService.save(workspaceId, file);
        String title = ensureUniqueTitle(workspaceId, stripFileExt(filename));
        DocumentEntity entity = new DocumentEntity();
        entity.setWorkspaceId(workspaceId);
        entity.setTitle(title);
        entity.setStatus("uploading");
        entity.setIndexStatus("not_indexed");
        entity.setIndexedVersionNo(0);
        entity.setIndexErrorMsg(null);
        entity.setParseErrorMsg(null);
        entity.setSourceFileName(filename);
        entity.setSourceFilePath(sourceFilePath);
        entity.setLatestVersionNo(0);
        documentMapper.insert(entity);
        entity.setStatus("parsing");
        documentMapper.updateById(entity);

        try {
            String content = documentContentExtractService.extractMarkdown(file);
            DocumentVersionEntity version = new DocumentVersionEntity();
            version.setDocumentId(entity.getId());
            version.setVersionNo(1);
            version.setTitleSnapshot(title);
            version.setContent(content == null ? "" : content);
            version.setStatus("draft");
            version.setSaveUserId(userId);
            version.setSaveUsername(UserContext.getUsername());
            documentVersionMapper.insert(version);

            entity.setStatus("parsed");
            entity.setParseErrorMsg(null);
            entity.setLatestVersionNo(1);
            documentMapper.updateById(entity);
            return toDocumentVO(entity);
        } catch (RuntimeException exception) {
            // 解析失败时保留失败状态，便于前端识别并提示。
            entity.setStatus("failed");
            entity.setParseErrorMsg(exception.getMessage());
            documentMapper.updateById(entity);
            throw exception;
        }
    }

    /**
     * 重试解析已上传文件。
     *
     * @param userId 当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId 文档 ID
     * @return 文档信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO retryParseDocument(Long userId, Long workspaceId, Long documentId) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner", "editor"));
        DocumentEntity document = requireDocument(workspaceId, documentId);
        if (document.getSourceFilePath() == null || document.getSourceFileName() == null) {
            throw new BizException(40026, "该文档不是上传文档，无法重试解析");
        }
        document.setStatus("parsing");
        document.setParseErrorMsg(null);
        documentMapper.updateById(document);
        try {
            String content = documentContentExtractService.extractMarkdownFromStoredFile(
                    document.getSourceFilePath(),
                    document.getSourceFileName()
            );
            int newVersionNo = document.getLatestVersionNo() + 1;
            DocumentVersionEntity version = new DocumentVersionEntity();
            version.setDocumentId(document.getId());
            version.setVersionNo(newVersionNo);
            version.setTitleSnapshot(document.getTitle());
            version.setContent(content == null ? "" : content);
            version.setStatus("draft");
            version.setSaveUserId(userId);
            version.setSaveUsername(UserContext.getUsername());
            documentVersionMapper.insert(version);

            document.setLatestVersionNo(newVersionNo);
            document.setStatus("parsed");
            document.setParseErrorMsg(null);
            document.setIndexStatus("not_indexed");
            document.setIndexErrorMsg(null);
            documentMapper.updateById(document);
            return toDocumentVO(document);
        } catch (RuntimeException exception) {
            document.setStatus("failed");
            document.setParseErrorMsg(exception.getMessage());
            documentMapper.updateById(document);
            throw exception;
        }
    }

    /**
     * 手动提取文档向量（若已存在旧向量则先删除再重建）。
     *
     * @param userId 当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId 文档 ID
     * @return 文档信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO extractVector(Long userId, Long workspaceId, Long documentId) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner", "editor"));
        DocumentEntity document = requireDocument(workspaceId, documentId);
        if (document.getLatestVersionNo() == null || document.getLatestVersionNo() <= 0) {
            throw new BizException(40030, "当前文档暂无可提取内容");
        }
        DocumentVersionEntity latestVersion = requireVersionByNo(documentId, document.getLatestVersionNo());
        document.setIndexStatus("indexing");
        document.setIndexErrorMsg(null);
        documentMapper.updateById(document);
        try {
            // 已存在则先删除历史向量，确保检索永远只命中当前激活版本。
            chunkMapper.deleteByDocumentId(documentId);
            List<String> chunks = splitToChunks(latestVersion.getContent());
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                chunkMapper.insertChunk(
                        workspaceId,
                        documentId,
                        latestVersion.getVersionNo(),
                        i + 1,
                        latestVersion.getTitleSnapshot(),
                        chunk,
                        toVectorLiteral(embeddingService.embed(chunk))
                );
            }
            document.setIndexStatus("indexed");
            document.setIndexedVersionNo(latestVersion.getVersionNo());
            document.setIndexErrorMsg(null);
            documentMapper.updateById(document);
            return toDocumentVO(document);
        } catch (RuntimeException exception) {
            document.setIndexStatus("failed");
            document.setIndexErrorMsg(exception.getMessage());
            documentMapper.updateById(document);
            throw exception;
        }
    }

    /**
     * 删除文档及其关联数据（版本、向量分块）。
     *
     * @param userId 当前用户 ID
     * @param workspaceId 知识库 ID
     * @param documentId 文档 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long userId, Long workspaceId, Long documentId) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner", "editor"));
        requireDocument(workspaceId, documentId);
        // 先删向量分块与版本，再删除文档主记录，避免遗留脏数据。
        chunkMapper.deleteByDocumentId(documentId);
        documentVersionMapper.delete(new LambdaQueryWrapper<DocumentVersionEntity>()
                .eq(DocumentVersionEntity::getDocumentId, documentId));
        documentMapper.deleteById(documentId);
    }

    /**
     * 校验知识库存在。
     *
     * @param workspaceId 知识库 ID
     */
    private void requireWorkspace(Long workspaceId) {
        WorkspaceEntity workspace = workspaceMapper.selectById(workspaceId);
        if (workspace == null) {
            throw new BizException(40401, "知识库不存在");
        }
    }

    /**
     * 校验并返回文档。
     *
     * @param workspaceId 知识库 ID
     * @param documentId  文档 ID
     * @return 文档实体
     */
    private DocumentEntity requireDocument(Long workspaceId, Long documentId) {
        DocumentEntity document = documentMapper.selectById(documentId);
        if (document == null || !document.getWorkspaceId().equals(workspaceId)) {
            throw new BizException(40403, "文档不存在");
        }
        return document;
    }

    /**
     * 按版本号查询版本。
     *
     * @param documentId 文档 ID
     * @param versionNo  版本号
     * @return 版本实体
     */
    private DocumentVersionEntity requireVersionByNo(Long documentId, Integer versionNo) {
        DocumentVersionEntity version = documentVersionMapper.selectOne(new LambdaQueryWrapper<DocumentVersionEntity>()
                .eq(DocumentVersionEntity::getDocumentId, documentId)
                .eq(DocumentVersionEntity::getVersionNo, versionNo)
                .last("LIMIT 1"));
        if (version == null) {
            throw new BizException(40404, "文档版本不存在");
        }
        return version;
    }

    /**
     * 校验角色权限。
     *
     * @param userId       用户 ID
     * @param workspaceId  知识库 ID
     * @param allowedRoles 允许角色
     * @return 当前用户角色
     */
    private String requireRole(Long userId, Long workspaceId, Set<String> allowedRoles) {
        WorkspaceMemberEntity member = workspaceMemberMapper.selectOne(new LambdaQueryWrapper<WorkspaceMemberEntity>()
                .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId)
                .eq(WorkspaceMemberEntity::getUserId, userId)
                .last("LIMIT 1"));
        if (member == null) {
            throw new BizException(40301, "无权访问该知识库");
        }
        if (!allowedRoles.contains(member.getRole())) {
            throw new BizException(40302, "权限不足");
        }
        return member.getRole();
    }

    /**
     * 转换文档 VO。
     *
     * @param entity 实体
     * @return VO
     */
    private DocumentVO toDocumentVO(DocumentEntity entity) {
        return new DocumentVO(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getTitle(),
                entity.getStatus(),
                entity.getIndexStatus(),
                entity.getIndexedVersionNo(),
                entity.getIndexErrorMsg(),
                entity.getParseErrorMsg(),
                entity.getLatestVersionNo()
        );
    }

    /**
     * 去除文件扩展名得到默认文档标题。
     *
     * @param filename 文件名
     * @return 标题
     */
    private String stripFileExt(String filename) {
        String normalized = filename.trim();
        int idx = normalized.lastIndexOf(".");
        if (idx <= 0) {
            return normalized;
        }
        String ext = normalized.substring(idx).toLowerCase(Locale.ROOT);
        if (ext.equals(".md") || ext.equals(".pdf") || ext.equals(".docx")) {
            return normalized.substring(0, idx);
        }
        return normalized;
    }

    /**
     * 校验上传文件基础约束（大小与扩展名）。
     *
     * @param file 上传文件
     */
    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(40027, "上传文件不能为空");
        }
        if (file.getSize() > maxUploadSizeBytes) {
            throw new BizException(40028, "文件超过大小限制，最大 " + maxUploadSizeBytes + " 字节");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new BizException(40024, "上传文件名不能为空");
        }
        String ext = getFileExt(filename);
        Set<String> allowed = new HashSet<>();
        for (String allowedExt : allowedUploadExts.split(",")) {
            allowed.add(allowedExt.trim().toLowerCase(Locale.ROOT));
        }
        if (!allowed.contains(ext)) {
            throw new BizException(40022, "仅支持 " + allowedUploadExts + " 文件");
        }
    }

    /**
     * 获取文件扩展名（不含点）。
     *
     * @param filename 文件名
     * @return 扩展名
     */
    private String getFileExt(String filename) {
        int idx = filename.lastIndexOf(".");
        if (idx < 0 || idx >= filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 文本分块（固定长度 + overlap）。
     *
     * @param content 原始内容
     * @return 分块列表
     */
    private List<String> splitToChunks(String content) {
        final int chunkSize = 800;
        final int overlap = 150;
        String safeContent = content == null ? "" : content.trim();
        if (safeContent.isEmpty()) {
            return List.of("");
        }
        List<String> chunks = new java.util.ArrayList<>();
        int start = 0;
        while (start < safeContent.length()) {
            int end = Math.min(start + chunkSize, safeContent.length());
            chunks.add(safeContent.substring(start, end));
            if (end >= safeContent.length()) {
                break;
            }
            start = Math.max(0, end - overlap);
        }
        return chunks;
    }

    /**
     * 将 embedding 数组转换为 pgvector 文本格式。
     *
     * @param vector 向量
     * @return pgvector 文本格式
     */
    private String toVectorLiteral(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            throw new BizException(50025, "embedding 结果为空");
        }
        if (vector.size() != embeddingDim) {
            throw new BizException(50026, "embedding 维度不匹配，期望 " + embeddingDim + "，实际 " + vector.size());
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            builder.append(String.format(Locale.ROOT, "%.8f", vector.get(i)));
            if (i < vector.size() - 1) {
                builder.append(",");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * 保证同一知识库下文档标题唯一。
     * 若重名则自动追加 (2)/(3) 后缀。
     *
     * @param workspaceId 知识库 ID
     * @param rawTitle 原始标题
     * @return 可用标题
     */
    private String ensureUniqueTitle(Long workspaceId, String rawTitle) {
        String base = rawTitle == null ? "" : rawTitle.trim();
        if (base.isEmpty()) {
            throw new BizException(40029, "文档标题不能为空");
        }
        String candidate = base;
        int suffix = 2;
        while (existsTitle(workspaceId, candidate)) {
            candidate = base + " (" + suffix + ")";
            suffix++;
        }
        return candidate;
    }

    /**
     * 判断知识库下是否存在同名文档。
     *
     * @param workspaceId 知识库 ID
     * @param title 标题
     * @return true=存在
     */
    private boolean existsTitle(Long workspaceId, String title) {
        return documentMapper.selectCount(new LambdaQueryWrapper<DocumentEntity>()
                .eq(DocumentEntity::getWorkspaceId, workspaceId)
                .eq(DocumentEntity::getTitle, title)) > 0;
    }

    /**
     * 转换版本 VO。
     *
     * @param entity 实体
     * @return VO
     */
    private DocumentVersionVO toVersionVO(DocumentVersionEntity entity) {
        return new DocumentVersionVO(entity.getId(), entity.getVersionNo(), entity.getStatus(),
                entity.getTitleSnapshot(), entity.getSaveUsername(), entity.getCreateTime());
    }
}

