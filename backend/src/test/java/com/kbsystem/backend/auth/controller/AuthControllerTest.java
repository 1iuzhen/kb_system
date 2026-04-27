package com.kbsystem.backend.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbsystem.backend.auth.model.LoginResponse;
import com.kbsystem.backend.auth.model.TokenPair;
import com.kbsystem.backend.auth.model.UserInfo;
import com.kbsystem.backend.auth.service.AuthService;
import com.kbsystem.backend.common.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证控制器单元测试。
 */
class AuthControllerTest {

    /**
     * MockMvc。
     */
    private MockMvc mockMvc;

    /**
     * auth service mock。
     */
    private AuthService authService;

    /**
     * JSON 序列化工具。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化 MockMvc。
     */
    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 测试：登录成功返回 token 和用户信息。
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldReturnTokensWhenLoginSuccess() throws Exception {
        Mockito.when(authService.login(Mockito.any()))
                .thenReturn(new LoginResponse("access-token", "refresh-token", new UserInfo(1L, "admin")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginBody("admin", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    /**
     * 测试：刷新 token 成功。
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldReturnNewTokensWhenRefreshSuccess() throws Exception {
        Mockito.when(authService.refreshToken(Mockito.anyString()))
                .thenReturn(new TokenPair("new-access", "new-refresh"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshBody("old-refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
    }

    /**
     * 登录请求体（测试内部使用）。
     */
    private record LoginBody(String username, String password) {
    }

    /**
     * 刷新请求体（测试内部使用）。
     */
    private record RefreshBody(String refreshToken) {
    }
}

