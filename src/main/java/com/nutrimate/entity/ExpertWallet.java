package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "Expert_Wallets")
@Data
public class ExpertWallet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_id")
    private String id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private BigDecimal balance;
    
    @Column(name = "total_earnings")
    private BigDecimal totalEarnings;
}