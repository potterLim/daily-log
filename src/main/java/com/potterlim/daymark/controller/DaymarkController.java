package com.potterlim.daymark.controller;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import com.potterlim.daymark.dto.daymark.DaymarkDayStatusDto;
import com.potterlim.daymark.dto.daymark.DaymarkLibraryItemDto;
import com.potterlim.daymark.dto.daymark.DaymarkLibrarySearchCriteria;
import com.potterlim.daymark.dto.daymark.DaymarkLibraryViewDto;
import com.potterlim.daymark.dto.daymark.EveningFormDto;
import com.potterlim.daymark.dto.daymark.EveningGoalItemDto;
import com.potterlim.daymark.dto.daymark.MorningFormDto;
import com.potterlim.daymark.dto.daymark.WeeklyProgressItemDto;
import com.potterlim.daymark.entity.EOperationEventType;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.entity.UserAccountId;
import com.potterlim.daymark.service.IDaymarkLibraryService;
import com.potterlim.daymark.service.IDaymarkService;
import com.potterlim.daymark.service.OperationUsageEventService;
import com.potterlim.daymark.support.EDaymarkSectionType;
import com.potterlim.daymark.support.SimpleMarkdownRenderer;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/daymark")
public class DaymarkController {

    private static final String EMPTY_MORNING_LOG_HTML = "<p><em>아침 계획이 없습니다.</em></p>";
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy. MM. dd.");

    private final IDaymarkService mDaymarkService;
    private final IDaymarkLibraryService mDaymarkLibraryService;
    private final SimpleMarkdownRenderer mSimpleMarkdownRenderer;
    private final OperationUsageEventService mOperationUsageEventService;
    private final Clock mClock;

    public DaymarkController(
        IDaymarkService daymarkService,
        IDaymarkLibraryService daymarkLibraryService,
        SimpleMarkdownRenderer simpleMarkdownRenderer,
        OperationUsageEventService operationUsageEventService,
        Clock clock
    ) {
        mDaymarkService = daymarkService;
        mDaymarkLibraryService = daymarkLibraryService;
        mSimpleMarkdownRenderer = simpleMarkdownRenderer;
        mOperationUsageEventService = operationUsageEventService;
        mClock = clock;
    }

    @GetMapping("/morning")
    public String showMorningDateList(@AuthenticationPrincipal UserAccount userAccount, Model model) {
        LocalDate currentDate = LocalDate.now(mClock);
        UserAccountId userAccountId = userAccount.getUserAccountId();
        mOperationUsageEventService.recordUserEvent(EOperationEventType.MORNING_PLAN_VIEWED, userAccountId);
        List<LocalDate> morningDates = mDaymarkService.listWeek(currentDate, userAccountId)
            .stream()
            .filter(DaymarkDayStatusDto::hasMorningEntry)
            .map(DaymarkDayStatusDto::getDate)
            .toList();

        model.addAttribute("morningDates", morningDates);
        model.addAttribute("defaultDate", currentDate);
        return "daymark/morning";
    }

    @GetMapping("/morning/edit")
    public String showMorningEditPage(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        UserAccountId userAccountId = userAccount.getUserAccountId();
        mOperationUsageEventService.recordUserEvent(EOperationEventType.MORNING_PLAN_VIEWED, userAccountId);
        MorningFormDto morningFormDto = new MorningFormDto();
        morningFormDto.setDate(date);
        morningFormDto.setGoals(mDaymarkService.readSection(date, userAccountId, EDaymarkSectionType.GOALS));
        morningFormDto.setFocus(mDaymarkService.readSection(date, userAccountId, EDaymarkSectionType.FOCUS));
        morningFormDto.setChallenges(mDaymarkService.readSection(date, userAccountId, EDaymarkSectionType.CHALLENGES));

        model.addAttribute("morningFormDto", morningFormDto);
        return "daymark/morning-edit";
    }

