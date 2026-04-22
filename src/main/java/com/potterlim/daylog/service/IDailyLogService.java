package com.potterlim.daylog.service;

import java.time.LocalDate;
import java.util.List;
import com.potterlim.daylog.dto.dailylog.DailyLogDayStatusDto;
import com.potterlim.daylog.entity.UserAccountId;
import com.potterlim.daylog.support.EDailyLogSectionType;

public interface IDailyLogService {

    /**
     * Reads the content stored under a given daily log section.
     *
     * <p>Preconditions: the date, user account id, and section type must already be validated by
     * the caller. When the entry or section does not exist, the method returns an empty string.</p>
     *
     * @return The section body with list prefixes removed for form editing.
     */
    String readSection(LocalDate date, UserAccountId userAccountId, EDailyLogSectionType dailyLogSectionType);

    /**
     * Writes a daily log section for a specific user and date.
     *
     * <p>Preconditions: the date, user account id, and section type must already be validated by
     * the caller. The body may be null or empty, and the method creates the day entry when it does
     * not exist yet.</p>
     *
     * @return Nothing. Successful execution updates the persisted daily log entry.
     */
    void writeSection(LocalDate date, UserAccountId userAccountId, EDailyLogSectionType dailyLogSectionType, String bodyOrNull);

    /**
     * Lists the available daily log entries for the week that contains the reference date.
     *
     * <p>Preconditions: the reference date and user account id must already be validated by the
     * caller. Only dates with a persisted day entry are returned.</p>
     *
     * @return Ordered day status entries for the matching week.
     */
    List<DailyLogDayStatusDto> listWeek(LocalDate referenceDate, UserAccountId userAccountId);

    /**
     * Reads the reconstructed markdown content for preview rendering and detailed analysis.
     *
     * <p>Preconditions: the date and user account id must already be validated by the caller. When
     * the target day entry does not exist, the method returns an empty string.</p>
     *
     * @return The full markdown representation of the stored day entry, or an empty string when the
     * entry is missing.
     */
    String readLogFileContent(LocalDate date, UserAccountId userAccountId);

    /**
     * Reads the goal texts that are marked as completed in the evening section.
     *
     * <p>Preconditions: the date and user account id must already be validated by the caller. When
     * the target day entry does not exist, the method returns an empty list.</p>
     *
     * @return Checked goal texts in the order they appear in the saved evening checklist.
     */
    List<String> readCheckedGoalTexts(LocalDate date, UserAccountId userAccountId);
}
