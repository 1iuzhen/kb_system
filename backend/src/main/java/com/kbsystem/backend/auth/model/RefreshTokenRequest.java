package com.kbsystem.backend.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌请求对象。
 */
@Data
@Schema(description = "刷新令牌请求")
public class RefreshTokenRequest {

    /**
     * 刷新令牌。
     */
    @NotBlank
    @Schema(description = "refreshToken")
    private String refreshToken;
}

