package ru.zolotuhin.bonus_server.exception;

import java.math.BigDecimal;

public class InsufficientBonusException extends RuntimeException {
    public InsufficientBonusException(
            String cardNumber,
            BigDecimal currentBalance,
            BigDecimal requestedAmount
    ) { super( "Недостаточно бонусных средств на карте " + cardNumber
            + ". Текущий баланс: " + currentBalance
            + ", запрошено на списание: " + requestedAmount);
    }
}
