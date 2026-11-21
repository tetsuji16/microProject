/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.format;

import java.io.Serializable;

/**
 * Immutable text format for a sheet text style category.
 * Mirrors the UI-side TaskFormatPainterService.TaskTextFormat.
 */
public record SheetTextFormat(
    String family,
    int size,
    Integer color,
    boolean bold,
    boolean italic,
    boolean strikethrough,
    boolean underline,
    Integer background
) implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Returns an empty format (all fields null/zero/false).
     */
    public static SheetTextFormat empty() {
        return new SheetTextFormat(null, 0, null, false, false, false, false, null);
    }

    /**
     * Returns true if this format specifies no attributes (all defaults).
     */
    public boolean isEmpty() {
        return family == null && size == 0 && color == null && !bold && !italic
            && !strikethrough && !underline && background == null;
    }

    /**
     * Returns a new format with the non-null/non-zero fields from {@code other} overriding this.
     * Only attributes that are explicitly set in {@code other} are applied.
     */
    public SheetTextFormat overrideWith(SheetTextFormat other) {
        if (other == null) return this;
        return new SheetTextFormat(
            other.family != null ? other.family : this.family,
            other.size != 0 ? other.size : this.size,
            other.color != null ? other.color : this.color,
            other.bold ? other.bold : this.bold,
            other.italic ? other.italic : this.italic,
            other.strikethrough ? other.strikethrough : this.strikethrough,
            other.underline ? other.underline : this.underline,
            other.background != null ? other.background : this.background
        );
    }
}