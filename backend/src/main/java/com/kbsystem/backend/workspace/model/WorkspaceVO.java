package com.kbsystem.backend.workspace.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 知识库展示对象。
 */
@Data
@AllArgsConstructor
@Schema(description = "知识库展示对象")
public class WorkspaceVO {

    /**
     * 知识库 ID。
     */
    @Schema(description = "知识库ID")
    private Long id;

    /**
     * 知识库名称。
     */
    @Schema(description = "知识库名称")
    private String name;

    /**
     * 当前用户在该库中的角色。
     */
    @Schema(description = "角色")
    private String role;
}

