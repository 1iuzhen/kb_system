package com.kbsystem.backend.auth.service;

import com.kbsystem.backend.auth.model.LoginRequest;
import com.kbsystem.backend.auth.model.LoginResponse;
import com.kbsystem.backend.auth.model.TokenPair;

/**
 * 认证服务接口。
 */
public interface AuthService {

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request);

    /**
     * 刷新令牌。
     *
     * @param refreshToken 刷新令牌
     * @return 新的令牌对
     */
    TokenPair refreshToken(String refreshToken);
}

