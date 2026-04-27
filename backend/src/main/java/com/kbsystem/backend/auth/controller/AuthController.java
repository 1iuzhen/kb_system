package com.kbsystem.backend.auth.controller;

import com.kbsystem.backend.auth.model.LoginRequest;
import com.kbsystem.backend.auth.model.LoginResponse;
import com.kbsystem.backend.auth.model.RefreshTokenRequest;
import com.kbsystem.backend.auth.model.TokenPair;
import com.kbsystem.backend.auth.service.AuthService;
import com.kbsystem.backend.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "认证模块")
public class AuthController {

    /**
     * 认证服务。
     */
    private final AuthService authService;

    /**
     * 登录接口。
     *
     * @param request 登录请求参数
     * @return 登录结果
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /**
     * 刷新令牌接口。
     *
     * @param request 刷新请求
     * @return 新令牌对
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新访问令牌")
    public Result<TokenPair> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.ok(authService.refreshToken(request.getRefreshToken()));
    }
}

