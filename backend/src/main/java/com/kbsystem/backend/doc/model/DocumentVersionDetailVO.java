package com.kbsystem.backend.doc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档版本详情对象。
 */
@Data
@AllArgsConstructor
@Schema(description = "文档版本详情")
public class DocumentVersionDetailVO {

    /**
     * 版本 ID。
     */
    @Schema(description = "版本ID")
    private Long id;

    /**
     * 文档 ID。
     */
    @Schema(description = "文档ID")
    private Long documentId;

    /**
     * 版本号。
     */
    @Schema(description = "版本号")
    private Integer versionNo;

    /**
     * 版本状态。
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 标题快照。
     */
    @Schema(description = "标题快照")
    private String titleSnapshot;

    /**
     * 版本内容。
     */
    @Schema(description = "Markdown内容")
    private String content;

    /**
     * 保存人用户名。
     */
    @Schema(description = "保存人用户名")
    private String saveUsername;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

