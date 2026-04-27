package com.kbsystem.backend.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbsystem.backend.ai.service.EmbeddingService;
import com.kbsystem.backend.ai.service.LlmService;
import com.kbsystem.backend.chat.service.impl.RagChatServiceImpl;
import com.kbsystem.backend.doc.mapper.ChunkMapper;
import com.kbsystem.backend.workspace.entity.WorkspaceEntity;
import com.kbsystem.backend.workspace.entity.WorkspaceMemberEntity;
import com.kbsystem.backend.workspace.mapper.WorkspaceMapper;
import com.kbsystem.backend.workspace.mapper.WorkspaceMemberMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * RAG 对话服务测试。
 */
class RagChatServiceImplTest {

    /**
     * 被测服务。
     */
    private RagChatServiceImpl ragChatService;

    /**
     * 依赖 mocks。
     */
    private EmbeddingService embeddingService;
    private LlmService llmService;
    private ChunkMapper chunkMapper;
    private WorkspaceMapper workspaceMapper;
    private WorkspaceMemberMapper workspaceMemberMapper;

    /**
     * 初始化。
     */
    @BeforeEach
    void setUp() {
        embeddingService = Mockito.mock(EmbeddingService.class);
        llmService = Mockito.mock(LlmService.class);
        chunkMapper = Mockito.mock(ChunkMapper.class);
        workspaceMapper = Mockito.mock(WorkspaceMapper.class);
        workspaceMemberMapper = Mockito.mock(WorkspaceMemberMapper.class);
        ragChatService = new RagChatServiceImpl(
                embeddingService,
                llmService,
                chunkMapper,
                workspaceMapper,
                workspaceMemberMapper,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(ragChatService, "embeddingDim", 1536);
    }

    /**
     * 测试：召回成功时返回 SSE。
     */
    @Test
    void shouldReturnEmitterWhenAskSuccess() {
        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setId(1L);
        Mockito.when(workspaceMapper.selectById(1L)).thenReturn(workspace);
        WorkspaceMemberEntity member = new WorkspaceMemberEntity();
        member.setWorkspaceId(1L);
        member.setUserId(1L);
        member.setRole("owner");
        Mockito.when(workspaceMemberMapper.selectOne(Mockito.any(LambdaQueryWrapper.class))).thenReturn(member);
        Mockito.when(embeddingService.embed("什么是RAG"))
                .thenReturn(java.util.Collections.nCopies(1536, 0.1D));
        ChunkMapper.ChunkSearchRow row = new ChunkMapper.ChunkSearchRow();
        row.setChunkId(101L);
        row.setDocumentId(11L);
        row.setDocTitle("文档A");
        row.setContent("RAG 是检索增强生成。");
        row.setScore(0.98D);
        Mockito.when(chunkMapper.searchTopKByWorkspace(Mockito.eq(1L), Mockito.anyString(), Mockito.eq(6)))
                .thenReturn(List.of(row));
        Mockito.when(llmService.generate(Mockito.anyString(), Mockito.anyString())).thenReturn("RAG 是检索增强生成。");

        SseEmitter emitter = ragChatService.ask(1L, 1L, "什么是RAG");

        Assertions.assertNotNull(emitter);
        Mockito.verify(llmService, Mockito.times(1)).generate(Mockito.anyString(), Mockito.anyString());
    }
}

