package ru.zolotuhin.bonus_server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Логин обязателен")
        @Size(min = 3, max = 100, message = "Логин должен содержать от 3 до 100 символов")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Логин может содержать только латинские буквы, цифры и символы ._-")
        String username,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 6, max = 20, message = "Пароль должен содержать от 6 до 20 символов")
        String password
) {
}
