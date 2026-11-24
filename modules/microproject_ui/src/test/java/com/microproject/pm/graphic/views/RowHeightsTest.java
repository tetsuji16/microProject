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
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class RowHeightsTest {

    @Test
    public void noOverrides_matchesLegacyCalculation() {
        RowHeights rh = new RowHeights(20, 10);
        // yOf(row) == row * defaultHeight
        for (int r = 0; r <= 10; r++) {
            assertEquals(r * 20, rh.yOf(r));
        }
        // rowAt(y) == y / defaultHeight (clamped)
        for (int y = 0; y < 200; y++) {
            int expected = Math.min(9, y / 20);
            assertEquals(expected, rh.rowAt(y));
        }
        // totalHeight == rowCount * defaultHeight
        assertEquals(200, rh.totalHeight(10));
        assertEquals(100, rh.totalHeight(5));
        // heightOf == defaultHeight
        for (int r = 0; r < 10; r++) {
            assertEquals(20, rh.heightOf(r));
        }
    }

    @Test
    public void singleOverride_rowAt_yOf_inverse() {
        RowHeights rh = new RowHeights(20, 5);
        rh.setRowHeight(2, 40); // row 2 is double height
        // y positions: row0=0, row1=20, row2=40, row3=80, row4=100, row5=120
        assertEquals(0, rh.yOf(0));
        assertEquals(20, rh.yOf(1));
        assertEquals(40, rh.yOf(2));
        assertEquals(80, rh.yOf(3));
        assertEquals(100, rh.yOf(4));
        assertEquals(120, rh.yOf(5));
        // rowAt inverse
        assertEquals(0, rh.rowAt(0));
        assertEquals(0, rh.rowAt(19));
        assertEquals(1, rh.rowAt(20));
        assertEquals(1, rh.rowAt(39));
        assertEquals(2, rh.rowAt(40));
        assertEquals(2, rh.rowAt(79));
        assertEquals(3, rh.rowAt(80));
        assertEquals(3, rh.rowAt(99));
        assertEquals(4, rh.rowAt(100));
        assertEquals(4, rh.rowAt(119));
        assertEquals(4, rh.rowAt(120));
        assertEquals(4, rh.rowAt(200));
    }

    @Test
    public void multipleOverrides() {
        RowHeights rh = new RowHeights(20, 6);
        rh.setRowHeight(1, 30);
        rh.setRowHeight(3, 50);
        rh.setRowHeight(5, 10);
        // y positions: 0, 20, 50, 70, 120, 140, 150
        assertEquals(0, rh.yOf(0));
        assertEquals(20, rh.yOf(1));
        assertEquals(50, rh.yOf(2));
        assertEquals(70, rh.yOf(3));
        assertEquals(120, rh.yOf(4));
        assertEquals(140, rh.yOf(5));
        assertEquals(150, rh.yOf(6));
        assertEquals(150, rh.totalHeight(6));
    }

    @Test
    public void clearOverride_revertsToDefault() {
        RowHeights rh = new RowHeights(20, 4);
        rh.setRowHeight(1, 40);
        assertEquals(40, rh.heightOf(1));
        rh.clearRowHeight(1);
        assertEquals(20, rh.heightOf(1));
        assertEquals(40, rh.yOf(2)); // 0+20+20
    }

    @Test
    public void clearAllOverrides() {
        RowHeights rh = new RowHeights(20, 4);
        rh.setRowHeight(0, 30);
        rh.setRowHeight(2, 40);
        assertTrue(rh.hasOverrides());
        rh.clearAllOverrides();
        assertFalse(rh.hasOverrides());
        for (int r = 0; r < 4; r++) {
            assertEquals(20, rh.heightOf(r));
        }
    }

    @Test
    public void setRowCount_expandsAndContracts() {
        RowHeights rh = new RowHeights(20, 3);
        rh.setRowHeight(2, 40);
        assertEquals(80, rh.totalHeight(3));
        rh.setRowCount(5);
        assertEquals(5, rh.getRowCount());
        assertEquals(40, rh.heightOf(2)); // override preserved
        assertEquals(120, rh.totalHeight(5)); // 20+20+40+20+20
        rh.setRowCount(2);
        assertEquals(2, rh.getRowCount());
        assertEquals(40, rh.totalHeight(2)); // 20+20 (override on row 2 dropped)
    }

    @Test
    public void boundaryYValues() {
        RowHeights rh = new RowHeights(20, 3);
        rh.setRowHeight(1, 30);
        // Row 0: y=[0,20), Row 1: y=[20,50), Row 2: y=[50,70)
        assertEquals(0, rh.rowAt(-1));
        assertEquals(0, rh.rowAt(0));
        assertEquals(0, rh.rowAt(19));
        assertEquals(1, rh.rowAt(20));
        assertEquals(1, rh.rowAt(49));
        assertEquals(2, rh.rowAt(50));
        assertEquals(2, rh.rowAt(69));
        assertEquals(2, rh.rowAt(70));
        assertEquals(2, rh.rowAt(100));
    }

    @Test
    public void heightOf_outOfBounds() {
        RowHeights rh = new RowHeights(20, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> rh.heightOf(3));
    }

    @Test
    public void yOf_outOfBounds() {
        RowHeights rh = new RowHeights(20, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> rh.yOf(4));
    }

    @Test
    public void totalHeight_outOfBounds() {
        RowHeights rh = new RowHeights(20, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> rh.totalHeight(4));
    }

    @Test
    public void constructor_invalidDefaultHeight() {
        assertThrows(IllegalArgumentException.class, () -> new RowHeights(0, 3));
    }

    @Test
    public void setRowHeight_invalidHeight() {
        RowHeights rh = new RowHeights(20, 3);
        assertThrows(IllegalArgumentException.class, () -> rh.setRowHeight(0, -1));
    }

    @Test
    public void setRowHeight_outOfBounds() {
        RowHeights rh = new RowHeights(20, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> rh.setRowHeight(3, 30));
    }
}