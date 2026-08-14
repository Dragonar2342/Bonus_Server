package ru.zolotuhin.bonus_server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.zolotuhin.bonus_server.dto.BalanceResponse;
import ru.zolotuhin.bonus_server.dto.CreateCardRequest;
import ru.zolotuhin.bonus_server.dto.MoneyRequest;
import ru.zolotuhin.bonus_server.dto.OperationResponse;
import ru.zolotuhin.bonus_server.dto.PageResponse;
import ru.zolotuhin.bonus_server.entity.OperationType;
import ru.zolotuhin.bonus_server.exception.CardAlreadyExistsException;
import ru.zolotuhin.bonus_server.exception.CardNotFoundException;
import ru.zolotuhin.bonus_server.exception.InsufficientBonusException;
import ru.zolotuhin.bonus_server.exception.OperationAlreadyReversedException;
import ru.zolotuhin.bonus_server.exception.OperationNotFoundException;
import ru.zolotuhin.bonus_server.repository.BonusCardRepository;
import ru.zolotuhin.bonus_server.repository.BonusOperationRepository;
import ru.zolotuhin.bonus_server.service.interfaces.BonusCardService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BonusCardServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private BonusCardService bonusCardService;

    @Autowired
    private BonusCardRepository bonusCardRepository;

    @Autowired
    private BonusOperationRepository bonusOperationRepository;

    @BeforeEach
    void cleanDatabase() {
        bonusOperationRepository.deleteAll();bonusCardRepository.deleteAll();
    }

    @Test
    void shouldCreateCardWithZeroBalance() {

        BalanceResponse response = bonusCardService.createCard(new CreateCardRequest("12345678"));

        assertThat(response.cardNumber()).isEqualTo("12345678");
        assertThat(response.balance()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldNotCreateDuplicateCard() {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        assertThatThrownBy(() -> bonusCardService.createCard(new CreateCardRequest("12345678")))
                .isInstanceOf(CardAlreadyExistsException.class);
    }

    @Test
    void shouldAccrueBonuses() {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        BalanceResponse response =
                bonusCardService.accrue("12345678", new MoneyRequest(new BigDecimal("100.00")));

        assertThat(response.balance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void shouldDebitBonuses() {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        bonusCardService.accrue("12345678", new MoneyRequest(new BigDecimal("100.00")));

        BalanceResponse response =
                bonusCardService.debit("12345678", new MoneyRequest(new BigDecimal("40.00")));

        assertThat(response.balance())
                .isEqualByComparingTo("60.00");
    }

    @Test
    void shouldRejectDebitWhenBalanceIsInsufficient() {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        assertThatThrownBy(() ->
                bonusCardService.debit("12345678", new MoneyRequest(new BigDecimal("1.00"))))
                .isInstanceOf(InsufficientBonusException.class);
    }

    @Test
    void shouldRejectUnknownCard() {

        assertThatThrownBy(() -> bonusCardService.getBalance("12345678"))
                .isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void shouldReverseAccrual() {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        bonusCardService.accrue("12345678", new MoneyRequest(new BigDecimal("100.00")));

        PageResponse<OperationResponse> page =
                bonusCardService.getOperations("12345678", 0, 20);

        Long operationId = page.content().getFirst().id();

        OperationResponse reversal = bonusCardService.reverse("12345678", operationId);

        assertThat(reversal.type())
                .isEqualTo(OperationType.REVERSAL);

        BalanceResponse balance = bonusCardService.getBalance("12345678");

        assertThat(balance.balance())
                .isEqualByComparingTo("0.00");
    }

    @Test
    void shouldReverseDebit() {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        bonusCardService.accrue("12345678", new MoneyRequest(new BigDecimal("100.00")));

        bonusCardService.debit("12345678", new MoneyRequest(new BigDecimal("40.00")));

        PageResponse<OperationResponse> operations =
                bonusCardService.getOperations("12345678", 0, 20);

        Long debitOperationId =
                operations.content()
                        .stream()
                        .filter(operation -> operation.type() == OperationType.DEBIT)
                        .findFirst()
                        .orElseThrow()
                        .id();

        bonusCardService.reverse("12345678", debitOperationId);

        BalanceResponse balance = bonusCardService.getBalance("12345678");

        assertThat(balance.balance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void shouldNotAllowDoubleReversal() {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        bonusCardService.accrue("12345678", new MoneyRequest(new BigDecimal("100.00")));

        Long operationId = bonusCardService.getOperations("12345678", 0, 20)
                        .content()
                        .getFirst()
                        .id();

        bonusCardService.reverse("12345678", operationId);

        assertThatThrownBy(() -> bonusCardService.reverse("12345678", operationId))
                .isInstanceOf(OperationAlreadyReversedException.class);
    }

    @Test
    void shouldRejectReversalOfReversal() {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        bonusCardService.accrue("12345678", new MoneyRequest(new BigDecimal("100.00")));

        Long operationId = bonusCardService.getOperations("12345678", 0, 20)
                        .content()
                        .get(0)
                        .id();

        OperationResponse reversal = bonusCardService.reverse("12345678", operationId);

        assertThatThrownBy(() -> bonusCardService.reverse("12345678", reversal.id()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectUnknownOperation() {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        assertThatThrownBy(() -> bonusCardService.reverse("12345678", 999999L))
                .isInstanceOf(OperationNotFoundException.class);
    }

    @Test
    void shouldReturnOperationHistoryInStableOrder() {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        bonusCardService.accrue("12345678", new MoneyRequest(new BigDecimal("100.00")));

        bonusCardService.debit("12345678", new MoneyRequest(new BigDecimal("30.00")));

        PageResponse<OperationResponse> response =
                bonusCardService.getOperations("12345678", 0, 20);

        assertThat(response.totalElements())
                .isEqualTo(2);

        assertThat(response.content())
                .hasSize(2);

        assertThat(response.content().get(0).type())
                .isEqualTo(OperationType.DEBIT);

        assertThat(response.content().get(0).balanceAfter())
                .isEqualByComparingTo("70.00");

        assertThat(response.content().get(1).type())
                .isEqualTo(OperationType.ACCRUAL);

        assertThat(response.content().get(1).balanceAfter())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void shouldPaginateOperationHistory() {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        bonusCardService.accrue("12345678", new MoneyRequest(new BigDecimal("10.00")));

        bonusCardService.accrue("12345678", new MoneyRequest(new BigDecimal("20.00")));

        bonusCardService.accrue("12345678", new MoneyRequest(new BigDecimal("30.00")));

        PageResponse<OperationResponse> firstPage =
                bonusCardService.getOperations("12345678", 0, 2);

        PageResponse<OperationResponse> secondPage =
                bonusCardService.getOperations("12345678", 1, 2);

        assertThat(firstPage.content())
                .hasSize(2);

        assertThat(secondPage.content())
                .hasSize(1);

        assertThat(firstPage.totalElements())
                .isEqualTo(3);

        assertThat(firstPage.totalPages())
                .isEqualTo(2);
    }

    @Test
    void shouldRejectAmountWithMoreThanTwoDecimalPlaces() {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        assertThatThrownBy(() ->
                bonusCardService.accrue("12345678", new MoneyRequest(new BigDecimal("10.001"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectZeroAmount() {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        assertThatThrownBy(() ->
                bonusCardService.accrue("12345678", new MoneyRequest(new BigDecimal("0.00"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldKeepBalanceConsistentUnderConcurrentDebits() throws Exception {

        bonusCardService.createCard(new CreateCardRequest("12345678"));

        bonusCardService.accrue("12345678", new MoneyRequest(new BigDecimal("100.00")));

        int numberOfRequests = 2;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfRequests);

        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfRequests; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();

                try {
                    bonusCardService.debit("12345678", new MoneyRequest(new BigDecimal("60.00")));
                    return true;

                } catch (InsufficientBonusException exception) {
                    return false;
                }
            }));
        }

        startLatch.countDown();

        List<Boolean> results = new ArrayList<>();

        for (Future<Boolean> future : futures) {
            results.add(future.get(10, TimeUnit.SECONDS));
        }

        executor.shutdown();

        assertThat(results)
                .containsExactlyInAnyOrder(true, false);

        BalanceResponse balance = bonusCardService.getBalance("12345678");

        assertThat(balance.balance())
                .isEqualByComparingTo("40.00");
    }
}