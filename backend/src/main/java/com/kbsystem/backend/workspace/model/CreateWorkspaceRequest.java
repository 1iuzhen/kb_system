package com.kbsystem.backend.workspace.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建知识库请求。
 */
@Data
@Schema(description = "创建知识库请求")
public class CreateWorkspaceRequest {

    /**
     * 知识库名称。
     */
    @NotBlank
    @Schema(description = "知识库名称", example = "产品知识库")
    private String name;
}

