package com.kbsystem.backend.chat.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * RAG 提问请求。
 */
@Data
@Schema(description = "RAG提问请求")
public class RagAskRequest {

    /**
     * 用户问题。
     */
    @NotBlank(message = "问题不能为空")
    @Schema(description = "问题")
    private String question;
}

