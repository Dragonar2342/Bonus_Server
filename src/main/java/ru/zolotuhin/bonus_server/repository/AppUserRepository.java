package ru.zolotuhin.bonus_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.zolotuhin.bonus_server.entity.AppUser;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
}
