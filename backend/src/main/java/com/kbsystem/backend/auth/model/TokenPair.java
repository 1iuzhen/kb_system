package com.kbsystem.backend.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 令牌对对象。
 */
@Data
@AllArgsConstructor
@Schema(description = "令牌对")
public class TokenPair {

    /**
     * 访问令牌。
     */
    private String accessToken;

    /**
     * 刷新令牌。
     */
    private String refreshToken;
}

