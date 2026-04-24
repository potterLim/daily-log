package com.potterlim.daylog.service;

import com.potterlim.daylog.dto.dailylog.DailyLogLibrarySearchCriteria;
import com.potterlim.daylog.dto.dailylog.DailyLogLibraryViewDto;
import com.potterlim.daylog.entity.UserAccountId;

public interface IDailyLogLibraryService {

    /**
     * Searches the long-term daily log library for a user.
     *
     * <p>Preconditions: the search criteria and user account id must already be validated by the
     * caller. The result is ordered for timeline display from newest to oldest.</p>
     *
     * @return A complete library view model containing timeline, trend, calendar, and summary data.
     */
    DailyLogLibraryViewDto searchLibrary(DailyLogLibrarySearchCriteria searchCriteria, UserAccountId userAccountId);

    /**
     * Builds a Markdown export for the selected library range.
     *
     * <p>Preconditions: the search criteria and user account id must already be validated by the
     * caller. The export uses chronological order so the file reads like a continuous journal.</p>
     *
     * @return Markdown text ready to download.
     */
    String buildLibraryMarkdownExport(DailyLogLibrarySearchCriteria searchCriteria, UserAccountId userAccountId);
}
