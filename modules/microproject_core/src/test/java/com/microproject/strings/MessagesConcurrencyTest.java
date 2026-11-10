/*
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
 */
package com.microproject.strings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class MessagesConcurrencyTest {
	private static final int THREADS = 8;

	@Test
	void concurrentLookupsAfterResetReturnSameValues() throws Exception {
		Messages.reset();
		String expectedString = Messages.getString("ResourceType.Work");
		String expectedMeta = Messages.getMetaString("ResourceBundles");
		for (int round = 0; round < 20; round++) {
			Messages.reset();
			ExecutorService pool = Executors.newFixedThreadPool(THREADS);
			try {
				CountDownLatch start = new CountDownLatch(1);
				List<Future<String[]>> futures = new ArrayList<>();
				for (int i = 0; i < THREADS; i++)
					futures.add(pool.submit(() -> {
						start.await();
						return new String[] { Messages.getString("ResourceType.Work"), Messages.getMetaString("ResourceBundles") };
					}));
				start.countDown();
				for (Future<String[]> future : futures) {
					String[] values = future.get(30, TimeUnit.SECONDS);
					assertEquals(expectedString, values[0]);
					assertEquals(expectedMeta, values[1]);
				}
			} finally {
				pool.shutdownNow();
			}
		}
	}
}
