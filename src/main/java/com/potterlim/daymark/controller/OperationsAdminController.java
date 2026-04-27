package com.potterlim.daymark.controller;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import com.potterlim.daymark.dto.operations.OperationsTrendViewDto;
import com.potterlim.daymark.entity.WeeklyOperationMetricSnapshot;
import com.potterlim.daymark.service.WeeklyOperationMetricSnapshotService;
import com.potterlim.daymark.service.WeeklyOperationsSummary;
import com.potterlim.daymark.service.WeeklyOperationsSummaryService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/operations")
public class OperationsAdminController {

    private static final String ADMINISTRATOR_ROLE_AUTHORITY = "ROLE_ADMIN";

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
    public String showOperationsDashboard(
        Authentication authenticationOrNull,
        HttpServletResponse httpServletResponse,
        Model model
    ) {
        if (!hasAdministratorRole(authenticationOrNull)) {
            httpServletResponse.setStatus(HttpStatus.NOT_FOUND.value());
            return "error/404";
        }

        LocalDate currentDate = LocalDate.now(mClock);
        LocalDate currentWeekStartDate = resolveWeekStartDate(currentDate);
        WeeklyOperationsSummary currentWeeklySummary =
            mWeeklyOperationsSummaryService.buildWeeklySummary(currentWeekStartDate, currentDate);
        List<WeeklyOperationMetricSnapshot> recentWeeklySnapshots =
            mWeeklyOperationMetricSnapshotService.listRecentWeeklySnapshots();

        model.addAttribute("currentWeeklySummary", currentWeeklySummary);
        model.addAttribute("recentWeeklySnapshots", recentWeeklySnapshots);
        model.addAttribute(
            "operationsTrendViewDto",
            OperationsTrendViewDto.create(recentWeeklySnapshots, currentWeeklySummary)
        );
        return "admin/operations";
    }

    private static LocalDate resolveWeekStartDate(LocalDate referenceDate) {
        return referenceDate.minusDays(
            referenceDate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue()
        );
    }

    private static boolean hasAdministratorRole(Authentication authenticationOrNull) {
        return authenticationOrNull != null
            && authenticationOrNull.getAuthorities()
                .stream()
                .anyMatch(
                    grantedAuthority -> ADMINISTRATOR_ROLE_AUTHORITY.equals(grantedAuthority.getAuthority())
                );
    }
}
