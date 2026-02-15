package com.savadanko.aviasales.payment.repository;

import com.savadanko.aviasales.payment.entity.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransactionEntity, String> {
}

