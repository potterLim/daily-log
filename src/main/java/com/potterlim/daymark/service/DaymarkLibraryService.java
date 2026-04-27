package com.potterlim.daymark.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import com.potterlim.daymark.dto.daymark.DaymarkLibraryContentBlockDto;
import com.potterlim.daymark.dto.daymark.DaymarkLibraryCalendarDayDto;
import com.potterlim.daymark.dto.daymark.DaymarkLibraryGoalPreviewDto;
import com.potterlim.daymark.dto.daymark.DaymarkLibraryItemDto;
import com.potterlim.daymark.dto.daymark.DaymarkLibrarySearchCriteria;
import com.potterlim.daymark.dto.daymark.DaymarkLibraryViewDto;
import com.potterlim.daymark.dto.daymark.EDaymarkLibraryContentTone;
import com.potterlim.daymark.entity.DaymarkEntry;
import com.potterlim.daymark.entity.UserAccountId;
import com.potterlim.daymark.repository.IDaymarkEntryRepository;
import com.potterlim.daymark.support.EDaymarkSectionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DaymarkLibraryService implements IDaymarkLibraryService {

    private static final int MAX_EXCERPT_LENGTH = 96;
    private static final int MAX_GOAL_PREVIEW_COUNT = 3;
    private static final int MAX_CONTENT_BLOCK_LINE_COUNT = 2;
    private static final int TREND_ITEM_LIMIT = 14;
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy. MM. dd.");

    private final IDaymarkEntryRepository mDaymarkEntryRepository;
    private final Clock mClock;

    public DaymarkLibraryService(IDaymarkEntryRepository daymarkEntryRepository, Clock clock) {
        mDaymarkEntryRepository = daymarkEntryRepository;
        mClock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public DaymarkLibraryViewDto searchLibrary(
        DaymarkLibrarySearchCriteria searchCriteria,
        UserAccountId userAccountId
    ) {
        List<DaymarkLibraryItemDto> matchingItemsAscending = listMatchingItems(searchCriteria, userAccountId);
        List<DaymarkLibraryItemDto> timelineItems = new ArrayList<>(matchingItemsAscending);
        Collections.reverse(timelineItems);

        int morningEntryCount = 0;
        int eveningEntryCount = 0;
        int achievedGoalCount = 0;
        int totalGoalCount = 0;
        for (DaymarkLibraryItemDto libraryItem : matchingItemsAscending) {
            if (libraryItem.hasMorningEntry()) {
                morningEntryCount++;
            }

            if (libraryItem.hasEveningEntry()) {
                eveningEntryCount++;
            }

            achievedGoalCount += libraryItem.getAchievedGoalCount();
            totalGoalCount += libraryItem.getTotalGoalCount();
        }

        int averageCompletionPercent = totalGoalCount == 0 ? 0 : (int) ((achievedGoalCount / (double) totalGoalCount) * 100);
        List<DaymarkLibraryItemDto> trendItems = buildTrendItems(matchingItemsAscending);
        LocalDate calendarMonthDate = searchCriteria.getEndDate().withDayOfMonth(1);
        List<DaymarkLibraryCalendarDayDto> calendarDays = buildCalendarDays(
            calendarMonthDate,
            matchingItemsAscending,
            LocalDate.now(mClock)
        );

        return new DaymarkLibraryViewDto(
            searchCriteria,
            timelineItems,
            trendItems,
            calendarDays,
            calendarMonthDate,
            morningEntryCount,
            eveningEntryCount,
            achievedGoalCount,
            totalGoalCount,
            averageCompletionPercent
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String buildLibraryMarkdownExport(
        DaymarkLibrarySearchCriteria searchCriteria,
        UserAccountId userAccountId
    ) {
        List<DaymarkLibraryItemDto> matchingItemsAscending = listMatchingItems(searchCriteria, userAccountId);
        StringBuilder markdownBuilder = new StringBuilder();
        markdownBuilder.append("# Daymark 기록 라이브러리\r\n\r\n");
        markdownBuilder.append("- 기간: ")
            .append(formatDisplayDate(searchCriteria.getStartDate()))
            .append(" ~ ")
            .append(formatDisplayDate(searchCriteria.getEndDate()))
            .append("\r\n");

        if (searchCriteria.hasKeyword()) {
            markdownBuilder.append("- 검색어: ").append(searchCriteria.getKeywordOrNull()).append("\r\n");
        }

        markdownBuilder.append("- 기록: ").append(matchingItemsAscending.size()).append("일\r\n");

        if (matchingItemsAscending.isEmpty()) {
            markdownBuilder.append("\r\n선택한 조건에 해당하는 기록이 없습니다.");
            return markdownBuilder.toString();
        }

        for (DaymarkLibraryItemDto libraryItem : matchingItemsAscending) {
            markdownBuilder.append("\r\n\r\n---\r\n\r\n");
            markdownBuilder.append("## ").append(formatDisplayDate(libraryItem.getDate())).append("\r\n\r\n");
            markdownBuilder.append("- 흐름: ").append(libraryItem.getFlowLabel()).append("\r\n");
            markdownBuilder.append("- 완료율: ")
                .append(libraryItem.getCompletionPercent())
                .append("% (")
                .append(libraryItem.getAchievedGoalCount())
                .append(" / ")
                .append(libraryItem.getTotalGoalCount())
                .append(")\r\n\r\n");
            markdownBuilder.append(libraryItem.getMarkdownText());
        }

        return markdownBuilder.toString().stripTrailing();
    }

    private List<DaymarkLibraryItemDto> listMatchingItems(
        DaymarkLibrarySearchCriteria searchCriteria,
        UserAccountId userAccountId
    ) {
        List<DaymarkLibraryItemDto> libraryItems = new ArrayList<>();
        for (DaymarkEntry daymarkEntry : mDaymarkEntryRepository.findEntriesByUserAccountIdWithinDateRange(
            userAccountId.getValue(),
            searchCriteria.getStartDate(),
            searchCriteria.getEndDate()
        )) {
            if (!daymarkEntry.hasAnyEntryContent()) {
                continue;
            }

            DaymarkLibraryItemDto libraryItem = buildLibraryItem(daymarkEntry);
            if (matchesKeyword(libraryItem, searchCriteria.getKeywordOrNull())) {
                libraryItems.add(libraryItem);
            }
        }

        return libraryItems;
    }

    private static DaymarkLibraryItemDto buildLibraryItem(DaymarkEntry daymarkEntry) {
        List<String> goalLines = splitContentLines(daymarkEntry.readSection(EDaymarkSectionType.GOALS));
        Set<String> completedGoalTexts = buildCompletedGoalTextSet(daymarkEntry);
        int totalGoalCount = goalLines.size();
        int achievedGoalCount = countAchievedGoals(goalLines, completedGoalTexts);
        int completionPercent = totalGoalCount == 0 ? 0 : (int) ((achievedGoalCount / (double) totalGoalCount) * 100);
        String markdownText = daymarkEntry.buildMarkdownText();

        return new DaymarkLibraryItemDto(
            daymarkEntry.getEntryDate(),
            daymarkEntry.hasMorningEntry(),
            daymarkEntry.hasEveningEntry(),
            achievedGoalCount,
            totalGoalCount,
            completionPercent,
            buildExcerpt(daymarkEntry),
            markdownText,
            buildGoalPreviewItems(goalLines, completedGoalTexts),
            Math.max(0, goalLines.size() - MAX_GOAL_PREVIEW_COUNT),
            buildContentBlocks(daymarkEntry)
        );
    }

    private static List<DaymarkLibraryItemDto> buildTrendItems(List<DaymarkLibraryItemDto> matchingItemsAscending) {
        if (matchingItemsAscending.size() <= TREND_ITEM_LIMIT) {
            return List.copyOf(matchingItemsAscending);
        }

        int fromIndex = matchingItemsAscending.size() - TREND_ITEM_LIMIT;
        return List.copyOf(matchingItemsAscending.subList(fromIndex, matchingItemsAscending.size()));
    }

    private static List<DaymarkLibraryCalendarDayDto> buildCalendarDays(
        LocalDate calendarMonthDate,
        List<DaymarkLibraryItemDto> matchingItemsAscending,
        LocalDate today
    ) {
        Map<LocalDate, DaymarkLibraryItemDto> libraryItemByDate = new HashMap<>();
        for (DaymarkLibraryItemDto libraryItem : matchingItemsAscending) {
            libraryItemByDate.put(libraryItem.getDate(), libraryItem);
        }

        YearMonth yearMonth = YearMonth.from(calendarMonthDate);
        LocalDate firstDateOfMonth = yearMonth.atDay(1);
        LocalDate lastDateOfMonth = yearMonth.atEndOfMonth();
        LocalDate firstCalendarDate = firstDateOfMonth.minusDays(
            firstDateOfMonth.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue()
        );
        LocalDate lastCalendarDate = lastDateOfMonth.plusDays(
            DayOfWeek.SUNDAY.getValue() - lastDateOfMonth.getDayOfWeek().getValue()
        );

        List<DaymarkLibraryCalendarDayDto> calendarDays = new ArrayList<>();
        LocalDate cursorDate = firstCalendarDate;
        while (!cursorDate.isAfter(lastCalendarDate)) {
            DaymarkLibraryItemDto libraryItemOrNull = libraryItemByDate.get(cursorDate);
            calendarDays.add(new DaymarkLibraryCalendarDayDto(
                cursorDate,
                YearMonth.from(cursorDate).equals(yearMonth),
                libraryItemOrNull != null,
                libraryItemOrNull == null ? 0 : libraryItemOrNull.getCompletionPercent(),
                cursorDate.equals(today)
            ));
            cursorDate = cursorDate.plusDays(1L);
        }

        return calendarDays;
    }

    private static boolean matchesKeyword(DaymarkLibraryItemDto libraryItem, String keywordOrNull) {
        if (keywordOrNull == null || keywordOrNull.isBlank()) {
            return true;
        }

        String normalizedKeyword = keywordOrNull.toLowerCase(Locale.ROOT);
        String searchableText = new StringJoiner(" ")
            .add(libraryItem.getDate().toString())
            .add(formatDisplayDate(libraryItem.getDate()))
            .add(libraryItem.getFlowLabel())
            .add(libraryItem.getExcerpt())
            .add(libraryItem.getMarkdownText())
            .toString()
            .toLowerCase(Locale.ROOT);
        return searchableText.contains(normalizedKeyword);
    }

    private static String buildExcerpt(DaymarkEntry daymarkEntry) {
        List<EDaymarkSectionType> excerptSectionTypes = List.of(
            EDaymarkSectionType.ACHIEVEMENTS,
            EDaymarkSectionType.IMPROVEMENTS,
            EDaymarkSectionType.NOTES,
            EDaymarkSectionType.FOCUS,
            EDaymarkSectionType.GOALS
        );

        for (EDaymarkSectionType daymarkSectionType : excerptSectionTypes) {
            List<String> contentLines = splitContentLines(daymarkEntry.readSection(daymarkSectionType));
            if (!contentLines.isEmpty()) {
                return abbreviate(contentLines.get(0));
            }
        }

        return "기록 내용이 저장되었습니다.";
    }

    private static Set<String> buildCompletedGoalTextSet(DaymarkEntry daymarkEntry) {
        Set<String> completedGoalTexts = new HashSet<>();
        for (String checkedGoalText : daymarkEntry.readCheckedGoalTexts()) {
            completedGoalTexts.add(normalizeComparableGoalText(checkedGoalText));
        }

        return completedGoalTexts;
    }

    private static List<DaymarkLibraryGoalPreviewDto> buildGoalPreviewItems(
        List<String> goalLines,
        Set<String> completedGoalTexts
    ) {
        List<DaymarkLibraryGoalPreviewDto> goalPreviewItems = new ArrayList<>();
        int visibleGoalCount = Math.min(goalLines.size(), MAX_GOAL_PREVIEW_COUNT);
        for (int goalIndex = 0; goalIndex < visibleGoalCount; goalIndex++) {
            String goalLine = goalLines.get(goalIndex);
            goalPreviewItems.add(new DaymarkLibraryGoalPreviewDto(
                abbreviate(goalLine),
                completedGoalTexts.contains(normalizeComparableGoalText(goalLine))
            ));
        }

        return goalPreviewItems;
    }

    private static int countAchievedGoals(List<String> goalLines, Set<String> completedGoalTexts) {
        int achievedGoalCount = 0;
        for (String goalLine : goalLines) {
            if (completedGoalTexts.contains(normalizeComparableGoalText(goalLine))) {
                achievedGoalCount++;
            }
        }

        return achievedGoalCount;
    }

    private static List<DaymarkLibraryContentBlockDto> buildContentBlocks(DaymarkEntry daymarkEntry) {
        List<DaymarkLibraryContentBlockDto> contentBlocks = new ArrayList<>();
        addContentBlockIfPresent(
            contentBlocks,
            daymarkEntry,
            EDaymarkSectionType.FOCUS,
            "Focus",
            "집중 영역",
            EDaymarkLibraryContentTone.FOCUS
        );
        addContentBlockIfPresent(
            contentBlocks,
            daymarkEntry,
            EDaymarkSectionType.CHALLENGES,
            "Risk",
            "예상 변수",
            EDaymarkLibraryContentTone.RISK
        );
        addContentBlockIfPresent(
            contentBlocks,
            daymarkEntry,
            EDaymarkSectionType.ACHIEVEMENTS,
            "Outcome",
            "성과",
            EDaymarkLibraryContentTone.OUTCOME
        );
        addContentBlockIfPresent(
            contentBlocks,
            daymarkEntry,
            EDaymarkSectionType.IMPROVEMENTS,
            "Learn",
            "개선점",
            EDaymarkLibraryContentTone.IMPROVEMENT
        );
        addContentBlockIfPresent(
            contentBlocks,
            daymarkEntry,
            EDaymarkSectionType.NOTES,
            "Next",
            "내일 메모",
            EDaymarkLibraryContentTone.NEXT
        );

        return contentBlocks;
    }

    private static void addContentBlockIfPresent(
        List<DaymarkLibraryContentBlockDto> contentBlocks,
        DaymarkEntry daymarkEntry,
        EDaymarkSectionType daymarkSectionType,
        String eyebrow,
        String title,
        EDaymarkLibraryContentTone contentTone
    ) {
        List<String> contentLines = splitContentLines(daymarkEntry.readSection(daymarkSectionType));
        if (contentLines.isEmpty()) {
            return;
        }

        List<String> visibleContentLines = new ArrayList<>();
        int visibleLineCount = Math.min(contentLines.size(), MAX_CONTENT_BLOCK_LINE_COUNT);
        for (int lineIndex = 0; lineIndex < visibleLineCount; lineIndex++) {
            visibleContentLines.add(abbreviate(contentLines.get(lineIndex)));
        }

        contentBlocks.add(new DaymarkLibraryContentBlockDto(
            eyebrow,
            title,
            visibleContentLines,
            Math.max(0, contentLines.size() - MAX_CONTENT_BLOCK_LINE_COUNT),
            contentTone
        ));
    }

    private static String abbreviate(String text) {
        if (text.length() <= MAX_EXCERPT_LENGTH) {
            return text;
        }

        return text.substring(0, MAX_EXCERPT_LENGTH - 1).stripTrailing() + "…";
    }

    private static String normalizeComparableGoalText(String textOrNull) {
        if (textOrNull == null) {
            return "";
        }

        return textOrNull.trim().toLowerCase(Locale.ROOT);
    }

    private static List<String> splitContentLines(String textOrNull) {
        if (textOrNull == null || textOrNull.isBlank()) {
            return List.of();
        }

        List<String> contentLines = new ArrayList<>();
        for (String line : textOrNull.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            String normalizedLine = line.stripLeading()
                .replaceFirst("^- \\[[xX ]\\]\\s*", "")
                .replaceFirst("^-\\s*", "")
                .trim();
            if (!normalizedLine.isEmpty()) {
                contentLines.add(normalizedLine);
            }
        }

        return contentLines;
    }

    private static String formatDisplayDate(LocalDate date) {
        return DISPLAY_DATE_FORMATTER.format(date);
    }
}
