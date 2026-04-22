package com.potterlim.daylog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;
import com.potterlim.daylog.dto.auth.RegisterUserAccountCommand;
import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.repository.IUserAccountRepository;
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

    private static final Path TEST_LOGS_ROOT_PATH = Path.of("build", "test-logs");

    @Autowired
    private MockMvc mMockMvc;

    @Autowired
    private IUserAccountService mUserAccountService;

    @Autowired
    private IUserAccountRepository mUserAccountRepository;

    @BeforeEach
    void setUp() throws IOException {
        mUserAccountRepository.deleteAll();

        if (Files.exists(TEST_LOGS_ROOT_PATH)) {
            try (Stream<Path> paths = Files.walk(TEST_LOGS_ROOT_PATH)) {
                paths.sorted((leftPath, rightPath) -> rightPath.compareTo(leftPath))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ioException) {
                            throw new IllegalStateException("Failed to clean test logs.", ioException);
                        }
                    });
            }
        }
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
    void loginShouldShowErrorWhenPasswordIsWrong() throws Exception {
        mUserAccountService.registerUserAccount(new RegisterUserAccountCommand("tester", "pass1234"));

        mMockMvc.perform(post("/login")
                .with(csrf())
                .param("userName", "tester")
                .param("password", "wrong-password")
                .param("rememberMe", "true"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("비밀번호가 올바르지 않습니다.")));
    }

    @Test
    void morningSaveShouldWriteMarkdownFile() throws Exception {
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

        Path markdownFilePath = TEST_LOGS_ROOT_PATH
            .resolve(String.valueOf(userAccount.getId()))
            .resolve("2026_04_Week1")
            .resolve(LocalDate.of(2026, 4, 1) + ".md");

        String markdownText = Files.readString(markdownFilePath, StandardCharsets.UTF_8);
        assertTrue(markdownText.contains("## 🚀 Today's Goals"));
        assertTrue(markdownText.contains("- 운동하기"));
        assertTrue(markdownText.contains("- 책 읽기"));
        assertTrue(markdownText.contains("## 🎯 Focus Areas"));
        assertTrue(markdownText.contains("- 집중 업무"));
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
}
