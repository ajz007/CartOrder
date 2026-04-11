package com.scaler.capstone.cartorder.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final String orderCreatedTopic;

    public OrderEventPublisher(
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
            @Value("${app.kafka.topic.order-created}") String orderCreatedTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderCreatedTopic = orderCreatedTopic;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(orderCreatedTopic, String.valueOf(event.getOrderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        LOGGER.error("Failed to publish order.created event for order {}", event.getOrderId(), ex);
                        return;
                    }
                    LOGGER.info("Published order.created event for order {}", event.getOrderId());
                });
    }
}
