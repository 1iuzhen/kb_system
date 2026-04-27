package com.kbsystem.backend.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbsystem.backend.ai.service.EmbeddingService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 embedding 服务实现。
 */
@Service
@RequiredArgsConstructor
public class OpenAiCompatibleEmbeddingServiceImpl implements EmbeddingService {

    /**
     * JSON 序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * API 地址（例如 https://dashscope.aliyuncs.com/compatible-mode/v1）。
     */
    @Value("${app.ai.base-url:}")
    private String baseUrl;

    /**
     * API Key。
     */
    @Value("${app.ai.api-key:}")
    private String apiKey;

    /**
     * embedding 模型名。
     */
    @Value("${app.ai.embedding-model:text-embedding-v2}")
    private String embeddingModel;

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
     * 文本向量化。
     *
     * @param text 原始文本
     * @return 向量
     */
    @Override
    public List<Double> embed(String text) {
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new BizException(50021, "AI embedding 配置缺失，请设置 app.ai.base-url 与 app.ai.api-key");
        }
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", embeddingModel,
                    "input", text == null ? "" : text
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimRightSlash(baseUrl) + "/embeddings"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(50022, "embedding 调用失败: HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode embeddingNode = root.path("data").path(0).path("embedding");
            if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
                throw new BizException(50023, "embedding 响应格式异常");
            }
            List<Double> vector = new ArrayList<>(embeddingNode.size());
            for (JsonNode node : embeddingNode) {
                vector.add(node.asDouble());
            }
            return vector;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BizException(50024, "embedding 调用异常: " + exception.getMessage());
        } catch (IOException exception) {
            throw new BizException(50024, "embedding 调用异常: " + exception.getMessage());
        }
    }

    /**
     * 去除 URL 末尾斜杠。
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

