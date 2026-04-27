package com.kbsystem.backend.security;

import com.kbsystem.backend.auth.model.UserInfo;
import com.kbsystem.backend.auth.util.JwtTokenUtil;
import com.kbsystem.backend.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 演示鉴权拦截器。
 * 从 Authorization 解析 JWT access token，并写入 UserContext。
 */
@Component
@RequiredArgsConstructor
public class DemoAuthInterceptor implements HandlerInterceptor {

    /**
     * JWT 工具类。
     */
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 接口执行前校验认证信息。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @return 是否继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/v1/auth/login")
                || uri.startsWith("/api/v1/auth/refresh")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui")) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BizException(40100, "未登录或令牌无效");
        }
        String token = authHeader.substring("Bearer ".length());
        UserInfo userInfo = jwtTokenUtil.parseAccessToken(token);
        UserContext.set(userInfo.getUserId(), userInfo.getUsername());
        return true;
    }

    /**
     * 请求完成后清理用户上下文。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @param ex       异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}

