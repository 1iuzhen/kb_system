package com.kbsystem.backend.workspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.kbsystem.backend.workspace.model.*;
import com.kbsystem.backend.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库服务实现。
 * 使用 MyBatis-Plus 操作 PostgreSQL，落地真实持久化。
 */
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    /**
     * 知识库 Mapper。
     */
    private final WorkspaceMapper workspaceMapper;

    /**
     * 成员 Mapper。
     */
    private final WorkspaceMemberMapper workspaceMemberMapper;

    /**
     * 用户 Mapper。
     */
    private final SysUserMapper sysUserMapper;

    /**
     * 文档 Mapper。
     */
    private final DocumentMapper documentMapper;

    /**
     * 文档版本 Mapper。
     */
    private final DocumentVersionMapper documentVersionMapper;

    /**
     * 文档向量分块 Mapper。
     */
    private final ChunkMapper chunkMapper;

    /**
     * 创建知识库并默认把创建者设为 owner。
     *
     * @param userId  当前用户 ID
     * @param request 请求参数
     * @return 知识库信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceVO createWorkspace(Long userId, CreateWorkspaceRequest request) {
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setName(request.getName());
        entity.setOwnerUserId(userId);
        workspaceMapper.insert(entity);

        WorkspaceMemberEntity owner = new WorkspaceMemberEntity();
        owner.setWorkspaceId(entity.getId());
        owner.setUserId(userId);
        owner.setRole("owner");
        workspaceMemberMapper.insert(owner);
        return new WorkspaceVO(entity.getId(), entity.getName(), "owner");
    }

    /**
     * 查询当前用户可见知识库。
     *
     * @param userId 当前用户 ID
     * @return 知识库列表
     */
    @Override
    public List<WorkspaceVO> listMyWorkspaces(Long userId) {
        List<WorkspaceMemberEntity> members = workspaceMemberMapper.selectList(new LambdaQueryWrapper<WorkspaceMemberEntity>()
                .eq(WorkspaceMemberEntity::getUserId, userId));
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, String> workspaceRoleMap = members.stream()
                .collect(Collectors.toMap(WorkspaceMemberEntity::getWorkspaceId, WorkspaceMemberEntity::getRole, (a, b) -> a));
        List<Long> workspaceIds = new ArrayList<>(workspaceRoleMap.keySet());
        return workspaceMapper.selectBatchIds(workspaceIds).stream()
                .map(workspace -> new WorkspaceVO(workspace.getId(), workspace.getName(), workspaceRoleMap.get(workspace.getId())))
                .sorted(Comparator.comparing(WorkspaceVO::getId))
                .collect(Collectors.toList());
    }

    /**
     * 更新知识库名称，仅 owner 或 editor 可操作。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param request     请求参数
     */
    @Override
    public void updateWorkspace(Long userId, Long workspaceId, UpdateWorkspaceRequest request) {
        WorkspaceEntity entity = requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner", "editor"));
        entity.setName(request.getName());
        workspaceMapper.updateById(entity);
    }

    /**
     * 删除知识库，仅 owner 可操作。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkspace(Long userId, Long workspaceId) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner"));
        // 先清理知识库下文档关联数据：向量分块 -> 版本 -> 文档主表。
        List<DocumentEntity> documents = documentMapper.selectList(new LambdaQueryWrapper<DocumentEntity>()
                .eq(DocumentEntity::getWorkspaceId, workspaceId));
        List<Long> documentIds = documents.stream().map(DocumentEntity::getId).toList();
        for (Long documentId : documentIds) {
            chunkMapper.deleteByDocumentId(documentId);
        }
        if (!documentIds.isEmpty()) {
            documentVersionMapper.delete(new LambdaQueryWrapper<DocumentVersionEntity>()
                    .in(DocumentVersionEntity::getDocumentId, documentIds));
        }
        documentMapper.delete(new LambdaQueryWrapper<DocumentEntity>()
                .eq(DocumentEntity::getWorkspaceId, workspaceId));
        // 最后删除成员与知识库。
        workspaceMapper.deleteById(workspaceId);
        workspaceMemberMapper.delete(new LambdaQueryWrapper<WorkspaceMemberEntity>()
                .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId));
    }

    /**
     * 查询成员列表，知识库成员可查看。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @return 成员列表
     */
    @Override
    public List<WorkspaceMemberVO> listMembers(Long userId, Long workspaceId) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner", "editor", "viewer"));
        List<WorkspaceMemberEntity> members = workspaceMemberMapper.selectList(new LambdaQueryWrapper<WorkspaceMemberEntity>()
                .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId));
        Set<Long> userIds = members.stream().map(WorkspaceMemberEntity::getUserId).collect(Collectors.toSet());
        Map<Long, String> usernameMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUserEntity::getId, SysUserEntity::getUsername, (a, b) -> a));
        return members.stream()
                .map(member -> new WorkspaceMemberVO(member.getUserId(),
                        usernameMap.getOrDefault(member.getUserId(), "user-" + member.getUserId()),
                        member.getRole()))
                .sorted(Comparator.comparing(WorkspaceMemberVO::getUserId))
                .collect(Collectors.toList());
    }

    /**
     * 新增或更新成员，仅 owner 可操作。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param request     请求参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsertMember(Long userId, Long workspaceId, UpsertMemberRequest request) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner"));
        if (!Set.of("owner", "editor", "viewer").contains(request.getRole())) {
            throw new BizException(40002, "角色非法");
        }
        WorkspaceMemberEntity existing = workspaceMemberMapper.selectOne(new LambdaQueryWrapper<WorkspaceMemberEntity>()
                .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId)
                .eq(WorkspaceMemberEntity::getUserId, request.getUserId())
                .last("LIMIT 1"));
        if (existing != null) {
            existing.setRole(request.getRole());
            workspaceMemberMapper.updateById(existing);
            return;
        }
        // 校验成员用户是否存在，避免产生无效用户关联。
        SysUserEntity targetUser = sysUserMapper.selectById(request.getUserId());
        if (targetUser == null) {
            throw new BizException(40402, "成员用户不存在");
        }
        WorkspaceMemberEntity member = new WorkspaceMemberEntity();
        member.setWorkspaceId(workspaceId);
        member.setUserId(request.getUserId());
        member.setRole(request.getRole());
        workspaceMemberMapper.insert(member);
    }

    /**
     * 删除成员，仅 owner 可操作。
     *
     * @param userId       当前用户 ID
     * @param workspaceId  知识库 ID
     * @param memberUserId 成员用户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long userId, Long workspaceId, Long memberUserId) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner"));
        // 禁止 owner 把自己移出，防止知识库无管理员。
        if (userId.equals(memberUserId)) {
            throw new BizException(40003, "owner 不能移除自己");
        }
        workspaceMemberMapper.delete(new LambdaQueryWrapper<WorkspaceMemberEntity>()
                .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId)
                .eq(WorkspaceMemberEntity::getUserId, memberUserId));
    }

    /**
     * 校验知识库存在性。
     *
     * @param workspaceId 知识库 ID
     * @return 知识库实体
     */
    private WorkspaceEntity requireWorkspace(Long workspaceId) {
        WorkspaceEntity workspace = workspaceMapper.selectById(workspaceId);
        if (workspace == null) {
            throw new BizException(40401, "知识库不存在");
        }
        return workspace;
    }

    /**
     * 校验用户角色是否满足操作要求。
     *
     * @param userId        用户 ID
     * @param workspaceId   知识库 ID
     * @param allowedRoles  允许角色集合
     */
    private void requireRole(Long userId, Long workspaceId, Set<String> allowedRoles) {
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
    }
}

