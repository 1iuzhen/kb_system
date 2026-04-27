import { create } from "zustand";
import {
  createWorkspaceApi,
  deleteWorkspaceApi,
  listWorkspaceApi,
  updateWorkspaceApi
} from "../api/workspaceApi";
import type { Workspace } from "../types";

/**
 * 知识库状态定义。
 */
interface WorkspaceState {
  workspaces: Workspace[];
  loading: boolean;
  loadWorkspaces: () => Promise<void>;
  createWorkspace: (name: string) => Promise<void>;
  updateWorkspace: (workspaceId: number, name: string) => Promise<void>;
  deleteWorkspace: (workspaceId: number) => Promise<void>;
}

/**
 * 知识库状态仓库。
 */
export const useWorkspaceStore = create<WorkspaceState>((set, get) => ({
  workspaces: [],
  loading: false,
  loadWorkspaces: async () => {
    set({ loading: true });
    try {
      const list = await listWorkspaceApi();
      set({ workspaces: list });
    } finally {
      set({ loading: false });
    }
  },
  createWorkspace: async (name: string) => {
    await createWorkspaceApi(name);
    await get().loadWorkspaces();
  },
  updateWorkspace: async (workspaceId: number, name: string) => {
    await updateWorkspaceApi(workspaceId, name);
    await get().loadWorkspaces();
  },
  deleteWorkspace: async (workspaceId: number) => {
    await deleteWorkspaceApi(workspaceId);
    await get().loadWorkspaces();
  }
}));

