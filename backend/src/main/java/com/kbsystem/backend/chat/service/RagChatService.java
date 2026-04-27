package com.kbsystem.backend.chat.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * RAG 对话服务。
 */
public interface RagChatService {

    /**
     * 在指定知识库内进行 RAG 问答，并通过 SSE 推送结果。
     *
     * @param userId 用户 ID
     * @param workspaceId 知识库 ID
     * @param question 用户问题
     * @return SSE 发送器
     */
    SseEmitter ask(Long userId, Long workspaceId, String question);
}

