package com.kbsystem.backend.doc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文档详情对象。
 */
@Data
@AllArgsConstructor
@Schema(description = "文档详情对象")
public class DocumentDetailVO {

    /**
     * 文档 ID。
     */
    @Schema(description = "文档ID")
    private Long id;

    /**
     * 知识库 ID。
     */
    @Schema(description = "知识库ID")
    private Long workspaceId;

    /**
     * 文档标题。
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 内容。
     */
    @Schema(description = "Markdown内容")
    private String content;

    /**
     * 最新版本号。
     */
    @Schema(description = "当前版本号")
    private Integer latestVersionNo;

    /**
     * 向量索引状态。
     */
    @Schema(description = "向量索引状态")
    private String indexStatus;

    /**
     * 已索引版本号。
     */
    @Schema(description = "已索引版本号")
    private Integer indexedVersionNo;

    /**
     * 向量索引错误信息。
     */
    @Schema(description = "向量索引错误信息")
    private String indexErrorMsg;

    /**
     * 文档状态。
     */
    @Schema(description = "文档状态")
    private String status;

    /**
     * 解析失败错误信息。
     */
    @Schema(description = "解析失败错误信息")
    private String parseErrorMsg;

    /**
     * 当前用户角色。
     */
    @Schema(description = "当前用户角色")
    private String role;

    /**
     * 兼容旧调用的构造函数。
     *
     * @param id 文档 ID
     * @param workspaceId 知识库 ID
     * @param title 标题
     * @param content 内容
     * @param latestVersionNo 最新版本号
     * @param role 角色
     */
    public DocumentDetailVO(Long id, Long workspaceId, String title, String content, Integer latestVersionNo, String role) {
        this(id, workspaceId, title, content, latestVersionNo, "not_indexed", 0, null, null, null, role);
    }
}

