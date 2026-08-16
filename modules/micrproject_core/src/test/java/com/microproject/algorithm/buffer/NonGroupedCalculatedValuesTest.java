/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.algorithm.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NonGroupedCalculatedValuesTest {
    @Test
    void exposesOrderedSparsePointsWithoutFinishStep() {
        NonGroupedCalculatedValues values = new NonGroupedCalculatedValues(2.0, false, 0L);
        values.set(0, 20L, 30L, 8.0, null);
        values.set(0, 10L, 20L, 4.0, null);

        assertEquals(3, values.size());
        assertEquals(10L, values.getDate(0));
        assertEquals(2.0, values.getValue(0));
        assertEquals(2.0, values.getValue(1));
        assertEquals(-4.0, values.getValue(2));
    }

    @Test
    void cumulativeConversionRoundTripsDeltaValues() {
        NonGroupedCalculatedValues values = new NonGroupedCalculatedValues(false, 0L);
        values.set(0, 10L, 20L, 4.0, null);
        values.set(0, 20L, 30L, 8.0, null);
        List<Double> original = snapshot(values);

        values.makeCumulative(true);
        values.makeCumulative(false);

        assertEquals(original, snapshot(values));
    }

    @Test
    void reusesIndexedSnapshotUntilValuesChange() throws ReflectiveOperationException {
        NonGroupedCalculatedValues values = new NonGroupedCalculatedValues(false, 0L);
        for (int index = 0; index < 1_000; index++)
            values.set(index, index + 1L, index + 2L, index, null);

        Field indexedDates = NonGroupedCalculatedValues.class.getDeclaredField("indexedDates");
        indexedDates.setAccessible(true);
        assertNull(indexedDates.get(values));

        values.getDate(values.size() - 1);
        Object firstSnapshot = indexedDates.get(values);
        values.getValue(values.size() - 1);
        values.getDate(0);
        assertSame(firstSnapshot, indexedDates.get(values));

        values.set(1_000, 2_000L, 2_001L, 1.0, null);
        assertNull(indexedDates.get(values));
        values.getValue(values.size() - 1);
        assertNotSame(firstSnapshot, indexedDates.get(values));
    }

    private static List<Double> snapshot(NonGroupedCalculatedValues values) {
        List<Double> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++)
            result.add(values.getValue(index));
        return result;
    }
}
