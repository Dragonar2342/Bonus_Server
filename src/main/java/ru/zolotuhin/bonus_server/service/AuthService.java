package ru.zolotuhin.bonus_server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.zolotuhin.bonus_server.dto.AuthResponse;
import ru.zolotuhin.bonus_server.dto.LoginRequest;
import ru.zolotuhin.bonus_server.dto.RegisterRequest;
import ru.zolotuhin.bonus_server.entity.AppRole;
import ru.zolotuhin.bonus_server.entity.AppUser;
import ru.zolotuhin.bonus_server.entity.RoleName;
import ru.zolotuhin.bonus_server.exception.InvalidCredentialsException;
import ru.zolotuhin.bonus_server.exception.UserAlreadyExistsException;
import ru.zolotuhin.bonus_server.repository.AppRoleRepository;
import ru.zolotuhin.bonus_server.repository.AppUserRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String username = normalizeUsername(request.username());

        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new UserAlreadyExistsException(username);
        }

        AppRole readRole = appRoleRepository
                .findByName(RoleName.READ)
                .orElseThrow(() -> new IllegalStateException("Роль READ не найдена в базе данных."));

        AppUser user = AppUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.password()))
                .enabled(true)
                .createdAt(OffsetDateTime.now())
                .build();

        user.getRoles().add(readRole);

        try {
            AppUser savedUser = appUserRepository.save(user);

            return toResponse(savedUser, "Пользователь зарегистрирован.");
        } catch (DataIntegrityViolationException exception) {
            throw new UserAlreadyExistsException(username);
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        AppUser user = appUserRepository
                .findByUsernameIgnoreCase(normalizeUsername(request.username()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());

        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        return toResponse(user, "Логин и пароль подтверждены.");
    }

    private AuthResponse toResponse(AppUser user, String message
    ) {

        List<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList();

        return new AuthResponse(user.getId(), user.getUsername(), roles, message);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }
}
