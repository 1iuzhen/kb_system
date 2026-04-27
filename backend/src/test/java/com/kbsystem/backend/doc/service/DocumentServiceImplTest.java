package com.kbsystem.backend.doc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kbsystem.backend.common.exception.BizException;
import com.kbsystem.backend.doc.entity.DocumentEntity;
import com.kbsystem.backend.doc.entity.DocumentVersionEntity;
import com.kbsystem.backend.doc.mapper.ChunkMapper;
import com.kbsystem.backend.doc.mapper.DocumentMapper;
import com.kbsystem.backend.doc.mapper.DocumentVersionMapper;
import com.kbsystem.backend.doc.model.CreateDocumentRequest;
import com.kbsystem.backend.doc.model.SaveDocumentRequest;
import com.kbsystem.backend.ai.service.EmbeddingService;
import com.kbsystem.backend.doc.service.DocumentContentExtractService;
import com.kbsystem.backend.doc.service.UploadedFileStorageService;
import com.kbsystem.backend.doc.service.impl.DocumentServiceImpl;
import com.kbsystem.backend.workspace.entity.WorkspaceEntity;
import com.kbsystem.backend.workspace.entity.WorkspaceMemberEntity;
import com.kbsystem.backend.workspace.mapper.WorkspaceMapper;
import com.kbsystem.backend.workspace.mapper.WorkspaceMemberMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

/**
 * 文档服务单元测试。
 */
class DocumentServiceImplTest {

    /**
     * 被测对象。
     */
    private DocumentServiceImpl documentService;

    /**
     * 依赖 mocks。
     */
    private DocumentMapper documentMapper;
    private DocumentVersionMapper documentVersionMapper;
    private WorkspaceMapper workspaceMapper;
    private WorkspaceMemberMapper workspaceMemberMapper;
    private DocumentContentExtractService documentContentExtractService;
    private UploadedFileStorageService uploadedFileStorageService;
    private ChunkMapper chunkMapper;
    private EmbeddingService embeddingService;

    /**
     * 初始化 mock。
     */
    @BeforeEach
    void setUp() {
        documentMapper = Mockito.mock(DocumentMapper.class);
        documentVersionMapper = Mockito.mock(DocumentVersionMapper.class);
        workspaceMapper = Mockito.mock(WorkspaceMapper.class);
        workspaceMemberMapper = Mockito.mock(WorkspaceMemberMapper.class);
        documentContentExtractService = Mockito.mock(DocumentContentExtractService.class);
        uploadedFileStorageService = Mockito.mock(UploadedFileStorageService.class);
        chunkMapper = Mockito.mock(ChunkMapper.class);
        embeddingService = Mockito.mock(EmbeddingService.class);
        documentService = new DocumentServiceImpl(
                documentMapper,
                documentVersionMapper,
                chunkMapper,
                workspaceMapper,
                workspaceMemberMapper,
                documentContentExtractService,
                uploadedFileStorageService,
                embeddingService
        );
        ReflectionTestUtils.setField(documentService, "maxUploadSizeBytes", 10 * 1024 * 1024L);
        ReflectionTestUtils.setField(documentService, "allowedUploadExts", "md,pdf,docx");
    }

    /**
     * 测试：owner 创建文档成功。
     */
    @Test
    void shouldCreateDocumentWhenOwnerRole() {
        WorkspaceEntity workspaceEntity = new WorkspaceEntity();
        workspaceEntity.setId(1L);
        Mockito.when(workspaceMapper.selectById(1L)).thenReturn(workspaceEntity);
        WorkspaceMemberEntity member = new WorkspaceMemberEntity();
        member.setRole("owner");
        Mockito.when(workspaceMemberMapper.selectOne(Mockito.any(LambdaQueryWrapper.class))).thenReturn(member);
        Mockito.doAnswer(invocation -> {
            DocumentEntity entity = invocation.getArgument(0);
            entity.setId(101L);
            return 1;
        }).when(documentMapper).insert(Mockito.any(DocumentEntity.class));

        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setTitle("文档A");
        request.setContent("# hello");
        Assertions.assertEquals(101L, documentService.createDocument(1L, 1L, request).getId());
    }

    /**
     * 测试：viewer 保存文档会被拒绝。
     */
    @Test
    void shouldThrowWhenViewerSaveDocument() {
        WorkspaceEntity workspaceEntity = new WorkspaceEntity();
        workspaceEntity.setId(1L);
        Mockito.when(workspaceMapper.selectById(1L)).thenReturn(workspaceEntity);
        WorkspaceMemberEntity member = new WorkspaceMemberEntity();
        member.setRole("viewer");
        Mockito.when(workspaceMemberMapper.selectOne(Mockito.any(LambdaQueryWrapper.class))).thenReturn(member);
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setId(100L);
        documentEntity.setWorkspaceId(1L);
        documentEntity.setLatestVersionNo(1);
        Mockito.when(documentMapper.selectById(100L)).thenReturn(documentEntity);

        SaveDocumentRequest request = new SaveDocumentRequest();
        request.setTitle("文档A");
        request.setContent("内容");
        request.setBaseVersion(1);
        Assertions.assertThrows(BizException.class, () -> documentService.saveDocument(2L, 1L, 100L, request));
    }

