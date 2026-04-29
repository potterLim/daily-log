package com.potterlim.daymark;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import com.potterlim.daymark.dto.auth.GoogleRegistrationSession;
import com.potterlim.daymark.dto.auth.RegisterUserAccountCommand;
import com.potterlim.daymark.entity.EOperationEventType;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.entity.UserAccountId;
import com.potterlim.daymark.entity.WeeklyOperationMetricSnapshot;
import com.potterlim.daymark.repository.IDaymarkEntryRepository;
import com.potterlim.daymark.repository.IOperationUsageEventRepository;
import com.potterlim.daymark.repository.IUserAccountRepository;
import com.potterlim.daymark.repository.IWeeklyOperationMetricSnapshotRepository;
import com.potterlim.daymark.security.InMemoryRateLimiter;
import com.potterlim.daymark.service.AdministratorAccountInitializer;
import com.potterlim.daymark.service.IAlertNotificationService;
import com.potterlim.daymark.service.IDaymarkService;
import com.potterlim.daymark.service.IUserAccountService;
import com.potterlim.daymark.service.OperationUsageEventService;
import com.potterlim.daymark.service.WeeklyOperationsSummary;
import com.potterlim.daymark.support.EDaymarkSectionType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    private IDaymarkService mDaymarkService;

    @Autowired
    private IUserAccountService mUserAccountService;

    @Autowired
    private IDaymarkEntryRepository mDaymarkEntryRepository;

    @Autowired
    private IUserAccountRepository mUserAccountRepository;

    @Autowired
    private IOperationUsageEventRepository mOperationUsageEventRepository;

    @Autowired
    private OperationUsageEventService mOperationUsageEventService;

    @Autowired
    private AdministratorAccountInitializer mAdministratorAccountInitializer;

    @Autowired
    private IWeeklyOperationMetricSnapshotRepository mWeeklyOperationMetricSnapshotRepository;

    @Autowired
    private InMemoryRateLimiter mInMemoryRateLimiter;

    @MockitoBean
    private IAlertNotificationService mAlertNotificationService;

    @MockitoBean
    private Clock mClock;

    @BeforeEach
    void setUpTestEnvironment() {
        Instant testInstant = TEST_CURRENT_DATE.atStartOfDay(TEST_ZONE_ID).toInstant();
        when(mClock.instant()).thenReturn(testInstant);
        when(mClock.getZone()).thenReturn(TEST_ZONE_ID);

        mWeeklyOperationMetricSnapshotRepository.deleteAll();
        mOperationUsageEventRepository.deleteAll();
        mDaymarkEntryRepository.deleteAll();
        mUserAccountRepository.deleteAll();
        mInMemoryRateLimiter.clear();
    }

    @Test
    void registerShouldShowGoogleStartWhenGoogleIdentityIsNotConfirmed() throws Exception {
        mMockMvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Continue with Google")))
            .andExpect(content().string(containsString("google-g.svg")))
            .andExpect(content().string(containsString("Google 계정 확인 후 Workspace ID를 만듭니다.")))
            .andExpect(content().string(not(containsString("사용할 워크스페이스 ID를 입력하세요"))));
    }

    @Test
    void registerShouldCreateGoogleConnectedUserAndRedirectToHome() throws Exception {
        mMockMvc.perform(post("/register")
                .session(createGoogleRegistrationSession("google-subject-1", "tester@example.com"))
                .with(csrf())
                .param("userName", "tester")
                .param("password", "pass1234")
                .param("confirmPassword", "pass1234"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"))
            .andExpect(flash().attribute("accountSuccessMessage", "Workspace가 생성되었습니다."));

        UserAccount userAccount = mUserAccountRepository.findByUserName("tester").orElseThrow();
        assertEquals("tester@example.com", userAccount.getEmailAddress());
        assertTrue(userAccount.hasVerifiedEmailAddress());
        assertTrue(userAccount.hasConnectedGoogleAccount());
    }

    @Test
    void configuredAdministratorWorkspaceShouldReceiveAdministratorRole() throws Exception {
        mMockMvc.perform(post("/register")
                .session(createGoogleRegistrationSession("google-subject-admin", "admin@example.com"))
                .with(csrf())
                .param("userName", "potterLim")
                .param("password", "pass1234")
                .param("confirmPassword", "pass1234"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        UserAccount userAccount = mUserAccountRepository.findByUserName("potterLim").orElseThrow();
        assertTrue(userAccount.isAdministrator());
    }

    @Test
    void administratorInitializerShouldPromoteExistingConfiguredWorkspace() {
        UserAccount userAccount = UserAccount.createRegularUser(
            "potterlim",
            "existing-admin@example.com",
            "encoded-password"
        );
        mUserAccountRepository.saveAndFlush(userAccount);

        mAdministratorAccountInitializer.promoteConfiguredAdministratorAccounts();

        UserAccount administratorUserAccount = mUserAccountRepository.findByUserName("potterlim").orElseThrow();
        assertTrue(administratorUserAccount.isAdministrator());
    }

    @Test
    void registerShouldRejectShortPassword() throws Exception {
        mMockMvc.perform(post("/register")
                .session(createGoogleRegistrationSession("google-subject-2", "tester@example.com"))
                .with(csrf())
                .param("userName", "tester")
                .param("password", "pass12")
                .param("confirmPassword", "pass12"))
            .andExpect(status().isOk());

        assertTrue(mUserAccountRepository.findByUserName("tester").isEmpty());
    }

    @Test
    void registerShouldRejectDuplicateWorkspaceId() throws Exception {
        mMockMvc.perform(post("/register")
                .session(createGoogleRegistrationSession("google-subject-3", "first@example.com"))
                .with(csrf())
                .param("userName", "privacy-user")
                .param("password", "pass1234")
                .param("confirmPassword", "pass1234"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        mMockMvc.perform(post("/register")
                .session(createGoogleRegistrationSession("google-subject-4", "second@example.com"))
                .with(csrf())
                .param("userName", "privacy-user")
                .param("password", "pass1234")
                .param("confirmPassword", "pass1234"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("이미 사용 중인 워크스페이스 ID입니다.")));
    }

    @Test
    void registerShouldRedirectToGoogleStartWithoutPendingGoogleIdentity() throws Exception {
        mMockMvc.perform(post("/register")
                .with(csrf())
                .param("userName", "tester")
                .param("password", "pass1234")
                .param("confirmPassword", "pass1234"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/register"));

        assertTrue(mUserAccountRepository.findByUserName("tester").isEmpty());
    }

    @Test
    void forgotPasswordShouldRedirectToSignInHelp() throws Exception {
        mMockMvc.perform(get("/forgot-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/sign-in-help"));
    }

    @Test
    void signInHelpShouldRenderGoogleRecoveryPath() throws Exception {
        mMockMvc.perform(get("/sign-in-help"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("로그인 도움말")))
            .andExpect(content().string(containsString("Google로 접속 후 Account에서 비밀번호를 변경하세요.")))
            .andExpect(content().string(containsString("Continue with Google")))
            .andExpect(content().string(containsString("google-g.svg")));
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
    void loginShouldRenderProductStyledFieldValidationMessages() throws Exception {
        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("loginIdentifier", "")
                .param("password", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("워크스페이스 ID 또는 이메일을 입력해주세요.")))
            .andExpect(content().string(containsString("비밀번호를 입력해주세요.")));
    }

    @Test
    void loginShouldRateLimitRepeatedAttemptsByClient() throws Exception {
        for (int attemptIndex = 0; attemptIndex < 10; attemptIndex++) {
            mMockMvc.perform(post("/login")
                    .with(csrf())
                    .param("loginIdentifier", "missing-user-" + attemptIndex)
                    .param("password", "wrong-password"))
                .andExpect(status().isOk());
        }

        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("loginIdentifier", "missing-user-over-limit")
                .param("password", "wrong-password"))
            .andExpect(status().isTooManyRequests())
            .andExpect(content().string(containsString("요청이 잠시 제한되었습니다.")));
    }

    @Test
    void loginPageShouldCanonicalizeUnexpectedQueryString() throws Exception {
        mMockMvc.perform(get("/login?next=/daymark/preview?date=" + TEST_CURRENT_DATE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginShouldRedirectToSavedProductPathAfterAuthentication() throws Exception {
        mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("next-user", "next-user@example.com", "pass1234")
        );

        MvcResult protectedPageResult = mMockMvc.perform(get("/daymark/preview?date=" + TEST_CURRENT_DATE))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"))
            .andReturn();
        MockHttpSession session = (MockHttpSession) protectedPageResult.getRequest().getSession(false);
        assertNotNull(session);

        mMockMvc.perform(post("/login")
                .session(session)
                .with(csrf())
                .param("loginIdentifier", "next-user")
                .param("password", "pass1234"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/daymark/preview?date=" + TEST_CURRENT_DATE));
    }

    @Test
    void loginShouldIgnoreUnexpectedNextPathParameterAfterAuthentication() throws Exception {
        mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("safe-next-user", "safe-next-user@example.com", "pass1234")
        );

        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("loginIdentifier", "safe-next-user")
                .param("password", "pass1234")
                .param("nextPath", "https://example.com/daymark/preview"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
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
    void changePasswordShouldRenderProductStyledFieldValidationMessages() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("blank-changer", "blank-changer@example.com", "pass1234")
        );

        mMockMvc.perform(post("/account/password")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("currentPassword", "")
                .param("newPassword", "")
                .param("confirmNewPassword", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("현재 비밀번호를 입력해주세요.")))
            .andExpect(content().string(containsString("새 비밀번호를 입력해주세요.")))
            .andExpect(content().string(containsString("새 비밀번호 확인을 입력해주세요.")));
    }

    @Test
    void morningSaveShouldPersistDaymarkEntry() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("writer", "writer@example.com", "pass1234")
        );

        mMockMvc.perform(post("/daymark/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", "2026-04-01")
                .param("goals", "운동하기\n책 읽기")
                .param("focus", "집중 업무")
                .param("challenges", "피곤함 관리"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/daymark/morning"));

        String markdownText = mDaymarkService.readEntryMarkdownContent(LocalDate.of(2026, 4, 1), userAccount.getUserAccountId());

        assertTrue(markdownText.contains("## 오늘의 목표"));
        assertTrue(markdownText.contains("- 운동하기"));
        assertTrue(markdownText.contains("- 책 읽기"));
        assertTrue(markdownText.contains("## 집중 영역"));
        assertEquals(1, mDaymarkEntryRepository.count());
    }

    @Test
    void morningSaveShouldRejectOversizedInput() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("oversized-writer", "oversized-writer@example.com", "pass1234")
        );

        mMockMvc.perform(post("/daymark/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString())
                .param("goals", "가".repeat(301))
                .param("focus", "집중 업무")
                .param("challenges", "피곤함 관리"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("목표는 한 줄당 300자 이하로 입력해주세요.")));

        assertEquals(0, mDaymarkEntryRepository.count());
    }

    @Test
    void eveningSaveShouldRejectOversizedInput() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("oversized-reviewer", "oversized-reviewer@example.com", "pass1234")
        );

        mMockMvc.perform(post("/daymark/evening/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString())
                .param("achievements", "성과")
                .param("improvements", "개선")
                .param("gratitude", "감사".repeat(501))
                .param("notes", "메모"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("감사는 1,000자 이하로 입력해주세요.")));

        assertEquals(0, mDaymarkEntryRepository.count());
    }

    @Test
    void blankMorningSaveShouldNotCreateVisibleDaymarkEntry() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("blank-writer", "blank-writer@example.com", "pass1234")
        );

        mMockMvc.perform(post("/daymark/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString())
                .param("goals", "   ")
                .param("focus", "")
                .param("challenges", "\n"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/daymark/morning"));

        assertEquals(0, mDaymarkEntryRepository.count());

        mMockMvc.perform(get("/daymark/morning")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("아직 저장된 계획이 없습니다.")))
            .andExpect(content().string(not(containsString("계획 수정"))));

        mMockMvc.perform(get("/daymark/week")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("이번 주 기록이 없습니다.")))
            .andExpect(content().string(not(containsString("0 / 0 완료"))));

        mMockMvc.perform(get("/daymark/preview")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("아직 저장된 기록이 없습니다.")))
            .andExpect(content().string(containsString("New Plan")))
            .andExpect(content().string(not(containsString("오늘의 목표"))));
    }

    @Test
    void clearingMorningSectionsShouldRemoveDaymarkEntry() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("clear-writer", "clear-writer@example.com", "pass1234")
        );

        mMockMvc.perform(post("/daymark/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString())
                .param("goals", "운동하기")
                .param("focus", "집중 업무")
                .param("challenges", "피곤함 관리"))
            .andExpect(status().is3xxRedirection());

        assertEquals(1, mDaymarkEntryRepository.count());

        mMockMvc.perform(post("/daymark/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString())
                .param("goals", "")
                .param("focus", "")
                .param("challenges", ""))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/daymark/morning"));

        assertEquals(0, mDaymarkEntryRepository.count());
    }

    @Test
    void previewShouldOmitEmptySectionHeaders() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("preview-writer", "preview-writer@example.com", "pass1234")
        );

        mMockMvc.perform(post("/daymark/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString())
                .param("goals", "운동하기")
                .param("focus", "")
                .param("challenges", ""))
            .andExpect(status().is3xxRedirection());

        mMockMvc.perform(get("/daymark/preview")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("2026. 04. 24.")))
            .andExpect(content().string(not(containsString("읽기 전용"))))
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

        mMockMvc.perform(post("/daymark/morning/save")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", morningDate.toString())
                .param("goals", "운동하기")
                .param("focus", "중요 업무")
                .param("challenges", "집중 유지"))
            .andExpect(status().is3xxRedirection());

        mMockMvc.perform(get("/daymark/morning")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("2026. 04. 24.")));
    }

    @Test
    void eveningPageShouldRenderDirectDateSelectionWithoutWeekNavigation() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("evening-reviewer", "evening-reviewer@example.com", "pass1234")
        );

        mMockMvc.perform(get("/daymark/evening")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("저녁 회고")))
            .andExpect(content().string(containsString("날짜 선택")))
            .andExpect(content().string(containsString("Open Review")))
            .andExpect(content().string(containsString("/daymark/evening/edit?date=" + TEST_CURRENT_DATE)))
            .andExpect(content().string(not(containsString("주 이동"))))
            .andExpect(content().string(not(containsString("Prev"))))
            .andExpect(content().string(not(containsString("Current"))))
            .andExpect(content().string(not(containsString("Next"))));
    }

    @Test
    void weekPageShouldNavigateBetweenWeeks() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("week-reviewer", "week-reviewer@example.com", "pass1234")
        );
        LocalDate previousWeekDate = LocalDate.of(2026, 4, 13);

        mDaymarkService.writeSection(
            previousWeekDate,
            userAccount.getUserAccountId(),
            EDaymarkSectionType.GOALS,
            "- 이전 주 목표"
        );

        mMockMvc.perform(get("/daymark/week")
                .param("week", "-1")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("2026. 04. 13. ~ 2026. 04. 19.")))
            .andExpect(content().string(containsString("2026. 04. 13.")))
            .andExpect(content().string(containsString("Prev")))
            .andExpect(content().string(containsString("Current")))
            .andExpect(content().string(containsString("Next")))
            .andExpect(content().string(containsString("week-current-link")))
            .andExpect(content().string(not(containsString("week-nav-card-current"))))
            .andExpect(content().string(not(containsString("week-nav-trio"))))
            .andExpect(content().string(containsString("2026. 04. 06. ~ 2026. 04. 12.")))
            .andExpect(content().string(containsString("2026. 04. 20. ~ 2026. 04. 26.")))
            .andExpect(content().string(containsString("/daymark/week?week=-2")))
            .andExpect(content().string(containsString("/daymark/week")))
            .andExpect(content().string(containsString("/daymark/week?week=0")));
    }

    @Test
    void libraryShouldSearchTimelineAndExportSelectedRecords() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("library-reviewer", "library-reviewer@example.com", "pass1234")
        );
        UserAccountId userAccountId = userAccount.getUserAccountId();
        LocalDate matchingDate = LocalDate.of(2026, 4, 22);
        LocalDate otherDate = LocalDate.of(2026, 4, 24);

        mDaymarkService.writeSection(
            matchingDate,
            userAccountId,
            EDaymarkSectionType.GOALS,
            "- 검색 가능한 제품 흐름 점검"
        );
        mDaymarkService.writeSection(
            matchingDate,
            userAccountId,
            EDaymarkSectionType.EVENING_GOALS,
            "- [x] 검색 가능한 제품 흐름 점검"
        );
        mDaymarkService.writeSection(
            matchingDate,
            userAccountId,
            EDaymarkSectionType.ACHIEVEMENTS,
            "검색 가능한 성과를 남겼다."
        );
        mDaymarkService.writeSection(
            otherDate,
            userAccountId,
            EDaymarkSectionType.GOALS,
            "- 온보딩 메모 정리"
        );

        mMockMvc.perform(get("/daymark/library")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("from", "2026-04-20")
                .param("to", "2026-04-24")
                .param("keyword", "제품 흐름"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("기록 라이브러리")))
            .andExpect(content().string(containsString("타임라인")))
            .andExpect(content().string(containsString("목표 완료율 추이")))
            .andExpect(content().string(containsString("Preview PDF")))
            .andExpect(content().string(containsString("오늘의 목표")))
            .andExpect(content().string(containsString("검색 가능한 제품 흐름 점검")))
            .andExpect(content().string(containsString("검색 가능한 성과를 남겼다.")))
            .andExpect(content().string(not(containsString("온보딩 메모 정리"))));

        mMockMvc.perform(get("/daymark/library/export/markdown")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("from", "2026-04-20")
                .param("to", "2026-04-24")
                .param("keyword", "제품 흐름"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", containsString("attachment")))
            .andExpect(content().string(containsString("# Daymark 기록 라이브러리")))
            .andExpect(content().string(containsString("검색 가능한 제품 흐름 점검")))
            .andExpect(content().string(not(containsString("온보딩 메모 정리"))));

        mMockMvc.perform(get("/daymark/library/export/pdf")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("from", "2026-04-20")
                .param("to", "2026-04-24")
                .param("keyword", "제품 흐름"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("PDF 리포트 미리보기")))
            .andExpect(content().string(containsString("완료율")))
            .andExpect(content().string(containsString("선택한 기간")))
            .andExpect(content().string(containsString("Save PDF")))
            .andExpect(content().string(containsString("검색 가능한 성과를 남겼다.")));
    }

    @Test
    void coreProductPagesShouldRender() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("reviewer", "reviewer@example.com", "pass1234")
        );

        mMockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Daymark")))
            .andExpect(content().string(containsString("Sign In")))
            .andExpect(content().string(containsString("Create Account")))
            .andExpect(content().string(containsString("Contact:")))
            .andExpect(content().string(containsString("potterLim0808@gmail.com")))
            .andExpect(content().string(containsString("/daymark/morning/edit?date=" + TEST_CURRENT_DATE)))
            .andExpect(content().string(containsString("/daymark/evening/edit?date=" + TEST_CURRENT_DATE)))
            .andExpect(content().string(containsString("/daymark/preview?date=" + TEST_CURRENT_DATE)))
            .andExpect(content().string(not(containsString("/login?next="))))
            .andExpect(content().string(not(containsString("Sign Out"))));

        mMockMvc.perform(get("/")
            .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Today")))
            .andExpect(content().string(containsString("Daymark")))
            .andExpect(content().string(containsString("aria-label=\"Daymark\"")))
            .andExpect(content().string(containsString("daymark-logo.svg")))
            .andExpect(content().string(containsString("Private Workspace")))
            .andExpect(content().string(not(containsString("Local-first"))))
            .andExpect(content().string(not(containsString("하단 메뉴"))))
            .andExpect(content().string(containsString("Plan")))
            .andExpect(content().string(containsString("Check")))
            .andExpect(content().string(containsString("Keep")))
            .andExpect(content().string(containsString("Review")))
            .andExpect(content().string(containsString("View Today")))
            .andExpect(content().string(containsString("/daymark/morning/edit?date=" + TEST_CURRENT_DATE)))
            .andExpect(content().string(containsString("/daymark/evening/edit?date=" + TEST_CURRENT_DATE)))
            .andExpect(content().string(containsString("/daymark/preview?date=" + TEST_CURRENT_DATE)));

        mMockMvc.perform(get("/daymark/morning")
            .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("New Plan")))
            .andExpect(content().string(containsString("/daymark/morning/edit?date=" + TEST_CURRENT_DATE)));

        mMockMvc.perform(get("/daymark/morning/edit")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("한 줄에 하나씩 목표를 적어주세요")))
            .andExpect(content().string(containsString("예: 출시 전 화면 점검")))
            .andExpect(content().string(not(containsString("field-guide"))));

        mMockMvc.perform(get("/daymark/evening")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("저녁 회고")))
            .andExpect(content().string(containsString("Open Review")))
            .andExpect(content().string(containsString("New Review")))
            .andExpect(content().string(not(containsString("주 이동"))));

        mMockMvc.perform(get("/daymark/evening/edit")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount))
                .param("date", TEST_CURRENT_DATE.toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("<span class=\"content-badge\">읽기 전용</span>"))))
            .andExpect(content().string(containsString("아침 계획이 없습니다.")))
            .andExpect(content().string(not(containsString("아침 기록가 없습니다."))));

        mMockMvc.perform(get("/daymark/week")
            .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("주간 리뷰")))
            .andExpect(content().string(containsString("완료율")))
            .andExpect(content().string(containsString("Prev")))
            .andExpect(content().string(containsString("Current")))
            .andExpect(content().string(containsString("Next")))
            .andExpect(content().string(containsString("New Plan")));

        mMockMvc.perform(get("/daymark/library")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("기록 라이브러리")))
            .andExpect(content().string(containsString("Download MD")))
            .andExpect(content().string(containsString("Preview PDF")));

        mMockMvc.perform(get("/account/password")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("비밀번호 변경")))
            .andExpect(content().string(containsString("8자 이상 72자 이하")));

        mMockMvc.perform(get("/account")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("계정")))
            .andExpect(content().string(containsString("가입 Google 메일")))
            .andExpect(content().string(containsString("Workspace ID")))
            .andExpect(content().string(containsString("Workspace 생성일")))
            .andExpect(content().string(containsString("reviewer")))
            .andExpect(content().string(containsString("reviewer@example.com")))
            .andExpect(content().string(not(containsString(">Google</span>"))))
            .andExpect(content().string(not(containsString("연결됨"))))
            .andExpect(content().string(containsString("Change Password")));

        mMockMvc.perform(get("/images/daymark-logo.svg"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("daymark-bg")))
            .andExpect(content().string(containsString("rounded blue calendar mark")));
    }

    @Test
    void logoutShouldReturnToPublicHome() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("logout-reviewer", "logout-reviewer@example.com", "pass1234")
        );

        mMockMvc.perform(post("/logout")
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/?logout"));

        mMockMvc.perform(get("/").param("logout", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("로그아웃되었습니다.")))
            .andExpect(content().string(containsString("Sign In")))
            .andExpect(content().string(containsString("Create Account")))
            .andExpect(content().string(not(containsString("Sign Out"))));
    }

    @Test
    void missingProductPageShouldRenderFriendlyNotFoundPage() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("missing-page-reviewer", "missing-page-reviewer@example.com", "pass1234")
        );

        mMockMvc.perform(get("/missing-public-page"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("페이지를 찾을 수 없습니다.")))
            .andExpect(content().string(containsString("Home")))
            .andExpect(content().string(containsString("potterLim0808@gmail.com")))
            .andExpect(content().string(not(containsString("Library"))));

        mMockMvc.perform(get("/missing-product-page")
            .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("페이지를 찾을 수 없습니다.")))
            .andExpect(content().string(containsString("Home")))
            .andExpect(content().string(containsString("potterLim0808@gmail.com")))
            .andExpect(content().string(not(containsString("Library"))));
    }

    @Test
    void malformedProductRequestsShouldRenderNotFoundPage() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("malformed-user", "malformed-user@example.com", "pass1234")
        );

        mMockMvc.perform(get("/daymark/morning/edit")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("페이지를 찾을 수 없습니다.")))
            .andExpect(content().string(not(containsString("timestamp"))));

        mMockMvc.perform(get("/daymark/morning/edit")
                .param("date", "not-a-date")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("페이지를 찾을 수 없습니다.")))
            .andExpect(content().string(not(containsString("MethodArgumentTypeMismatchException"))));
    }

    @Test
    void errorEndpointShouldRenderNotFoundPageWithoutDiagnosticDetails() throws Exception {
        mMockMvc.perform(get("/error")
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                .requestAttr(
                    RequestDispatcher.ERROR_EXCEPTION,
                    new IllegalStateException("internal-secret")
                ))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("페이지를 찾을 수 없습니다.")))
            .andExpect(content().string(not(containsString("internal-secret"))))
            .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void protectedProductPagesShouldStillRequireLogin() throws Exception {
        for (String protectedPath : new String[] {
            "/account",
            "/account/password",
            "/admin/operations",
            "/daymark/morning",
            "/daymark/morning/edit",
            "/daymark/evening",
            "/daymark/evening/edit",
            "/daymark/week",
            "/daymark/library",
            "/daymark/library/export/markdown",
            "/daymark/library/export/pdf",
            "/daymark/preview"
        }) {
            mMockMvc.perform(get(protectedPath))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
        }
    }

    @Test
    void operationsDashboardShouldRequireAdministratorRole() throws Exception {
        mMockMvc.perform(get("/admin/operations")
                .with(SecurityMockMvcRequestPostProcessors.user("regular-user").roles("USER")))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("페이지를 찾을 수 없습니다.")));

        mMockMvc.perform(get("/admin/operations")
                .with(SecurityMockMvcRequestPostProcessors.user("operations-admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("운영 지표")))
            .andExpect(content().string(containsString("저장된 주간 스냅샷")));
    }

    @Test
    void operationsDashboardShouldExcludeAdministratorActivity() throws Exception {
        savePreviousWeeklyOperationsSnapshot();

        UserAccount firstUser = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("metrics-user-1", "metrics-user-1@example.com", "pass1234")
        );
        UserAccount secondUser = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("metrics-user-2", "metrics-user-2@example.com", "pass1234")
        );
        UserAccount adminUser = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("metrics-admin", "metrics-admin@example.com", "pass1234")
        );
        adminUser.grantAdministratorRole();
        mUserAccountRepository.saveAndFlush(adminUser);

        mDaymarkService.writeSection(
            TEST_CURRENT_DATE,
            firstUser.getUserAccountId(),
            EDaymarkSectionType.GOALS,
            "사용자 목표 1"
        );
        mDaymarkService.writeSection(
            TEST_CURRENT_DATE,
            firstUser.getUserAccountId(),
            EDaymarkSectionType.EVENING_GOALS,
            "- [x] 사용자 목표 1"
        );
        mDaymarkService.writeSection(
            TEST_CURRENT_DATE,
            secondUser.getUserAccountId(),
            EDaymarkSectionType.GOALS,
            "사용자 목표 2"
        );
        mDaymarkService.writeSection(
            TEST_CURRENT_DATE,
            secondUser.getUserAccountId(),
            EDaymarkSectionType.EVENING_GOALS,
            "- [ ] 사용자 목표 2"
        );
        mDaymarkService.writeSection(
            TEST_CURRENT_DATE,
            adminUser.getUserAccountId(),
            EDaymarkSectionType.GOALS,
            "관리자 목표"
        );
        mDaymarkService.writeSection(
            TEST_CURRENT_DATE,
            adminUser.getUserAccountId(),
            EDaymarkSectionType.EVENING_GOALS,
            "- [x] 관리자 목표"
        );

        mOperationUsageEventService.recordUserEvent(
            EOperationEventType.SIGN_IN_SUCCEEDED,
            firstUser.getUserAccountId()
        );
        mOperationUsageEventService.recordUserEvent(
            EOperationEventType.SIGN_IN_SUCCEEDED,
            secondUser.getUserAccountId()
        );
        mOperationUsageEventService.recordUserEvent(
            EOperationEventType.SIGN_IN_SUCCEEDED,
            adminUser.getUserAccountId()
        );
        mOperationUsageEventService.recordUserEvent(
            EOperationEventType.RECORD_LIBRARY_VIEWED,
            firstUser.getUserAccountId()
        );
        mOperationUsageEventService.recordUserEvent(
            EOperationEventType.RECORD_LIBRARY_VIEWED,
            adminUser.getUserAccountId()
        );
        mOperationUsageEventService.recordUserEvent(
            EOperationEventType.WEEKLY_REVIEW_VIEWED,
            firstUser.getUserAccountId()
        );
        mOperationUsageEventService.recordUserEvent(
            EOperationEventType.MARKDOWN_EXPORTED,
            firstUser.getUserAccountId()
        );
        mOperationUsageEventService.recordUserEvent(
            EOperationEventType.PDF_EXPORT_VIEWED,
            secondUser.getUserAccountId()
        );
        mOperationUsageEventService.recordUserEvent(
            EOperationEventType.MARKDOWN_EXPORTED,
            adminUser.getUserAccountId()
        );

        mMockMvc.perform(get("/admin/operations")
                .with(SecurityMockMvcRequestPostProcessors.user(adminUser)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Growth")))
            .andExpect(content().string(containsString("Routine")))
            .andExpect(content().string(containsString("Goal Completion")))
            .andExpect(content().string(containsString("Export")))
            .andExpect(content().string(containsString("Security")))
            .andExpect(content().string(containsString("Active Workspace")))
            .andExpect(content().string(containsString("New Workspace")))
            .andExpect(content().string(containsString("Routine Days")))
            .andExpect(content().string(containsString("Morning Plan")))
            .andExpect(content().string(containsString("Evening Review")))
            .andExpect(content().string(containsString("Plan → Review")))
            .andExpect(content().string(containsString("Weekly Review")))
            .andExpect(content().string(containsString("Exporting Workspace")))
            .andExpect(content().string(containsString("Markdown")))
            .andExpect(content().string(containsString("PDF")))
            .andExpect(content().string(containsString("50.0%")))
            .andExpect(content().string(not(containsString("66.7%"))))
            .andExpect(content().string(not(containsString("Records Viewed"))))
            .andExpect(content().string(not(containsString("Quality"))))
            .andExpect(content().string(containsString("주차별 성장 추이")))
            .andExpect(content().string(containsString("Base Date")))
            .andExpect(content().string(containsString("12W")))
            .andExpect(content().string(containsString("trend-line-active")))
            .andExpect(content().string(containsString("trend-line-goal")))
            .andExpect(content().string(containsString("<span>Sign In</span>")))
            .andExpect(content().string(containsString("<span>Failed Sign In</span>")))
            .andExpect(content().string(containsString("<strong>2</strong>")));
    }

    @Test
    void operationsDashboardShouldAllowTrendPeriodAndDateSelection() throws Exception {
        UserAccount adminUser = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("trend-admin", "trend-admin@example.com", "pass1234")
        );
        adminUser.grantAdministratorRole();
        mUserAccountRepository.saveAndFlush(adminUser);

        saveWeeklyOperationsSnapshot(LocalDate.of(2026, 3, 9), 1L, 1L, 40.0);
        saveWeeklyOperationsSnapshot(LocalDate.of(2026, 3, 23), 3L, 2L, 58.0);

        mMockMvc.perform(get("/admin/operations")
                .param("date", "2026-04-17")
                .param("weeks", "4")
                .with(SecurityMockMvcRequestPostProcessors.user(adminUser)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("value=\"2026-04-17\"")))
            .andExpect(content().string(containsString("4W")))
            .andExpect(content().string(containsString("03. 23.")))
            .andExpect(content().string(containsString("04. 13.")))
            .andExpect(content().string(not(containsString("03. 09."))));
    }

    @Test
    void healthEndpointShouldBePublicAndUp() throws Exception {
        mMockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"status\":\"UP\"")));
    }

    private static MockHttpSession createGoogleRegistrationSession(String googleSubject, String emailAddress) {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute(
            GoogleRegistrationSession.SESSION_ATTRIBUTE_NAME,
            new GoogleRegistrationSession(googleSubject, emailAddress, "")
        );
        return mockHttpSession;
    }

    private void savePreviousWeeklyOperationsSnapshot() {
        LocalDate previousWeekStartDate = TEST_CURRENT_DATE.minusDays(11L);
        saveWeeklyOperationsSnapshot(previousWeekStartDate, 1L, 1L, 33.3);
    }

    private void saveWeeklyOperationsSnapshot(
        LocalDate weekStartDate,
        long weeklyActiveUsers,
        long weeklyWritingUsers,
        double goalCompletionRatePercent
    ) {
        LocalDate weekEndDate = weekStartDate.plusDays(6L);
        WeeklyOperationsSummary previousWeeklyOperationsSummary = new WeeklyOperationsSummary(
            weekStartDate,
            weekEndDate,
            3L,
            1L,
            weeklyActiveUsers,
            weeklyWritingUsers,
            2L,
            2L,
            1L,
            1L,
            2L,
            1L,
            1L,
            1L,
            1L,
            0L,
            1L,
            1L,
            2.0,
            3.0,
            50.0,
            100.0,
            goalCompletionRatePercent
        );
        WeeklyOperationMetricSnapshot weeklyOperationMetricSnapshot =
            WeeklyOperationMetricSnapshot.create(weekStartDate, weekEndDate);
        weeklyOperationMetricSnapshot.updateFrom(
            previousWeeklyOperationsSummary,
            weekEndDate.atTime(9, 0)
        );
        mWeeklyOperationMetricSnapshotRepository.save(weeklyOperationMetricSnapshot);
    }
}
