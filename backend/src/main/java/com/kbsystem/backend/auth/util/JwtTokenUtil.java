package com.kbsystem.backend.auth.util;

import com.kbsystem.backend.auth.config.JwtProperties;
import com.kbsystem.backend.auth.model.UserInfo;
import com.kbsystem.backend.common.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 工具类。
 * 负责 access/refresh 令牌的生成与解析。
 */
@Component
public class JwtTokenUtil {

    /**
     * JWT 配置。
     */
    private final JwtProperties jwtProperties;

    /**
     * 构造函数注入。
     *
     * @param jwtProperties JWT 配置
     */
    public JwtTokenUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成访问令牌。
     *
     * @param userInfo 用户信息
     * @return 访问令牌
     */
    public String generateAccessToken(UserInfo userInfo) {
        return generateToken(userInfo, jwtProperties.getAccessTokenExpireSeconds(), "access");
    }

    /**
     * 生成刷新令牌。
     *
     * @param userInfo 用户信息
     * @return 刷新令牌
     */
    public String generateRefreshToken(UserInfo userInfo) {
        return generateToken(userInfo, jwtProperties.getRefreshTokenExpireSeconds(), "refresh");
    }

    /**
     * 解析访问令牌。
     *
     * @param token 访问令牌
     * @return 用户信息
     */
    public UserInfo parseAccessToken(String token) {
        Claims claims = parseToken(token);
        if (!"access".equals(claims.get("tokenType", String.class))) {
            throw new BizException(40100, "访问令牌类型错误");
        }
        return toUserInfo(claims);
    }

    /**
     * 解析刷新令牌。
     *
     * @param token 刷新令牌
     * @return 用户信息
     */
    public UserInfo parseRefreshToken(String token) {
        Claims claims = parseToken(token);
        if (!"refresh".equals(claims.get("tokenType", String.class))) {
            throw new BizException(40100, "刷新令牌类型错误");
        }
        return toUserInfo(claims);
    }

    /**
     * 生成指定类型令牌。
     *
     * @param userInfo   用户信息
     * @param expireSecs 有效期秒数
     * @param tokenType  token 类型
     * @return JWT 字符串
     */
    private String generateToken(UserInfo userInfo, long expireSecs, String tokenType) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(userInfo.getUserId()))
                .claim("username", userInfo.getUsername())
                .claim("tokenType", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSecs)))
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 通用 token 解析。
     *
     * @param token JWT 字符串
     * @return claims
     */
    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            throw new BizException(40100, "令牌无效或已过期");
        }
    }

    /**
     * 从 claims 转换用户信息。
     *
     * @param claims JWT claims
     * @return 用户信息
     */
    private UserInfo toUserInfo(Claims claims) {
        Long userId = Long.parseLong(claims.getSubject());
        String username = claims.get("username", String.class);
        return new UserInfo(userId, username);
    }

    /**
     * 获取签名密钥。
     *
     * @return 密钥对象
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}