    /**
     * 测试：上传 markdown 文件后应生成版本 1。
     */
    @Test
    void shouldUploadAndParseMarkdownSuccessfully() {
        WorkspaceEntity workspaceEntity = new WorkspaceEntity();
        workspaceEntity.setId(1L);
        Mockito.when(workspaceMapper.selectById(1L)).thenReturn(workspaceEntity);
        WorkspaceMemberEntity member = new WorkspaceMemberEntity();
        member.setRole("owner");
        Mockito.when(workspaceMemberMapper.selectOne(Mockito.any(LambdaQueryWrapper.class))).thenReturn(member);
        Mockito.doAnswer(invocation -> {
            DocumentEntity entity = invocation.getArgument(0);
            entity.setId(201L);
            return 1;
        }).when(documentMapper).insert(Mockito.any(DocumentEntity.class));
        Mockito.when(uploadedFileStorageService.save(Mockito.eq(1L), Mockito.any())).thenReturn("uploads/1/demo.md");
        Mockito.when(documentContentExtractService.extractMarkdown(Mockito.any())).thenReturn("# title");
        MockMultipartFile file = new MockMultipartFile("file", "demo.md", "text/markdown", "# title".getBytes());

        Assertions.assertEquals(201L, documentService.uploadAndParseDocument(1L, 1L, file).getId());
        Mockito.verify(documentVersionMapper, Mockito.times(1)).insert(Mockito.any(DocumentVersionEntity.class));
        Mockito.verify(documentMapper, Mockito.atLeastOnce()).updateById(Mockito.any(DocumentEntity.class));
    }

    /**
     * 测试：解析失败文档可重试并生成新版本。
     */
    @Test
    void shouldRetryParseWhenDocumentHasSourceFile() {
        WorkspaceEntity workspaceEntity = new WorkspaceEntity();
        workspaceEntity.setId(1L);
        Mockito.when(workspaceMapper.selectById(1L)).thenReturn(workspaceEntity);
        WorkspaceMemberEntity member = new WorkspaceMemberEntity();
        member.setRole("owner");
        Mockito.when(workspaceMemberMapper.selectOne(Mockito.any(LambdaQueryWrapper.class))).thenReturn(member);
        DocumentEntity document = new DocumentEntity();
        document.setId(300L);
        document.setWorkspaceId(1L);
        document.setTitle("demo");
        document.setStatus("failed");
        document.setLatestVersionNo(1);
        document.setSourceFileName("demo.docx");
        document.setSourceFilePath("uploads/1/demo.docx");
        Mockito.when(documentMapper.selectById(300L)).thenReturn(document);
        Mockito.when(documentContentExtractService.extractMarkdownFromStoredFile("uploads/1/demo.docx", "demo.docx"))
                .thenReturn("retry content");

        Assertions.assertEquals(300L, documentService.retryParseDocument(1L, 1L, 300L).getId());
        Mockito.verify(documentVersionMapper, Mockito.times(1)).insert(Mockito.any(DocumentVersionEntity.class));
    }

    /**
     * 测试：同知识库同名文档会自动重命名后保存。
     */
    @Test
    void shouldAppendSuffixWhenCreateDocumentHasDuplicateTitle() {
        WorkspaceEntity workspaceEntity = new WorkspaceEntity();
        workspaceEntity.setId(1L);
        Mockito.when(workspaceMapper.selectById(1L)).thenReturn(workspaceEntity);
        WorkspaceMemberEntity member = new WorkspaceMemberEntity();
        member.setRole("owner");
        Mockito.when(workspaceMemberMapper.selectOne(Mockito.any(LambdaQueryWrapper.class))).thenReturn(member);
        Mockito.when(documentMapper.selectCount(Mockito.any(LambdaQueryWrapper.class))).thenReturn(1L, 0L);

        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setTitle("文档A");
        request.setContent("content");
        documentService.createDocument(1L, 1L, request);

        ArgumentCaptor<DocumentEntity> captor = ArgumentCaptor.forClass(DocumentEntity.class);
        Mockito.verify(documentMapper, Mockito.times(1)).insert(captor.capture());
        Assertions.assertEquals("文档A (2)", captor.getValue().getTitle());
    }

    /**
     * 测试：上传超出大小限制会被拒绝。
     */
    @Test
    void shouldThrowWhenUploadFileTooLarge() {
        ReflectionTestUtils.setField(documentService, "maxUploadSizeBytes", 1L);
        WorkspaceEntity workspaceEntity = new WorkspaceEntity();
        workspaceEntity.setId(1L);
        Mockito.when(workspaceMapper.selectById(1L)).thenReturn(workspaceEntity);
        WorkspaceMemberEntity member = new WorkspaceMemberEntity();
        member.setRole("owner");
        Mockito.when(workspaceMemberMapper.selectOne(Mockito.any(LambdaQueryWrapper.class))).thenReturn(member);
        MockMultipartFile file = new MockMultipartFile("file", "demo.md", "text/markdown", "AB".getBytes());

        Assertions.assertThrows(BizException.class, () -> documentService.uploadAndParseDocument(1L, 1L, file));
    }

    /**
     * 测试：删除文档会删除版本和向量分块。
     */
    @Test
    void shouldDeleteDocumentAndRelatedData() {
        WorkspaceEntity workspaceEntity = new WorkspaceEntity();
        workspaceEntity.setId(1L);
        Mockito.when(workspaceMapper.selectById(1L)).thenReturn(workspaceEntity);
        WorkspaceMemberEntity member = new WorkspaceMemberEntity();
        member.setRole("owner");
        Mockito.when(workspaceMemberMapper.selectOne(Mockito.any(LambdaQueryWrapper.class))).thenReturn(member);
        DocumentEntity document = new DocumentEntity();
        document.setId(300L);
        document.setWorkspaceId(1L);
        Mockito.when(documentMapper.selectById(300L)).thenReturn(document);

        documentService.deleteDocument(1L, 1L, 300L);

        Mockito.verify(chunkMapper, Mockito.times(1)).deleteByDocumentId(300L);
        Mockito.verify(documentVersionMapper, Mockito.times(1)).delete(Mockito.any(LambdaQueryWrapper.class));
        Mockito.verify(documentMapper, Mockito.times(1)).deleteById(300L);
    }
}

