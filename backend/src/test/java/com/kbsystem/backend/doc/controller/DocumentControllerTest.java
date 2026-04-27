package com.kbsystem.backend.doc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbsystem.backend.common.handler.GlobalExceptionHandler;
import com.kbsystem.backend.doc.model.DocumentVO;
import com.kbsystem.backend.doc.service.DocumentService;
import com.kbsystem.backend.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文档控制器测试。
 */
class DocumentControllerTest {

    /**
     * MockMvc。
     */
    private MockMvc mockMvc;

    /**
     * service mock。
     */
    private DocumentService documentService;

    /**
     * JSON 工具。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化测试环境。
     */
    @BeforeEach
    void setUp() {
        documentService = Mockito.mock(DocumentService.class);
        DocumentController controller = new DocumentController(documentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        UserContext.set(1L, "admin");
    }

    /**
     * 测试创建文档接口。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldCreateDocumentSuccessfully() throws Exception {
        Mockito.when(documentService.createDocument(Mockito.eq(1L), Mockito.eq(1L), Mockito.any()))
                .thenReturn(new DocumentVO(10L, 1L, "文档1", "draft", 1));

        mockMvc.perform(post("/api/v1/workspaces/1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBody("文档1", "# abc"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(10));
    }

    /**
     * 测试查询文档列表接口。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldListDocumentsSuccessfully() throws Exception {
        Mockito.when(documentService.listDocuments(Mockito.eq(1L), Mockito.eq(1L)))
                .thenReturn(List.of(new DocumentVO(10L, 1L, "文档1", "draft", 1)));

        mockMvc.perform(get("/api/v1/workspaces/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].title").value("文档1"));
    }

    /**
     * 测试上传并解析接口。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldUploadDocumentSuccessfully() throws Exception {
        Mockito.when(documentService.uploadAndParseDocument(Mockito.eq(1L), Mockito.eq(1L), Mockito.any()))
                .thenReturn(new DocumentVO(11L, 1L, "demo", "draft", 1));
        MockMultipartFile file = new MockMultipartFile("file", "demo.md", MediaType.TEXT_PLAIN_VALUE, "# hello".getBytes());

        mockMvc.perform(multipart("/api/v1/workspaces/1/documents/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(11));
    }

    /**
     * 测试重试解析接口。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldRetryParseSuccessfully() throws Exception {
        Mockito.when(documentService.retryParseDocument(Mockito.eq(1L), Mockito.eq(1L), Mockito.eq(11L)))
                .thenReturn(new DocumentVO(11L, 1L, "demo", "parsed", null, 2));

        mockMvc.perform(post("/api/v1/workspaces/1/documents/11/retry-parse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("parsed"));
    }

    /**
     * 测试提取向量接口。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldExtractVectorSuccessfully() throws Exception {
        Mockito.when(documentService.extractVector(Mockito.eq(1L), Mockito.eq(1L), Mockito.eq(11L)))
                .thenReturn(new DocumentVO(11L, 1L, "demo", "parsed", "indexed", 2, null, null, 2));

        mockMvc.perform(post("/api/v1/workspaces/1/documents/11/extract-vector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.indexStatus").value("indexed"));
    }

    /**
     * 测试删除文档接口。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldDeleteDocumentSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/1/documents/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        Mockito.verify(documentService, Mockito.times(1))
                .deleteDocument(Mockito.eq(1L), Mockito.eq(1L), Mockito.eq(11L));
    }

    /**
     * 测试兼容 POST 删除文档接口。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldDeleteDocumentByPostSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/1/documents/11/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        Mockito.verify(documentService, Mockito.times(1))
                .deleteDocument(Mockito.eq(1L), Mockito.eq(1L), Mockito.eq(11L));
    }

    /**
     * 创建文档请求体（测试使用）。
     */
    private record CreateBody(String title, String content) {
    }
}

