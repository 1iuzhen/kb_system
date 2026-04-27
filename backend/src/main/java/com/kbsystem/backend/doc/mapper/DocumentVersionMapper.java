package com.kbsystem.backend.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kbsystem.backend.doc.entity.DocumentVersionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档版本 Mapper。
 */
@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersionEntity> {
}

