import { useAuthStore } from "../../auth/store/useAuthStore";
import type { RagSource } from "../types";

/**
 * SSE 事件处理器。
 */
export interface RagSseHandlers {
  onText: (content: string) => void;
  onSources: (sources: RagSource[]) => void;
  onDone: () => void;
  onError: (message: string) => void;
}

/**
 * 发起 RAG SSE 对话请求。
 *
 * @param workspaceId 知识库 ID
 * @param question 问题
 * @param handlers 事件回调
 */
export async function askRagSseApi(workspaceId: number, question: string, handlers: RagSseHandlers): Promise<void> {
  const token = useAuthStore.getState().accessToken;
  const response = await fetch(`/api/v1/workspaces/${workspaceId}/chat/ask`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify({ question })
  });
  if (!response.ok || !response.body) {
    handlers.onError(`请求失败：HTTP ${response.status}`);
    return;
  }
  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";
  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true });
    const blocks = buffer.split("\n\n");
    buffer = blocks.pop() ?? "";
    for (const block of blocks) {
      const dataLine = block
        .split("\n")
        .find((line) => line.startsWith("data:"));
      if (!dataLine) {
        continue;
      }
      const jsonText = dataLine.substring(5).trim();
      if (!jsonText) {
        continue;
      }
      try {
        const payload = JSON.parse(jsonText) as {
          type: string;
          content?: string;
          message?: string;
          sources?: RagSource[];
        };
        if (payload.type === "text" && payload.content) {
          handlers.onText(payload.content);
        } else if ((payload.type === "ref" || payload.type === "done") && payload.sources) {
          handlers.onSources(payload.sources);
        } else if (payload.type === "done") {
          handlers.onDone();
        } else if (payload.type === "error") {
          handlers.onError(payload.message ?? "SSE 调用失败");
        }
      } catch {
        // 忽略非 JSON 数据片段，避免中断整个流。
      }
    }
  }
  handlers.onDone();
}

