package com.scaler.capstone.cartorder.payment.repository;

import com.scaler.capstone.cartorder.payment.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByOrderId(Long orderId);
}
