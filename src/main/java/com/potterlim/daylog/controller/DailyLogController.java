package com.potterlim.daylog.controller;

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
import com.potterlim.daylog.service.IDailyLogService;
import com.potterlim.daylog.support.DailyLogSectionType;
import com.potterlim.daylog.support.SimpleMarkdownRenderer;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.BindingResult;
import org.springframework.http.HttpStatus;
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

    private final IDailyLogService mDailyLogService;
    private final SimpleMarkdownRenderer mSimpleMarkdownRenderer;

    public DailyLogController(IDailyLogService dailyLogService, SimpleMarkdownRenderer simpleMarkdownRenderer) {
        mDailyLogService = dailyLogService;
        mSimpleMarkdownRenderer = simpleMarkdownRenderer;
    }

    @GetMapping("/morning")
    public String showMorningDateList(@AuthenticationPrincipal UserAccount userAccount, Model model) {
        List<String> morningDates = mDailyLogService.listWeek(LocalDate.now(), userAccount.getId())
            .stream()
            .filter(DailyLogDayStatusDto::hasMorning)
            .map(dailyLogDayStatusDto -> dailyLogDayStatusDto.getDate().toString())
            .collect(Collectors.toList());

        model.addAttribute("morningDates", morningDates);
        model.addAttribute("defaultDate", LocalDate.now());
        return "dailylog/morning";
    }

    @GetMapping("/morning/edit")
    public String showMorningEditPage(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        MorningFormDto morningFormDto = new MorningFormDto();
        morningFormDto.setDate(date);
        morningFormDto.setGoals(mDailyLogService.readSection(date, userAccount.getId(), DailyLogSectionType.GOALS));
        morningFormDto.setFocus(mDailyLogService.readSection(date, userAccount.getId(), DailyLogSectionType.FOCUS));
        morningFormDto.setChallenges(mDailyLogService.readSection(date, userAccount.getId(), DailyLogSectionType.CHALLENGES));

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

        String goalsBlock = buildGoalLines(morningFormDto.getGoals());
        mDailyLogService.writeSection(morningFormDto.getDate(), userAccount.getId(), DailyLogSectionType.GOALS, goalsBlock);
        mDailyLogService.writeSection(morningFormDto.getDate(), userAccount.getId(), DailyLogSectionType.FOCUS, morningFormDto.getFocus());
        mDailyLogService.writeSection(morningFormDto.getDate(), userAccount.getId(), DailyLogSectionType.CHALLENGES, morningFormDto.getChallenges());

        redirectAttributes.addFlashAttribute("message", "✅ 아침 계획이 저장되었습니다.");
        return "redirect:/daily-log/morning";
    }

    @GetMapping("/evening")
    public String showEveningDateList(
        @RequestParam(name = "week", defaultValue = "0") int weekOffset,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        LocalDate referenceDate = LocalDate.now().plusDays((long) weekOffset * 7L);
        LocalDate startDate = referenceDate.minusDays(3L);
        LocalDate endDate = startDate.plusDays(6L);

        List<String> eveningDates = mDailyLogService.listWeek(referenceDate, userAccount.getId())
            .stream()
            .filter(dailyLogDayStatusDto -> dailyLogDayStatusDto.hasMorning() || dailyLogDayStatusDto.hasEvening())
            .map(dailyLogDayStatusDto -> dailyLogDayStatusDto.getDate().toString())
            .collect(Collectors.toList());

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
        model.addAttribute("morningLogHtml", buildMorningLogHtmlForDate(date, userAccount.getId()));
        model.addAttribute("eveningFormDto", buildEveningFormDto(date, userAccount.getId()));
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
            model.addAttribute("morningLogHtml", buildMorningLogHtmlForDate(eveningFormDto.getDate(), userAccount.getId()));
            return "dailylog/evening-edit";
        }

        mDailyLogService.writeSection(
            eveningFormDto.getDate(),
            userAccount.getId(),
            DailyLogSectionType.EVENING_GOALS,
            buildCheckedGoalsBlock(eveningFormDto.getGoals())
        );
        mDailyLogService.writeSection(
            eveningFormDto.getDate(),
            userAccount.getId(),
            DailyLogSectionType.ACHIEVEMENTS,
            eveningFormDto.getAchievements()
        );
        mDailyLogService.writeSection(
            eveningFormDto.getDate(),
            userAccount.getId(),
            DailyLogSectionType.IMPROVEMENTS,
            eveningFormDto.getImprovements()
        );
        mDailyLogService.writeSection(
            eveningFormDto.getDate(),
            userAccount.getId(),
            DailyLogSectionType.GRATITUDE,
            eveningFormDto.getGratitude()
        );
        mDailyLogService.writeSection(
            eveningFormDto.getDate(),
            userAccount.getId(),
            DailyLogSectionType.NOTES,
            eveningFormDto.getNotes()
        );

        redirectAttributes.addFlashAttribute("message", "🌙 저녁 회고가 저장되었습니다.");
        return "redirect:/daily-log/evening";
    }

    @GetMapping("/week")
    public String showWeekPage(@AuthenticationPrincipal UserAccount userAccount, Model model) {
        List<WeeklyProgressItemDto> weeklyProgressItems = new ArrayList<>();
        int weekAchieved = 0;
        int weekTotal = 0;

        for (DailyLogDayStatusDto dailyLogDayStatusDto : mDailyLogService.listWeek(LocalDate.now(), userAccount.getId())) {
            List<String> goals = splitNonBlankLines(
                mDailyLogService.readSection(dailyLogDayStatusDto.getDate(), userAccount.getId(), DailyLogSectionType.GOALS)
            );

            int total = goals.size();
            int achieved = 0;

            if (dailyLogDayStatusDto.hasEvening()) {
                Set<String> checkedGoalTexts = new HashSet<>(
                    mDailyLogService.readCheckedGoalTexts(dailyLogDayStatusDto.getDate(), userAccount.getId())
                );

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
        String markdownText = mDailyLogService.readLogFileContent(date, userAccount.getId());

        if (markdownText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        model.addAttribute("previewDate", date);
        model.addAttribute("previewHtml", mSimpleMarkdownRenderer.render(preprocessMarkdown(markdownText)));
        return "dailylog/log-preview";
    }

    private static String buildGoalLines(String goalsOrNull) {
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
    private String buildMorningLogHtmlForDate(LocalDate dateOrNull, Long userAccountId) {
        if (dateOrNull == null) {
            return "<p><em>아침 로그가 없습니다.</em></p>";
        }

        String goals = mDailyLogService.readSection(dateOrNull, userAccountId, DailyLogSectionType.GOALS);
        String focus = mDailyLogService.readSection(dateOrNull, userAccountId, DailyLogSectionType.FOCUS);
        String challenges = mDailyLogService.readSection(dateOrNull, userAccountId, DailyLogSectionType.CHALLENGES);

        String markdownText = buildMorningPreviewMarkdown(goals, focus, challenges);
        if (markdownText.isBlank()) {
            return "<p><em>아침 로그가 없습니다.</em></p>";
        }

        return mSimpleMarkdownRenderer.render(preprocessMarkdown(markdownText));
    }

    private EveningFormDto buildEveningFormDto(LocalDate date, Long userAccountId) {
        String goals = mDailyLogService.readSection(date, userAccountId, DailyLogSectionType.GOALS);
        Set<String> checkedGoalTexts = new HashSet<>(mDailyLogService.readCheckedGoalTexts(date, userAccountId));

        EveningFormDto eveningFormDto = new EveningFormDto();
        eveningFormDto.setDate(date);
        eveningFormDto.setGoals(buildEveningGoalItems(goals, checkedGoalTexts));
        eveningFormDto.setAchievements(mDailyLogService.readSection(date, userAccountId, DailyLogSectionType.ACHIEVEMENTS));
        eveningFormDto.setImprovements(mDailyLogService.readSection(date, userAccountId, DailyLogSectionType.IMPROVEMENTS));
        eveningFormDto.setGratitude(mDailyLogService.readSection(date, userAccountId, DailyLogSectionType.GRATITUDE));
        eveningFormDto.setNotes(mDailyLogService.readSection(date, userAccountId, DailyLogSectionType.NOTES));
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

    private static String buildCheckedGoalsBlock(List<EveningGoalItemDto> goalItemsOrNull) {
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

    private static String buildMorningPreviewMarkdown(String goals, String focus, String challenges) {
        StringBuilder markdownBuilder = new StringBuilder();
        appendPreviewSection(markdownBuilder, DailyLogSectionType.GOALS, goals);
        appendPreviewSection(markdownBuilder, DailyLogSectionType.FOCUS, focus);
        appendPreviewSection(markdownBuilder, DailyLogSectionType.CHALLENGES, challenges);
        return markdownBuilder.toString().stripTrailing();
    }

    private static void appendPreviewSection(
        StringBuilder markdownBuilder,
        DailyLogSectionType dailyLogSectionType,
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

    private static String preprocessMarkdown(String markdownTextOrNull) {
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

    private static List<String> splitNonBlankLines(String textOrNull) {
        if (textOrNull == null || textOrNull.isBlank()) {
            return List.of();
        }

        return textOrNull.lines()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(Collectors.toList());
    }
}
