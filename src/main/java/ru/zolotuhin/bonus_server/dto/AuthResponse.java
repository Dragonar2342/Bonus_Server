package ru.zolotuhin.bonus_server.dto;

import java.util.List;

public record AuthResponse(
        Long id,
        String username,
        List<String> roles,
        String message
) { }