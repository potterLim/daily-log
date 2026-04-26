package com.potterlim.daymark.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.potterlim.daymark.dto.daymark.DaymarkDayStatusDto;
import com.potterlim.daymark.entity.DaymarkEntry;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.entity.UserAccountId;
import com.potterlim.daymark.repository.IDaymarkEntryRepository;
import com.potterlim.daymark.repository.IUserAccountRepository;
import com.potterlim.daymark.support.EDaymarkSectionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DaymarkService implements IDaymarkService {

    private final IDaymarkEntryRepository mDaymarkEntryRepository;
    private final IUserAccountRepository mUserAccountRepository;

    public DaymarkService(IDaymarkEntryRepository daymarkEntryRepository, IUserAccountRepository userAccountRepository) {
        mDaymarkEntryRepository = daymarkEntryRepository;
        mUserAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public String readSection(LocalDate date, UserAccountId userAccountId, EDaymarkSectionType daymarkSectionType) {
        String body = findDaymarkEntry(date, userAccountId)
            .map(daymarkEntry -> daymarkEntry.readSection(daymarkSectionType))
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
        EDaymarkSectionType daymarkSectionType,
        String bodyOrNull
    ) {
        String normalizedBody = normalizeSectionBody(bodyOrNull);
        Optional<DaymarkEntry> daymarkEntryOptional = findDaymarkEntry(date, userAccountId);
        if (daymarkEntryOptional.isEmpty() && normalizedBody.isEmpty()) {
            return;
        }

        DaymarkEntry daymarkEntry = daymarkEntryOptional
            .orElseGet(() -> createDaymarkEntry(date, userAccountId));

        daymarkEntry.writeSection(daymarkSectionType, normalizedBody);
        if (!daymarkEntry.hasAnyEntryContent()) {
            mDaymarkEntryRepository.delete(daymarkEntry);
            return;
        }

        mDaymarkEntryRepository.save(daymarkEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DaymarkDayStatusDto> listWeek(LocalDate referenceDate, UserAccountId userAccountId) {
        LocalDate monday = referenceDate.minusDays(referenceDate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        LocalDate sunday = monday.plusDays(6L);
        List<DaymarkDayStatusDto> dayStatuses = new ArrayList<>();

        for (DaymarkEntry daymarkEntry : mDaymarkEntryRepository.findWeekEntries(userAccountId.getValue(), monday, sunday)) {
            if (!daymarkEntry.hasAnyEntryContent()) {
                continue;
            }

            dayStatuses.add(new DaymarkDayStatusDto(
                daymarkEntry.getEntryDate(),
                daymarkEntry.hasMorningEntry(),
                daymarkEntry.hasEveningEntry()
            ));
        }

        return dayStatuses;
    }

    @Override
    @Transactional(readOnly = true)
    public String readEntryMarkdownContent(LocalDate date, UserAccountId userAccountId) {
        return findDaymarkEntry(date, userAccountId)
            .map(DaymarkEntry::buildMarkdownText)
            .orElse("");
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> readCheckedGoalTexts(LocalDate date, UserAccountId userAccountId) {
        return findDaymarkEntry(date, userAccountId)
            .map(DaymarkEntry::readCheckedGoalTexts)
            .orElse(List.of());
    }

    private Optional<DaymarkEntry> findDaymarkEntry(LocalDate date, UserAccountId userAccountId) {
        return mDaymarkEntryRepository.findByUserAccountIdAndLogDate(userAccountId.getValue(), date);
    }

    private DaymarkEntry createDaymarkEntry(LocalDate date, UserAccountId userAccountId) {
        UserAccount userAccount = mUserAccountRepository.getReferenceById(userAccountId.getValue());
        return DaymarkEntry.create(userAccount, date);
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