    @PostMapping("/morning/save")
    public String saveMorningLog(
        @Valid @ModelAttribute("morningFormDto") MorningFormDto morningFormDto,
        BindingResult bindingResult,
        @AuthenticationPrincipal UserAccount userAccount,
        RedirectAttributes redirectAttributes
    ) {
        validateMorningFormInput(morningFormDto, bindingResult);

        if (bindingResult.hasErrors()) {
            return "daymark/morning-edit";
        }

        UserAccountId userAccountId = userAccount.getUserAccountId();
        String goalsMarkdownList = buildGoalMarkdownList(morningFormDto.getGoals());
        mDaymarkService.writeSection(morningFormDto.getDate(), userAccountId, EDaymarkSectionType.GOALS, goalsMarkdownList);
        mDaymarkService.writeSection(morningFormDto.getDate(), userAccountId, EDaymarkSectionType.FOCUS, morningFormDto.getFocus());
        mDaymarkService.writeSection(
            morningFormDto.getDate(),
            userAccountId,
            EDaymarkSectionType.CHALLENGES,
            morningFormDto.getChallenges()
        );

        redirectAttributes.addFlashAttribute("message", "아침 계획이 저장되었습니다.");
        mOperationUsageEventService.recordUserEvent(EOperationEventType.MORNING_PLAN_SAVED, userAccountId);
        return "redirect:/daymark/morning";
    }

    @GetMapping("/evening")
    public String showEveningDateList(
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        LocalDate currentDate = LocalDate.now(mClock);
        UserAccountId userAccountId = userAccount.getUserAccountId();
        mOperationUsageEventService.recordUserEvent(EOperationEventType.EVENING_REVIEW_VIEWED, userAccountId);

        List<LocalDate> eveningDates = mDaymarkService.listWeek(currentDate, userAccountId)
            .stream()
            .filter(daymarkDayStatusDto -> daymarkDayStatusDto.hasMorningEntry() || daymarkDayStatusDto.hasEveningEntry())
            .map(DaymarkDayStatusDto::getDate)
            .toList();

        model.addAttribute("eveningDates", eveningDates);
        model.addAttribute("defaultDate", currentDate);
        return "daymark/evening";
    }

    @GetMapping("/evening/edit")
    public String showEveningEditPage(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        UserAccountId userAccountId = userAccount.getUserAccountId();
        mOperationUsageEventService.recordUserEvent(EOperationEventType.EVENING_REVIEW_VIEWED, userAccountId);
        model.addAttribute("morningEntryHtml", buildMorningLogHtmlForDate(date, userAccountId));
        model.addAttribute("eveningFormDto", buildEveningFormDto(date, userAccountId));
        return "daymark/evening-edit";
    }

    @PostMapping("/evening/save")
    public String saveEveningLog(
        @Valid @ModelAttribute("eveningFormDto") EveningFormDto eveningFormDto,
        BindingResult bindingResult,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        validateEveningFormInput(eveningFormDto, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                "morningEntryHtml",
                buildMorningLogHtmlForDate(eveningFormDto.getDate(), userAccount.getUserAccountId())
            );
            return "daymark/evening-edit";
        }

        UserAccountId userAccountId = userAccount.getUserAccountId();
        mDaymarkService.writeSection(
            eveningFormDto.getDate(),
            userAccountId,
            EDaymarkSectionType.EVENING_GOALS,
            buildCheckedGoalMarkdownList(eveningFormDto.getGoals())
        );
        mDaymarkService.writeSection(
            eveningFormDto.getDate(),
            userAccountId,
            EDaymarkSectionType.ACHIEVEMENTS,
            eveningFormDto.getAchievements()
        );
        mDaymarkService.writeSection(
            eveningFormDto.getDate(),
            userAccountId,
            EDaymarkSectionType.IMPROVEMENTS,
            eveningFormDto.getImprovements()
        );
        mDaymarkService.writeSection(
            eveningFormDto.getDate(),
            userAccountId,
            EDaymarkSectionType.GRATITUDE,
            eveningFormDto.getGratitude()
        );
        mDaymarkService.writeSection(
            eveningFormDto.getDate(),
            userAccountId,
            EDaymarkSectionType.NOTES,
            eveningFormDto.getNotes()
        );

