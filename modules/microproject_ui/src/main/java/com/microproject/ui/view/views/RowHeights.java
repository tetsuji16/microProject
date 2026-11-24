/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.ui.view.views;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages per-row heights for Gantt and spreadsheet views. When no per-row
 * overrides are set, behaves identically to the legacy fixed-row-height calculation.
 * Caches cumulative y-positions for fast row<->y conversion.
 */
public final class RowHeights {
    private final int defaultHeight;
    private final Map<Integer, Integer> overrides = new HashMap<>();
    private int[] cumulativeHeights; // cumulativeHeights[i] = sum of heights of rows 0..i-1
    private int rowCount = 0;
    private boolean cacheValid = false;

    public RowHeights(int defaultHeight) {
        if (defaultHeight <= 0) {
            throw new IllegalArgumentException("defaultHeight must be > 0");
        }
        this.defaultHeight = defaultHeight;
    }

    public RowHeights(int defaultHeight, int rowCount) {
        this(defaultHeight);
        setRowCount(rowCount);
    }

    /** Returns the default row height (used when no override exists). */
    public int getDefaultHeight() {
        return defaultHeight;
    }

    /** Sets the number of rows and invalidates the cache. */
    public void setRowCount(int rowCount) {
        if (rowCount < 0) {
            throw new IllegalArgumentException("rowCount must be >= 0");
        }
        if (rowCount < this.rowCount) {
            overrides.keySet().removeIf(row -> row >= rowCount);
        }
        this.rowCount = rowCount;
        invalidateCache();
    }

    /** Returns the current number of rows. */
    public int getRowCount() {
        return rowCount;
    }

    /** Sets a per-row height override. Invalidates cache. */
    public void setRowHeight(int row, int height) {
        if (row < 0 || row >= rowCount) {
            throw new IndexOutOfBoundsException("row " + row + " not in [0, " + rowCount + ")");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be > 0");
        }
        if (height == defaultHeight) {
            overrides.remove(row);
        } else {
            overrides.put(row, height);
        }
        invalidateCache();
    }

    /** Clears a per-row height override, reverting to default. Invalidates cache. */
    public void clearRowHeight(int row) {
        if (row < 0 || row >= rowCount) {
            throw new IndexOutOfBoundsException("row " + row + " not in [0, " + rowCount + ")");
        }
        if (overrides.remove(row) != null) {
            invalidateCache();
        }
    }

    /** Clears all per-row height overrides. Invalidates cache. */
    public void clearAllOverrides() {
        if (!overrides.isEmpty()) {
            overrides.clear();
            invalidateCache();
        }
    }

    /** Returns the height of a specific row (override or default). */
    public int heightOf(int row) {
        if (row < 0 || row >= rowCount) {
            throw new IndexOutOfBoundsException("row " + row + " not in [0, " + rowCount + ")");
        }
        return overrides.getOrDefault(row, defaultHeight);
    }

    /** Returns the y-coordinate of the top of the given row. */
    public int yOf(int row) {
        if (row < 0 || row > rowCount) {
            throw new IndexOutOfBoundsException("row " + row + " not in [0, " + rowCount + "]");
        }
        ensureCache();
        return cumulativeHeights[row];
    }

    /** Returns the total height of the first {@code rowCount} rows. */
    public int totalHeight(int rowCount) {
        if (rowCount < 0 || rowCount > this.rowCount) {
            throw new IndexOutOfBoundsException("rowCount " + rowCount + " not in [0, " + this.rowCount + "]");
        }
        ensureCache();
        return cumulativeHeights[rowCount];
    }

    /** Returns the row index at or above the given y-coordinate. */
    public int rowAt(int y) {
        if (y < 0) {
            return 0;
        }
        ensureCache();
        int total = cumulativeHeights[rowCount];
        if (y >= total) {
            return rowCount - 1;
        }
        // Binary search on cumulativeHeights
        int lo = 0;
        int hi = rowCount;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cumulativeHeights[mid] > y) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return Math.max(0, lo - 1);
    }

    /** Returns true if any per-row overrides are set. */
    public boolean hasOverrides() {
        return !overrides.isEmpty();
    }

    /** Returns the number of rows with non-default heights. */
    public int overrideCount() {
        return overrides.size();
    }

    /** Invalidates the cumulative height cache. */
    private void invalidateCache() {
        cacheValid = false;
    }

    /** Builds the cumulative height array if needed. */
    private void ensureCache() {
        if (cacheValid) {
            return;
        }
        cumulativeHeights = new int[rowCount + 1];
        int sum = 0;
        cumulativeHeights[0] = 0;
        for (int i = 0; i < rowCount; i++) {
            sum += overrides.getOrDefault(i, defaultHeight);
            cumulativeHeights[i + 1] = sum;
        }
        cacheValid = true;
    }
}
