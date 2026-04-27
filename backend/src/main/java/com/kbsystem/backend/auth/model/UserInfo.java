package com.kbsystem.backend.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户信息对象。
 */
@Data
@AllArgsConstructor
@Schema(description = "用户信息")
public class UserInfo {

    /**
     * 用户 ID。
     */
    @Schema(description = "用户 ID")
    private Long userId;

    /**
     * 用户名。
     */
    @Schema(description = "用户名")
    private String username;
}

