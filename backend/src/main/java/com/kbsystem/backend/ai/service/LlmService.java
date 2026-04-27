package com.kbsystem.backend.ai.service;

/**
 * 大模型生成服务接口。
 */
public interface LlmService {

    /**
     * 基于系统提示词与用户输入生成答案。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户输入
     * @return 模型答案
     */
    String generate(String systemPrompt, String userPrompt);
}

