package com.kbsystem.backend.workspace.service;

import com.kbsystem.backend.workspace.model.*;

import java.util.List;

/**
 * 知识库服务接口。
 */
public interface WorkspaceService {

    /**
     * 创建知识库。
     *
     * @param userId  当前用户 ID
     * @param request 请求参数
     * @return 知识库信息
     */
    WorkspaceVO createWorkspace(Long userId, CreateWorkspaceRequest request);

    /**
     * 查询当前用户可见知识库。
     *
     * @param userId 当前用户 ID
     * @return 知识库列表
     */
    List<WorkspaceVO> listMyWorkspaces(Long userId);

    /**
     * 更新知识库名称。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param request     请求参数
     */
    void updateWorkspace(Long userId, Long workspaceId, UpdateWorkspaceRequest request);

    /**
     * 删除知识库。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     */
    void deleteWorkspace(Long userId, Long workspaceId);

    /**
     * 查询知识库成员列表。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @return 成员列表
     */
    List<WorkspaceMemberVO> listMembers(Long userId, Long workspaceId);

    /**
     * 新增或更新成员。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param request     请求参数
     */
    void upsertMember(Long userId, Long workspaceId, UpsertMemberRequest request);

    /**
     * 删除成员。
     *
     * @param userId      当前用户 ID
     * @param workspaceId 知识库 ID
     * @param memberUserId 成员用户 ID
     */
    void removeMember(Long userId, Long workspaceId, Long memberUserId);
}

