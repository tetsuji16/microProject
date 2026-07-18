package com.projectlibre1.grouping.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class OutlineCollectionImplTest {
    @Test
    void everyConfiguredOutlineIsCreatedLazilyAndRetained() {
        OutlineCollectionImpl outlines = new OutlineCollectionImpl(3, null);

        assertNotNull(outlines.getOutline(0));
        assertNotNull(outlines.getOutline(1));
        assertNotNull(outlines.getOutline(2));
        assertSame(outlines.getOutline(1), outlines.getOutline(1));
    }
}
