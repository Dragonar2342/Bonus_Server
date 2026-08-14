package ru.zolotuhin.bonus_server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "bonus_card",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bonus_card_card_number",
                        columnNames = "card_number"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BonusCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "card_number",
            nullable = false,
            length = 16
    )
    private String cardNumber;

    @Column(
            name = "balance",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal balance;

    @Column(
            name = "created_at",
            nullable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    public void applyBalanceChange(BigDecimal change) {
        this.balance = this.balance.add(change);
        this.updatedAt = OffsetDateTime.now();
    }
}
