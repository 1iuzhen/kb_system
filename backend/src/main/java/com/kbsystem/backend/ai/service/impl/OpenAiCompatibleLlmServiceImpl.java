package com.kbsystem.backend.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbsystem.backend.ai.service.LlmService;
import com.kbsystem.backend.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容聊天模型服务实现。
 */
@Service
@RequiredArgsConstructor
public class OpenAiCompatibleLlmServiceImpl implements LlmService {

    /**
     * JSON 工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * API 地址。
     */
    @Value("${app.ai.base-url:}")
    private String baseUrl;

    /**
     * API Key。
     */
    @Value("${app.ai.api-key:}")
    private String apiKey;

    /**
     * 聊天模型名。
     */
    @Value("${app.ai.chat-model:qwen-plus}")
    private String chatModel;

    /**
     * HTTP 超时时间（毫秒）。
     */
    @Value("${app.ai.timeout-ms:30000}")
    private int timeoutMs;

    /**
     * HTTP 客户端。
     */
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    /**
     * 调用 LLM 生成答案。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户输入
     * @return 答案文本
     */
    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new BizException(50031, "AI chat 配置缺失，请设置 app.ai.base-url 与 app.ai.api-key");
        }
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", chatModel,
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimRightSlash(baseUrl) + "/chat/completions"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(50032, "chat 调用失败: HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new BizException(50033, "chat 响应内容为空");
            }
            return content;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BizException(50034, "chat 调用异常: " + exception.getMessage());
        } catch (IOException exception) {
            throw new BizException(50034, "chat 调用异常: " + exception.getMessage());
        }
    }

    /**
     * 去除 URL 尾部斜杠。
     *
     * @param raw 原始地址
     * @return 处理后地址
     */
    private String trimRightSlash(String raw) {
        if (raw.endsWith("/")) {
            return raw.substring(0, raw.length() - 1);
        }
        return raw;
    }
}

