package com.nutrimate.repository;

import com.nutrimate.entity.ExpertWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ExpertWalletRepository extends JpaRepository<ExpertWallet, String> {
    Optional<ExpertWallet> findByUserId(String userId);
}