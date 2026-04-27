package com.kbsystem.backend.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体。
 */
@Data
@TableName("sys_operation_log")
public class OperationLogEntity {

    /**
     * 主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作用户 ID。
     */
    private Long userId;

    /**
     * 操作用户名。
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

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer deleted;

    /**
     * 乐观锁版本。
     */
    @Version
    private Long updateVersion;
}

