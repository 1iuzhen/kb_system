package com.kbsystem.backend.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性。
 * 用于统一读取 issuer、密钥和有效期等参数。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 签发方。
     */
    private String issuer;

    /**
     * 访问令牌有效期（秒）。
     */
    private long accessTokenExpireSeconds;

    /**
     * 刷新令牌有效期（秒）。
     */
    private long refreshTokenExpireSeconds;

    /**
     * 签名密钥。
     */
    private String secret;
}

