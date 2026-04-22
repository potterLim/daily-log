package com.potterlim.daylog;

import java.time.LocalDate;
import com.potterlim.daylog.dto.auth.RegisterUserAccountCommand;
import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.repository.IDailyLogEntryRepository;
import com.potterlim.daylog.repository.IUserAccountRepository;
import com.potterlim.daylog.repository.IUserEmailVerificationTokenRepository;
import com.potterlim.daylog.repository.IUserPasswordResetTokenRepository;
import com.potterlim.daylog.service.IDailyLogService;
import com.potterlim.daylog.service.IUserAccountService;
import com.potterlim.daylog.service.WeeklyOperationsSummary;
import com.potterlim.daylog.service.WeeklyOperationsSummaryService;
import com.potterlim.daylog.support.EDailyLogSectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class WeeklyOperationsSummaryServiceTests {

    @Autowired
    private WeeklyOperationsSummaryService mWeeklyOperationsSummaryService;

    @Autowired
    private IUserAccountService mUserAccountService;

    @Autowired
    private IDailyLogService mDailyLogService;

    @Autowired
    private IDailyLogEntryRepository mDailyLogEntryRepository;

    @Autowired
    private IUserPasswordResetTokenRepository mUserPasswordResetTokenRepository;

    @Autowired
    private IUserEmailVerificationTokenRepository mUserEmailVerificationTokenRepository;

    @Autowired
    private IUserAccountRepository mUserAccountRepository;

    @BeforeEach
    void setUpTestEnvironment() {
        mDailyLogEntryRepository.deleteAll();
        mUserPasswordResetTokenRepository.deleteAll();
        mUserEmailVerificationTokenRepository.deleteAll();
        mUserAccountRepository.deleteAll();
    }

    @Test
    void weeklyOperationsSummaryShouldAggregateWeeklyUsageMetrics() {
        LocalDate weekEndDate = LocalDate.now();
        LocalDate weekStartDate = weekEndDate.minusDays(6L);

        UserAccount firstUser = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("summary-user-1", "summary-user-1@example.com", "pass1234")
        );
        UserAccount secondUser = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("summary-user-2", "summary-user-2@example.com", "pass1234")
        );
        mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("summary-user-3", "summary-user-3@example.com", "pass1234")
        );

        mDailyLogService.writeSection(
            weekStartDate,
            firstUser.getUserAccountId(),
            EDailyLogSectionType.GOALS,
            "핵심 작업 정리"
        );
        mDailyLogService.writeSection(
            weekStartDate,
            firstUser.getUserAccountId(),
            EDailyLogSectionType.EVENING_GOALS,
            "- [x] 목표 1\r\n- [ ] 목표 2"
        );
        mDailyLogService.writeSection(
            weekStartDate.plusDays(2L),
            firstUser.getUserAccountId(),
            EDailyLogSectionType.GOALS,
            "리뷰 준비"
        );
        mDailyLogService.writeSection(
            weekStartDate.plusDays(4L),
            secondUser.getUserAccountId(),
            EDailyLogSectionType.EVENING_GOALS,
            "- [x] 목표 3"
        );

        WeeklyOperationsSummary weeklyOperationsSummary =
            mWeeklyOperationsSummaryService.buildWeeklySummary(weekStartDate, weekEndDate);

        assertEquals(3L, weeklyOperationsSummary.getTotalRegisteredUsers());
        assertEquals(3L, weeklyOperationsSummary.getNewlyRegisteredUsers());
        assertEquals(2L, weeklyOperationsSummary.getWeeklyActiveUsers());
        assertEquals(3L, weeklyOperationsSummary.getWeeklyWritingDays());
        assertEquals(2L, weeklyOperationsSummary.getWeeklyMorningLogs());
        assertEquals(2L, weeklyOperationsSummary.getWeeklyEveningLogs());
        assertEquals(1.5, weeklyOperationsSummary.getAverageWritingDaysPerActiveUser(), 0.0001);
        assertEquals(2.0, weeklyOperationsSummary.getAverageLogCompletionsPerActiveUser(), 0.0001);
        assertEquals(66.66666666666667, weeklyOperationsSummary.getGoalCompletionRatePercent(), 0.0001);
    }
}
