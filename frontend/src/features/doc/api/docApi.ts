import { http, type ApiResult } from "../../../shared/api/http";
import type { DocumentDetail, DocumentItem, DocumentVersion, DocumentVersionDetail } from "../types";

/**
 * 创建文档请求参数。
 */
export interface CreateDocumentRequest {
  title: string;
  content?: string;
}

/**
 * 保存文档请求参数。
 */
export interface SaveDocumentRequest {
  title: string;
  content: string;
  baseVersion: number;
}

/**
 * 创建文档。
 *
 * @param workspaceId 知识库 ID
 * @param payload 请求参数
 * @returns 文档信息
 */
export async function createDocumentApi(workspaceId: number, payload: CreateDocumentRequest): Promise<DocumentItem> {
  const resp = await http.post<ApiResult<DocumentItem>>(`/workspaces/${workspaceId}/documents`, payload);
  return resp.data.data;
}

/**
 * 上传并解析文档文件（支持 md/pdf/docx）。
 *
 * @param workspaceId 知识库 ID
 * @param file 上传文件
 * @returns 文档信息
 */
export async function uploadDocumentApi(workspaceId: number, file: File): Promise<DocumentItem> {
  const formData = new FormData();
  formData.append("file", file);
  const resp = await http.post<ApiResult<DocumentItem>>(`/workspaces/${workspaceId}/documents/upload`, formData, {
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });
  return resp.data.data;
}

/**
 * 重试解析上传文档。
 *
 * @param workspaceId 知识库 ID
 * @param documentId 文档 ID
 * @returns 最新文档信息
 */
export async function retryParseDocumentApi(workspaceId: number, documentId: number): Promise<DocumentItem> {
  const resp = await http.post<ApiResult<DocumentItem>>(`/workspaces/${workspaceId}/documents/${documentId}/retry-parse`);
  return resp.data.data;
}

/**
 * 手动提取文档到向量知识库。
 *
 * @param workspaceId 知识库 ID
 * @param documentId 文档 ID
 * @returns 最新文档信息
 */
export async function extractVectorApi(workspaceId: number, documentId: number): Promise<DocumentItem> {
  const resp = await http.post<ApiResult<DocumentItem>>(`/workspaces/${workspaceId}/documents/${documentId}/extract-vector`);
  return resp.data.data;
}

/**
 * 查询文档列表。
 *
 * @param workspaceId 知识库 ID
 * @returns 列表
 */
export async function listDocumentsApi(workspaceId: number): Promise<DocumentItem[]> {
  const resp = await http.get<ApiResult<DocumentItem[]>>(`/workspaces/${workspaceId}/documents`);
  return resp.data.data;
}

/**
 * 查询文档详情。
 *
 * @param workspaceId 知识库 ID
 * @param documentId 文档 ID
 * @returns 详情
 */
export async function getDocumentApi(workspaceId: number, documentId: number): Promise<DocumentDetail> {
  const resp = await http.get<ApiResult<DocumentDetail>>(`/workspaces/${workspaceId}/documents/${documentId}`);
  return resp.data.data;
}

/**
 * 保存文档。
 *
 * @param workspaceId 知识库 ID
 * @param documentId 文档 ID
 * @param payload 请求参数
 */
export async function saveDocumentApi(workspaceId: number, documentId: number, payload: SaveDocumentRequest): Promise<void> {
  await http.post<ApiResult<unknown>>(`/workspaces/${workspaceId}/documents/${documentId}/save`, payload);
}

/**
 * 删除文档。
 *
 * @param workspaceId 知识库 ID
 * @param documentId 文档 ID
 */
export async function deleteDocumentApi(workspaceId: number, documentId: number): Promise<void> {
  await http.post<ApiResult<unknown>>(`/workspaces/${workspaceId}/documents/${documentId}/delete`);
}

/**
 * 查询版本列表。
 *
 * @param workspaceId 知识库 ID
 * @param documentId 文档 ID
 * @returns 版本列表
 */
export async function listDocumentVersionsApi(workspaceId: number, documentId: number): Promise<DocumentVersion[]> {
  const resp = await http.get<ApiResult<DocumentVersion[]>>(`/workspaces/${workspaceId}/documents/${documentId}/versions`);
  return resp.data.data;
}

/**
 * 回滚到指定版本。
 *
 * @param workspaceId 知识库 ID
 * @param documentId 文档 ID
 * @param versionNo 版本号
 */
export async function rollbackVersionApi(workspaceId: number, documentId: number, versionNo: number): Promise<void> {
  await http.post<ApiResult<unknown>>(`/workspaces/${workspaceId}/documents/${documentId}/rollback/${versionNo}`);
}

/**
 * 查询指定版本详情。
 *
 * @param workspaceId 知识库 ID
 * @param documentId 文档 ID
 * @param versionNo 版本号
 * @returns 版本详情
 */
export async function getVersionDetailApi(
  workspaceId: number,
  documentId: number,
  versionNo: number
): Promise<DocumentVersionDetail> {
  const resp = await http.get<ApiResult<DocumentVersionDetail>>(
    `/workspaces/${workspaceId}/documents/${documentId}/versions/${versionNo}`
  );
  return resp.data.data;
}

