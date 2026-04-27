package com.kbsystem.backend.workspace.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新知识库请求。
 */
@Data
@Schema(description = "更新知识库请求")
public class UpdateWorkspaceRequest {

    /**
     * 新名称。
     */
    @NotBlank
    @Schema(description = "新名称", example = "研发知识库")
    private String name;
}

