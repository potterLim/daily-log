package com.potterlim.daylog.controller;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import com.potterlim.daylog.dto.dailylog.DailyLogDayStatusDto;
import com.potterlim.daylog.dto.dailylog.EveningFormDto;
import com.potterlim.daylog.dto.dailylog.EveningGoalItemDto;
import com.potterlim.daylog.dto.dailylog.MorningFormDto;
import com.potterlim.daylog.dto.dailylog.WeeklyProgressItemDto;
import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.entity.UserAccountId;
import com.potterlim.daylog.service.IDailyLogService;
import com.potterlim.daylog.support.EDailyLogSectionType;
import com.potterlim.daylog.support.SimpleMarkdownRenderer;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/daily-log")
public class DailyLogController {

    private static final String EMPTY_MORNING_LOG_HTML = "<p><em>아침 로그가 없습니다.</em></p>";

    private final IDailyLogService mDailyLogService;
    private final SimpleMarkdownRenderer mSimpleMarkdownRenderer;
    private final Clock mClock;

    public DailyLogController(IDailyLogService dailyLogService, SimpleMarkdownRenderer simpleMarkdownRenderer, Clock clock) {
        mDailyLogService = dailyLogService;
        mSimpleMarkdownRenderer = simpleMarkdownRenderer;
        mClock = clock;
    }

    @GetMapping("/morning")
    public String showMorningDateList(@AuthenticationPrincipal UserAccount userAccount, Model model) {
        LocalDate currentDate = LocalDate.now(mClock);
        UserAccountId userAccountId = userAccount.getUserAccountId();
        List<String> morningDates = mDailyLogService.listWeek(currentDate, userAccountId)
            .stream()
            .filter(DailyLogDayStatusDto::hasMorningLog)
            .map(dailyLogDayStatusDto -> dailyLogDayStatusDto.getDate().toString())
            .toList();

        model.addAttribute("morningDates", morningDates);
        model.addAttribute("defaultDate", currentDate);
        return "dailylog/morning";
    }

    @GetMapping("/morning/edit")
    public String showMorningEditPage(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        UserAccountId userAccountId = userAccount.getUserAccountId();
        MorningFormDto morningFormDto = new MorningFormDto();
        morningFormDto.setDate(date);
        morningFormDto.setGoals(mDailyLogService.readSection(date, userAccountId, EDailyLogSectionType.GOALS));
        morningFormDto.setFocus(mDailyLogService.readSection(date, userAccountId, EDailyLogSectionType.FOCUS));
        morningFormDto.setChallenges(mDailyLogService.readSection(date, userAccountId, EDailyLogSectionType.CHALLENGES));

        model.addAttribute("morningFormDto", morningFormDto);
        return "dailylog/morning-edit";
    }

    @PostMapping("/morning/save")
    public String saveMorningLog(
        @Valid @ModelAttribute("morningFormDto") MorningFormDto morningFormDto,
        BindingResult bindingResult,
        @AuthenticationPrincipal UserAccount userAccount,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "dailylog/morning-edit";
        }

        UserAccountId userAccountId = userAccount.getUserAccountId();
        String goalsMarkdownList = buildGoalMarkdownList(morningFormDto.getGoals());
        mDailyLogService.writeSection(morningFormDto.getDate(), userAccountId, EDailyLogSectionType.GOALS, goalsMarkdownList);
        mDailyLogService.writeSection(morningFormDto.getDate(), userAccountId, EDailyLogSectionType.FOCUS, morningFormDto.getFocus());
        mDailyLogService.writeSection(
            morningFormDto.getDate(),
            userAccountId,
            EDailyLogSectionType.CHALLENGES,
            morningFormDto.getChallenges()
        );

