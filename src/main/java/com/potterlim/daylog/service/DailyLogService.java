package com.potterlim.daylog.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.potterlim.daylog.dto.dailylog.DailyLogDayStatusDto;
import com.potterlim.daylog.entity.DailyLogEntry;
import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.entity.UserAccountId;
import com.potterlim.daylog.repository.IDailyLogEntryRepository;
import com.potterlim.daylog.repository.IUserAccountRepository;
import com.potterlim.daylog.support.EDailyLogSectionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyLogService implements IDailyLogService {

    private final IDailyLogEntryRepository mDailyLogEntryRepository;
    private final IUserAccountRepository mUserAccountRepository;

    public DailyLogService(IDailyLogEntryRepository dailyLogEntryRepository, IUserAccountRepository userAccountRepository) {
        mDailyLogEntryRepository = dailyLogEntryRepository;
        mUserAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public String readSection(LocalDate date, UserAccountId userAccountId, EDailyLogSectionType dailyLogSectionType) {
        String body = findDailyLogEntry(date, userAccountId)
            .map(dailyLogEntry -> dailyLogEntry.readSection(dailyLogSectionType))
            .orElse("");

        if (body.isBlank()) {
            return "";
        }

        List<String> cleanedLines = new ArrayList<>();
        for (String line : splitLines(body)) {
            if (line.isBlank()) {
                continue;
            }

            if (line.startsWith("- ")) {
                cleanedLines.add(line.substring(2).stripTrailing());
            } else {
                cleanedLines.add(line.stripTrailing());
            }
        }

        return String.join("\r\n", cleanedLines);
    }

    @Override
    @Transactional
    public void writeSection(
        LocalDate date,
        UserAccountId userAccountId,
        EDailyLogSectionType dailyLogSectionType,
        String bodyOrNull
    ) {
        DailyLogEntry dailyLogEntry = findDailyLogEntry(date, userAccountId)
            .orElseGet(() -> createDailyLogEntry(date, userAccountId));

        dailyLogEntry.writeSection(dailyLogSectionType, normalizeSectionBody(bodyOrNull));
        mDailyLogEntryRepository.save(dailyLogEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyLogDayStatusDto> listWeek(LocalDate referenceDate, UserAccountId userAccountId) {
        LocalDate monday = referenceDate.minusDays(referenceDate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        LocalDate sunday = monday.plusDays(6L);
        List<DailyLogDayStatusDto> dayStatuses = new ArrayList<>();

        for (DailyLogEntry dailyLogEntry : mDailyLogEntryRepository.findWeekEntries(userAccountId.getValue(), monday, sunday)) {
            dayStatuses.add(new DailyLogDayStatusDto(
                dailyLogEntry.getLogDate(),
                dailyLogEntry.hasMorningLog(),
                dailyLogEntry.hasEveningLog()
            ));
        }

        return dayStatuses;
    }

    @Override
    @Transactional(readOnly = true)
    public String readLogFileContent(LocalDate date, UserAccountId userAccountId) {
        return findDailyLogEntry(date, userAccountId)
            .map(DailyLogEntry::buildMarkdownText)
            .orElse("");
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> readCheckedGoalTexts(LocalDate date, UserAccountId userAccountId) {
        return findDailyLogEntry(date, userAccountId)
            .map(DailyLogEntry::readCheckedGoalTexts)
            .orElse(List.of());
    }

    private Optional<DailyLogEntry> findDailyLogEntry(LocalDate date, UserAccountId userAccountId) {
        return mDailyLogEntryRepository.findByUserAccountIdAndLogDate(userAccountId.getValue(), date);
    }

    private DailyLogEntry createDailyLogEntry(LocalDate date, UserAccountId userAccountId) {
        UserAccount userAccount = mUserAccountRepository.getReferenceById(userAccountId.getValue());
        return DailyLogEntry.create(userAccount, date);
    }

    private static String normalizeSectionBody(String bodyOrNull) {
        if (bodyOrNull == null || bodyOrNull.isBlank()) {
            return "";
        }

        List<String> normalizedLines = new ArrayList<>();
        for (String line : splitLines(bodyOrNull)) {
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty()) {
                continue;
            }

            normalizedLines.add(trimmedLine);
        }

        return String.join("\r\n", normalizedLines);
    }

    private static String[] splitLines(String textOrNull) {
        if (textOrNull == null || textOrNull.isEmpty()) {
            return new String[0];
        }

        return textOrNull.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }
}
