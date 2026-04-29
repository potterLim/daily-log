package com.potterlim.daymark;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.potterlim.daymark.dto.auth.GoogleRegistrationSession;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.repository.IDaymarkEntryRepository;
import com.potterlim.daymark.repository.IOperationUsageEventRepository;
import com.potterlim.daymark.repository.IUserAccountRepository;
import com.potterlim.daymark.repository.IWeeklyOperationMetricSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    "daymark.security.remember-me-key=mysql-integration-test-remember-me-key",
    "logging.file.path=./build/test-logs/mysql-integration"
})
class MySqlIntegrationTests {

    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy. MM. dd.");

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0.41")
        .withDatabaseName("daymark_test")
        .withUsername("daymark")
        .withPassword("daymark");

    @Autowired
    private MockMvc mMockMvc;

    @Autowired
    private IUserAccountRepository mUserAccountRepository;

    @Autowired
    private IOperationUsageEventRepository mOperationUsageEventRepository;

    @Autowired
    private IWeeklyOperationMetricSnapshotRepository mWeeklyOperationMetricSnapshotRepository;

    @Autowired
    private IDaymarkEntryRepository mDaymarkEntryRepository;

    @BeforeEach
    void setUpTestEnvironment() {
        mWeeklyOperationMetricSnapshotRepository.deleteAll();
        mOperationUsageEventRepository.deleteAll();
        mDaymarkEntryRepository.deleteAll();
        mUserAccountRepository.deleteAll();
    }

    @Test
    void mysqlBackedGoogleWorkspaceRegistrationShouldSucceed() throws Exception {
        mMockMvc.perform(post("/register")
                .session(createGoogleRegistrationSession("mysql-google-subject", "mysql-user@example.com"))
                .with(csrf())
                .param("userName", "mysql-user")
                .param("password", "pass1234")
                .param("confirmPassword", "pass1234"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("loginIdentifier", "mysql-user@example.com")
                .param("password", "pass1234"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        UserAccount userAccount = mUserAccountRepository.findByEmailAddress("mysql-user@example.com").orElseThrow();
        assertTrue(userAccount.hasVerifiedEmailAddress());
        assertTrue(userAccount.hasConnectedGoogleAccount());
        assertEquals(1, mUserAccountRepository.count());
    }

    @Test
    void mysqlBackedMorningLogSaveShouldPersistEntry() throws Exception {
        LocalDate morningDate = LocalDate.now();

        UserAccount userAccount = mUserAccountRepository.save(
            UserAccount.createRegularUser(
                "mysql-planner",
                "mysql-planner@example.com",
                "$2a$10$PtR3XieP7rUoPxPAtxEvsOrHwLJ27LVa9Ezg4Q8VE3d2rYZRk5v7a"
            )
        );
        userAccount.markEmailAddressVerified(java.time.LocalDateTime.now());
        mUserAccountRepository.save(userAccount);

        mMockMvc.perform(post("/daymark/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", morningDate.toString())
                .param("goals", "실행 로그 저장")
                .param("focus", "실제 MySQL 검증")
                .param("challenges", "환경 차이 점검"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/daymark/morning"));

        mMockMvc.perform(get("/daymark/morning")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(DISPLAY_DATE_FORMATTER.format(morningDate))));

        assertEquals(1, mDaymarkEntryRepository.count());
    }

    private static MockHttpSession createGoogleRegistrationSession(String googleSubject, String emailAddress) {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute(
            GoogleRegistrationSession.SESSION_ATTRIBUTE_NAME,
            new GoogleRegistrationSession(googleSubject, emailAddress, "")
        );
        return mockHttpSession;
    }
}
