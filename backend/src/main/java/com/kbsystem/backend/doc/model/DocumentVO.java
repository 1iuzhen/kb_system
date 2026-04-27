package com.kbsystem.backend.doc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文档展示对象。
 */
@Data
@AllArgsConstructor
@Schema(description = "文档展示对象")
public class DocumentVO {

    /**
     * 文档 ID。
     */
    @Schema(description = "文档ID")
    private Long id;

    /**
     * 所属知识库 ID。
     */
    @Schema(description = "知识库ID")
    private Long workspaceId;

    /**
     * 文档标题。
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 文档状态。
     */
    @Schema(description = "状态")
    private String status;

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
     * 解析失败错误信息。
     */
    @Schema(description = "解析失败错误信息")
    private String parseErrorMsg;

    /**
     * 最新版本号。
     */
    @Schema(description = "最新版本号")
    private Integer latestVersionNo;

    /**
     * 兼容旧调用的构造函数。
     *
     * @param id 文档 ID
     * @param workspaceId 知识库 ID
     * @param title 标题
     * @param status 状态
     * @param latestVersionNo 最新版本号
     */
    public DocumentVO(Long id, Long workspaceId, String title, String status, Integer latestVersionNo) {
        this(id, workspaceId, title, status, "not_indexed", 0, null, null, latestVersionNo);
    }

    /**
     * 兼容中间调用的构造函数。
     *
     * @param id 文档 ID
     * @param workspaceId 知识库 ID
     * @param title 标题
     * @param status 文档状态
     * @param parseErrorMsg 解析错误
     * @param latestVersionNo 最新版本号
     */
    public DocumentVO(Long id, Long workspaceId, String title, String status, String parseErrorMsg, Integer latestVersionNo) {
        this(id, workspaceId, title, status, "not_indexed", 0, null, parseErrorMsg, latestVersionNo);
    }
}

