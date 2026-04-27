package com.kbsystem.backend.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应对象。
 */
@Data
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {

    /**
     * 访问令牌。
     */
    @Schema(description = "访问令牌")
    private String accessToken;

    /**
     * 刷新令牌。
     */
    @Schema(description = "刷新令牌")
    private String refreshToken;

    /**
     * 用户信息。
     */
    @Schema(description = "用户信息")
    private UserInfo userInfo;
}

