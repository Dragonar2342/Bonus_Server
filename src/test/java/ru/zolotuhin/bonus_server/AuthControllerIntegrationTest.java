package ru.zolotuhin.bonus_server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.zolotuhin.bonus_server.repository.AppRoleRepository;
import ru.zolotuhin.bonus_server.repository.AppUserRepository;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.security.admin.username=test-admin",
        "app.security.admin.password=test-password"
})
class AuthControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AppRoleRepository appRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        appUserRepository.deleteAll();
    }

    @Test
    void shouldRegisterUser() throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "reader",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("reader"))
                .andExpect(jsonPath("$.roles[0]").value("READ"))
                .andExpect(jsonPath("$.message")
                        .value("Пользователь зарегистрирован."))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void shouldStorePasswordAsBcryptHash() throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "reader",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isCreated());

        var user = appUserRepository
                .findByUsernameIgnoreCase("reader")
                .orElseThrow();

        assertThat(user.getPasswordHash())
                .isNotEqualTo("secret123");

        assertThat(user.getPasswordHash())
                .startsWith("$2");

        assertThat(
                passwordEncoder.matches("secret123", user.getPasswordHash()))
                .isTrue();
    }

    @Test
    void shouldNeverStorePlainPassword() throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "reader",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isCreated());

        var user = appUserRepository
                .findByUsernameIgnoreCase("reader")
                .orElseThrow();

        assertThat(user.getPasswordHash())
                .doesNotContain("secret123");
    }

    @Test
    void shouldAssignReadRoleOnRegistration() throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "reader",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("READ"));

        var user = appUserRepository
                .findByUsernameIgnoreCase("reader")
                .orElseThrow();

        assertThat(user.getRoles())
                .hasSize(1);

        assertThat(user.getRoles().iterator().next().getName().name())
                .isEqualTo("READ");
    }

    @Test
    void shouldLoginWithCorrectPassword() throws Exception {

        registerReader();

        mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "username": "reader",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("reader"))
                .andExpect(jsonPath("$.roles[0]").value("READ"))
                .andExpect(jsonPath("$.message")
                        .value("Логин и пароль подтверждены."))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void shouldRejectWrongPasswordDuringLogin() throws Exception {

        registerReader();

        mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "reader",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldRejectUnknownUserDuringLogin() throws Exception {

        mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "unknown-user",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldRejectDuplicateUsername() throws Exception {

        registerReader();

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "reader",
                                  "password": "another-password"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectDuplicateUsernameIgnoringCase() throws Exception {

        registerReader();

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "READER",
                                  "password": "another-password"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldTrimUsernameDuringRegistration() throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "  reader  ",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("reader"));
    }

    @Test
    void shouldRejectTooShortUsername() throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "ab",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTooShortPassword() throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "reader",
                                  "password": "123"
                                }
                                """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectEmptyUsername() throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectEmptyPassword() throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "reader",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidUsernameCharacters() throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "reader@test",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedWithoutBasicAuthentication() throws Exception {

        mockMvc.perform(get("/api/v1/bonus-cards/12345678/balance"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedForWrongBasicPassword() throws Exception {

        registerReader();

        mockMvc.perform(get("/api/v1/bonus-cards/12345678/balance")
                            .header("Authorization", basicAuth("reader", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedForUnknownBasicUser() throws Exception {

        mockMvc.perform(get("/api/v1/bonus-cards/12345678/balance")
                            .header("Authorization", basicAuth("unknown", "secret123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowReadRoleToRead() throws Exception {

        registerReader();

        mockMvc.perform(get("/api/v1/bonus-cards/12345678/balance")
                            .header("Authorization", basicAuth("reader", "secret123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldForbidReadRoleFromWriting() throws Exception {

        registerReader();

        mockMvc.perform(post("/api/v1/bonus-cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", basicAuth("reader", "secret123"))
                            .content(
                                """
                                {
                                  "cardNumber": "12345678"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotExposeToken() throws Exception {

        registerReader();

        String response = mockMvc.perform(post("/api/v1/auth/login")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                """
                                                {
                                                  "username": "reader",
                                                  "password": "secret123"
                                                }
                                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("token");

        assertThat(response).doesNotContain("jwt");

        assertThat(response).doesNotContain("bearer");
    }

    @Test
    void shouldUseCaseInsensitiveUsernameDuringLogin() throws Exception {

        registerReader();

        mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "READER",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("reader"));
    }

    private void registerReader() throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "username": "reader",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private String basicAuth(String username, String password) {

        String credentials = username + ":" + password;

        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
