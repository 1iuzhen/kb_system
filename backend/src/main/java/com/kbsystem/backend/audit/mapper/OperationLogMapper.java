package com.kbsystem.backend.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kbsystem.backend.audit.entity.OperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper。
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {
}

