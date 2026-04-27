package com.kbsystem.backend.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbsystem.backend.chat.service.RagChatService;
import com.kbsystem.backend.common.handler.GlobalExceptionHandler;
import com.kbsystem.backend.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RAG 控制器测试。
 */
class RagChatControllerTest {

    /**
     * MockMvc。
     */
    private MockMvc mockMvc;

    /**
     * 服务 mock。
     */
    private RagChatService ragChatService;

    /**
     * JSON 工具。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化测试环境。
     */
    @BeforeEach
    void setUp() {
        ragChatService = Mockito.mock(RagChatService.class);
        RagChatController controller = new RagChatController(ragChatService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        UserContext.set(1L, "admin");
    }

    /**
     * 测试：SSE 提问接口可用。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldAskBySseSuccessfully() throws Exception {
        Mockito.when(ragChatService.ask(Mockito.eq(1L), Mockito.eq(1L), Mockito.eq("RAG 是什么")))
                .thenReturn(new SseEmitter());

        mockMvc.perform(post("/api/v1/workspaces/1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("question", "RAG 是什么"))))
                .andExpect(status().isOk());
    }
}

