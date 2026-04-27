package com.kbsystem.backend.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kbsystem.backend.auth.entity.SysUserEntity;
import com.kbsystem.backend.auth.mapper.SysUserMapper;
import com.kbsystem.backend.auth.model.LoginRequest;
import com.kbsystem.backend.auth.model.LoginResponse;
import com.kbsystem.backend.auth.model.TokenPair;
import com.kbsystem.backend.auth.model.UserInfo;
import com.kbsystem.backend.auth.service.AuthService;
import com.kbsystem.backend.auth.util.JwtTokenUtil;
import com.kbsystem.backend.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现。
 * 使用 MyBatis-Plus 查询用户并签发 JWT。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /**
     * 用户 Mapper。
     */
    private final SysUserMapper sysUserMapper;

    /**
     * JWT 工具类。
     */
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 登录实现。
     * 成功后返回简化版 token（演示用途），后续可以替换为 JWT。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        SysUserEntity user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, request.getUsername())
                .last("LIMIT 1"));
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new BizException(40101, "用户名或密码错误");
        }
        UserInfo userInfo = new UserInfo(user.getId(), user.getUsername());
        String accessToken = jwtTokenUtil.generateAccessToken(userInfo);
        String refreshToken = jwtTokenUtil.generateRefreshToken(userInfo);
        return new LoginResponse(accessToken, refreshToken, userInfo);
    }

    /**
     * 刷新 token。
     *
     * @param refreshToken 刷新令牌
     * @return 新的令牌对
     */
    @Override
    public TokenPair refreshToken(String refreshToken) {
        UserInfo userInfo = jwtTokenUtil.parseRefreshToken(refreshToken);
        String newAccessToken = jwtTokenUtil.generateAccessToken(userInfo);
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(userInfo);
        return new TokenPair(newAccessToken, newRefreshToken);
    }
}

