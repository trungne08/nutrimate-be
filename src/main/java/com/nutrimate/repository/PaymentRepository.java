package com.nutrimate.repository;

import com.nutrimate.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    // Lịch sử giao dịch của user
    List<Payment> findByUserIdOrderByPaymentDateDesc(String userId);
}