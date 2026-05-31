package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderHandlerTest {

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    private OrderHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderHandler();
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void shouldProcessValidSQSMessage() {
        SQSEvent event = createSQSEvent(
            "{\"Message\":\"{\\\"orderId\\\":\\\"ORD-001\\\",\\\"customerId\\\":\\\"CUST-001\\\"," +
            "\\\"customerName\\\":\\\"john smith\\\",\\\"orderDate\\\":\\\"2025-01-15T10:30:00.000Z\\\"," +
            "\\\"items\\\":[{\\\"itemId\\\":\\\"ITM-1\\\",\\\"quantity\\\":2,\\\"price\\\":29.99}]," +
            "\\\"metadata\\\":{\\\"correlationId\\\":\\\"abc-123\\\"}}\"}"
        );

        assertDoesNotThrow(() -> handler.handleRequest(event, context));
        verify(logger, atLeastOnce()).log(contains("CANONICAL_RECORD"));
    }

    @Test
    void shouldHandleMessageWithoutSnsEnvelope() {
        SQSEvent event = createSQSEvent(
            "{\"orderId\":\"ORD-002\",\"customerId\":\"CUST-002\",\"customerName\":\"jane doe\"," +
            "\"orderDate\":\"2025-02-20T15:00:00Z\",\"items\":[{\"itemId\":\"ITM-2\",\"quantity\":1,\"price\":49.99}]," +
            "\"metadata\":{\"correlationId\":\"def-456\"}}"
        );

        assertDoesNotThrow(() -> handler.handleRequest(event, context));
        verify(logger, atLeastOnce()).log(contains("CANONICAL_RECORD"));
    }

    @Test
    void shouldThrowExceptionOnInvalidJson() {
        SQSEvent event = createSQSEvent("{invalid json}");

        assertThrows(RuntimeException.class, () -> handler.handleRequest(event, context));
    }

    @Test
    void shouldHandleMessageWithoutMetadata() {
        SQSEvent event = createSQSEvent(
            "{\"orderId\":\"ORD-003\",\"customerId\":\"CUST-003\",\"customerName\":\"bob\"," +
            "\"orderDate\":\"2025-03-10T08:00:00Z\",\"items\":[]}"
        );

        assertDoesNotThrow(() -> handler.handleRequest(event, context));
        verify(logger, atLeastOnce()).log(contains("CANONICAL_RECORD"));
    }

    @Test
    void shouldProcessMultipleMessagesInBatch() {
        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage msg1 = new SQSEvent.SQSMessage();
        msg1.setBody(
            "{\"Message\":\"{\\\"orderId\\\":\\\"ORD-A\\\",\\\"customerId\\\":\\\"CUST-A\\\"," +
            "\\\"customerName\\\":\\\"alice\\\",\\\"orderDate\\\":\\\"2025-03-10T08:00:00Z\\\"," +
            "\\\"items\\\":[],\\\"metadata\\\":{\\\"correlationId\\\":\\\"corr-a\\\"}}\"}"
        );
        SQSEvent.SQSMessage msg2 = new SQSEvent.SQSMessage();
        msg2.setBody(
            "{\"Message\":\"{\\\"orderId\\\":\\\"ORD-B\\\",\\\"customerId\\\":\\\"CUST-B\\\"," +
            "\\\"customerName\\\":\\\"bob\\\",\\\"orderDate\\\":\\\"2025-03-10T08:00:00Z\\\"," +
            "\\\"items\\\":[],\\\"metadata\\\":{\\\"correlationId\\\":\\\"corr-b\\\"}}\"}"
        );
        event.setRecords(List.of(msg1, msg2));

        assertDoesNotThrow(() -> handler.handleRequest(event, context));
        verify(logger, atLeast(2)).log(contains("CANONICAL_RECORD"));
    }

    private SQSEvent createSQSEvent(String body) {
        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setBody(body);
        event.setRecords(Collections.singletonList(message));
        return event;
    }
}
