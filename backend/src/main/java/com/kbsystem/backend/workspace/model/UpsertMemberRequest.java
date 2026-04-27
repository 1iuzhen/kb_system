package com.kbsystem.backend.workspace.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增或更新成员请求。
 */
@Data
@Schema(description = "新增或更新成员请求")
public class UpsertMemberRequest {

    /**
     * 成员用户 ID。
     */
    @NotNull
    @Schema(description = "成员用户ID", example = "2")
    private Long userId;

    /**
     * 成员角色。
     */
    @NotBlank
    @Schema(description = "角色 owner/editor/viewer", example = "editor")
    private String role;
}

