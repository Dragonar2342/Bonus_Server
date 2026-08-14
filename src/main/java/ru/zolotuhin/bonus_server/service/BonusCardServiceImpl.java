package ru.zolotuhin.bonus_server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.zolotuhin.bonus_server.dto.BalanceResponse;
import ru.zolotuhin.bonus_server.dto.CreateCardRequest;
import ru.zolotuhin.bonus_server.dto.MoneyRequest;
import ru.zolotuhin.bonus_server.dto.OperationResponse;
import ru.zolotuhin.bonus_server.dto.PageResponse;
import ru.zolotuhin.bonus_server.entity.BonusCard;
import ru.zolotuhin.bonus_server.entity.BonusOperation;
import ru.zolotuhin.bonus_server.entity.OperationType;
import ru.zolotuhin.bonus_server.exception.CardAlreadyExistsException;
import ru.zolotuhin.bonus_server.exception.CardNotFoundException;
import ru.zolotuhin.bonus_server.exception.InsufficientBonusException;
import ru.zolotuhin.bonus_server.exception.OperationAlreadyReversedException;
import ru.zolotuhin.bonus_server.exception.OperationNotFoundException;
import ru.zolotuhin.bonus_server.exception.OperationPersistenceException;
import ru.zolotuhin.bonus_server.repository.BonusCardRepository;
import ru.zolotuhin.bonus_server.repository.BonusOperationRepository;
import ru.zolotuhin.bonus_server.service.interfaces.BonusCardService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class BonusCardServiceImpl implements BonusCardService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final BonusCardRepository bonusCardRepository;
    private final BonusOperationRepository bonusOperationRepository;

    @Override
    public BalanceResponse createCard(CreateCardRequest request) {

        String normalizedCardNumber = normalizeCardNumber(request.cardNumber());

        if (bonusCardRepository.existsByCardNumber(normalizedCardNumber)) {
            throw new CardAlreadyExistsException(normalizedCardNumber);
        }

        OffsetDateTime now = OffsetDateTime.now();

        BonusCard card = BonusCard.builder()
                .cardNumber(normalizedCardNumber)
                .balance(ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            BonusCard savedCard = bonusCardRepository.save(card);
            return new BalanceResponse(savedCard.getCardNumber(), savedCard.getBalance());
        } catch (DataIntegrityViolationException exception) {
            throw new CardAlreadyExistsException(normalizedCardNumber);
        }
    }

    @Override
    public BalanceResponse accrue(String cardNumber, MoneyRequest request) {

        BonusCard card = getCardWithLock(cardNumber);
        BigDecimal amount = normalizeAmount(request.amount());
        BigDecimal newBalance = card.getBalance().add(amount);

        card.applyBalanceChange(amount);

        BonusOperation operation = BonusOperation.builder()
                .card(card)
                .type(OperationType.ACCRUAL)
                .amount(amount)
                .balanceChange(amount)
                .balanceAfter(newBalance)
                .createdAt(OffsetDateTime.now())
                .build();

        saveOperation(operation);

        return new BalanceResponse(card.getCardNumber(), card.getBalance());
    }

    @Override
    public BalanceResponse debit(String cardNumber, MoneyRequest request) {

        BonusCard card = getCardWithLock(cardNumber);

        BigDecimal amount = normalizeAmount(request.amount());

        if (card.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBonusException(card.getCardNumber(), card.getBalance(), amount);
        }

        BigDecimal balanceChange = amount.negate();
        BigDecimal newBalance = card.getBalance().add(balanceChange);

        card.applyBalanceChange(balanceChange);

        BonusOperation operation = BonusOperation.builder()
                .card(card)
                .type(OperationType.DEBIT)
                .amount(amount)
                .balanceChange(balanceChange)
                .balanceAfter(newBalance)
                .createdAt(OffsetDateTime.now())
                .build();

        saveOperation(operation);

        return new BalanceResponse(card.getCardNumber(), card.getBalance());
    }

    @Override
    public OperationResponse reverse(String cardNumber, Long operationId) {

        BonusCard card = getCardWithLock(cardNumber);

        BonusOperation originalOperation =
                bonusOperationRepository
                        .findByIdAndCard_CardNumber(operationId, card.getCardNumber())
                        .orElseThrow(() -> new OperationNotFoundException(operationId));

        if (originalOperation.getType() == OperationType.REVERSAL) {
            throw new IllegalArgumentException("Операция отмены не может быть обращена.");
        }

        if (bonusOperationRepository.existsByReversalOfId(operationId)) {
            throw new OperationAlreadyReversedException(operationId);
        }

        BigDecimal reversalChange = originalOperation.getBalanceChange().negate();
        BigDecimal newBalance = card.getBalance().add(reversalChange);

        if (newBalance.compareTo(ZERO) < 0) {
            throw new InsufficientBonusException(card.getCardNumber(), card.getBalance(), reversalChange.abs());
        }

        card.applyBalanceChange(reversalChange);

        BonusOperation reversalOperation = BonusOperation.builder()
                .card(card)
                .type(OperationType.REVERSAL)
                .amount(originalOperation.getAmount())
                .balanceChange(reversalChange)
                .balanceAfter(newBalance)
                .reversalOf(originalOperation)
                .createdAt(OffsetDateTime.now())
                .build();

        BonusOperation savedOperation = saveOperation(reversalOperation);

        return toResponse(savedOperation);
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String cardNumber) {

        BonusCard card = getCard(cardNumber);

        return new BalanceResponse(card.getCardNumber(), card.getBalance());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OperationResponse> getOperations(String cardNumber, int page, int size) {

        BonusCard card = getCard(cardNumber);

        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id"));

        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<BonusOperation> operations =
                bonusOperationRepository.findByCardCardNumber(card.getCardNumber(), pageRequest);

        return new PageResponse<>(
                operations
                        .map(this::toResponse)
                        .getContent(),
                operations.getNumber(),
                operations.getSize(),
                operations.getTotalElements(),
                operations.getTotalPages()
        );
    }

    private BonusCard getCard(String cardNumber) {

        String normalizedCardNumber =
                normalizeCardNumber(cardNumber);

        return bonusCardRepository
                .findByCardNumber(normalizedCardNumber)
                .orElseThrow(
                        () -> new CardNotFoundException(
                                normalizedCardNumber
                        )
                );
    }

    private BonusCard getCardWithLock(String cardNumber) {

        String normalizedCardNumber = normalizeCardNumber(cardNumber);

        return bonusCardRepository
                .findWithLockByCardNumber(normalizedCardNumber)
                .orElseThrow(() -> new CardNotFoundException(normalizedCardNumber));
    }

    private String normalizeCardNumber(String cardNumber) {

        if (cardNumber == null) {
            throw new IllegalArgumentException("Номер карты не должен быть пустым.");
        }

        String normalized = cardNumber.trim();

        if (!normalized.matches("\\d{8,16}")) {
            throw new IllegalArgumentException("Номер карты должен содержать от 8 до 16 цифр.");
        }

        return normalized;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {

        if (amount == null) {
            throw new IllegalArgumentException("Сумма обязательна.");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Сумма должна содержать не более 2 знаков после запятой.");
        }

        if (amount.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля.");
        }

        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private OperationResponse toResponse(
            BonusOperation operation
    ) {

        Long reversalOfOperationId = operation.getReversalOf() == null ? null : operation.getReversalOf().getId();

        return new OperationResponse(
                operation.getId(),
                operation.getCard().getCardNumber(),
                operation.getType(),
                operation.getAmount(),
                operation.getBalanceChange(),
                operation.getBalanceAfter(),
                reversalOfOperationId,
                operation.getCreatedAt()
        );
    }

    private BonusOperation saveOperation(BonusOperation operation) {
        try {
            return bonusOperationRepository.save(operation);
        } catch (DataIntegrityViolationException exception) {

            if (operation.getReversalOf() != null) {
                throw new OperationAlreadyReversedException(operation.getReversalOf().getId());
            }

            throw new OperationPersistenceException(exception);
        }
    }
}