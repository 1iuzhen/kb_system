import { create } from "zustand";
import { askRagSseApi } from "../api/chatApi";
import type { ChatMessage, RagSource } from "../types";

/**
 * 会话持久化 key 前缀。
 */
const CHAT_STORAGE_KEY_PREFIX = "kb_chat_workspace_";

/**
 * 单知识库会话快照。
 */
interface WorkspaceChatSnapshot {
  messages: ChatMessage[];
  sources: RagSource[];
}

/**
 * 对话状态定义。
 */
interface ChatState {
  messages: ChatMessage[];
  sources: RagSource[];
  streaming: boolean;
  activeWorkspaceId: number | null;
  loadWorkspaceConversation: (workspaceId: number) => void;
  ask: (workspaceId: number, question: string) => Promise<void>;
  clear: (workspaceId: number) => void;
}

/**
 * 对话状态仓库。
 */
export const useChatStore = create<ChatState>((set, get) => ({
  messages: [],
  sources: [],
  streaming: false,
  activeWorkspaceId: null,
  loadWorkspaceConversation: (workspaceId: number) => {
    if (!Number.isFinite(workspaceId)) {
      return;
    }
    const storageKey = `${CHAT_STORAGE_KEY_PREFIX}${workspaceId}`;
    const raw = window.localStorage.getItem(storageKey);
    if (!raw) {
      set({ activeWorkspaceId: workspaceId, messages: [], sources: [] });
      return;
    }
    try {
      const parsed = JSON.parse(raw) as WorkspaceChatSnapshot;
      set({
        activeWorkspaceId: workspaceId,
        messages: Array.isArray(parsed.messages) ? parsed.messages : [],
        sources: Array.isArray(parsed.sources) ? parsed.sources : []
      });
    } catch {
      set({ activeWorkspaceId: workspaceId, messages: [], sources: [] });
    }
  },
  ask: async (workspaceId: number, question: string) => {
    const trimmed = question.trim();
    if (!trimmed) {
      return;
    }
    set((state) => ({
      streaming: true,
      sources: [],
      messages: [...state.messages, { role: "user", content: trimmed }, { role: "assistant", content: "" }]
    }));
    await askRagSseApi(workspaceId, trimmed, {
      onText: (content) => {
        set((state) => {
          const next = [...state.messages];
          const last = next[next.length - 1];
          if (last && last.role === "assistant") {
            last.content = `${last.content}${content}`;
          }
          persistWorkspaceConversation(workspaceId, next, state.sources);
          return { messages: next };
        });
      },
      onSources: (sources) => {
        persistWorkspaceConversation(workspaceId, get().messages, sources);
        set({ sources });
      },
      onDone: () => {
        set({ streaming: false });
      },
      onError: (message) => {
        set((state) => {
          const next = [...state.messages];
          const last = next[next.length - 1];
          if (last && last.role === "assistant" && !last.content) {
            last.content = `调用失败：${message}`;
          }
          persistWorkspaceConversation(workspaceId, next, state.sources);
          return { messages: next, streaming: false };
        });
      }
    });
    persistWorkspaceConversation(workspaceId, get().messages, get().sources);
    if (get().streaming) {
      set({ streaming: false });
    }
  },
  clear: (workspaceId: number) => {
    window.localStorage.removeItem(`${CHAT_STORAGE_KEY_PREFIX}${workspaceId}`);
    set({ messages: [], sources: [], streaming: false });
  }
}));

/**
 * 持久化指定知识库会话。
 *
 * @param workspaceId 知识库 ID
 * @param messages 消息列表
 * @param sources 引用列表
 */
function persistWorkspaceConversation(workspaceId: number, messages: ChatMessage[], sources: RagSource[]): void {
  if (!Number.isFinite(workspaceId)) {
    return;
  }
  const storageKey = `${CHAT_STORAGE_KEY_PREFIX}${workspaceId}`;
  const snapshot: WorkspaceChatSnapshot = { messages, sources };
  window.localStorage.setItem(storageKey, JSON.stringify(snapshot));
}

