package com.potterlim.daylog.service;

import java.time.LocalDate;
import java.util.List;
import com.potterlim.daylog.dto.dailylog.DailyLogDayStatusDto;
import com.potterlim.daylog.support.DailyLogSectionType;

public interface IDailyLogService {

    /**
     * Reads the content stored under a given markdown section.
     *
     * <p>Preconditions: the date, user account id, and section type must already be validated by
     * the caller. When the file or section does not exist, the method returns an empty string.</p>
     *
     * @return The section body with list prefixes removed for form editing.
     */
    String readSection(LocalDate date, Long userAccountId, DailyLogSectionType dailyLogSectionType);

    /**
     * Writes a markdown section for a specific user and date.
     *
     * <p>Preconditions: the date, user account id, and section type must already be validated by
     * the caller. The body may be empty, and the method preserves the shared section ordering used
     * by the log file format.</p>
     *
     * @return Nothing. Successful execution updates the markdown file on disk.
     */
    void writeSection(LocalDate date, Long userAccountId, DailyLogSectionType dailyLogSectionType, String body);

    /**
     * Lists the available daily log files for the week that contains the reference date.
     *
     * <p>Preconditions: the reference date and user account id must already be validated by the
     * caller. Only dates with an existing markdown file are returned.</p>
     *
     * @return Ordered day status entries for the matching week.
     */
    List<DailyLogDayStatusDto> listWeek(LocalDate referenceDate, Long userAccountId);

    /**
     * Reads the raw markdown file content for preview rendering and detailed analysis.
     *
     * <p>Preconditions: the date and user account id must already be validated by the caller. When
     * the target file does not exist, the method returns an empty string.</p>
     *
     * @return The full markdown file content, or an empty string when the file is missing.
     */
    String readLogFileContent(LocalDate date, Long userAccountId);

    /**
     * Reads the goal texts that are marked as completed in the evening section.
     *
     * <p>Preconditions: the date and user account id must already be validated by the caller. When
     * the target file does not exist, the method returns an empty list.</p>
     *
     * @return Checked goal texts in the order they appear in the file.
     */
    List<String> readCheckedGoalTexts(LocalDate date, Long userAccountId);
}
