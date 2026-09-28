/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.job;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class JobQueueListenerTest {
	@Test
	void dispatchesInReverseRegistrationOrderFromStableSnapshot() {
		JobQueue queue = new JobQueue("listener-test", false);
		List<String> calls = new ArrayList<>();
		List<JobQueueEvent> events = new ArrayList<>();
		JobQueueListener first = event -> {
			calls.add("first");
			events.add(event);
		};
		JobQueueListener second = event -> {
			calls.add("second");
			events.add(event);
			queue.removeListener(first);
		};
		queue.addListener(first);
		queue.addListener(second);

		assertArrayEquals(new JobQueueListener[] { second, first }, queue.getListeners());
		assertArrayEquals(new JobQueueListener[] { second, first },
			queue.getListeners(JobQueueListener.class));

		queue.fireProgressChanged(queue, 0.5f);
		queue.fireProgressChanged(queue, 0.75f);

		assertEquals(List.of("second", "first", "second"), calls);
		assertSame(events.get(0), events.get(1));
		assertNotSame(events.get(1), events.get(2));
		assertArrayEquals(new JobQueueListener[] { second }, queue.getListeners());
	}
}
