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
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.microproject.transaction.DomainChangeJournal;

class TaskViewUpdateCoordinatorTest {
	@Test
	void installsLatestSnapshotBeforeAllNotificationsInOneRunnable() {
		List<Runnable> edt = new ArrayList<>();
		List<String> order = new ArrayList<>();
		AtomicLong revision = new AtomicLong(2L);
		TaskViewUpdateCoordinator coordinator = new TaskViewUpdateCoordinator(revision::get,
				value -> order.add("install:" + value), edt::add);

		coordinator.afterInstall(() -> order.add("table"));
		coordinator.requestRevision(3L);
		revision.set(4L);
		coordinator.afterInstall(() -> order.add("gantt"));

		assertEquals(1, edt.size());
		edt.remove(0).run();
		assertEquals(List.of("install:4", "table", "gantt"), order);
	}

	@Test
	void legacyEventsCoalesceAndSuppressedOrClosedEventsDisappear() throws Exception {
		DomainChangeJournal journal = new DomainChangeJournal();
		List<Runnable> edt = new ArrayList<>();
		LegacyChangeAccumulator accumulator = new LegacyChangeAccumulator(journal, edt::add);

		accumulator.record();
		accumulator.record();
		assertEquals(1, edt.size());
		edt.remove(0).run();
		assertEquals(1L, journal.revision());
		try (DomainChangeJournal.Scope ignored = journal.suppressLegacyEvents()) {
			accumulator.record();
		}
		assertTrue(edt.isEmpty());
		accumulator.record();
		accumulator.close();
		edt.remove(0).run();
		assertEquals(2L, journal.revision(), "closing one view must not cancel the project-wide batch");
	}

	@Test
	void offEdtMutationAdvancesRevisionBeforeItsSwingPublicationRuns() {
		DomainChangeJournal journal = new DomainChangeJournal();
		List<Runnable> edt = new ArrayList<>();
		LegacyChangeAccumulator accumulator = new LegacyChangeAccumulator(journal, edt::add);

		accumulator.recordImmediately(new Object());

		assertEquals(1L, journal.revision(), "stale gestures must see the mutation before EDT catches up");
		assertTrue(edt.isEmpty(), "the journal listener owns UI dispatch; the accumulator must not mutate Swing");
	}

	@Test
	void severalViewAccumulatorsPublishOneProjectRevision() {
		DomainChangeJournal journal = new DomainChangeJournal();
		List<Runnable> edt = new ArrayList<>();
		LegacyChangeAccumulator first = new LegacyChangeAccumulator(journal, edt::add);
		LegacyChangeAccumulator second = new LegacyChangeAccumulator(journal, edt::add);

		first.record();
		second.record();

		assertEquals(1, edt.size());
		edt.remove(0).run();
		assertEquals(1L, journal.revision());
	}

	@Test
	void sameOffEdtEventDeliveredToSeveralViewsIsRecordedOnce() {
		DomainChangeJournal journal = new DomainChangeJournal();
		LegacyChangeAccumulator first = new LegacyChangeAccumulator(journal, Runnable::run);
		LegacyChangeAccumulator second = new LegacyChangeAccumulator(journal, Runnable::run);
		Object event = new Object();

		first.recordImmediately(event);
		second.recordImmediately(event);

		assertEquals(1L, journal.revision());
	}
}
