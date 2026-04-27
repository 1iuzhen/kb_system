package com.kbsystem.backend.audit.event;

import lombok.Builder;
import lombok.Data;

/**
 * 审计日志事件。
 */
@Data
@Builder
public class AuditLogEvent {

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 目标类型。
     */
    private String targetType;

    /**
     * 目标 ID。
     */
    private Long targetId;

    /**
     * 操作动作。
     */
    private String action;

    /**
     * 操作详情 JSON。
     */
    private String operationDetail;

    /**
     * 请求 IP。
     */
    private String ip;

    /**
     * 用户代理。
     */
    private String userAgent;

    /**
     * 请求方法。
     */
    private String requestMethod;

    /**
     * 请求 URI。
     */
    private String requestUri;

    /**
     * 请求参数 JSON。
     */
    private String requestParams;

    /**
     * 执行状态。
     */
    private String status;

    /**
     * 错误信息。
     */
    private String errorMsg;

    /**
     * 执行耗时毫秒。
     */
    private Long executionTimeMs;
}

