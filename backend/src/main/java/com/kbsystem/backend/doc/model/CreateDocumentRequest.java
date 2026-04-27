package com.kbsystem.backend.doc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建文档请求。
 */
@Data
@Schema(description = "创建文档请求")
public class CreateDocumentRequest {

    /**
     * 文档标题。
     */
    @NotBlank
    @Schema(description = "文档标题", example = "项目说明")
    private String title;

    /**
     * 初始 Markdown 内容。
     */
    @Schema(description = "初始Markdown内容")
    private String content;
}

