/**
 * 文档信息。
 */
export interface DocumentItem {
  id: number;
  workspaceId: number;
  title: string;
  status: string;
  indexStatus?: string;
  indexedVersionNo?: number;
  indexErrorMsg?: string | null;
  parseErrorMsg?: string | null;
  latestVersionNo: number;
}

/**
 * 文档详情。
 */
export interface DocumentDetail {
  id: number;
  workspaceId: number;
  title: string;
  content: string;
  latestVersionNo: number;
  indexStatus?: string;
  indexedVersionNo?: number;
  indexErrorMsg?: string | null;
  status?: string;
  parseErrorMsg?: string | null;
  role: "owner" | "editor" | "viewer";
}

/**
 * 文档版本。
 */
export interface DocumentVersion {
  id: number;
  versionNo: number;
  status: string;
  titleSnapshot: string;
  saveUsername: string;
  createTime: string;
}

/**
 * 文档版本详情。
 */
export interface DocumentVersionDetail {
  id: number;
  documentId: number;
  versionNo: number;
  status: string;
  titleSnapshot: string;
  content: string;
  saveUsername: string;
  createTime: string;
}

