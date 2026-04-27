package com.kbsystem.backend.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kbsystem.backend.doc.entity.DocumentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档 Mapper。
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity> {
}

