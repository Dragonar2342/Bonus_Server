package ru.zolotuhin.bonus_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.zolotuhin.bonus_server.entity.AppRole;
import ru.zolotuhin.bonus_server.entity.RoleName;

import java.util.Optional;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {
    Optional<AppRole> findByName(RoleName name);
}
