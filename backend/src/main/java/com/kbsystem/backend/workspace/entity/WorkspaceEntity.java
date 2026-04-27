package com.kbsystem.backend.workspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库实体。
 */
@Data
@TableName("kb_workspace")
public class WorkspaceEntity {

    /**
     * 知识库 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识库名称。
     */
    private String name;

    /**
     * 创建人 ID。
     */
    private Long ownerUserId;

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
     * 乐观锁版本字段。
     */
    @Version
    private Long updateVersion;
}

