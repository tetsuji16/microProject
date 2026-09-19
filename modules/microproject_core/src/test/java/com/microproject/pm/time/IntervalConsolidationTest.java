/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntervalConsolidationTest {
    @Test
    void mutableIntervalOwnsRangeOperations() {
        MutableInterval interval = new MutableInterval(10L, 20L);

        interval.union(new ImmutableInterval(5L, 25L));
        assertEquals(5L, interval.getStart());
        assertEquals(25L, interval.getEnd());

        interval.inter(new ImmutableInterval(8L, 12L));
        assertEquals(8L, interval.getStart());
        assertEquals(12L, interval.getEnd());
    }

    @Test
    void legacyDefaultTimeIntervalDelegatesToCanonicalImplementation() {
        com.microproject.core.time.DefaultTimeInterval legacy =
                new com.microproject.core.time.DefaultTimeInterval(10L, 20L);

        legacy.union(new com.microproject.core.time.DefaultTimeInterval(5L, 25L));
        assertEquals(5L, legacy.getStart());
        assertEquals(25L, legacy.getEnd());

        legacy.clear();
        assertTrue(legacy.isEmpty());
        assertFalse(new MutableInterval(0L, 1L).isEmpty());
        assertFalse(new MutableInterval(-3L, -2L).isEmpty());
    }
}
