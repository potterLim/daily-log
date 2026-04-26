package com.potterlim.daymark.service;

import java.time.LocalDate;
import java.util.List;
import com.potterlim.daymark.dto.daymark.DaymarkDayStatusDto;
import com.potterlim.daymark.entity.UserAccountId;
import com.potterlim.daymark.support.EDaymarkSectionType;

public interface IDaymarkService {

    /**
     * Reads the content stored under a given Daymark section.
     *
     * <p>Preconditions: the date, user account id, and section type must already be validated by
     * the caller. When the entry or section does not exist, the method returns an empty string.</p>
     *
     * @return The section body with list prefixes removed for form editing.
     */
    String readSection(LocalDate date, UserAccountId userAccountId, EDaymarkSectionType daymarkSectionType);

    /**
     * Writes a Daymark section for a specific user and date.
     *
     * <p>Preconditions: the date, user account id, and section type must already be validated by
     * the caller. Blank bodies clear the section and do not create empty entries.</p>
     *
     * @return Nothing. Successful execution updates the persisted Daymark entry.
     */
    void writeSection(
        LocalDate date,
        UserAccountId userAccountId,
        EDaymarkSectionType daymarkSectionType,
        String bodyOrNull
    );

    /**
     * Lists the available Daymark entries for the week that contains the reference date.
     *
     * <p>Preconditions: the reference date and user account id must already be validated by the
     * caller. Only dates with persisted entry content are returned.</p>
     *
     * @return Ordered day status entries for the matching week.
     */
    List<DaymarkDayStatusDto> listWeek(LocalDate referenceDate, UserAccountId userAccountId);

    /**
     * Reads the reconstructed markdown content for preview rendering and detailed analysis.
     *
     * <p>Preconditions: the date and user account id must already be validated by the caller. When
     * the target entry does not exist, the method returns an empty string.</p>
     *
     * @return The full markdown representation of the stored entry, or an empty string when the
     * entry is missing.
     */
    String readEntryMarkdownContent(LocalDate date, UserAccountId userAccountId);

    /**
     * Reads the goal texts that are marked as completed in the evening section.
     *
     * <p>Preconditions: the date and user account id must already be validated by the caller. When
     * the target entry does not exist, the method returns an empty list.</p>
     *
     * @return Checked goal texts in the order they appear in the saved evening checklist.
     */
    List<String> readCheckedGoalTexts(LocalDate date, UserAccountId userAccountId);
}
