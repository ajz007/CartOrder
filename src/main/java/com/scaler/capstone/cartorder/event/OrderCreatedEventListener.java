package com.scaler.capstone.cartorder.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OrderCreatedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCreatedEventListener.class);

    private final OrderEventAuditRepository orderEventAuditRepository;
    private final ObjectMapper objectMapper;

    public OrderCreatedEventListener(
            OrderEventAuditRepository orderEventAuditRepository,
            ObjectMapper objectMapper
    ) {
        this.orderEventAuditRepository = orderEventAuditRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topic.order-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderCreated(OrderCreatedEvent event) {
        LOGGER.info(
                "Consumed order.created event: orderId={}, userId={}, cartId={}, totalAmount={}, itemCount={}",
                event.getOrderId(),
                event.getUserId(),
                event.getCartId(),
                event.getTotalAmount(),
                event.getItems() == null ? 0 : event.getItems().size()
        );

        OrderEventAudit audit = new OrderEventAudit();
        audit.setEventType(event.getEventType());
        audit.setOrderId(event.getOrderId());
        audit.setUserId(event.getUserId());
        audit.setPayload(serialize(event));
        audit.setProcessedAt(Instant.now());
        orderEventAuditRepository.save(audit);
    }

    private String serialize(OrderCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            LOGGER.warn("Failed to serialize order.created event for audit, storing fallback payload", ex);
            return String.format(
                    "{\"eventType\":\"%s\",\"orderId\":%d,\"userId\":\"%s\"}",
                    event.getEventType(),
                    event.getOrderId(),
                    event.getUserId()
            );
        }
    }
}
