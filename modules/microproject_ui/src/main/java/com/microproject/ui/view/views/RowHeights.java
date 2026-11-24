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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages per-row heights for Gantt and spreadsheet views. When no per-row
 * overrides are set, behaves identically to the legacy fixed-row-height calculation.
 * Caches cumulative y-positions for fast row<->y conversion.
 */
public final class RowHeights {
    private final int defaultHeight;
    private final Map<Integer, Integer> overrides = new HashMap<>();
    private final Map<Object, Integer> keyedOverrides = new HashMap<>();
    private List<?> rowKeys = List.of();
    private Object boundProjectionSource;
    private long boundTopologyRevision = Long.MIN_VALUE;
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

    /** Returns an independent copy of this row-height state. */
    public RowHeights copy() {
        RowHeights copy = new RowHeights(defaultHeight, rowCount);
        copy.overrides.putAll(overrides);
        copy.keyedOverrides.putAll(keyedOverrides);
        copy.rowKeys = new ArrayList<>(rowKeys);
        copy.boundProjectionSource = boundProjectionSource;
        copy.boundTopologyRevision = boundTopologyRevision;
        copy.invalidateCache();
        return copy;
    }

    /** Returns a copy with a different default while retaining row overrides. */
    public RowHeights withDefaultHeight(int height) {
        RowHeights copy = new RowHeights(height, rowCount);
        copy.keyedOverrides.putAll(keyedOverrides);
        if (rowKeys.size() == rowCount) {
            copy.rowKeys = rowKeys;
            copy.boundProjectionSource = boundProjectionSource;
            copy.boundTopologyRevision = boundTopologyRevision;
            copy.keyedOverrides.entrySet().removeIf(entry -> entry.getValue() == height);
            overrides.forEach((row, rowHeight) -> {
                if (rowHeight != height) copy.overrides.put(row, rowHeight);
            });
        } else {
            for (Map.Entry<Integer, Integer> entry : overrides.entrySet()) {
                if (entry.getValue() != height) copy.setRowHeight(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }

    /** Rebinds visible rows to stable projection identities, preserving hidden overrides. */
    public void setRowKeys(List<?> keys) {
        if (keys == null) throw new IllegalArgumentException("keys must not be null");
        List<?> newKeys = new ArrayList<>(keys);
        if (rowCount == newKeys.size() && rowKeys.equals(newKeys)) return;
        rebindRowKeys(newKeys);
        boundProjectionSource = null;
        boundTopologyRevision = Long.MIN_VALUE;
    }

    /** Rebinds only when this source's visible projection topology changes. */
    public void bindToProjection(Object source, long topologyRevision, List<?> keys) {
        if (source == null || keys == null) throw new IllegalArgumentException("projection source and keys are required");
        if (boundProjectionSource == source && boundTopologyRevision == topologyRevision
                && rowCount == keys.size()) return;
        rebindRowKeys(keys);
        boundProjectionSource = source;
        boundTopologyRevision = topologyRevision;
    }

    private void rebindRowKeys(List<?> keys) {
        if (rowKeys.size() == rowCount && !overrides.isEmpty()) {
            for (Map.Entry<Integer, Integer> entry : overrides.entrySet()) {
                int row = entry.getKey();
                if (row < rowKeys.size()) keyedOverrides.put(rowKeys.get(row), entry.getValue());
            }
        }
        // Projection row keys are immutable snapshots, so retain the reference without copying it.
        rowKeys = keys;
        rowCount = keys.size();
        overrides.clear();
        if (!keyedOverrides.isEmpty()) {
            for (int row = 0; row < rowKeys.size(); row++) {
                Integer height = keyedOverrides.get(rowKeys.get(row));
                if (height != null && height != defaultHeight) overrides.put(row, height);
                else if (height != null) keyedOverrides.remove(rowKeys.get(row));
            }
        }
        invalidateCache();
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
        if (rowKeys.size() != rowCount) rowKeys = List.of();
        boundProjectionSource = null;
        boundTopologyRevision = Long.MIN_VALUE;
        invalidateCache();
    }

    /** Returns the current number of rows. */
    public int getRowCount() {
        return rowCount;
    }

    public boolean hasRowKeys() {
        return rowKeys.size() == rowCount && rowCount > 0;
    }

    public Object rowKeyAt(int row) {
        if (row < 0 || row >= rowKeys.size()) throw new IndexOutOfBoundsException("row " + row);
        return rowKeys.get(row);
    }

    public int heightForKey(Object key) {
        return keyedOverrides.getOrDefault(key, defaultHeight);
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
            if (row < rowKeys.size()) keyedOverrides.remove(rowKeys.get(row));
        } else {
            overrides.put(row, height);
            if (row < rowKeys.size()) keyedOverrides.put(rowKeys.get(row), height);
        }
        invalidateCache();
    }

    /** Clears a per-row height override, reverting to default. Invalidates cache. */
    public void clearRowHeight(int row) {
        if (row < 0 || row >= rowCount) {
            throw new IndexOutOfBoundsException("row " + row + " not in [0, " + rowCount + ")");
        }
        if (overrides.remove(row) != null) {
            if (row < rowKeys.size()) keyedOverrides.remove(rowKeys.get(row));
            invalidateCache();
        }
    }

    /** Clears all per-row height overrides. Invalidates cache. */
    public void clearAllOverrides() {
        if (!overrides.isEmpty() || !keyedOverrides.isEmpty()) {
            overrides.clear();
            keyedOverrides.clear();
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

    /** Returns the row index for y; results outside the configured rows are not clamped. */
    public int rowAt(int y) {
        if (overrides.isEmpty()) return y / defaultHeight;
        if (y < 0) return y / defaultHeight;
        ensureCache();
        int total = cumulativeHeights[rowCount];
        if (y >= total) {
            return rowCount + (y - total) / defaultHeight;
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
