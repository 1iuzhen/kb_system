package com.kbsystem.backend.audit.event;

import com.kbsystem.backend.audit.entity.OperationLogEntity;
import com.kbsystem.backend.audit.mapper.OperationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 审计事件监听器测试。
 */
class AuditLogEventListenerTest {

    /**
     * mapper mock。
     */
    private OperationLogMapper operationLogMapper;

    /**
     * 被测监听器。
     */
    private AuditLogEventListener listener;

    /**
     * 初始化测试对象。
     */
    @BeforeEach
    void setUp() {
        operationLogMapper = Mockito.mock(OperationLogMapper.class);
        listener = new AuditLogEventListener(operationLogMapper);
    }

    /**
     * 测试：监听器应将事件写入数据库。
     */
    @Test
    void shouldInsertOperationLogWhenReceiveEvent() {
        AuditLogEvent event = AuditLogEvent.builder()
                .userId(1L)
                .username("admin")
                .targetType("document")
                .targetId(10L)
                .action("save")
                .status("success")
                .executionTimeMs(25L)
                .build();

        listener.onAuditLogEvent(event);

        ArgumentCaptor<OperationLogEntity> captor = ArgumentCaptor.forClass(OperationLogEntity.class);
        Mockito.verify(operationLogMapper, Mockito.times(1)).insert(captor.capture());
        OperationLogEntity inserted = captor.getValue();
        assertEquals(1L, inserted.getUserId());
        assertEquals("document", inserted.getTargetType());
        assertEquals("save", inserted.getAction());
        assertEquals("success", inserted.getStatus());
        assertEquals(25L, inserted.getExecutionTimeMs());
    }
}

