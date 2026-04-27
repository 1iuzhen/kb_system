package com.kbsystem.backend.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求对象。
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    /**
     * 用户名。
     */
    @NotBlank
    @Schema(description = "用户名", example = "admin")
    private String username;

    /**
     * 密码。
     */
    @NotBlank
    @Schema(description = "密码", example = "123456")
    private String password;
}

