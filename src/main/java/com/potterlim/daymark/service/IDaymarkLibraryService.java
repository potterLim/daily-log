package com.potterlim.daymark.service;

import com.potterlim.daymark.dto.daymark.DaymarkLibrarySearchCriteria;
import com.potterlim.daymark.dto.daymark.DaymarkLibraryViewDto;
import com.potterlim.daymark.entity.UserAccountId;

public interface IDaymarkLibraryService {

    /**
     * Searches the long-term Daymark library for a user.
     *
     * <p>Preconditions: the search criteria and user account id must already be validated by the
     * caller. The result is ordered for timeline display from newest to oldest.</p>
     *
     * @return A complete library view model containing timeline, trend, calendar, and summary data.
     */
    DaymarkLibraryViewDto searchLibrary(DaymarkLibrarySearchCriteria searchCriteria, UserAccountId userAccountId);

    /**
     * Builds a Markdown export for the selected library range.
     *
     * <p>Preconditions: the search criteria and user account id must already be validated by the
     * caller. The export uses chronological order so the file reads like a continuous journal.</p>
     *
     * @return Markdown text ready to download.
     */
    String buildLibraryMarkdownExport(DaymarkLibrarySearchCriteria searchCriteria, UserAccountId userAccountId);
}
