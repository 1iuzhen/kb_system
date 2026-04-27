import { http, type ApiResult } from "../../../shared/api/http";
import type { Workspace, WorkspaceMember } from "../types";

/**
 * 创建知识库。
 *
 * @param name 知识库名称
 * @returns 创建结果
 */
export async function createWorkspaceApi(name: string): Promise<Workspace> {
  const resp = await http.post<ApiResult<Workspace>>("/workspaces", { name });
  return resp.data.data;
}

/**
 * 查询我的知识库列表。
 *
 * @returns 列表
 */
export async function listWorkspaceApi(): Promise<Workspace[]> {
  const resp = await http.get<ApiResult<Workspace[]>>("/workspaces");
  return resp.data.data;
}

/**
 * 更新知识库名称。
 *
 * @param workspaceId 知识库 ID
 * @param name 新名称
 */
export async function updateWorkspaceApi(workspaceId: number, name: string): Promise<void> {
  await http.put<ApiResult<null>>(`/workspaces/${workspaceId}`, { name });
}

/**
 * 删除知识库。
 *
 * @param workspaceId 知识库 ID
 */
export async function deleteWorkspaceApi(workspaceId: number): Promise<void> {
  await http.delete<ApiResult<null>>(`/workspaces/${workspaceId}`);
}

/**
 * 查询成员列表。
 *
 * @param workspaceId 知识库 ID
 * @returns 成员列表
 */
export async function listMembersApi(workspaceId: number): Promise<WorkspaceMember[]> {
  const resp = await http.get<ApiResult<WorkspaceMember[]>>(`/workspaces/${workspaceId}/members`);
  return resp.data.data;
}

/**
 * 新增或更新成员。
 *
 * @param workspaceId 知识库 ID
 * @param userId 用户 ID
 * @param role 角色
 */
export async function upsertMemberApi(workspaceId: number, userId: number, role: WorkspaceMember["role"]): Promise<void> {
  await http.put<ApiResult<null>>(`/workspaces/${workspaceId}/members`, { userId, role });
}

/**
 * 删除成员。
 *
 * @param workspaceId 知识库 ID
 * @param memberUserId 成员用户 ID
 */
export async function removeMemberApi(workspaceId: number, memberUserId: number): Promise<void> {
  await http.delete<ApiResult<null>>(`/workspaces/${workspaceId}/members/${memberUserId}`);
}

