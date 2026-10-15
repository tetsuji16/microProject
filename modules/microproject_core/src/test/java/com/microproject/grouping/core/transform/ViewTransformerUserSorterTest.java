/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.grouping.core.transform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.transform.sorting.NodeSorter;
import com.microproject.grouping.core.transform.sorting.TaskSortSpec;
import java.util.List;

class ViewTransformerUserSorterTest {
	@Test
	void directionFollowsTheSelectedViewSorterAndResetsWhenCleared() {
		ViewTransformer transformer = new ViewTransformer();
		int[] notifications = { 0 };
		transformer.addViewTransformerListener(event -> notifications[0]++);

		transformer.setUserSorter("Sorter.StartDate", false);
		assertFalse(transformer.isUserSorterAscending());
		int changedNotifications = notifications[0];
		assertEquals(1, changedNotifications, "sorter and direction are applied in one transformer update");
		transformer.setUserSorterAscending(false);
		assertEquals(changedNotifications, notifications[0], "reapplying the same direction is a no-op");

		transformer.setUserSorterId("Sorter.FinishDate");
		assertFalse(transformer.isUserSorterAscending(), "changing sorter fields preserves the selected direction");
		transformer.setUserSorterId(ViewTransformer.SORTER_NONE_ID);
		assertTrue(transformer.isUserSorterAscending(), "clearing the view sorter resets direction to its default");
		transformer.setUserSorterAscending(false);
		assertTrue(transformer.isUserSorterAscending(), "the None sorter cannot retain descending direction");
	}

	@Test
	void directUserSorterIsActiveAndNotifiesTheView() {
		ViewTransformer transformer = new ViewTransformer();
		int[] notifications = { 0 };
		transformer.addViewTransformerListener(event -> notifications[0]++);

		transformer.setUserSorter("Sorter.StartDate", false);
		assertFalse(transformer.isUserSorterAscending());
		int changedNotifications = notifications[0];
		transformer.setUserSorter(new NodeSorter());

		assertFalse(transformer.isNoneSorter());
		assertTrue(transformer.isUserSorterAscending(), "a direct sorter starts with default ascending direction");
		assertTrue(notifications[0] > 0);
		assertTrue(notifications[0] > changedNotifications);

		transformer.setUserSorter(null);
		assertTrue(transformer.isNoneSorter());
	}

	@Test
	void clearingLegacySorterClearsEveryCanonicalKeyInOneNotification() {
		ViewTransformer transformer = new ViewTransformer();
		transformer.setTaskSortSpec(new TaskSortSpec(List.of(
				new TaskSortSpec.Key("Sorter.StartDate", false),
				new TaskSortSpec.Key("Sorter.Name", true)), false));
		int[] notifications = { 0 };
		transformer.addViewTransformerListener(event -> notifications[0]++);

		transformer.setUserSorterId(ViewTransformer.SORTER_NONE_ID);

		assertTrue(transformer.isNoneSorter());
		assertTrue(transformer.getTaskSortSpec().keys().isEmpty());
		assertFalse(transformer.getTaskSortSpec().keepOutline());
		assertEquals(ViewTransformer.SORTER_NONE_ID, transformer.getUserSorterId());
		assertEquals(1, notifications[0], "clearing legacy sort state publishes one canonical update");
	}
}