        redirectAttributes.addFlashAttribute("message", "✅ 아침 계획이 저장되었습니다.");
        return "redirect:/daily-log/morning";
    }

    @GetMapping("/evening")
    public String showEveningDateList(
        @RequestParam(name = "week", defaultValue = "0") int weekOffset,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        LocalDate referenceDate = LocalDate.now(mClock).plusDays((long) weekOffset * 7L);
        LocalDate startDate = resolveWeekStartDate(referenceDate);
        LocalDate endDate = startDate.plusDays(6L);
        UserAccountId userAccountId = userAccount.getUserAccountId();

        List<String> eveningDates = mDailyLogService.listWeek(referenceDate, userAccountId)
            .stream()
            .filter(dailyLogDayStatusDto -> dailyLogDayStatusDto.hasMorningLog() || dailyLogDayStatusDto.hasEveningLog())
            .map(dailyLogDayStatusDto -> dailyLogDayStatusDto.getDate().toString())
            .toList();

        model.addAttribute("eveningDates", eveningDates);
        model.addAttribute("weekOffset", weekOffset);
        model.addAttribute("previousWeekOffset", weekOffset - 1);
        model.addAttribute("nextWeekOffset", weekOffset + 1);
        model.addAttribute("rangeLabel", startDate + " ~ " + endDate);
        return "dailylog/evening";
    }

    @GetMapping("/evening/edit")
    public String showEveningEditPage(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        UserAccountId userAccountId = userAccount.getUserAccountId();
        model.addAttribute("morningLogHtml", buildMorningLogHtmlForDate(date, userAccountId));
        model.addAttribute("eveningFormDto", buildEveningFormDto(date, userAccountId));
        return "dailylog/evening-edit";
    }

    @PostMapping("/evening/save")
    public String saveEveningLog(
        @Valid @ModelAttribute("eveningFormDto") EveningFormDto eveningFormDto,
        BindingResult bindingResult,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(
                "morningLogHtml",
                buildMorningLogHtmlForDate(eveningFormDto.getDate(), userAccount.getUserAccountId())
            );
            return "dailylog/evening-edit";
        }

        UserAccountId userAccountId = userAccount.getUserAccountId();
        mDailyLogService.writeSection(
            eveningFormDto.getDate(),
            userAccountId,
            EDailyLogSectionType.EVENING_GOALS,
            buildCheckedGoalMarkdownList(eveningFormDto.getGoals())
        );
        mDailyLogService.writeSection(
            eveningFormDto.getDate(),
            userAccountId,
            EDailyLogSectionType.ACHIEVEMENTS,
            eveningFormDto.getAchievements()
        );
        mDailyLogService.writeSection(
            eveningFormDto.getDate(),
            userAccountId,
            EDailyLogSectionType.IMPROVEMENTS,
            eveningFormDto.getImprovements()
        );
        mDailyLogService.writeSection(
            eveningFormDto.getDate(),
            userAccountId,
            EDailyLogSectionType.GRATITUDE,
            eveningFormDto.getGratitude()
        );
        mDailyLogService.writeSection(
            eveningFormDto.getDate(),
            userAccountId,
            EDailyLogSectionType.NOTES,
            eveningFormDto.getNotes()
        );

        redirectAttributes.addFlashAttribute("message", "🌙 저녁 회고가 저장되었습니다.");
        return "redirect:/daily-log/evening";
    }

    @GetMapping("/week")
    public String showWeekPage(@AuthenticationPrincipal UserAccount userAccount, Model model) {
        UserAccountId userAccountId = userAccount.getUserAccountId();
        List<WeeklyProgressItemDto> weeklyProgressItems = new ArrayList<>();
        int weekAchieved = 0;
        int weekTotal = 0;

        for (DailyLogDayStatusDto dailyLogDayStatusDto : mDailyLogService.listWeek(LocalDate.now(mClock), userAccountId)) {
            List<String> goals = splitNonBlankLines(
                mDailyLogService.readSection(dailyLogDayStatusDto.getDate(), userAccountId, EDailyLogSectionType.GOALS)
            );

            int total = goals.size();
            int achieved = 0;

            if (dailyLogDayStatusDto.hasEveningLog()) {
                Set<String> checkedGoalTexts =
                    new HashSet<>(mDailyLogService.readCheckedGoalTexts(dailyLogDayStatusDto.getDate(), userAccountId));

                for (String goal : goals) {
                    if (checkedGoalTexts.contains(goal)) {
                        achieved++;
                    }
                }
            }

            int percent = total == 0 ? 0 : (int) ((achieved / (double) total) * 100);
            weeklyProgressItems.add(new WeeklyProgressItemDto(dailyLogDayStatusDto.getDate(), achieved, total, percent));
            weekAchieved += achieved;
            weekTotal += total;
        }

        int weekPercent = weekTotal == 0 ? 0 : (int) ((weekAchieved / (double) weekTotal) * 100);

        model.addAttribute("weeklyProgressItems", weeklyProgressItems);
        model.addAttribute("weekAchieved", weekAchieved);
        model.addAttribute("weekTotal", weekTotal);
        model.addAttribute("weekPercent", weekPercent);
        return "dailylog/week";
    }

    @GetMapping("/preview")
    public String showLogPreview(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        UserAccountId userAccountId = userAccount.getUserAccountId();
        String markdownText = mDailyLogService.readLogFileContent(date, userAccountId);

        if (markdownText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        model.addAttribute("previewDate", date);
        model.addAttribute(
            "previewHtml",
            mSimpleMarkdownRenderer.renderMarkdown(normalizePreviewMarkdownForRendering(markdownText))
        );
        return "dailylog/log-preview";
    }

    private static String buildGoalMarkdownList(String goalsOrNull) {
        if (goalsOrNull == null || goalsOrNull.isBlank()) {
            return "";
        }

        return goalsOrNull.lines()
            .map(String::trim)
            .filter(goalLine -> !goalLine.isEmpty())
            .map(goalLine -> "- " + goalLine)
            .collect(Collectors.joining("\r\n"));
    }

    /**
     * Rebuilds the morning plan as preview markdown so the evening page can render it in the same
     * structure users expect from the saved log format.
     *
     * <p>Preconditions: the date may be null when a validation error occurs while posting the
     * evening form. In that case, the method returns the empty-state HTML.</p>
     *
     * @return HTML markup ready for the evening view.
     */
    private String buildMorningLogHtmlForDate(LocalDate dateOrNull, UserAccountId userAccountId) {
        if (dateOrNull == null) {
            return EMPTY_MORNING_LOG_HTML;
        }

        String goals = mDailyLogService.readSection(dateOrNull, userAccountId, EDailyLogSectionType.GOALS);
        String focus = mDailyLogService.readSection(dateOrNull, userAccountId, EDailyLogSectionType.FOCUS);
        String challenges = mDailyLogService.readSection(dateOrNull, userAccountId, EDailyLogSectionType.CHALLENGES);

        String markdownText = buildMorningPreviewMarkdownText(goals, focus, challenges);
        if (markdownText.isBlank()) {
            return EMPTY_MORNING_LOG_HTML;
        }

        return mSimpleMarkdownRenderer.renderMarkdown(normalizePreviewMarkdownForRendering(markdownText));
    }

    private EveningFormDto buildEveningFormDto(LocalDate date, UserAccountId userAccountId) {
        String goals = mDailyLogService.readSection(date, userAccountId, EDailyLogSectionType.GOALS);
        Set<String> checkedGoalTexts = new HashSet<>(mDailyLogService.readCheckedGoalTexts(date, userAccountId));

        EveningFormDto eveningFormDto = new EveningFormDto();
        eveningFormDto.setDate(date);
        eveningFormDto.setGoals(buildEveningGoalItems(goals, checkedGoalTexts));
        eveningFormDto.setAchievements(mDailyLogService.readSection(date, userAccountId, EDailyLogSectionType.ACHIEVEMENTS));
        eveningFormDto.setImprovements(mDailyLogService.readSection(date, userAccountId, EDailyLogSectionType.IMPROVEMENTS));
        eveningFormDto.setGratitude(mDailyLogService.readSection(date, userAccountId, EDailyLogSectionType.GRATITUDE));
        eveningFormDto.setNotes(mDailyLogService.readSection(date, userAccountId, EDailyLogSectionType.NOTES));
        return eveningFormDto;
    }

    private static List<EveningGoalItemDto> buildEveningGoalItems(String goals, Set<String> checkedGoalTexts) {
        List<EveningGoalItemDto> goalItems = new ArrayList<>();

        for (String goal : splitNonBlankLines(goals)) {
            EveningGoalItemDto eveningGoalItemDto = new EveningGoalItemDto();
            eveningGoalItemDto.setText(goal);
            eveningGoalItemDto.setDone(checkedGoalTexts.contains(goal));
            goalItems.add(eveningGoalItemDto);
        }

        return goalItems;
    }

    private static String buildCheckedGoalMarkdownList(List<EveningGoalItemDto> goalItemsOrNull) {
        if (goalItemsOrNull == null || goalItemsOrNull.isEmpty()) {
            return "";
        }

        StringJoiner stringJoiner = new StringJoiner("\r\n");
        for (EveningGoalItemDto eveningGoalItemDto : goalItemsOrNull) {
            String goalText = eveningGoalItemDto.getText() == null ? "" : eveningGoalItemDto.getText().trim();

            if (goalText.isEmpty()) {
                continue;
            }

            stringJoiner.add("- [" + (eveningGoalItemDto.isDone() ? "x" : " ") + "] " + goalText);
        }

        return stringJoiner.toString();
    }

    private static String buildMorningPreviewMarkdownText(String goals, String focus, String challenges) {
        StringBuilder markdownBuilder = new StringBuilder();
        appendPreviewSection(markdownBuilder, EDailyLogSectionType.GOALS, goals);
        appendPreviewSection(markdownBuilder, EDailyLogSectionType.FOCUS, focus);
        appendPreviewSection(markdownBuilder, EDailyLogSectionType.CHALLENGES, challenges);
        return markdownBuilder.toString().stripTrailing();
    }

    private static void appendPreviewSection(
        StringBuilder markdownBuilder,
        EDailyLogSectionType dailyLogSectionType,
        String sectionBody
    ) {
        List<String> lines = splitNonBlankLines(sectionBody);
        if (lines.isEmpty()) {
            return;
        }

        if (markdownBuilder.length() > 0) {
            markdownBuilder.append("\r\n");
        }

        markdownBuilder.append(dailyLogSectionType.getHeaderText()).append("\r\n\r\n");
        for (String line : lines) {
            markdownBuilder.append("- ").append(line).append("\r\n");
        }
    }

    private static String normalizePreviewMarkdownForRendering(String markdownTextOrNull) {
        if (markdownTextOrNull == null || markdownTextOrNull.isBlank()) {
            return "";
        }

        StringBuilder processedMarkdownBuilder = new StringBuilder();

        for (String line : markdownTextOrNull.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            if (line.startsWith("#")) {
                int hashCount = 0;

                while (hashCount < line.length() && line.charAt(hashCount) == '#') {
                    hashCount++;
                }

                int newHashCount = Math.min(hashCount + 2, 6);
                String rest = line.substring(hashCount).trim();
                processedMarkdownBuilder.append("\r\n")
                    .append("#".repeat(newHashCount))
                    .append(" ")
                    .append(rest)
                    .append("\r\n");
                continue;
            }

            processedMarkdownBuilder.append(line).append("\r\n");
        }

        return processedMarkdownBuilder.toString().stripTrailing();
    }

    private static LocalDate resolveWeekStartDate(LocalDate referenceDate) {
        return referenceDate.minusDays(referenceDate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
    }

    private static List<String> splitNonBlankLines(String textOrNull) {
        if (textOrNull == null || textOrNull.isBlank()) {
            return List.of();
        }

        return textOrNull.lines()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .toList();
    }
}
