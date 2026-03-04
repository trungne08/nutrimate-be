package com.nutrimate.repository;

import com.nutrimate.entity.PaymentOrderMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentOrderMappingRepository extends JpaRepository<PaymentOrderMapping, String> {
    Optional<PaymentOrderMapping> findByOrderCode(Long orderCode);
}