        redirectAttributes.addFlashAttribute("message", "저녁 회고가 저장되었습니다.");
        mOperationUsageEventService.recordUserEvent(EOperationEventType.EVENING_REVIEW_SAVED, userAccountId);
        return "redirect:/daymark/evening";
    }

    @GetMapping("/week")
    public String showWeekPage(
        @RequestParam(name = "week", defaultValue = "0") int weekOffset,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        LocalDate currentDate = LocalDate.now(mClock);
        LocalDate referenceDate = currentDate.plusDays((long) weekOffset * 7L);
        LocalDate startDate = resolveWeekStartDate(referenceDate);
        UserAccountId userAccountId = userAccount.getUserAccountId();
        mOperationUsageEventService.recordUserEvent(EOperationEventType.WEEKLY_REVIEW_VIEWED, userAccountId);
        List<WeeklyProgressItemDto> weeklyProgressItems = new ArrayList<>();
        int weekAchieved = 0;
        int weekTotal = 0;

        for (DaymarkDayStatusDto daymarkDayStatusDto : mDaymarkService.listWeek(referenceDate, userAccountId)) {
            List<String> goals = splitNonBlankLines(
                mDaymarkService.readSection(daymarkDayStatusDto.getDate(), userAccountId, EDaymarkSectionType.GOALS)
            );

            int total = goals.size();
            int achieved = 0;

            if (daymarkDayStatusDto.hasEveningEntry()) {
                Set<String> checkedGoalTexts =
                    new HashSet<>(mDaymarkService.readCheckedGoalTexts(daymarkDayStatusDto.getDate(), userAccountId));

                for (String goal : goals) {
                    if (checkedGoalTexts.contains(goal)) {
                        achieved++;
                    }
                }
            }

            int percent = total == 0 ? 0 : (int) ((achieved / (double) total) * 100);
            weeklyProgressItems.add(new WeeklyProgressItemDto(daymarkDayStatusDto.getDate(), achieved, total, percent));
            weekAchieved += achieved;
            weekTotal += total;
        }

        int weekPercent = weekTotal == 0 ? 0 : (int) ((weekAchieved / (double) weekTotal) * 100);

        model.addAttribute("weeklyProgressItems", weeklyProgressItems);
        model.addAttribute("weekAchieved", weekAchieved);
        model.addAttribute("weekTotal", weekTotal);
        model.addAttribute("weekPercent", weekPercent);
        model.addAttribute("weekOffset", weekOffset);
        model.addAttribute("previousWeekOffset", weekOffset - 1);
        model.addAttribute("nextWeekOffset", weekOffset + 1);
        model.addAttribute("rangeLabel", buildWeekRangeLabel(startDate));
        model.addAttribute("currentWeekRangeLabel", buildWeekRangeLabel(resolveWeekStartDate(currentDate)));
        model.addAttribute("previousWeekRangeLabel", buildWeekRangeLabel(startDate.minusDays(7L)));
        model.addAttribute("nextWeekRangeLabel", buildWeekRangeLabel(startDate.plusDays(7L)));
        model.addAttribute("defaultDate", currentDate);
        return "daymark/week";
    }

    @GetMapping("/library")
    public String showLibraryPage(
        @RequestParam(name = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDateOrNull,
        @RequestParam(name = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDateOrNull,
        @RequestParam(name = "keyword", required = false) String keywordOrNull,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        DaymarkLibrarySearchCriteria searchCriteria = buildLibrarySearchCriteria(
            startDateOrNull,
            endDateOrNull,
            keywordOrNull
        );
        DaymarkLibraryViewDto libraryViewDto =
            mDaymarkLibraryService.searchLibrary(searchCriteria, userAccount.getUserAccountId());

        mOperationUsageEventService.recordUserEvent(EOperationEventType.RECORD_LIBRARY_VIEWED, userAccount.getUserAccountId());
        model.addAttribute("libraryViewDto", libraryViewDto);
        return "daymark/library";
    }

    @GetMapping(value = "/library/export/markdown", produces = "text/markdown; charset=UTF-8")
    public ResponseEntity<String> downloadLibraryMarkdown(
        @RequestParam(name = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDateOrNull,
        @RequestParam(name = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDateOrNull,
        @RequestParam(name = "keyword", required = false) String keywordOrNull,
        @AuthenticationPrincipal UserAccount userAccount
    ) {
        DaymarkLibrarySearchCriteria searchCriteria = buildLibrarySearchCriteria(
            startDateOrNull,
            endDateOrNull,
            keywordOrNull
        );
        String markdownText = mDaymarkLibraryService.buildLibraryMarkdownExport(
            searchCriteria,
            userAccount.getUserAccountId()
        );
        mOperationUsageEventService.recordUserEvent(EOperationEventType.MARKDOWN_EXPORTED, userAccount.getUserAccountId());
        ContentDisposition contentDisposition = ContentDisposition.attachment()
            .filename(buildLibraryExportFileName(searchCriteria, "md"), StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .body(markdownText);
    }

    @GetMapping("/library/export/pdf")
    public String showLibraryPdfExportPage(
        @RequestParam(name = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDateOrNull,
        @RequestParam(name = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDateOrNull,
        @RequestParam(name = "keyword", required = false) String keywordOrNull,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        DaymarkLibrarySearchCriteria searchCriteria = buildLibrarySearchCriteria(
            startDateOrNull,
            endDateOrNull,
            keywordOrNull
        );
        DaymarkLibraryViewDto libraryViewDto =
            mDaymarkLibraryService.searchLibrary(searchCriteria, userAccount.getUserAccountId());

        mOperationUsageEventService.recordUserEvent(EOperationEventType.PDF_EXPORT_VIEWED, userAccount.getUserAccountId());
        model.addAttribute("libraryViewDto", libraryViewDto);
        model.addAttribute("exportItemHtmlByDate", buildExportItemHtmlByDate(libraryViewDto.getItems()));
        model.addAttribute("exportFileName", buildLibraryExportFileName(searchCriteria, "pdf"));
        return "daymark/library-export-print";
    }

    @GetMapping("/preview")
    public String showLogPreview(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        UserAccountId userAccountId = userAccount.getUserAccountId();
        mOperationUsageEventService.recordUserEvent(EOperationEventType.RECORD_PREVIEW_VIEWED, userAccountId);
        String markdownText = mDaymarkService.readEntryMarkdownContent(date, userAccountId);
        boolean hasPreviewContent = !markdownText.isBlank();

        model.addAttribute("previewDate", date);
        model.addAttribute("hasPreviewContent", hasPreviewContent);
        model.addAttribute(
            "previewHtml",
            hasPreviewContent
                ? mSimpleMarkdownRenderer.renderMarkdown(normalizePreviewMarkdownForRendering(markdownText))
                : ""
        );
        return "daymark/log-preview";
    }

    private static void validateMorningFormInput(
        MorningFormDto morningFormDto,
        BindingResult bindingResult
    ) {
        if (!morningFormDto.hasValidGoalLineCount()) {
            bindingResult.rejectValue(
                "goals",
                "morningForm.goalLineCount",
                "목표는 30개 이하로 입력해주세요."
            );
        }

        if (!morningFormDto.hasValidGoalLineLengths()) {
            bindingResult.rejectValue(
                "goals",
                "morningForm.goalLineLength",
                "목표는 한 줄당 300자 이하로 입력해주세요."
            );
        }

        if (morningFormDto.isWithinTotalBodyLimit()) {
            return;
        }

        bindingResult.reject(
            "morningForm.totalBodyLength",
            "한 번에 저장할 수 있는 전체 기록은 8,000자까지입니다."
        );
    }

    private static void validateEveningFormInput(
        EveningFormDto eveningFormDto,
        BindingResult bindingResult
    ) {
        if (eveningFormDto.isWithinTotalBodyLimit()) {
            return;
        }

        bindingResult.reject(
            "eveningForm.totalBodyLength",
            "한 번에 저장할 수 있는 전체 기록은 8,000자까지입니다."
        );
    }

    private DaymarkLibrarySearchCriteria buildLibrarySearchCriteria(
        LocalDate startDateOrNull,
        LocalDate endDateOrNull,
        String keywordOrNull
    ) {
        return DaymarkLibrarySearchCriteria.create(startDateOrNull, endDateOrNull, keywordOrNull, LocalDate.now(mClock));
    }

    private static String buildLibraryExportFileName(DaymarkLibrarySearchCriteria searchCriteria, String extension) {
        return "daymark-records-"
            + searchCriteria.getStartDate()
            + "-"
            + searchCriteria.getEndDate()
            + "."
            + extension;
    }

    private Map<LocalDate, String> buildExportItemHtmlByDate(List<DaymarkLibraryItemDto> libraryItems) {
        Map<LocalDate, String> exportItemHtmlByDate = new LinkedHashMap<>();
        for (DaymarkLibraryItemDto libraryItem : libraryItems) {
            exportItemHtmlByDate.put(
                libraryItem.getDate(),
                mSimpleMarkdownRenderer.renderMarkdown(
                    normalizePreviewMarkdownForRendering(libraryItem.getMarkdownText())
                )
            );
        }

        return exportItemHtmlByDate;
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

        String goals = mDaymarkService.readSection(dateOrNull, userAccountId, EDaymarkSectionType.GOALS);
        String focus = mDaymarkService.readSection(dateOrNull, userAccountId, EDaymarkSectionType.FOCUS);
        String challenges = mDaymarkService.readSection(dateOrNull, userAccountId, EDaymarkSectionType.CHALLENGES);

        String markdownText = buildMorningPreviewMarkdownText(goals, focus, challenges);
        if (markdownText.isBlank()) {
            return EMPTY_MORNING_LOG_HTML;
        }

        return mSimpleMarkdownRenderer.renderMarkdown(normalizePreviewMarkdownForRendering(markdownText));
    }

    private EveningFormDto buildEveningFormDto(LocalDate date, UserAccountId userAccountId) {
        String goals = mDaymarkService.readSection(date, userAccountId, EDaymarkSectionType.GOALS);
        Set<String> checkedGoalTexts = new HashSet<>(mDaymarkService.readCheckedGoalTexts(date, userAccountId));

        EveningFormDto eveningFormDto = new EveningFormDto();
        eveningFormDto.setDate(date);
        eveningFormDto.setGoals(buildEveningGoalItems(goals, checkedGoalTexts));
        eveningFormDto.setAchievements(mDaymarkService.readSection(date, userAccountId, EDaymarkSectionType.ACHIEVEMENTS));
        eveningFormDto.setImprovements(mDaymarkService.readSection(date, userAccountId, EDaymarkSectionType.IMPROVEMENTS));
        eveningFormDto.setGratitude(mDaymarkService.readSection(date, userAccountId, EDaymarkSectionType.GRATITUDE));
        eveningFormDto.setNotes(mDaymarkService.readSection(date, userAccountId, EDaymarkSectionType.NOTES));
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
        appendPreviewSection(markdownBuilder, EDaymarkSectionType.GOALS, goals);
        appendPreviewSection(markdownBuilder, EDaymarkSectionType.FOCUS, focus);
        appendPreviewSection(markdownBuilder, EDaymarkSectionType.CHALLENGES, challenges);
        return markdownBuilder.toString().stripTrailing();
    }

    private static void appendPreviewSection(
        StringBuilder markdownBuilder,
        EDaymarkSectionType daymarkSectionType,
        String sectionBody
    ) {
        List<String> lines = splitNonBlankLines(sectionBody);
        if (lines.isEmpty()) {
            return;
        }

        if (markdownBuilder.length() > 0) {
            markdownBuilder.append("\r\n");
        }

        markdownBuilder.append(daymarkSectionType.getHeaderText()).append("\r\n\r\n");
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

    private static String buildWeekRangeLabel(LocalDate startDate) {
        return formatDisplayDate(startDate) + " ~ " + formatDisplayDate(startDate.plusDays(6L));
    }

    private static String formatDisplayDate(LocalDate date) {
        return DISPLAY_DATE_FORMATTER.format(date);
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
