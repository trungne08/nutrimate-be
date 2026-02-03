package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "`Expert_Wallets`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpertWallet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_id")
    private String id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private BigDecimal balance;
    
    @Column(name = "total_earnings")
    private BigDecimal totalEarnings;
}