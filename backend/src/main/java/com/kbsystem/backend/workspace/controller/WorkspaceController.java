package com.kbsystem.backend.workspace.controller;

import com.kbsystem.backend.common.Result;
import com.kbsystem.backend.security.UserContext;
import com.kbsystem.backend.workspace.model.*;
import com.kbsystem.backend.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库控制器。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
@Tag(name = "知识库模块")
public class WorkspaceController {

    /**
     * 知识库服务。
     */
    private final WorkspaceService workspaceService;

    /**
     * 创建知识库。
     *
     * @param request 请求参数
     * @return 创建后的知识库
     */
    @PostMapping
    @Operation(summary = "创建知识库")
    public Result<WorkspaceVO> createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        return Result.ok(workspaceService.createWorkspace(UserContext.getUserId(), request));
    }

    /**
     * 查询我的知识库列表。
     *
     * @return 知识库列表
     */
    @GetMapping
    @Operation(summary = "查询我的知识库")
    public Result<List<WorkspaceVO>> listMyWorkspaces() {
        return Result.ok(workspaceService.listMyWorkspaces(UserContext.getUserId()));
    }

    /**
     * 更新知识库名称。
     *
     * @param workspaceId 知识库 ID
     * @param request     请求参数
     * @return 空响应
     */
    @PutMapping("/{workspaceId}")
    @Operation(summary = "更新知识库名称")
    public Result<Void> updateWorkspace(@PathVariable Long workspaceId, @Valid @RequestBody UpdateWorkspaceRequest request) {
        workspaceService.updateWorkspace(UserContext.getUserId(), workspaceId, request);
        return Result.ok(null);
    }

    /**
     * 删除知识库。
     *
     * @param workspaceId 知识库 ID
     * @return 空响应
     */
    @DeleteMapping("/{workspaceId}")
    @Operation(summary = "删除知识库")
    public Result<Void> deleteWorkspace(@PathVariable Long workspaceId) {
        workspaceService.deleteWorkspace(UserContext.getUserId(), workspaceId);
        return Result.ok(null);
    }

    /**
     * 查询成员列表。
     *
     * @param workspaceId 知识库 ID
     * @return 成员列表
     */
    @GetMapping("/{workspaceId}/members")
    @Operation(summary = "查询成员列表")
    public Result<List<WorkspaceMemberVO>> listMembers(@PathVariable Long workspaceId) {
        return Result.ok(workspaceService.listMembers(UserContext.getUserId(), workspaceId));
    }

    /**
     * 新增或更新成员。
     *
     * @param workspaceId 知识库 ID
     * @param request     请求参数
     * @return 空响应
     */
    @PutMapping("/{workspaceId}/members")
    @Operation(summary = "新增或更新成员")
    public Result<Void> upsertMember(@PathVariable Long workspaceId, @Valid @RequestBody UpsertMemberRequest request) {
        workspaceService.upsertMember(UserContext.getUserId(), workspaceId, request);
        return Result.ok(null);
    }

    /**
     * 删除成员。
     *
     * @param workspaceId  知识库 ID
     * @param memberUserId 成员用户 ID
     * @return 空响应
     */
    @DeleteMapping("/{workspaceId}/members/{memberUserId}")
    @Operation(summary = "删除成员")
    public Result<Void> removeMember(@PathVariable Long workspaceId, @PathVariable Long memberUserId) {
        workspaceService.removeMember(UserContext.getUserId(), workspaceId, memberUserId);
        return Result.ok(null);
    }
}

