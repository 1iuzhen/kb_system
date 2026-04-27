package com.kbsystem.backend.workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbsystem.backend.common.handler.GlobalExceptionHandler;
import com.kbsystem.backend.security.UserContext;
import com.kbsystem.backend.workspace.model.WorkspaceVO;
import com.kbsystem.backend.workspace.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 知识库控制器单元测试。
 */
class WorkspaceControllerTest {

    /**
     * MockMvc。
     */
    private MockMvc mockMvc;

    /**
     * service mock。
     */
    private WorkspaceService workspaceService;

    /**
     * JSON 序列化工具。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化测试环境。
     */
    @BeforeEach
    void setUp() {
        workspaceService = Mockito.mock(WorkspaceService.class);
        WorkspaceController controller = new WorkspaceController(workspaceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        UserContext.set(1L, "admin");
    }

    /**
     * 测试：创建知识库返回成功。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldCreateWorkspaceSuccessfully() throws Exception {
        Mockito.when(workspaceService.createWorkspace(Mockito.eq(1L), Mockito.any()))
                .thenReturn(new WorkspaceVO(100L, "测试库", "owner"));

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBody("测试库"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("测试库"));
    }

    /**
     * 测试：查询列表返回当前用户可见知识库。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldListMyWorkspacesSuccessfully() throws Exception {
        Mockito.when(workspaceService.listMyWorkspaces(Mockito.eq(1L)))
                .thenReturn(List.of(new WorkspaceVO(100L, "测试库", "owner")));

        mockMvc.perform(get("/api/v1/workspaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].role").value("owner"));
    }

    /**
     * 创建请求体（测试内部使用）。
     */
    private record CreateBody(String name) {
    }
}

