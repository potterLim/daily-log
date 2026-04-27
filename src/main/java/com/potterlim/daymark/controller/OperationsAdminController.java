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
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/operations")
public class OperationsAdminController {

    private static final String ADMINISTRATOR_ROLE_AUTHORITY = "ROLE_ADMIN";
    private static final int DEFAULT_TREND_WEEK_COUNT = 12;
    private static final int MINIMUM_TREND_WEEK_COUNT = 4;
    private static final int MAXIMUM_TREND_WEEK_COUNT = 24;
    private static final List<Integer> TREND_WEEK_OPTIONS = List.of(4, 8, 12, 24);

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
        @RequestParam(name = "date", required = false) LocalDate referenceDateOrNull,
        @RequestParam(name = "weeks", required = false) Integer trendWeekCountOrNull,
        Authentication authenticationOrNull,
        HttpServletResponse httpServletResponse,
        Model model
    ) {
        if (!hasAdministratorRole(authenticationOrNull)) {
            httpServletResponse.setStatus(HttpStatus.NOT_FOUND.value());
            return "error/404";
        }

        LocalDate currentDate = LocalDate.now(mClock);
        LocalDate selectedReferenceDate = resolveSelectedReferenceDate(referenceDateOrNull, currentDate);
        int selectedTrendWeekCount = resolveSelectedTrendWeekCount(trendWeekCountOrNull);
        LocalDate selectedWeekStartDate = resolveWeekStartDate(selectedReferenceDate);
        LocalDate selectedWeekEndDate = resolveSelectedWeekEndDate(selectedWeekStartDate, currentDate);
        LocalDate trendStartDate = selectedWeekStartDate.minusWeeks(selectedTrendWeekCount - 1L);
        List<WeeklyOperationMetricSnapshot> selectedWeeklySnapshots =
            mWeeklyOperationMetricSnapshotService.listWeeklySnapshotsWithinDateRange(
                trendStartDate,
                selectedWeekEndDate
            );
        WeeklyOperationsSummary currentWeeklySummary =
            mWeeklyOperationsSummaryService.buildWeeklySummary(selectedWeekStartDate, selectedWeekEndDate);

        model.addAttribute("currentWeeklySummary", currentWeeklySummary);
        model.addAttribute("recentWeeklySnapshots", selectedWeeklySnapshots);
        model.addAttribute("selectedTrendDate", selectedReferenceDate);
        model.addAttribute("selectedTrendWeekCount", selectedTrendWeekCount);
        model.addAttribute("trendWeekOptions", TREND_WEEK_OPTIONS);
        model.addAttribute(
            "operationsTrendViewDto",
            OperationsTrendViewDto.create(selectedWeeklySnapshots, currentWeeklySummary)
        );
        return "admin/operations";
    }

    private static LocalDate resolveSelectedReferenceDate(LocalDate referenceDateOrNull, LocalDate currentDate) {
        if (referenceDateOrNull == null || referenceDateOrNull.isAfter(currentDate)) {
            return currentDate;
        }

        return referenceDateOrNull;
    }

    private static int resolveSelectedTrendWeekCount(Integer trendWeekCountOrNull) {
        if (trendWeekCountOrNull == null) {
            return DEFAULT_TREND_WEEK_COUNT;
        }

        if (trendWeekCountOrNull < MINIMUM_TREND_WEEK_COUNT) {
            return MINIMUM_TREND_WEEK_COUNT;
        }

        if (trendWeekCountOrNull > MAXIMUM_TREND_WEEK_COUNT) {
            return MAXIMUM_TREND_WEEK_COUNT;
        }

        return trendWeekCountOrNull;
    }

    private static LocalDate resolveSelectedWeekEndDate(LocalDate selectedWeekStartDate, LocalDate currentDate) {
        LocalDate selectedWeekEndDate = selectedWeekStartDate.plusDays(6L);
        return selectedWeekEndDate.isAfter(currentDate) ? currentDate : selectedWeekEndDate;
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
