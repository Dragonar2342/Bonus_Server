package ru.zolotuhin.bonus_server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCardRequest(
        @NotBlank(message = "Номер карты не должен быть пустым")
        @Size(min = 8, max = 16, message = "Номер карты должен содержать от 8 до 16 символов")
        @Pattern(
                regexp = "^[0-9]+$",
                message = "Номер карты должен содержать только цифры"
        ) String cardNumber) {
}
