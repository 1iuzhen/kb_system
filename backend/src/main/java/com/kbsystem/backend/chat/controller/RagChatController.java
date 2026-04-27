package com.kbsystem.backend.chat.controller;

import com.kbsystem.backend.audit.annotation.AuditLog;
import com.kbsystem.backend.chat.model.RagAskRequest;
import com.kbsystem.backend.chat.service.RagChatService;
import com.kbsystem.backend.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * RAG 对话控制器。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceId}/chat")
@Tag(name = "RAG对话模块")
public class RagChatController {

    /**
     * RAG 服务。
     */
    private final RagChatService ragChatService;

    /**
     * 进行知识库内 RAG 提问（SSE）。
     *
     * @param workspaceId 知识库 ID
     * @param request 提问请求
     * @return SSE 流
     */
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "RAG提问（SSE）")
    @AuditLog(action = "query_rag", targetType = "workspace", targetIdArgIndex = 0)
    public SseEmitter ask(@PathVariable Long workspaceId, @Valid @RequestBody RagAskRequest request) {
        return ragChatService.ask(UserContext.getUserId(), workspaceId, request.getQuestion());
    }
}

