package ru.zolotuhin.bonus_server.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MoneyRequest(
        @NotNull(message = "Сумма обязательна")
        @DecimalMin(
                value = "0.01",
                message = "Сумма должна быть больше нуля"
        ) BigDecimal amount) { }
