package com.kbsystem.backend.audit.event;

import com.kbsystem.backend.audit.entity.OperationLogEntity;
import com.kbsystem.backend.audit.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 审计日志事件监听器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogEventListener {

    /**
     * 审计日志 mapper。
     */
    private final OperationLogMapper operationLogMapper;

    /**
     * 异步写入审计日志。
     *
     * @param event 审计事件
     */
    @Async
    @EventListener
    public void onAuditLogEvent(AuditLogEvent event) {
        try {
            OperationLogEntity entity = new OperationLogEntity();
            entity.setUserId(event.getUserId());
            entity.setUsername(event.getUsername());
            entity.setTargetType(event.getTargetType());
            entity.setTargetId(event.getTargetId());
            entity.setAction(event.getAction());
            entity.setOperationDetail(event.getOperationDetail());
            entity.setIp(event.getIp());
            entity.setUserAgent(event.getUserAgent());
            entity.setRequestMethod(event.getRequestMethod());
            entity.setRequestUri(event.getRequestUri());
            entity.setRequestParams(event.getRequestParams());
            entity.setStatus(event.getStatus());
            entity.setErrorMsg(event.getErrorMsg());
            entity.setExecutionTimeMs(event.getExecutionTimeMs());
            operationLogMapper.insert(entity);
        } catch (Exception exception) {
            // 审计日志写入失败不能影响主流程，仅记录错误日志。
            log.error("写入审计日志失败", exception);
        }
    }
}

