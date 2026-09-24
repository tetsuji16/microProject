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
package com.microproject.pm.key.uniqueid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.microproject.pm.time.MutableInterval;
import com.microproject.session.Session;

class UniqueIdPoolTest {
	@Test
	void getIdConsumesAndRemovesIntervalsInOrder() throws Exception {
		TestUniqueIdPool pool = new TestUniqueIdPool();
		pool.addInterval(10L, 11L);
		pool.addInterval(20L, 21L);

		assertEquals(10L, pool.getId(null));
		assertEquals("{[11,11],[20,21]}", pool.dump());
		assertEquals(11L, pool.getId(null));
		assertEquals("{[20,21]}", pool.dump());
		assertEquals(20L, pool.getId(null));
		assertEquals(21L, pool.getId(null));
		assertEquals("{}", pool.dump());
	}

	@Test
	void asyncReservationAdmissionAllowsOnlyOneConcurrentRequest() throws Exception {
		TestUniqueIdPool pool = new TestUniqueIdPool();
		int contenders = 16;
		CyclicBarrier start = new CyclicBarrier(contenders);
		ExecutorService executor = Executors.newFixedThreadPool(contenders);
		try {
			List<Future<Boolean>> admissions = new ArrayList<>();
			for (int i = 0; i < contenders; i++)
				admissions.add(executor.submit(() -> {
					start.await();
					return pool.tryStartAsyncReservation();
				}));

			int admitted = 0;
			for (Future<Boolean> admission : admissions)
				if (admission.get()) admitted++;

			assertEquals(1, admitted);
			assertFalse(pool.tryStartAsyncReservation());
			pool.finishAsyncReservation();
			assertTrue(pool.tryStartAsyncReservation());
			pool.finishAsyncReservation();
		} finally {
			executor.shutdownNow();
		}
	}

	private static final class TestUniqueIdPool extends UniqueIdPool {
		private void addInterval(long start, long end) {
			serverIntervals.add(new MutableInterval(start, end));
		}

		@Override
		protected void makeServerReservationAsync(int count, Session session) {
		}
	}
}
