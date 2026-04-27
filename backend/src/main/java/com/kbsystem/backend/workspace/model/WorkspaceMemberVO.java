package com.kbsystem.backend.workspace.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 成员展示对象。
 */
@Data
@AllArgsConstructor
@Schema(description = "成员展示对象")
public class WorkspaceMemberVO {

    /**
     * 成员用户 ID。
     */
    @Schema(description = "成员用户ID")
    private Long userId;

    /**
     * 成员用户名。
     */
    @Schema(description = "成员用户名")
    private String username;

    /**
     * 角色。
     */
    @Schema(description = "角色")
    private String role;
}

