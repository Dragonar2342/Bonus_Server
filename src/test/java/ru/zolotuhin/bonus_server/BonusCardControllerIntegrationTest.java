package ru.zolotuhin.bonus_server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.zolotuhin.bonus_server.repository.BonusCardRepository;
import ru.zolotuhin.bonus_server.repository.BonusOperationRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BonusCardControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BonusCardRepository bonusCardRepository;

    @Autowired
    private BonusOperationRepository bonusOperationRepository;

    @BeforeEach
    void cleanDatabase() {
        bonusOperationRepository.deleteAll();
        bonusCardRepository.deleteAll();
    }

    @Test
    void shouldCreateCardThroughRestApi() throws Exception {

        mockMvc.perform(
                        post("/api/v1/bonus-cards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "cardNumber": "12345678"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.cardNumber")
                                .value("12345678")
                )
                .andExpect(
                        jsonPath("$.balance")
                                .value(0.0)
                );
    }

    @Test
    void shouldAccrueBonusesThroughRestApi() throws Exception {

        createCard();

        mockMvc.perform(
                        post("/api/v1/bonus-cards/12345678/accruals")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "amount": 100.00
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.cardNumber")
                                .value("12345678")
                )
                .andExpect(
                        jsonPath("$.balance")
                                .value(100.0)
                );
    }

    @Test
    void shouldDebitBonusesThroughRestApi() throws Exception {

        createCard();

        accrue100();

        mockMvc.perform(
                        post("/api/v1/bonus-cards/12345678/debits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "amount": 40.00
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.balance")
                                .value(60.0)
                );
    }

    @Test
    void shouldGetBalanceThroughRestApi() throws Exception {

        createCard();

        accrue100();

        mockMvc.perform(
                        get("/api/v1/bonus-cards/12345678/balance")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.cardNumber")
                                .value("12345678")
                )
                .andExpect(
                        jsonPath("$.balance")
                                .value(100.0)
                );
    }

    @Test
    void shouldGetOperationHistoryThroughRestApi()
            throws Exception {

        createCard();

        accrue100();

        mockMvc.perform(
                        post("/api/v1/bonus-cards/12345678/debits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "amount": 30.00
                                }
                                """)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/v1/bonus-cards/12345678/operations")
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.content[0].type")
                                .value("DEBIT")
                )
                .andExpect(
                        jsonPath("$.content[1].type")
                                .value("ACCRUAL")
                );
    }

    @Test
    void shouldReturn404ForUnknownCard()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/bonus-cards/12345678/balance")
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                );
    }

    @Test
    void shouldReturn409ForDuplicateCard()
            throws Exception {

        createCard();

        mockMvc.perform(
                        post("/api/v1/bonus-cards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "cardNumber": "12345678"
                                }
                                """)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                );
    }

    @Test
    void shouldReturn400ForInvalidCardNumber()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/bonus-cards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "cardNumber": "123"
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                );
    }

    @Test
    void shouldReturn400ForInvalidAmount()
            throws Exception {

        createCard();

        mockMvc.perform(
                        post("/api/v1/bonus-cards/12345678/accruals")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "amount": 0
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                );
    }

    private void createCard() throws Exception {

        mockMvc.perform(
                        post("/api/v1/bonus-cards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "cardNumber": "12345678"
                                }
                                """)
                )
                .andExpect(status().isCreated());
    }

    private void accrue100() throws Exception {

        mockMvc.perform(
                        post("/api/v1/bonus-cards/12345678/accruals")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "amount": 100.00
                                }
                                """)
                )
                .andExpect(status().isOk());
    }
}
