/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.grouping.core.transform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.transform.sorting.NodeSorter;

class ViewTransformerUserSorterTest {
	@Test
	void directUserSorterIsActiveAndNotifiesTheView() {
		ViewTransformer transformer = new ViewTransformer();
		int[] notifications = { 0 };
		transformer.addViewTransformerListener(event -> notifications[0]++);

		transformer.setUserSorter(new NodeSorter());

		assertFalse(transformer.isNoneSorter());
		assertTrue(notifications[0] > 0);

		transformer.setUserSorter(null);
		assertTrue(transformer.isNoneSorter());
	}
}
