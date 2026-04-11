package com.scaler.capstone.cartorder.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "order_event_audit")
public class OrderEventAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String userId;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private Instant processedAt;

    @PrePersist
    public void prePersist() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }
}
