package ru.zolotuhin.bonus_server.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        String cardNumber,
        BigDecimal balance) { }
