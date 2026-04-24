package com.potterlim.daylog.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import com.potterlim.daylog.dto.dailylog.DailyLogLibraryCalendarDayDto;
import com.potterlim.daylog.dto.dailylog.DailyLogLibraryItemDto;
import com.potterlim.daylog.dto.dailylog.DailyLogLibrarySearchCriteria;
import com.potterlim.daylog.dto.dailylog.DailyLogLibraryViewDto;
import com.potterlim.daylog.entity.DailyLogEntry;
import com.potterlim.daylog.entity.UserAccountId;
import com.potterlim.daylog.repository.IDailyLogEntryRepository;
import com.potterlim.daylog.support.EDailyLogSectionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyLogLibraryService implements IDailyLogLibraryService {

    private static final int MAX_EXCERPT_LENGTH = 96;
    private static final int TREND_ITEM_LIMIT = 14;

    private final IDailyLogEntryRepository mDailyLogEntryRepository;
    private final Clock mClock;

    public DailyLogLibraryService(IDailyLogEntryRepository dailyLogEntryRepository, Clock clock) {
        mDailyLogEntryRepository = dailyLogEntryRepository;
        mClock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public DailyLogLibraryViewDto searchLibrary(
        DailyLogLibrarySearchCriteria searchCriteria,
        UserAccountId userAccountId
    ) {
        List<DailyLogLibraryItemDto> matchingItemsAscending = listMatchingItems(searchCriteria, userAccountId);
        List<DailyLogLibraryItemDto> timelineItems = new ArrayList<>(matchingItemsAscending);
        Collections.reverse(timelineItems);

        int morningLogCount = 0;
        int eveningLogCount = 0;
        int achievedGoalCount = 0;
        int totalGoalCount = 0;
        for (DailyLogLibraryItemDto libraryItem : matchingItemsAscending) {
            if (libraryItem.hasMorningLog()) {
                morningLogCount++;
            }

            if (libraryItem.hasEveningLog()) {
                eveningLogCount++;
            }

            achievedGoalCount += libraryItem.getAchievedGoalCount();
            totalGoalCount += libraryItem.getTotalGoalCount();
        }

        int averageCompletionPercent = totalGoalCount == 0 ? 0 : (int) ((achievedGoalCount / (double) totalGoalCount) * 100);
        List<DailyLogLibraryItemDto> trendItems = buildTrendItems(matchingItemsAscending);
        LocalDate calendarMonthDate = searchCriteria.getEndDate().withDayOfMonth(1);
        List<DailyLogLibraryCalendarDayDto> calendarDays = buildCalendarDays(
            calendarMonthDate,
            matchingItemsAscending,
            LocalDate.now(mClock)
        );

        return new DailyLogLibraryViewDto(
            searchCriteria,
            timelineItems,
            trendItems,
            calendarDays,
            calendarMonthDate,
            morningLogCount,
            eveningLogCount,
            achievedGoalCount,
            totalGoalCount,
            averageCompletionPercent
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String buildLibraryMarkdownExport(
        DailyLogLibrarySearchCriteria searchCriteria,
        UserAccountId userAccountId
    ) {
        List<DailyLogLibraryItemDto> matchingItemsAscending = listMatchingItems(searchCriteria, userAccountId);
        StringBuilder markdownBuilder = new StringBuilder();
        markdownBuilder.append("# DailyLog 기록 라이브러리\r\n\r\n");
        markdownBuilder.append("- 기간: ")
            .append(searchCriteria.getStartDate())
            .append(" ~ ")
            .append(searchCriteria.getEndDate())
            .append("\r\n");

        if (searchCriteria.hasKeyword()) {
            markdownBuilder.append("- 검색어: ").append(searchCriteria.getKeywordOrNull()).append("\r\n");
        }

        markdownBuilder.append("- 기록: ").append(matchingItemsAscending.size()).append("일\r\n");

        if (matchingItemsAscending.isEmpty()) {
            markdownBuilder.append("\r\n선택한 조건에 해당하는 기록이 없습니다.");
            return markdownBuilder.toString();
        }

        for (DailyLogLibraryItemDto libraryItem : matchingItemsAscending) {
            markdownBuilder.append("\r\n\r\n---\r\n\r\n");
            markdownBuilder.append("## ").append(libraryItem.getDate()).append("\r\n\r\n");
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

    private List<DailyLogLibraryItemDto> listMatchingItems(
        DailyLogLibrarySearchCriteria searchCriteria,
        UserAccountId userAccountId
    ) {
        List<DailyLogLibraryItemDto> libraryItems = new ArrayList<>();
        for (DailyLogEntry dailyLogEntry : mDailyLogEntryRepository.findEntriesByUserAccountIdWithinDateRange(
            userAccountId.getValue(),
            searchCriteria.getStartDate(),
            searchCriteria.getEndDate()
        )) {
            if (!dailyLogEntry.hasAnyLog()) {
                continue;
            }

            DailyLogLibraryItemDto libraryItem = buildLibraryItem(dailyLogEntry);
            if (matchesKeyword(libraryItem, searchCriteria.getKeywordOrNull())) {
                libraryItems.add(libraryItem);
            }
        }

        return libraryItems;
    }

    private static DailyLogLibraryItemDto buildLibraryItem(DailyLogEntry dailyLogEntry) {
        List<String> goalLines = splitContentLines(dailyLogEntry.readSection(EDailyLogSectionType.GOALS));
        int totalGoalCount = goalLines.size();
        int achievedGoalCount = dailyLogEntry.readCheckedGoalTexts().size();
        int completionPercent = totalGoalCount == 0 ? 0 : (int) ((achievedGoalCount / (double) totalGoalCount) * 100);
        String markdownText = dailyLogEntry.buildMarkdownText();

        return new DailyLogLibraryItemDto(
            dailyLogEntry.getLogDate(),
            dailyLogEntry.hasMorningLog(),
            dailyLogEntry.hasEveningLog(),
            achievedGoalCount,
            totalGoalCount,
            completionPercent,
            buildExcerpt(dailyLogEntry),
            markdownText
        );
    }

    private static List<DailyLogLibraryItemDto> buildTrendItems(List<DailyLogLibraryItemDto> matchingItemsAscending) {
        if (matchingItemsAscending.size() <= TREND_ITEM_LIMIT) {
            return List.copyOf(matchingItemsAscending);
        }

        int fromIndex = matchingItemsAscending.size() - TREND_ITEM_LIMIT;
        return List.copyOf(matchingItemsAscending.subList(fromIndex, matchingItemsAscending.size()));
    }

    private static List<DailyLogLibraryCalendarDayDto> buildCalendarDays(
        LocalDate calendarMonthDate,
        List<DailyLogLibraryItemDto> matchingItemsAscending,
        LocalDate today
    ) {
        Map<LocalDate, DailyLogLibraryItemDto> libraryItemByDate = new HashMap<>();
        for (DailyLogLibraryItemDto libraryItem : matchingItemsAscending) {
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

        List<DailyLogLibraryCalendarDayDto> calendarDays = new ArrayList<>();
        LocalDate cursorDate = firstCalendarDate;
        while (!cursorDate.isAfter(lastCalendarDate)) {
            DailyLogLibraryItemDto libraryItemOrNull = libraryItemByDate.get(cursorDate);
            calendarDays.add(new DailyLogLibraryCalendarDayDto(
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

    private static boolean matchesKeyword(DailyLogLibraryItemDto libraryItem, String keywordOrNull) {
        if (keywordOrNull == null || keywordOrNull.isBlank()) {
            return true;
        }

        String normalizedKeyword = keywordOrNull.toLowerCase(Locale.ROOT);
        String searchableText = new StringJoiner(" ")
            .add(libraryItem.getDate().toString())
            .add(libraryItem.getFlowLabel())
            .add(libraryItem.getExcerpt())
            .add(libraryItem.getMarkdownText())
            .toString()
            .toLowerCase(Locale.ROOT);
        return searchableText.contains(normalizedKeyword);
    }

    private static String buildExcerpt(DailyLogEntry dailyLogEntry) {
        List<EDailyLogSectionType> excerptSectionTypes = List.of(
            EDailyLogSectionType.ACHIEVEMENTS,
            EDailyLogSectionType.IMPROVEMENTS,
            EDailyLogSectionType.NOTES,
            EDailyLogSectionType.FOCUS,
            EDailyLogSectionType.GOALS
        );

        for (EDailyLogSectionType dailyLogSectionType : excerptSectionTypes) {
            List<String> contentLines = splitContentLines(dailyLogEntry.readSection(dailyLogSectionType));
            if (!contentLines.isEmpty()) {
                return abbreviate(contentLines.get(0));
            }
        }

        return "기록 내용이 저장되었습니다.";
    }

    private static String abbreviate(String text) {
        if (text.length() <= MAX_EXCERPT_LENGTH) {
            return text;
        }

        return text.substring(0, MAX_EXCERPT_LENGTH - 1).stripTrailing() + "…";
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
}
