/**
 * RAG 引用来源。
 */
export interface RagSource {
  chunkId: number;
  docId: number;
  docTitle: string;
  snippet: string;
  score: number;
}

/**
 * 聊天消息。
 */
export interface ChatMessage {
  role: "user" | "assistant";
  content: string;
}

