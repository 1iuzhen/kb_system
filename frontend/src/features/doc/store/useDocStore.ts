import { create } from "zustand";
import {
  createDocumentApi,
  deleteDocumentApi,
  extractVectorApi,
  getDocumentApi,
  getVersionDetailApi,
  listDocumentsApi,
  listDocumentVersionsApi,
  retryParseDocumentApi,
  rollbackVersionApi,
  saveDocumentApi,
  uploadDocumentApi
} from "../api/docApi";
import type { DocumentDetail, DocumentItem, DocumentVersion, DocumentVersionDetail } from "../types";

/**
 * 文档状态定义。
 */
interface DocState {
  documents: DocumentItem[];
  detail: DocumentDetail | null;
  versions: DocumentVersion[];
  versionDetail: DocumentVersionDetail | null;
  loading: boolean;
  loadDocuments: (workspaceId: number) => Promise<void>;
  createDocument: (workspaceId: number, title: string) => Promise<DocumentItem>;
  uploadDocument: (workspaceId: number, file: File) => Promise<DocumentItem>;
  retryParseDocument: (workspaceId: number, documentId: number) => Promise<DocumentItem>;
  extractVector: (workspaceId: number, documentId: number) => Promise<DocumentItem>;
  loadDetail: (workspaceId: number, documentId: number) => Promise<void>;
  saveDocument: (workspaceId: number, documentId: number, title: string, content: string, baseVersion: number) => Promise<void>;
  deleteDocument: (workspaceId: number, documentId: number) => Promise<void>;
  loadVersions: (workspaceId: number, documentId: number) => Promise<void>;
  loadVersionDetail: (workspaceId: number, documentId: number, versionNo: number) => Promise<void>;
  rollbackVersion: (workspaceId: number, documentId: number, versionNo: number) => Promise<void>;
}

/**
 * 文档状态仓库。
 */
export const useDocStore = create<DocState>((set, get) => ({
  documents: [],
  detail: null,
  versions: [],
  versionDetail: null,
  loading: false,
  loadDocuments: async (workspaceId: number) => {
    set({ loading: true });
    try {
      const data = await listDocumentsApi(workspaceId);
      set({ documents: data });
    } finally {
      set({ loading: false });
    }
  },
  createDocument: async (workspaceId: number, title: string) => {
    const created = await createDocumentApi(workspaceId, { title, content: "" });
    await get().loadDocuments(workspaceId);
    return created;
  },
  uploadDocument: async (workspaceId: number, file: File) => {
    const created = await uploadDocumentApi(workspaceId, file);
    await get().loadDocuments(workspaceId);
    return created;
  },
  retryParseDocument: async (workspaceId: number, documentId: number) => {
    const updated = await retryParseDocumentApi(workspaceId, documentId);
    await get().loadDocuments(workspaceId);
    return updated;
  },
  extractVector: async (workspaceId: number, documentId: number) => {
    const updated = await extractVectorApi(workspaceId, documentId);
    await get().loadDocuments(workspaceId);
    await get().loadDetail(workspaceId, documentId);
    return updated;
  },
  loadDetail: async (workspaceId: number, documentId: number) => {
    const detail = await getDocumentApi(workspaceId, documentId);
    set({ detail });
  },
  saveDocument: async (workspaceId: number, documentId: number, title: string, content: string, baseVersion: number) => {
    await saveDocumentApi(workspaceId, documentId, { title, content, baseVersion });
    await get().loadDetail(workspaceId, documentId);
    await get().loadDocuments(workspaceId);
  },
  deleteDocument: async (workspaceId: number, documentId: number) => {
    await deleteDocumentApi(workspaceId, documentId);
    set((state) => ({
      detail: state.detail?.id === documentId ? null : state.detail
    }));
    await get().loadDocuments(workspaceId);
  },
  loadVersions: async (workspaceId: number, documentId: number) => {
    const versions = await listDocumentVersionsApi(workspaceId, documentId);
    set({ versions });
  },
  loadVersionDetail: async (workspaceId: number, documentId: number, versionNo: number) => {
    const versionDetail = await getVersionDetailApi(workspaceId, documentId, versionNo);
    set({ versionDetail });
  },
  rollbackVersion: async (workspaceId: number, documentId: number, versionNo: number) => {
    await rollbackVersionApi(workspaceId, documentId, versionNo);
    await get().loadDetail(workspaceId, documentId);
    await get().loadVersions(workspaceId, documentId);
  }
}));

