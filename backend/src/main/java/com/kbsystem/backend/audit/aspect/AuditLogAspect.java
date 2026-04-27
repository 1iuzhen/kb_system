package com.kbsystem.backend.audit.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbsystem.backend.audit.annotation.AuditLog;
import com.kbsystem.backend.audit.event.AuditLogEvent;
import com.kbsystem.backend.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计日志切面。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    /**
     * 事件发布器。
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * JSON 序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 环绕通知：记录写操作审计日志。
     *
     * @param joinPoint 切点
     * @param auditLog 审计注解
     * @return 原方法返回值
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        Throwable throwable = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            throwable = ex;
            throw ex;
        } finally {
            publishAuditEvent(joinPoint, auditLog, start, throwable);
        }
    }

    /**
     * 发布审计事件。
     *
     * @param joinPoint 切点
     * @param auditLog 注解
     * @param start 开始时间
     * @param throwable 异常
     */
    private void publishAuditEvent(ProceedingJoinPoint joinPoint, AuditLog auditLog, long start, Throwable throwable) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs == null ? null : attrs.getRequest();
        long execution = System.currentTimeMillis() - start;
        String status = throwable == null ? "success" : "failed";
        String errorMsg = throwable == null ? null : throwable.getMessage();
        Long targetId = resolveTargetId(joinPoint.getArgs(), auditLog.targetIdArgIndex());
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(UserContext.getUserId())
                .username(UserContext.getUsername())
                .targetType(auditLog.targetType())
                .targetId(targetId)
                .action(auditLog.action())
                .operationDetail(buildOperationDetail(joinPoint))
                .ip(request == null ? null : request.getRemoteAddr())
                .userAgent(request == null ? null : request.getHeader("User-Agent"))
                .requestMethod(request == null ? null : request.getMethod())
                .requestUri(request == null ? null : request.getRequestURI())
                .requestParams(buildRequestParams(joinPoint.getArgs()))
                .status(status)
                .errorMsg(errorMsg)
                .executionTimeMs(execution)
                .build();
        eventPublisher.publishEvent(event);
    }

    /**
     * 解析目标 ID。
     *
     * @param args 方法参数
     * @param index 参数下标
     * @return 目标 ID
     */
    private Long resolveTargetId(Object[] args, int index) {
        if (index < 0 || index >= args.length) {
            return null;
        }
        Object candidate = args[index];
        if (candidate instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * 构造操作详情。
     *
     * @param joinPoint 切点
     * @return JSON 字符串
     */
    private String buildOperationDetail(ProceedingJoinPoint joinPoint) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("method", joinPoint.getSignature().toShortString());
        detail.put("argsCount", joinPoint.getArgs().length);
        return toJson(detail);
    }

    /**
     * 构造请求参数摘要。
     *
     * @param args 方法参数
     * @return JSON 字符串
     */
    private String buildRequestParams(Object[] args) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof MultipartFile file) {
                params.put("arg" + i, Map.of(
                        "type", "file",
                        "filename", file.getOriginalFilename(),
                        "size", file.getSize()
                ));
            } else if (arg instanceof Number || arg instanceof String || arg instanceof Boolean) {
                params.put("arg" + i, arg);
            } else if (arg == null) {
                params.put("arg" + i, null);
            } else {
                params.put("arg" + i, arg.getClass().getSimpleName());
            }
        }
        return toJson(params);
    }

    /**
     * JSON 序列化。
     *
     * @param value 任意对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{\"error\":\"json serialize failed\"}";
        }
    }
}

