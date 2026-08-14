package ru.zolotuhin.bonus_server.dto;

import ru.zolotuhin.bonus_server.entity.OperationType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OperationResponse(
        Long id,
        String cardNumber,
        OperationType type,
        BigDecimal amount,
        BigDecimal balanceChange,
        BigDecimal balanceAfter,
        Long reversalOfOperationId,
        OffsetDateTime createdAt) { }
