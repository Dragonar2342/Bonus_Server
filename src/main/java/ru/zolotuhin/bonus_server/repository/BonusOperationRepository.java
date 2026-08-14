package ru.zolotuhin.bonus_server.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.zolotuhin.bonus_server.entity.BonusOperation;

import java.util.Optional;

public interface BonusOperationRepository
        extends JpaRepository<BonusOperation, Long> {
    Page<BonusOperation> findByCardCardNumber(String cardNumber, Pageable pageable);
    Optional<BonusOperation> findByIdAndCardNumber(Long operationId, String cardNumber);
    boolean existsByReversalOfId(Long operationId);
}
