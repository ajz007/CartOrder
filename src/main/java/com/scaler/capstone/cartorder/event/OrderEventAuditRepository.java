package com.scaler.capstone.cartorder.event;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventAuditRepository extends JpaRepository<OrderEventAudit, Long> {
}
