/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class RibbonCustomizationDraftTest {
	private static RibbonCustomization sparse() {
		return new RibbonCustomization(List.of("ResourceRibbonTask", "TaskRibbonTask"), Set.of(), List.of(), Map.of());
	}

	@Test
	void untouchedDraftOfASparseOrderEqualsItsSource() {
		RibbonCustomization source = sparse();
		RibbonCustomizationDraft draft = new RibbonCustomizationDraft(source);
		assertEquals(source, draft.toCustomization(), "an untouched draft must not count as a change");
		assertEquals("ResourceRibbonTask", draft.defaultTabOrder().get(0), "display still uses the resolved order");
		assertTrue(draft.defaultTabOrder().size() > 2);
	}

	@Test
	void reorderingDefaultTabsWritesTheResolvedOrder() {
		RibbonCustomizationDraft draft = new RibbonCustomizationDraft(sparse());
		assertTrue(draft.moveTab("ResourceRibbonTask", 2));
		assertNotEquals(sparse(), draft.toCustomization());
		assertEquals(draft.defaultTabOrder(), draft.toCustomization().defaultTabOrder());
	}
}
