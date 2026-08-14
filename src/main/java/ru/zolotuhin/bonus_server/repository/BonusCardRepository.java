package ru.zolotuhin.bonus_server.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import ru.zolotuhin.bonus_server.entity.BonusCard;

import java.util.Optional;

public interface BonusCardRepository extends JpaRepository<BonusCard, Long> {
    Optional<BonusCard> findByCardNumber(String cardNumber);
    boolean existsByCardNumber(String cardNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BonusCard> findWithLockByCardNumber(String cardNumber);
}
