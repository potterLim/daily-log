package com.potterlim.daymark;

import java.time.LocalDate;
import com.potterlim.daymark.dto.auth.RegisterUserAccountCommand;
import com.potterlim.daymark.entity.DaymarkEntry;
import com.potterlim.daymark.entity.EOperationEventType;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.repository.IDaymarkEntryRepository;
import com.potterlim.daymark.repository.IOperationUsageEventRepository;
import com.potterlim.daymark.repository.IUserAccountRepository;
import com.potterlim.daymark.repository.IWeeklyOperationMetricSnapshotRepository;
import com.potterlim.daymark.service.IDaymarkService;
import com.potterlim.daymark.service.IUserAccountService;
import com.potterlim.daymark.service.OperationUsageEventService;
import com.potterlim.daymark.service.WeeklyOperationMetricSnapshotService;
import com.potterlim.daymark.service.WeeklyOperationsSummary;
import com.potterlim.daymark.service.WeeklyOperationsSummaryService;
import com.potterlim.daymark.support.EDaymarkSectionType;
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
    private IDaymarkService mDaymarkService;

    @Autowired
    private IDaymarkEntryRepository mDaymarkEntryRepository;

    @Autowired
    private OperationUsageEventService mOperationUsageEventService;

    @Autowired
    private WeeklyOperationMetricSnapshotService mWeeklyOperationMetricSnapshotService;

    @Autowired
    private IUserAccountRepository mUserAccountRepository;

    @Autowired
    private IOperationUsageEventRepository mOperationUsageEventRepository;

    @Autowired
    private IWeeklyOperationMetricSnapshotRepository mWeeklyOperationMetricSnapshotRepository;

    @BeforeEach
    void setUpTestEnvironment() {
        mWeeklyOperationMetricSnapshotRepository.deleteAll();
        mOperationUsageEventRepository.deleteAll();
        mDaymarkEntryRepository.deleteAll();
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
        UserAccount thirdUser = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("summary-user-3", "summary-user-3@example.com", "pass1234")
        );
        UserAccount adminUser = mUserAccountService.registerUserAccount(
            new RegisterUserAccountCommand("summary-admin", "summary-admin@example.com", "pass1234")
        );
        adminUser.grantAdministratorRole();
        mUserAccountRepository.saveAndFlush(adminUser);

        mDaymarkService.writeSection(
            weekStartDate,
            firstUser.getUserAccountId(),
            EDaymarkSectionType.GOALS,
            "핵심 작업 정리"
        );
        mDaymarkService.writeSection(
            weekStartDate,
            firstUser.getUserAccountId(),
            EDaymarkSectionType.EVENING_GOALS,
            "- [x] 목표 1\r\n- [ ] 목표 2"
        );
        mDaymarkService.writeSection(
            weekStartDate.plusDays(2L),
            firstUser.getUserAccountId(),
            EDaymarkSectionType.GOALS,
            "리뷰 준비"
        );
        mDaymarkService.writeSection(
            weekStartDate.plusDays(4L),
            secondUser.getUserAccountId(),
            EDaymarkSectionType.EVENING_GOALS,
            "- [x] 목표 3"
        );
        mDaymarkEntryRepository.save(DaymarkEntry.create(thirdUser, weekStartDate.plusDays(5L)));
        mDaymarkService.writeSection(
            weekStartDate.plusDays(1L),
            adminUser.getUserAccountId(),
            EDaymarkSectionType.GOALS,
            "관리자 점검"
        );
        mDaymarkService.writeSection(
            weekStartDate.plusDays(1L),
            adminUser.getUserAccountId(),
            EDaymarkSectionType.EVENING_GOALS,
            "- [x] 관리자 점검"
        );
        recordUserEvent(EOperationEventType.SIGN_IN_SUCCEEDED, firstUser);
        recordUserEvent(EOperationEventType.RECORD_LIBRARY_VIEWED, firstUser);
        recordUserEvent(EOperationEventType.MARKDOWN_EXPORTED, firstUser);
        recordUserEvent(EOperationEventType.SIGN_IN_SUCCEEDED, adminUser);
        recordUserEvent(EOperationEventType.RECORD_LIBRARY_VIEWED, adminUser);
        recordUserEvent(EOperationEventType.MARKDOWN_EXPORTED, adminUser);
        mOperationUsageEventService.recordAnonymousEvent(EOperationEventType.SIGN_IN_FAILED);

        WeeklyOperationsSummary weeklyOperationsSummary =
            mWeeklyOperationsSummaryService.buildWeeklySummary(weekStartDate, weekEndDate);

        assertEquals(3L, weeklyOperationsSummary.getTotalRegisteredUsers());
        assertEquals(3L, weeklyOperationsSummary.getNewlyRegisteredUsers());
        assertEquals(2L, weeklyOperationsSummary.getWeeklyActiveUsers());
        assertEquals(2L, weeklyOperationsSummary.getWeeklyWritingUsers());
        assertEquals(3L, weeklyOperationsSummary.getWeeklyWritingDays());
        assertEquals(2L, weeklyOperationsSummary.getWeeklyMorningEntries());
        assertEquals(2L, weeklyOperationsSummary.getWeeklyEveningEntries());
        assertEquals(1L, weeklyOperationsSummary.getSignInSucceededCount());
        assertEquals(1L, weeklyOperationsSummary.getSignInFailedCount());
        assertEquals(1L, weeklyOperationsSummary.getRecordLibraryViewedCount());
        assertEquals(1L, weeklyOperationsSummary.getMarkdownExportedCount());
        assertEquals(0L, weeklyOperationsSummary.getPdfExportViewedCount());
        assertEquals(1.5, weeklyOperationsSummary.getAverageWritingDaysPerActiveUser(), 0.0001);
        assertEquals(2.0, weeklyOperationsSummary.getAverageEntryCompletionsPerActiveUser(), 0.0001);
        assertEquals(66.66666666666667, weeklyOperationsSummary.getGoalCompletionRatePercent(), 0.0001);

        mWeeklyOperationMetricSnapshotService.saveWeeklySnapshot(weeklyOperationsSummary);

        assertEquals(1L, mWeeklyOperationMetricSnapshotRepository.count());
    }

    private void recordUserEvent(EOperationEventType operationEventType, UserAccount userAccount) {
        mOperationUsageEventService.recordUserEvent(operationEventType, userAccount.getUserAccountId());
    }
}
