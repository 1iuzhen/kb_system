package com.kbsystem.backend.workspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kbsystem.backend.workspace.entity.WorkspaceMemberEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库成员 Mapper。
 */
@Mapper
public interface WorkspaceMemberMapper extends BaseMapper<WorkspaceMemberEntity> {
}

