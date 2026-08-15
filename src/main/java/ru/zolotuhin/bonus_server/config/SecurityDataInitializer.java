package ru.zolotuhin.bonus_server.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.zolotuhin.bonus_server.entity.AppRole;
import ru.zolotuhin.bonus_server.entity.AppUser;
import ru.zolotuhin.bonus_server.entity.RoleName;
import ru.zolotuhin.bonus_server.repository.AppRoleRepository;
import ru.zolotuhin.bonus_server.repository.AppUserRepository;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class SecurityDataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin.username}")
    private String adminUsername;

    @Value("${app.security.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {

        if (adminUsername == null || adminUsername.isBlank()) {
            throw new IllegalStateException("Свойство app.security.admin.username не задано.");
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("Свойство app.security.admin.password не задано.");
        }

        String username = adminUsername.trim();

        AppRole readRole = appRoleRepository.findByName(RoleName.READ)
                .orElseThrow(() -> new IllegalStateException("Роль READ не найдена."));

        AppRole writeRole = appRoleRepository.findByName(RoleName.WRITE)
                .orElseThrow(() -> new IllegalStateException("Роль WRITE не найдена."));

        AppUser admin = appUserRepository
                .findByUsernameIgnoreCase(username)
                .orElse(null);

        if (admin == null) {admin = AppUser.builder()
                                .username(username)
                                .passwordHash(passwordEncoder.encode(adminPassword))
                                .enabled(true)
                                .createdAt(OffsetDateTime.now())
                                .build();

            admin.getRoles().add(readRole);
            admin.getRoles().add(writeRole);

            appUserRepository.save(admin);

            return;
        }

        boolean changed = false;

        if (!admin.isEnabled()) {
            admin.setEnabled(true);
            changed = true;
        }

        if (admin.getRoles().stream().noneMatch(role -> role.getName() == RoleName.READ)) {
            admin.getRoles().add(readRole);
            changed = true;
        }

        if (admin.getRoles().stream().noneMatch(role -> role.getName() == RoleName.WRITE)) {
            admin.getRoles().add(writeRole);
            changed = true;
        }

        if (changed) {
            appUserRepository.save(admin);
        }
    }
}
