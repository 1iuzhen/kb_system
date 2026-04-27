package com.kbsystem.backend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbsystem.backend.ai.service.EmbeddingService;
import com.kbsystem.backend.ai.service.LlmService;
import com.kbsystem.backend.chat.model.RagSourceVO;
import com.kbsystem.backend.chat.service.RagChatService;
import com.kbsystem.backend.common.exception.BizException;
import com.kbsystem.backend.doc.mapper.ChunkMapper;
import com.kbsystem.backend.workspace.entity.WorkspaceEntity;
import com.kbsystem.backend.workspace.entity.WorkspaceMemberEntity;
import com.kbsystem.backend.workspace.mapper.WorkspaceMapper;
import com.kbsystem.backend.workspace.mapper.WorkspaceMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * RAG 对话服务实现。
 */
@Service
@RequiredArgsConstructor
public class RagChatServiceImpl implements RagChatService {

    /**
     * 向量化服务。
     */
    private final EmbeddingService embeddingService;

    /**
     * 大模型服务。
     */
    private final LlmService llmService;

    /**
     * 向量分块 mapper。
     */
    private final ChunkMapper chunkMapper;

    /**
     * 知识库 mapper。
     */
    private final WorkspaceMapper workspaceMapper;

    /**
     * 知识库成员 mapper。
     */
    private final WorkspaceMemberMapper workspaceMemberMapper;

    /**
     * JSON 工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * embedding 维度。
     */
    @Value("${app.ai.embedding-dim:1536}")
    private int embeddingDim;

    /**
     * 知识库内 RAG 问答。
     *
     * @param userId 用户 ID
     * @param workspaceId 知识库 ID
     * @param question 问题
     * @return SSE
     */
    @Override
    public SseEmitter ask(Long userId, Long workspaceId, String question) {
        requireWorkspace(workspaceId);
        requireRole(userId, workspaceId, Set.of("owner", "editor", "viewer"));
        SseEmitter emitter = new SseEmitter(120000L);
        try {
            List<Double> queryVector = embeddingService.embed(question);
            String vectorLiteral = toVectorLiteral(queryVector);
            List<ChunkMapper.ChunkSearchRow> rows = chunkMapper.searchTopKByWorkspace(workspaceId, vectorLiteral, 6);
            if (rows.isEmpty()) {
                sendEvent(emitter, "text", Map.of("content", "当前知识库暂无可用索引内容，请先提取向量后再提问。"));
                sendEvent(emitter, "done", Map.of("sources", List.of()));
                emitter.complete();
                return emitter;
            }
            List<RagSourceVO> sources = rows.stream()
                    .map(row -> new RagSourceVO(
                            row.getChunkId(),
                            row.getDocumentId(),
                            row.getDocTitle(),
                            snippet(row.getContent()),
                            row.getScore()
                    ))
                    .toList();
            String userPrompt = buildUserPrompt(question, rows);
            String answer = llmService.generate(
                    "你是知识库问答助手。请仅基于给定上下文回答，若证据不足请明确说明。",
                    userPrompt
            );
            // 简单分段流式输出，前端可逐段渲染。
            for (String segment : splitSegments(answer)) {
                sendEvent(emitter, "text", Map.of("content", segment));
            }
            sendEvent(emitter, "ref", Map.of("sources", sources));
            sendEvent(emitter, "done", Map.of("sources", sources));
            emitter.complete();
            return emitter;
        } catch (Exception exception) {
            try {
                sendEvent(emitter, "error", Map.of("message", exception.getMessage()));
            } catch (Exception ignored) {
                // 忽略二次发送异常，避免覆盖原始错误。
            }
            emitter.completeWithError(exception);
            return emitter;
        }
    }

    /**
     * 构造给 LLM 的用户提示词。
     *
     * @param question 问题
     * @param rows 召回分块
     * @return 提示词
     */
    private String buildUserPrompt(String question, List<ChunkMapper.ChunkSearchRow> rows) {
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            ChunkMapper.ChunkSearchRow row = rows.get(i);
            contextBuilder.append("【证据")
                    .append(i + 1)
                    .append("】文档: ")
                    .append(row.getDocTitle())
                    .append("；相关度: ")
                    .append(String.format(Locale.ROOT, "%.4f", row.getScore() == null ? 0D : row.getScore()))
                    .append("\n")
                    .append(row.getContent())
                    .append("\n\n");
        }
        return "问题：\n" + question + "\n\n可用上下文：\n" + contextBuilder + "\n请给出准确答案，并在最后用简短一句说明依据来自哪些证据编号。";
    }

    /**
     * 将向量数组转换为 pgvector 字面量。
     *
     * @param vector 向量
     * @return pgvector 字符串
     */
    private String toVectorLiteral(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            throw new BizException(50041, "查询向量为空");
        }
        if (vector.size() != embeddingDim) {
            throw new BizException(50043, "查询向量维度不匹配，期望 " + embeddingDim + "，实际 " + vector.size());
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            builder.append(String.format(Locale.ROOT, "%.8f", vector.get(i)));
            if (i < vector.size() - 1) {
                builder.append(",");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * 截取引用片段。
     *
     * @param content 原文
     * @return 片段
     */
    private String snippet(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.length() <= 200) {
            return trimmed;
        }
        return trimmed.substring(0, 200);
    }

    /**
     * 简单分段，模拟流式输出。
     *
     * @param text 文本
     * @return 分段列表
     */
    private List<String> splitSegments(String text) {
        if (text == null || text.isBlank()) {
            return List.of("");
        }
        int step = 48;
        java.util.ArrayList<String> segments = new java.util.ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + step, text.length());
            segments.add(text.substring(start, end));
            start = end;
        }
        return segments;
    }

    /**
     * 发送 SSE 事件。
     *
     * @param emitter 发送器
     * @param type 事件类型
     * @param payload 事件数据
     */
    private void sendEvent(SseEmitter emitter, String type, Map<String, Object> payload) throws IOException {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("type", type);
        wrapper.putAll(payload);
        emitter.send(SseEmitter.event().name(type).data(toJson(wrapper)));
    }

    /**
     * JSON 序列化。
     *
     * @param payload 数据
     * @return JSON 字符串
     */
    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BizException(50042, "SSE 序列化失败");
        }
    }

    /**
     * 校验知识库存在。
     *
     * @param workspaceId 知识库 ID
     */
    private void requireWorkspace(Long workspaceId) {
        WorkspaceEntity workspace = workspaceMapper.selectById(workspaceId);
        if (workspace == null) {
            throw new BizException(40401, "知识库不存在");
        }
    }

    /**
     * 校验访问角色。
     *
     * @param userId 用户 ID
     * @param workspaceId 知识库 ID
     * @param allowedRoles 允许角色
     */
    private void requireRole(Long userId, Long workspaceId, Set<String> allowedRoles) {
        WorkspaceMemberEntity member = workspaceMemberMapper.selectOne(new LambdaQueryWrapper<WorkspaceMemberEntity>()
                .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId)
                .eq(WorkspaceMemberEntity::getUserId, userId)
                .last("LIMIT 1"));
        if (member == null) {
            throw new BizException(40301, "无权访问该知识库");
        }
        if (!allowedRoles.contains(member.getRole())) {
            throw new BizException(40302, "权限不足");
        }
    }
}

