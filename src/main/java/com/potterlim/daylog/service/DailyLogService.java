package com.potterlim.daylog.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.potterlim.daylog.dto.dailylog.DailyLogDayStatusDto;
import com.potterlim.daylog.support.DailyLogSectionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DailyLogService implements IDailyLogService {

    private static final List<DailyLogSectionType> SECTION_ORDER = List.of(
        DailyLogSectionType.GOALS,
        DailyLogSectionType.FOCUS,
        DailyLogSectionType.CHALLENGES,
        DailyLogSectionType.EVENING_GOALS,
        DailyLogSectionType.ACHIEVEMENTS,
        DailyLogSectionType.IMPROVEMENTS,
        DailyLogSectionType.GRATITUDE,
        DailyLogSectionType.NOTES
    );

    private static final EnumSet<DailyLogSectionType> MORNING_SECTIONS = EnumSet.of(
        DailyLogSectionType.GOALS,
        DailyLogSectionType.FOCUS,
        DailyLogSectionType.CHALLENGES
    );

    private static final String MORNING_TEMPLATE = String.join(
        "\r\n",
        DailyLogSectionType.GOALS.getHeaderText(),
        "",
        "",
        DailyLogSectionType.FOCUS.getHeaderText(),
        "",
        "",
        DailyLogSectionType.CHALLENGES.getHeaderText(),
        "",
        ""
    );

    private final Path mLogsRootPath;

    public DailyLogService(@Value("${day-log.logs-root-path:logs}") String logsRootPath) {
        mLogsRootPath = Path.of(logsRootPath).toAbsolutePath().normalize();
    }

    @Override
    public String readSection(LocalDate date, Long userAccountId, DailyLogSectionType dailyLogSectionType) {
        Path filePath = resolveFilePath(date, userAccountId);

        if (!Files.exists(filePath)) {
            return "";
        }

        Map<DailyLogSectionType, String> sectionBodyByType = parseSections(readFile(filePath));
        String body = sectionBodyByType.getOrDefault(dailyLogSectionType, "");

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
    public void writeSection(LocalDate date, Long userAccountId, DailyLogSectionType dailyLogSectionType, String body) {
        Path filePath = resolveFilePath(date, userAccountId);
        String fileText;

        if (Files.exists(filePath)) {
            fileText = readFile(filePath);
        } else if (MORNING_SECTIONS.contains(dailyLogSectionType)) {
            fileText = MORNING_TEMPLATE;
        } else {
            fileText = "";
        }

        Map<DailyLogSectionType, String> sectionBodyByType = parseSections(fileText);
        sectionBodyByType.put(dailyLogSectionType, normalizeBody(body));

        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, buildFileText(sectionBodyByType), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to write daily log file.", ioException);
        }
    }

    @Override
    public List<DailyLogDayStatusDto> listWeek(LocalDate referenceDate, Long userAccountId) {
        List<DailyLogDayStatusDto> dayStatuses = new ArrayList<>();
        LocalDate monday = referenceDate.minusDays(referenceDate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());

        for (int dayOffset = 0; dayOffset < 7; ++dayOffset) {
            LocalDate currentDate = monday.plusDays(dayOffset);
            Path filePath = resolveFilePath(currentDate, userAccountId);

            if (!Files.exists(filePath)) {
                continue;
            }

            dayStatuses.add(new DailyLogDayStatusDto(
                currentDate,
                hasSection(currentDate, userAccountId, DailyLogSectionType.GOALS),
                hasSection(currentDate, userAccountId, DailyLogSectionType.ACHIEVEMENTS)
            ));
        }

        return dayStatuses;
    }

    @Override
    public String readLogFileContent(LocalDate date, Long userAccountId) {
        Path filePath = resolveFilePath(date, userAccountId);

        if (!Files.exists(filePath)) {
            return "";
        }

        return readFile(filePath);
    }

    @Override
    public List<String> readCheckedGoalTexts(LocalDate date, Long userAccountId) {
        List<String> checkedGoalTexts = new ArrayList<>();

        for (String line : splitLines(readLogFileContent(date, userAccountId))) {
            if (line.startsWith("- [x]") || line.startsWith("- [X]")) {
                checkedGoalTexts.add(line.substring(5).trim());
            }
        }

        return checkedGoalTexts;
    }

    private boolean hasSection(LocalDate date, Long userAccountId, DailyLogSectionType dailyLogSectionType) {
        Path filePath = resolveFilePath(date, userAccountId);

        if (!Files.exists(filePath)) {
            return false;
        }

        String targetHeader = dailyLogSectionType.getHeaderText();
        for (String line : splitLines(readFile(filePath))) {
            if (line.stripTrailing().equals(targetHeader)) {
                return true;
            }
        }

        return false;
    }

    private Map<DailyLogSectionType, String> parseSections(String fileText) {
        Map<DailyLogSectionType, StringBuilder> sectionLinesByType = new LinkedHashMap<>();
        DailyLogSectionType currentSectionType = null;

        for (String line : splitLines(fileText)) {
            DailyLogSectionType matchedSectionType = findSectionType(line.stripTrailing());

            if (matchedSectionType != null) {
                currentSectionType = matchedSectionType;
                sectionLinesByType.putIfAbsent(currentSectionType, new StringBuilder());
                continue;
            }

            if (currentSectionType != null) {
                sectionLinesByType.get(currentSectionType).append(line).append("\r\n");
            }
        }

        Map<DailyLogSectionType, String> sectionBodyByType = new LinkedHashMap<>();
        for (Map.Entry<DailyLogSectionType, StringBuilder> entry : sectionLinesByType.entrySet()) {
            sectionBodyByType.put(entry.getKey(), entry.getValue().toString().stripTrailing());
        }

        return sectionBodyByType;
    }

    private String buildFileText(Map<DailyLogSectionType, String> sectionBodyByType) {
        List<String> blocks = new ArrayList<>();

        for (DailyLogSectionType dailyLogSectionType : SECTION_ORDER) {
            if (!sectionBodyByType.containsKey(dailyLogSectionType)) {
                continue;
            }

            List<String> blockLines = new ArrayList<>();
            blockLines.add(dailyLogSectionType.getHeaderText());

            String body = sectionBodyByType.get(dailyLogSectionType).strip();
            if (!body.isEmpty()) {
                for (String line : splitLines(body)) {
                    if (line.isBlank()) {
                        continue;
                    }

                    String normalizedLine = line.stripLeading().replaceFirst("^-\\s*", "").trim();
                    blockLines.add("- " + normalizedLine);
                }
            }

            blocks.add(String.join("\r\n", blockLines));
        }

        return String.join("\r\n\r\n", blocks).stripTrailing();
    }

    private static String normalizeBody(String bodyOrNull) {
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

    private static DailyLogSectionType findSectionType(String headerLine) {
        for (DailyLogSectionType dailyLogSectionType : DailyLogSectionType.values()) {
            if (dailyLogSectionType.getHeaderText().equals(headerLine)) {
                return dailyLogSectionType;
            }
        }

        return null;
    }

    private Path resolveFilePath(LocalDate date, Long userAccountId) {
        String folderName = String.format("%d_%02d_Week%d", date.getYear(), date.getMonthValue(), ((date.getDayOfMonth() - 1) / 7) + 1);
        return mLogsRootPath
            .resolve(String.valueOf(userAccountId))
            .resolve(folderName)
            .resolve(date + ".md");
    }

    private static String[] splitLines(String textOrNull) {
        if (textOrNull == null || textOrNull.isEmpty()) {
            return new String[0];
        }

        return textOrNull.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }

    private static String readFile(Path filePath) {
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to read daily log file.", ioException);
        }
    }
}
