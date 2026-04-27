package com.kbsystem.backend.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kbsystem.backend.auth.config.JwtProperties;
import com.kbsystem.backend.auth.entity.SysUserEntity;
import com.kbsystem.backend.auth.mapper.SysUserMapper;
import com.kbsystem.backend.auth.model.LoginRequest;
import com.kbsystem.backend.auth.model.LoginResponse;
import com.kbsystem.backend.auth.service.impl.AuthServiceImpl;
import com.kbsystem.backend.auth.util.JwtTokenUtil;
import com.kbsystem.backend.common.exception.BizException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * 认证服务单元测试。
 */
class AuthServiceImplTest {

    /**
     * 被测对象。
     */
    private AuthServiceImpl authService;

    /**
     * 用户 mapper mock。
     */
    private SysUserMapper sysUserMapper;

    /**
     * 初始化测试依赖。
     */
    @BeforeEach
    void setUp() {
        sysUserMapper = Mockito.mock(SysUserMapper.class);
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("kb-system-test");
        properties.setAccessTokenExpireSeconds(7200);
        properties.setRefreshTokenExpireSeconds(604800);
        properties.setSecret("kb-system-test-secret-key-123456789012345");
        authService = new AuthServiceImpl(sysUserMapper, new JwtTokenUtil(properties));
    }

    /**
     * 测试：用户名密码正确时应登录成功。
     */
    @Test
    void shouldLoginSuccessfullyWhenCredentialValid() {
        SysUserEntity user = new SysUserEntity();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("123456");
        Mockito.when(sysUserMapper.selectOne(Mockito.any(LambdaQueryWrapper.class))).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");

        LoginResponse response = authService.login(request);

        Assertions.assertNotNull(response.getAccessToken());
        Assertions.assertEquals(1L, response.getUserInfo().getUserId());
    }

    /**
     * 测试：用户名密码错误时应抛业务异常。
     */
    @Test
    void shouldThrowWhenCredentialInvalid() {
        Mockito.when(sysUserMapper.selectOne(Mockito.any(LambdaQueryWrapper.class))).thenReturn(null);
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        Assertions.assertThrows(BizException.class, () -> authService.login(request));
    }
}

