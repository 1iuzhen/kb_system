package com.kbsystem.backend.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档主表实体。
 */
@Data
@TableName("kb_document")
public class DocumentEntity {

    /**
     * 文档主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属知识库 ID。
     */
    private Long workspaceId;

    /**
     * 文档标题。
     */
    private String title;

    /**
     * 文档状态。
     */
    private String status;

    /**
     * 向量索引状态。
     */
    private String indexStatus;

    /**
     * 已索引版本号。
     */
    private Integer indexedVersionNo;

    /**
     * 向量索引错误信息。
     */
    private String indexErrorMsg;

    /**
     * 解析失败错误信息。
     */
    private String parseErrorMsg;

    /**
     * 上传源文件名。
     */
    private String sourceFileName;

    /**
     * 上传源文件存储路径。
     */
    private String sourceFilePath;

    /**
     * 最新版本号。
     */
    private Integer latestVersionNo;

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

