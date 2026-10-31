/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microproject.document.ObjectEventManager;
import com.microproject.pm.scheduling.ScheduleEventManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The shared deferred-batch state machine, and publication order through both event managers. */
class DeferredEventBatchTest {
	private static DeferredEventBatch<String> batch() {
		return new DeferredEventBatch<>("none", "bad checkpoint");
	}

	@Test
	void nestedRollbackMakesTheOutermostBatchUnpublishableAndResetsState() {
		DeferredEventBatch<String> batch = batch();
		batch.begin();
		batch.begin();
		assertTrue(batch.tryDefer(() -> "a"));
		assertNull(batch.detach(false, Function.identity()));
		assertTrue(batch.tryDefer(() -> "b"));
		DeferredEventBatch.Detached<String> detached = batch.detach(true, Function.identity());
		assertEquals(List.of("a", "b"), detached.events());
		assertFalse(detached.publish());
		assertFalse(batch.tryDefer(() -> "late"));
		assertThrows(IllegalStateException.class, () -> batch.detach(true, Function.identity()));
		batch.begin();
		assertTrue(batch.detach(true, Function.identity()).publish());
	}

	@Test
	void invalidCheckpointIsRejectedAndDiscardTruncates() {
		DeferredEventBatch<String> batch = batch();
		batch.begin();
		batch.tryDefer(() -> "a");
		assertEquals(1, batch.pendingCount());
		assertThrows(IllegalArgumentException.class, () -> batch.discardAfter(2));
		batch.discardAfter(0);
		assertEquals(0, batch.pendingCount());
		assertFalse(batch.anyFrom(0, x -> true));
	}

	/** Both managers publish facts in firing order, and publish nothing after a rollback. */
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void managersPublishInFiringOrderOnlyWhenCommitted(boolean commit) {
		List<String> seen = new ArrayList<>();
		ObjectEventManager objects = new ObjectEventManager();
		objects.addListener(event -> seen.add("o" + ((Integer) event.getObject())));
		ScheduleEventManager schedule = new ScheduleEventManager();
		schedule.addListener(event -> seen.add("s" + event.getObject()));
		objects.beginBatch();
		schedule.beginBatch();
		for (int i = 0; i < 3; i++) {
			objects.fireUpdateEvent(this, i);
			schedule.fire(this, "t", i);
		}
		assertEquals(3, objects.pendingEventCount());
		assertEquals(3, schedule.pendingEventCount());
		assertTrue(seen.isEmpty());
		objects.endBatch(commit);
		schedule.endBatch(commit);
		assertEquals(commit ? List.of("o0", "o1", "o2", "s0", "s1", "s2") : List.of(), seen);
		assertEquals(0, objects.pendingEventCount());
		assertEquals(0, schedule.pendingEventCount());
	}
}
