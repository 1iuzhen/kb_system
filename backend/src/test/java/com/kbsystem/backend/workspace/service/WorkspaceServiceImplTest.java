package com.kbsystem.backend.workspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kbsystem.backend.auth.entity.SysUserEntity;
import com.kbsystem.backend.auth.mapper.SysUserMapper;
import com.kbsystem.backend.common.exception.BizException;
import com.kbsystem.backend.doc.entity.DocumentEntity;
import com.kbsystem.backend.doc.entity.DocumentVersionEntity;
import com.kbsystem.backend.doc.mapper.ChunkMapper;
import com.kbsystem.backend.doc.mapper.DocumentMapper;
import com.kbsystem.backend.doc.mapper.DocumentVersionMapper;
import com.kbsystem.backend.workspace.entity.WorkspaceEntity;
import com.kbsystem.backend.workspace.entity.WorkspaceMemberEntity;
import com.kbsystem.backend.workspace.mapper.WorkspaceMapper;
import com.kbsystem.backend.workspace.mapper.WorkspaceMemberMapper;
import com.kbsystem.backend.workspace.model.CreateWorkspaceRequest;
import com.kbsystem.backend.workspace.model.UpdateWorkspaceRequest;
import com.kbsystem.backend.workspace.model.WorkspaceVO;
import com.kbsystem.backend.workspace.service.impl.WorkspaceServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

/**
 * 知识库服务单元测试。
 */
class WorkspaceServiceImplTest {

    /**
     * 被测对象。
     */
    private WorkspaceServiceImpl workspaceService;

    /**
     * mapper mocks。
     */
    private WorkspaceMapper workspaceMapper;
    private WorkspaceMemberMapper workspaceMemberMapper;
    private SysUserMapper sysUserMapper;
    private DocumentMapper documentMapper;
    private DocumentVersionMapper documentVersionMapper;
    private ChunkMapper chunkMapper;

    /**
     * 初始化 mock 依赖。
     */
    @BeforeEach
    void setUp() {
        workspaceMapper = Mockito.mock(WorkspaceMapper.class);
        workspaceMemberMapper = Mockito.mock(WorkspaceMemberMapper.class);
        sysUserMapper = Mockito.mock(SysUserMapper.class);
        documentMapper = Mockito.mock(DocumentMapper.class);
        documentVersionMapper = Mockito.mock(DocumentVersionMapper.class);
        chunkMapper = Mockito.mock(ChunkMapper.class);
        workspaceService = new WorkspaceServiceImpl(
                workspaceMapper,
                workspaceMemberMapper,
                sysUserMapper,
                documentMapper,
                documentVersionMapper,
                chunkMapper
        );
    }

    /**
     * 测试：创建后 owner 可在列表中看到自己的知识库。
     */
    @Test
    void shouldCreateAndListWorkspace() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest();
        request.setName("测试库");
        Mockito.doAnswer(invocation -> {
            WorkspaceEntity entity = invocation.getArgument(0);
            entity.setId(1001L);
            return 1;
        }).when(workspaceMapper).insert(Mockito.any(WorkspaceEntity.class));
        WorkspaceMemberEntity member = new WorkspaceMemberEntity();
        member.setWorkspaceId(1001L);
        member.setUserId(1L);
        member.setRole("owner");
        Mockito.when(workspaceMemberMapper.selectList(Mockito.any(LambdaQueryWrapper.class))).thenReturn(List.of(member));
        WorkspaceEntity workspaceEntity = new WorkspaceEntity();
        workspaceEntity.setId(1001L);
        workspaceEntity.setName("测试库");
        Mockito.when(workspaceMapper.selectBatchIds(Mockito.anyList())).thenReturn(List.of(workspaceEntity));

        WorkspaceVO created = workspaceService.createWorkspace(1L, request);
        List<WorkspaceVO> list = workspaceService.listMyWorkspaces(1L);

        Assertions.assertNotNull(created.getId());
        Assertions.assertTrue(list.stream().anyMatch(it -> it.getId().equals(created.getId())));
    }

    /**
     * 测试：viewer 不可修改知识库名称。
     */
    @Test
    void shouldThrowWhenViewerUpdateWorkspace() {
        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setId(2001L);
        workspace.setName("权限测试库");
        Mockito.when(workspaceMapper.selectById(2001L)).thenReturn(workspace);
        WorkspaceMemberEntity viewerMember = new WorkspaceMemberEntity();
        viewerMember.setWorkspaceId(2001L);
        viewerMember.setUserId(3L);
        viewerMember.setRole("viewer");
        Mockito.when(workspaceMemberMapper.selectOne(Mockito.any(LambdaQueryWrapper.class))).thenReturn(viewerMember);

        UpdateWorkspaceRequest updateRequest = new UpdateWorkspaceRequest();
        updateRequest.setName("尝试修改");
        Assertions.assertThrows(BizException.class, () -> workspaceService.updateWorkspace(3L, 2001L, updateRequest));
    }

    /**
     * 测试：删除知识库会级联删除文档、版本与向量分块。
     */
    @Test
    void shouldCascadeDeleteDocumentsAndVectorsWhenDeleteWorkspace() {
        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setId(3001L);
        Mockito.when(workspaceMapper.selectById(3001L)).thenReturn(workspace);
        WorkspaceMemberEntity ownerMember = new WorkspaceMemberEntity();
        ownerMember.setWorkspaceId(3001L);
        ownerMember.setUserId(1L);
        ownerMember.setRole("owner");
        Mockito.when(workspaceMemberMapper.selectOne(Mockito.any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        DocumentEntity documentA = new DocumentEntity();
        documentA.setId(11L);
        documentA.setWorkspaceId(3001L);
        DocumentEntity documentB = new DocumentEntity();
        documentB.setId(12L);
        documentB.setWorkspaceId(3001L);
        Mockito.when(documentMapper.selectList(Mockito.any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(documentA, documentB));

        workspaceService.deleteWorkspace(1L, 3001L);

        Mockito.verify(chunkMapper, Mockito.times(1)).deleteByDocumentId(11L);
        Mockito.verify(chunkMapper, Mockito.times(1)).deleteByDocumentId(12L);
        Mockito.verify(documentVersionMapper, Mockito.times(1)).delete(Mockito.any(LambdaQueryWrapper.class));
        Mockito.verify(documentMapper, Mockito.times(1)).delete(Mockito.any(LambdaQueryWrapper.class));
        Mockito.verify(workspaceMemberMapper, Mockito.times(1)).delete(Mockito.any(LambdaQueryWrapper.class));
        Mockito.verify(workspaceMapper, Mockito.times(1)).deleteById(3001L);
    }
}

