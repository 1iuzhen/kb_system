package com.kbsystem.backend.ai.service;

import java.util.List;

/**
 * 向量化服务接口。
 */
public interface EmbeddingService {

    /**
     * 将文本转为向量。
     *
     * @param text 原始文本
     * @return 向量值
     */
    List<Double> embed(String text);
}

