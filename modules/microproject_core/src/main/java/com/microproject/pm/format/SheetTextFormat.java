/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.format;

/**
 * Immutable description of a sheet text format.
 * Matches the attributes used by {@link com.microproject.pm.graphic.frames.TaskFormatPainterService.TaskTextFormat}
 * but lives in the core module so it can be persisted by the exchange layer without a UI dependency.
 *
 * <p>{@code null} family, {@code 0} size, {@code null} color/background mean "not specified"
 * and are rendered using the view defaults.
 */
public record SheetTextFormat(String family, int size, Integer color, boolean bold, boolean italic,
                              boolean strikethrough, boolean underline, Integer background) {

    public SheetTextFormat {
        // Normalize: empty string -> null for family
        if (family != null && family.isBlank()) family = null;
        // Clamp size to a reasonable range; 0 means inherit/default
        if (size < 0) size = 0;
        if (size > 72) size = 72;
    }

    /**
     * Creates a format with only the explicitly provided attributes; unspecified attributes
     * use their "not specified" defaults ({@code null}/{@code 0}/{@code false}).
     */
    public static SheetTextFormat of(String family, int size, Integer color, boolean bold,
                                     boolean italic, boolean strikethrough, boolean underline,
                                     Integer background) {
        return new SheetTextFormat(family, size, color, bold, italic, strikethrough, underline, background);
    }

    /** Returns a copy with the font family replaced. */
    public SheetTextFormat withFamily(String family) {
        return new SheetTextFormat(family, size, color, bold, italic, strikethrough, underline, background);
    }

    /** Returns a copy with the font size replaced. */
    public SheetTextFormat withSize(int size) {
        return new SheetTextFormat(family, size, color, bold, italic, strikethrough, underline, background);
    }

    /** Returns a copy with the bold flag replaced. */
    public SheetTextFormat withBold(boolean bold) {
        return new SheetTextFormat(family, size, color, bold, italic, strikethrough, underline, background);
    }

    /** Returns a copy with the italic flag replaced. */
    public SheetTextFormat withItalic(boolean italic) {
        return new SheetTextFormat(family, size, color, bold, italic, strikethrough, underline, background);
    }

    /** Returns a copy with the strikethrough flag replaced. */
    public SheetTextFormat withStrikethrough(boolean strikethrough) {
        return new SheetTextFormat(family, size, color, bold, italic, strikethrough, underline, background);
    }

    /** Returns a copy with the underline flag replaced. */
    public SheetTextFormat withUnderline(boolean underline) {
        return new SheetTextFormat(family, size, color, bold, italic, strikethrough, underline, background);
    }

    /** Returns a copy with the font color replaced. */
    public SheetTextFormat withColor(Integer color) {
        return new SheetTextFormat(family, size, color, bold, italic, strikethrough, underline, background);
    }

    /** Returns a copy with the background color replaced. */
    public SheetTextFormat withBackground(Integer background) {
        return new SheetTextFormat(family, size, color, bold, italic, strikethrough, underline, background);
    }

    /** True when no attribute is explicitly set (all defaults). */
    public boolean isEmpty() {
        return family == null && size == 0 && color == null && !bold && !italic
                && !strikethrough && !underline && background == null;
    }
}