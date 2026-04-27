package com.potterlim.daymark.controller;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import com.potterlim.daymark.service.WeeklyOperationMetricSnapshotService;
import com.potterlim.daymark.service.WeeklyOperationsSummary;
import com.potterlim.daymark.service.WeeklyOperationsSummaryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/operations")
public class OperationsAdminController {

    private final WeeklyOperationsSummaryService mWeeklyOperationsSummaryService;
    private final WeeklyOperationMetricSnapshotService mWeeklyOperationMetricSnapshotService;
    private final Clock mClock;

    public OperationsAdminController(
        WeeklyOperationsSummaryService weeklyOperationsSummaryService,
        WeeklyOperationMetricSnapshotService weeklyOperationMetricSnapshotService,
        Clock clock
    ) {
        mWeeklyOperationsSummaryService = weeklyOperationsSummaryService;
        mWeeklyOperationMetricSnapshotService = weeklyOperationMetricSnapshotService;
        mClock = clock;
    }

    @GetMapping
    public String showOperationsDashboard(Model model) {
        LocalDate currentDate = LocalDate.now(mClock);
        LocalDate currentWeekStartDate = resolveWeekStartDate(currentDate);
        WeeklyOperationsSummary currentWeeklySummary =
            mWeeklyOperationsSummaryService.buildWeeklySummary(currentWeekStartDate, currentDate);

        model.addAttribute("currentWeeklySummary", currentWeeklySummary);
        model.addAttribute(
            "recentWeeklySnapshots",
            mWeeklyOperationMetricSnapshotService.listRecentWeeklySnapshots()
        );
        return "admin/operations";
    }

    private static LocalDate resolveWeekStartDate(LocalDate referenceDate) {
        return referenceDate.minusDays(referenceDate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
    }
}
