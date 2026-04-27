package com.kbsystem.backend.workspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库成员实体。
 */
@Data
@TableName("kb_workspace_member")
public class WorkspaceMemberEntity {

    /**
     * 关系 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识库 ID。
     */
    private Long workspaceId;

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 成员角色：owner/editor/viewer。
     */
    private String role;

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

