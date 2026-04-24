package com.potterlim.daylog;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import com.potterlim.daylog.dto.auth.RegisterUserAccountCommand;
import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.repository.IDailyLogEntryRepository;
import com.potterlim.daylog.repository.IUserAccountRepository;
import com.potterlim.daylog.repository.IUserEmailVerificationTokenRepository;
import com.potterlim.daylog.repository.IUserPasswordResetTokenRepository;
import com.potterlim.daylog.service.IAuthenticationMailService;
import com.potterlim.daylog.service.IDailyLogService;
import com.potterlim.daylog.service.IUserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebFlowIntegrationTests {

    private static final ZoneId TEST_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final LocalDate TEST_CURRENT_DATE = LocalDate.of(2026, 4, 24);

    @Autowired
    private MockMvc mMockMvc;

    @Autowired
    private IDailyLogService mDailyLogService;

    @Autowired
    private IUserAccountService mUserAccountService;

    @Autowired
    private IDailyLogEntryRepository mDailyLogEntryRepository;

    @Autowired
    private IUserAccountRepository mUserAccountRepository;

    @Autowired
    private IUserPasswordResetTokenRepository mUserPasswordResetTokenRepository;

    @Autowired
    private IUserEmailVerificationTokenRepository mUserEmailVerificationTokenRepository;

    @MockitoBean
    private IAuthenticationMailService mAuthenticationMailService;

    @MockitoBean
    private Clock mClock;

    @BeforeEach
    void setUpTestEnvironment() {
        Instant testInstant = TEST_CURRENT_DATE.atStartOfDay(TEST_ZONE_ID).toInstant();
        when(mClock.instant()).thenReturn(testInstant);
        when(mClock.getZone()).thenReturn(TEST_ZONE_ID);

        mDailyLogEntryRepository.deleteAll();
        mUserPasswordResetTokenRepository.deleteAll();
        mUserEmailVerificationTokenRepository.deleteAll();
        mUserAccountRepository.deleteAll();
    }

    @Test
    void registerShouldCreateUserAndRedirectToHome() throws Exception {
        AtomicReference<String> sentVerificationUrl = new AtomicReference<>();
        doAnswer(invocation -> {
            sentVerificationUrl.set(invocation.getArgument(1));
            return null;
        }).when(mAuthenticationMailService).sendEmailVerificationMail(any(UserAccount.class), anyString());

        mMockMvc.perform(post("/register")
                .with(csrf())
                .param("userName", "tester")
                .param("emailAddress", "tester@example.com")
                .param("password", "pass1234")
                .param("confirmPassword", "pass1234"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        UserAccount userAccount = mUserAccountRepository.findByUserName("tester").orElseThrow();
        assertEquals("tester@example.com", userAccount.getEmailAddress());
        assertFalse(userAccount.hasVerifiedEmailAddress());
        assertNotNull(sentVerificationUrl.get());
    }

    @Test
    void registerShouldRejectShortPassword() throws Exception {
        mMockMvc.perform(post("/register")
                .with(csrf())
                .param("userName", "tester")
                .param("emailAddress", "tester@example.com")
                .param("password", "pass12")
                .param("confirmPassword", "pass12"))
            .andExpect(status().isOk());

        assertTrue(mUserAccountRepository.findByUserName("tester").isEmpty());
    }

    @Test
    void loginShouldShowGenericErrorWhenLoginIdentifierDoesNotExist() throws Exception {
        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("loginIdentifier", "missing-user")
                .param("password", "wrong-password"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("로그인 정보가 올바르지 않습니다.")));
    }

    @Test
    void loginShouldShowGenericErrorWhenPasswordIsWrong() throws Exception {
        mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("tester", "tester@example.com", "pass1234")
        );

        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("loginIdentifier", "tester")
                .param("password", "wrong-password")
                .param("rememberMe", "true"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("로그인 정보가 올바르지 않습니다.")));
    }

    @Test
    void loginShouldAcceptEmailAddress() throws Exception {
        mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("tester", "tester@example.com", "pass1234")
        );

        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("loginIdentifier", "tester@example.com")
                .param("password", "pass1234"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }

    @Test
    void forgotPasswordShouldReturnGenericResponseForUnknownEmailAddress() throws Exception {
        mMockMvc.perform(post("/forgot-password")
                .with(csrf())
                .param("emailAddress", "missing@example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/forgot-password"));

        assertEquals(0, mUserPasswordResetTokenRepository.count());
        verify(mAuthenticationMailService, never()).sendPasswordResetMail(any(UserAccount.class), anyString());
        verify(mAuthenticationMailService, never()).sendEmailVerificationMail(any(UserAccount.class), anyString());
    }

    @Test
    void forgotPasswordShouldResendEmailVerificationForUnverifiedEmailAddress() throws Exception {
        AtomicReference<String> sentVerificationUrl = new AtomicReference<>();
        doAnswer(invocation -> {
            sentVerificationUrl.set(invocation.getArgument(1));
            return null;
        }).when(mAuthenticationMailService).sendEmailVerificationMail(any(UserAccount.class), anyString());

        mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("unverified-user", "unverified@example.com", "pass1234")
        );

        mMockMvc.perform(post("/forgot-password")
                .with(csrf())
                .param("emailAddress", "unverified@example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/forgot-password"));

        verify(mAuthenticationMailService, never()).sendPasswordResetMail(any(UserAccount.class), anyString());
        assertNotNull(sentVerificationUrl.get());
    }

    @Test
    void resetPasswordShouldAllowLoginWithNewPassword() throws Exception {
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
                .param("userName", "reset-user")
                .param("emailAddress", "reset-user@example.com")
                .param("password", "pass1234")
                .param("confirmPassword", "pass1234"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        String rawVerificationToken = extractTokenFromUrl(sentVerificationUrl.get(), "token");
        mMockMvc.perform(get("/verify-email").param("token", rawVerificationToken))
            .andExpect(status().is3xxRedirection());

        mMockMvc.perform(post("/forgot-password")
                .with(csrf())
                .param("emailAddress", "reset-user@example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/forgot-password"));

        String rawToken = extractTokenFromUrl(sentResetPasswordUrl.get(), "token");
        mMockMvc.perform(post("/reset-password")
                .with(csrf())
                .param("token", rawToken)
                .param("password", "pass6789")
                .param("confirmPassword", "pass6789"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?passwordResetSuccess"));

        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("loginIdentifier", "reset-user")
                .param("password", "pass1234"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("로그인 정보가 올바르지 않습니다.")));

        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("loginIdentifier", "reset-user@example.com")
                .param("password", "pass6789"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }

    @Test
    void changePasswordShouldPersistNewPassword() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("changer", "changer@example.com", "pass1234")
        );

        mMockMvc.perform(post("/account/password")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("currentPassword", "pass1234")
                .param("newPassword", "pass6789")
                .param("confirmNewPassword", "pass6789"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/account/password"));

        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("loginIdentifier", "changer")
                .param("password", "pass1234"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("로그인 정보가 올바르지 않습니다.")));

        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("loginIdentifier", "changer")
                .param("password", "pass6789"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }

    @Test
    void morningSaveShouldPersistDailyLogEntry() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("writer", "writer@example.com", "pass1234")
        );

        mMockMvc.perform(post("/daily-log/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", "2026-04-01")
                .param("goals", "운동하기\n책 읽기")
                .param("focus", "집중 업무")
                .param("challenges", "피곤함 관리"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/daily-log/morning"));

        String markdownText = mDailyLogService.readLogFileContent(LocalDate.of(2026, 4, 1), userAccount.getUserAccountId());

        assertTrue(markdownText.contains("## 오늘의 목표"));
        assertTrue(markdownText.contains("- 운동하기"));
        assertTrue(markdownText.contains("- 책 읽기"));
        assertTrue(markdownText.contains("## 집중 영역"));
        assertEquals(1, mDailyLogEntryRepository.count());
    }

    @Test
    void blankMorningSaveShouldNotCreateVisibleDailyLogEntry() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("blank-writer", "blank-writer@example.com", "pass1234")
        );

        mMockMvc.perform(post("/daily-log/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString())
                .param("goals", "   ")
                .param("focus", "")
                .param("challenges", "\n"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/daily-log/morning"));

        assertEquals(0, mDailyLogEntryRepository.count());

        mMockMvc.perform(get("/daily-log/morning")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString(
                "href=\"/daily-log/morning/edit?date=" + TEST_CURRENT_DATE + "\""
            ))));

        mMockMvc.perform(get("/daily-log/week")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString(TEST_CURRENT_DATE.toString()))))
            .andExpect(content().string(not(containsString("0 / 0 완료"))));

        mMockMvc.perform(get("/daily-log/preview")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    void clearingMorningSectionsShouldRemoveDailyLogEntry() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("clear-writer", "clear-writer@example.com", "pass1234")
        );

        mMockMvc.perform(post("/daily-log/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString())
                .param("goals", "운동하기")
                .param("focus", "집중 업무")
                .param("challenges", "피곤함 관리"))
            .andExpect(status().is3xxRedirection());

        assertEquals(1, mDailyLogEntryRepository.count());

        mMockMvc.perform(post("/daily-log/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString())
                .param("goals", "")
                .param("focus", "")
                .param("challenges", ""))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/daily-log/morning"));

        assertEquals(0, mDailyLogEntryRepository.count());
    }

    @Test
    void previewShouldOmitEmptySectionHeaders() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("preview-writer", "preview-writer@example.com", "pass1234")
        );

        mMockMvc.perform(post("/daily-log/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString())
                .param("goals", "운동하기")
                .param("focus", "")
                .param("challenges", ""))
            .andExpect(status().is3xxRedirection());

        mMockMvc.perform(get("/daily-log/preview")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("오늘의 목표")))
            .andExpect(content().string(containsString("운동하기")))
            .andExpect(content().string(not(containsString("집중 영역"))))
            .andExpect(content().string(not(containsString("예상 변수"))));
    }

    @Test
    void morningListShouldRenderSavedDate() throws Exception {
        LocalDate morningDate = LocalDate.now(mClock);
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("planner", "planner@example.com", "pass1234")
        );

        mMockMvc.perform(post("/daily-log/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", morningDate.toString())
                .param("goals", "운동하기")
                .param("focus", "중요 업무")
                .param("challenges", "집중 유지"))
            .andExpect(status().is3xxRedirection());

        mMockMvc.perform(get("/daily-log/morning")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(morningDate.toString())));
    }

    @Test
    void eveningPageShouldRenderTheSameMondayToSundayRangeItQueries() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("range-reviewer", "range-reviewer@example.com", "pass1234")
        );

        mMockMvc.perform(get("/daily-log/evening")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("2026-04-20 ~ 2026-04-26")))
            .andExpect(content().string(not(containsString("2026-04-21 ~ 2026-04-27"))));
    }

    @Test
    void coreProductPagesShouldRender() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("reviewer", "reviewer@example.com", "pass1234")
        );

        mMockMvc.perform(get("/")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("오늘의 계획을 세우고, 저녁에 실행을 확인하고")));

        mMockMvc.perform(get("/daily-log/evening")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("저녁 회고")));

        mMockMvc.perform(get("/daily-log/week")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("주간 리뷰")));
    }

    @Test
    void healthEndpointShouldBePublicAndUp() throws Exception {
        mMockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"status\":\"UP\"")));
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
