package com.potterlim.daylog;

import java.net.URI;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.repository.IDailyLogEntryRepository;
import com.potterlim.daylog.repository.IUserAccountRepository;
import com.potterlim.daylog.repository.IUserEmailVerificationTokenRepository;
import com.potterlim.daylog.repository.IUserPasswordResetTokenRepository;
import com.potterlim.daylog.service.IAuthenticationMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
    "day-log.security.remember-me-key=mysql-integration-test-remember-me-key",
    "logging.file.path=./build/test-logs/mysql-integration"
})
class MySqlIntegrationTests {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0.41")
        .withDatabaseName("daylog_test")
        .withUsername("daylog")
        .withPassword("daylog");

    @Autowired
    private MockMvc mMockMvc;

    @Autowired
    private IUserAccountRepository mUserAccountRepository;

    @Autowired
    private IUserPasswordResetTokenRepository mUserPasswordResetTokenRepository;

    @Autowired
    private IUserEmailVerificationTokenRepository mUserEmailVerificationTokenRepository;

    @Autowired
    private IDailyLogEntryRepository mDailyLogEntryRepository;

    @MockitoBean
    private IAuthenticationMailService mAuthenticationMailService;

    @BeforeEach
    void setUpTestEnvironment() {
        mDailyLogEntryRepository.deleteAll();
        mUserPasswordResetTokenRepository.deleteAll();
        mUserEmailVerificationTokenRepository.deleteAll();
        mUserAccountRepository.deleteAll();
    }

    @Test
    void mysqlBackedVerificationAndRecoveryFlowShouldSucceed() throws Exception {
        AtomicReference<String> sentVerificationUrl = new AtomicReference<>();
        AtomicReference<String> sentResetPasswordUrl = new AtomicReference<>();

        doAnswer(invocation -> {
            sentVerificationUrl.set(invocation.getArgument(1));
            return null;
        }).when(mAuthenticationMailService).sendEmailVerificationMail(any(UserAccount.class), anyString());

        doAnswer(invocation -> {
            sentResetPasswordUrl.set(invocation.getArgument(1));
            return null;
        }).when(mAuthenticationMailService).sendPasswordResetMail(any(UserAccount.class), anyString());

        mMockMvc.perform(post("/register")
                .with(csrf())
                .param("userName", "mysql-user")
                .param("emailAddress", "mysql-user@example.com")
                .param("password", "pass1234")
                .param("confirmPassword", "pass1234"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        String rawVerificationToken = extractTokenFromUrl(sentVerificationUrl.get(), "token");
        mMockMvc.perform(get("/verify-email").param("token", rawVerificationToken))
            .andExpect(status().is3xxRedirection());

        mMockMvc.perform(post("/forgot-password")
                .with(csrf())
                .param("emailAddress", "mysql-user@example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/forgot-password"));

        String rawResetToken = extractTokenFromUrl(sentResetPasswordUrl.get(), "token");
        mMockMvc.perform(post("/reset-password")
                .with(csrf())
                .param("token", rawResetToken)
                .param("password", "pass6789")
                .param("confirmPassword", "pass6789"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?passwordResetSuccess"));

        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("loginIdentifier", "mysql-user@example.com")
                .param("password", "pass6789"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        UserAccount userAccount = mUserAccountRepository.findByEmailAddress("mysql-user@example.com").orElseThrow();
        assertTrue(userAccount.hasVerifiedEmailAddress());
        assertEquals(1, mUserAccountRepository.count());
        assertEquals(1, mUserPasswordResetTokenRepository.count());
        assertEquals(1, mUserEmailVerificationTokenRepository.count());
    }

    @Test
    void mysqlBackedMorningLogSaveShouldPersistEntry() throws Exception {
        UserAccount userAccount = mUserAccountRepository.save(
            UserAccount.createRegularUser(
                "mysql-planner",
                "mysql-planner@example.com",
                "$2a$10$PtR3XieP7rUoPxPAtxEvsOrHwLJ27LVa9Ezg4Q8VE3d2rYZRk5v7a"
            )
        );
        userAccount.markEmailAddressVerified(java.time.LocalDateTime.now());
        mUserAccountRepository.save(userAccount);

        mMockMvc.perform(post("/daily-log/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", "2026-04-08")
                .param("goals", "실행 로그 저장")
                .param("focus", "실제 MySQL 검증")
                .param("challenges", "환경 차이 점검"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/daily-log/morning"));

        mMockMvc.perform(get("/daily-log/morning")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(LocalDate.of(2026, 4, 8).toString())));

        assertEquals(1, mDailyLogEntryRepository.count());
    }

    private static String extractTokenFromUrl(String targetUrl, String parameterName) {
        assertNotNull(targetUrl);
        String query = URI.create(targetUrl).getQuery();
        assertNotNull(query);

        for (String queryParameter : query.split("&")) {
            String prefix = parameterName + "=";
            if (queryParameter.startsWith(prefix)) {
                return queryParameter.substring(prefix.length());
            }
        }

        throw new IllegalStateException("Expected query parameter was not found.");
    }
}
