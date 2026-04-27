package com.kbsystem.backend.audit.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbsystem.backend.audit.annotation.AuditLog;
import com.kbsystem.backend.audit.event.AuditLogEvent;
import com.kbsystem.backend.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 审计切面测试。
 */
class AuditLogAspectTest {

    /**
     * 事件发布器 mock。
     */
    private ApplicationEventPublisher publisher;

    /**
     * 被测切面。
     */
    private AuditLogAspect aspect;

    /**
     * 初始化测试环境。
     */
    @BeforeEach
    void setUp() {
        publisher = Mockito.mock(ApplicationEventPublisher.class);
        aspect = new AuditLogAspect(publisher, new ObjectMapper());
        UserContext.set(1L, "admin");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/workspaces/1/documents/10/save");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    /**
     * 清理上下文。
     */
    @AfterEach
    void tearDown() {
        UserContext.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 测试：成功调用时应发布 success 事件。
     *
     * @throws Throwable 异常
     */
    @Test
    void shouldPublishSuccessEventWhenMethodSucceeded() throws Throwable {
        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        Mockito.when(joinPoint.proceed()).thenReturn("ok");
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{1L, 10L, "payload"});
        Mockito.when(joinPoint.getSignature()).thenReturn(Mockito.mock(org.aspectj.lang.Signature.class));
        Mockito.when(joinPoint.getSignature().toShortString()).thenReturn("DocController.save(..)");

        Object result = aspect.around(joinPoint, auditLog("save", "document", 1));
        assertEquals("ok", result);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        Mockito.verify(publisher, Mockito.times(1)).publishEvent(captor.capture());
        AuditLogEvent event = captor.getValue();
        assertEquals("success", event.getStatus());
        assertEquals(10L, event.getTargetId());
        assertEquals("save", event.getAction());
    }

    /**
     * 测试：异常调用时应发布 failed 事件并透传异常。
     *
     * @throws Throwable 异常
     */
    @Test
    void shouldPublishFailedEventWhenMethodThrows() throws Throwable {
        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        Mockito.when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{1L, 10L});
        Mockito.when(joinPoint.getSignature()).thenReturn(Mockito.mock(org.aspectj.lang.Signature.class));
        Mockito.when(joinPoint.getSignature().toShortString()).thenReturn("DocController.save(..)");

        assertThrows(IllegalStateException.class, () -> aspect.around(joinPoint, auditLog("save", "document", 1)));

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        Mockito.verify(publisher, Mockito.times(1)).publishEvent(captor.capture());
        assertEquals("failed", captor.getValue().getStatus());
    }

    /**
     * 构造注解实例。
     *
     * @param action 动作
     * @param targetType 目标类型
     * @param targetIdArgIndex 目标 ID 参数下标
     * @return 注解实例
     */
    private AuditLog auditLog(String action, String targetType, int targetIdArgIndex) {
        return new AuditLog() {
            @Override
            public String action() {
                return action;
            }

            @Override
            public String targetType() {
                return targetType;
            }

            @Override
            public int targetIdArgIndex() {
                return targetIdArgIndex;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return AuditLog.class;
            }
        };
    }
}

