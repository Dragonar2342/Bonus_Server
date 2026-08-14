package ru.zolotuhin.bonus_server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "bonus_operation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bonus_operation_reversal_of",
                        columnNames = "reversal_of_operation_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BonusOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "card_id",
            nullable = false
    )
    private BonusCard card;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "operation_type",
            nullable = false,
            length = 20
    )
    private OperationType type;

    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "balance_change",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal balanceChange;

    @Column(
            name = "balance_after",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal balanceAfter;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reversal_of_operation_id"
    )
    private BonusOperation reversalOf;

    @Column(
            name = "created_at",
            nullable = false
    )
    private OffsetDateTime createdAt;
}
