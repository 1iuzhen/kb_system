package com.kbsystem.backend.doc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 保存文档请求。
 */
@Data
@Schema(description = "保存文档请求")
public class SaveDocumentRequest {

    /**
     * 文档标题。
     */
    @NotBlank
    @Schema(description = "文档标题")
    private String title;

    /**
     * Markdown 内容。
     */
    @NotNull
    @Schema(description = "Markdown内容")
    private String content;

    /**
     * 前端基于的版本号。
     * 服务端会校验该版本号是否仍是最新版本，以防并发覆盖。
     */
    @NotNull
    @Schema(description = "前端编辑基线版本号", example = "3")
    private Integer baseVersion;
}

