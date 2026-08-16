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
