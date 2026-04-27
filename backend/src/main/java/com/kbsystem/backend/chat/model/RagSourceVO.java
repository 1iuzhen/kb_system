package com.kbsystem.backend.chat.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * RAG 引用来源。
 */
@Data
@AllArgsConstructor
@Schema(description = "RAG引用来源")
public class RagSourceVO {

    /**
     * 分块 ID。
     */
    @Schema(description = "分块ID")
    private Long chunkId;

    /**
     * 文档 ID。
     */
    @Schema(description = "文档ID")
    private Long docId;

    /**
     * 文档标题。
     */
    @Schema(description = "文档标题")
    private String docTitle;

    /**
     * 引用片段（前 200 字）。
     */
    @Schema(description = "引用片段")
    private String snippet;

    /**
     * 相似度分数。
     */
    @Schema(description = "相似度分数")
    private Double score;
}

