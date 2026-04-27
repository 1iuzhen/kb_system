package com.kbsystem.backend.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档版本实体。
 */
@Data
@TableName("kb_document_version")
public class DocumentVersionEntity {

    /**
     * 版本主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文档 ID。
     */
    private Long documentId;

    /**
     * 版本号。
     */
    private Integer versionNo;

    /**
     * Markdown 内容。
     */
    private String content;

    /**
     * 标题快照。
     * 记录该版本保存时的标题，便于后续做标题变更追溯。
     */
    private String titleSnapshot;

    /**
     * 版本状态。
     */
    private String status;

    /**
     * 保存人用户 ID。
     */
    private Long saveUserId;

    /**
     * 保存人用户名。
     */
    private String saveUsername;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer deleted;

    /**
     * 乐观锁版本。
     */
    @Version
    private Long updateVersion;
}

