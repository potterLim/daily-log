package com.potterlim.daylog;

import java.time.LocalDate;
import com.potterlim.daylog.dto.auth.RegisterUserAccountCommand;
import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.repository.IDailyLogEntryRepository;
import com.potterlim.daylog.repository.IUserAccountRepository;
import com.potterlim.daylog.service.IDailyLogService;
import com.potterlim.daylog.service.IUserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
@ActiveProfiles("test")
class WebFlowIntegrationTests {

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

    @BeforeEach
    void setUpTestEnvironment() {
        mDailyLogEntryRepository.deleteAll();
        mUserAccountRepository.deleteAll();
    }

    @Test
    void registerShouldCreateUserAndRedirectToHome() throws Exception {
        mMockMvc.perform(post("/register")
                .with(csrf())
                .param("userName", "tester")
                .param("password", "pass1234")
                .param("confirmPassword", "pass1234"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        assertTrue(mUserAccountRepository.findByUserName("tester").isPresent());
    }

    @Test
    void registerShouldRejectShortPassword() throws Exception {
        mMockMvc.perform(post("/register")
                .with(csrf())
                .param("userName", "tester")
                .param("password", "pass12")
                .param("confirmPassword", "pass12"))
            .andExpect(status().isOk());

        assertTrue(mUserAccountRepository.findByUserName("tester").isEmpty());
    }

    @Test
    void loginShouldShowGenericErrorWhenUserNameDoesNotExist() throws Exception {
        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("userName", "missing-user")
                .param("password", "wrong-password"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("아이디 또는 비밀번호가 올바르지 않습니다.")));
    }

    @Test
    void loginShouldShowGenericErrorWhenPasswordIsWrong() throws Exception {
        mUserAccountService.registerUserAccount(new RegisterUserAccountCommand("tester", "pass1234"));

        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("userName", "tester")
                .param("password", "wrong-password")
                .param("rememberMe", "true"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("아이디 또는 비밀번호가 올바르지 않습니다.")));
    }

    @Test
    void morningSaveShouldPersistDailyLogEntry() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("writer", "pass1234")
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

        assertTrue(markdownText.contains("## 🚀 Today's Goals"));
        assertTrue(markdownText.contains("- 운동하기"));
        assertTrue(markdownText.contains("- 책 읽기"));
        assertTrue(markdownText.contains("## 🎯 Focus Areas"));
        assertEquals(1, mDailyLogEntryRepository.count());
    }

    @Test
    void morningListShouldRenderSavedDate() throws Exception {
        LocalDate morningDate = LocalDate.now();
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("planner", "pass1234")
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
    void coreProductPagesShouldRender() throws Exception {
        UserAccount userAccount = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("reviewer", "pass1234")
        );

        mMockMvc.perform(get("/")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("하루의 계획과 실행, 회고를 하나의 제품 경험으로 연결하세요.")));

        mMockMvc.perform(get("/daily-log/evening")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("저녁 회고")));

        mMockMvc.perform(get("/daily-log/week")
                .with(SecurityMockMvcRequestPostProcessors.user(userAccount)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("이번 주 성취 흐름")));
    }

    @Test
    void healthEndpointShouldBePublicAndUp() throws Exception {
        mMockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"status\":\"UP\"")));
    }
}
