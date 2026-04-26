package com.potterlim.daymark.dto.daymark;

import java.util.List;

public final class DaymarkLibraryContentBlockDto {

    private final String mEyebrow;
    private final String mTitle;
    private final List<String> mLines;
    private final int mHiddenLineCount;
    private final EDaymarkLibraryContentTone mContentTone;

    public DaymarkLibraryContentBlockDto(
        String eyebrow,
        String title,
        List<String> lines,
        int hiddenLineCount,
        EDaymarkLibraryContentTone contentTone
    ) {
        mEyebrow = eyebrow;
        mTitle = title;
        mLines = List.copyOf(lines);
        mHiddenLineCount = hiddenLineCount;
        mContentTone = contentTone;
    }

    public String getEyebrow() {
        return mEyebrow;
    }

    public String getTitle() {
        return mTitle;
    }

    public List<String> getLines() {
        return mLines;
    }

    public int getHiddenLineCount() {
        return mHiddenLineCount;
    }

    public boolean hasHiddenLines() {
        return mHiddenLineCount > 0;
    }

    public String getToneCssClass() {
        return mContentTone.getCssClassName();
    }
}
