package com.boundless.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "master_wallet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // A FinTech platform typically only has ONE master company wallet
    @Column(nullable = false, unique = true)
    private String companyName;

    @Column(nullable = false)
    private BigDecimal totalBalance;
}
