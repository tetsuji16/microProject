package com.microproject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import com.microproject.field.FieldContext;
import com.microproject.pm.time.Interval;

/**
 * Issue #158: Field.getLabel() previously used a shared static SimpleDateFormat,
 * which is not thread-safe. Verify concurrent calls all produce the same
 * day-of-week label for a fixed date.
 */
class FieldLabelConcurrencyTest {

	@Test
	void getLabelIsStableUnderConcurrentCalls() throws Exception {
		final long fixedDate = 1700000000000L; // a fixed instant
		FieldContext context = new FieldContext();
		context.setInterval(new Interval(fixedDate, fixedDate) {
			@Override
			public long getStart() {
				return fixedDate;
			}

			@Override
			public long getEnd() {
				return fixedDate;
			}
		});

		com.microproject.field.Field field = new com.microproject.field.Field();
		Field contextField = com.microproject.field.Field.class.getDeclaredField("specialFieldContext");
		contextField.setAccessible(true);
		contextField.set(field, context);

		final String reference = field.getLabel();

		int threads = 8;
		int iterations = 200;
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		List<String> results = Collections.synchronizedList(new ArrayList<>());
		for (int t = 0; t < threads; t++) {
			new Thread(() -> {
				try {
					start.await();
					for (int i = 0; i < iterations; i++) {
						results.add(field.getLabel());
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			}).start();
		}
		start.countDown();
		done.await();

		assertEquals(threads * iterations, results.size());
		for (String label : results) {
			assertEquals(reference, label);
		}
	}
}
