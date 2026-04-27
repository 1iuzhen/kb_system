package com.kbsystem.backend.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kbsystem.backend.auth.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {
}

