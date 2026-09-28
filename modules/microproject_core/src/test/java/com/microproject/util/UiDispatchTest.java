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
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UiDispatchTest {
	@AfterEach
	void restoreDefaultDispatcher() {
		UiDispatch.setDispatcher(null);
	}

	@Test
	void delegatesSchedulingAndThreadQueriesToInstalledDispatcher() {
		Runnable callback = () -> { };
		Runnable[] scheduled = new Runnable[1];
		UiDispatch.setDispatcher(new UiDispatcher() {
			@Override
			public void invokeLater(Runnable task) {
				scheduled[0] = task;
			}

			@Override
			public boolean isDispatchThread() {
				return true;
			}
		});

		UiDispatch.invokeLater(callback);

		assertSame(callback, scheduled[0]);
		assertTrue(UiDispatch.isDispatchThread());
	}

	@Test
	void defaultDispatcherPreservesCallbackOrder() throws InterruptedException {
		UiDispatch.setDispatcher(null);
		int[] observedOrder = new int[3];
		AtomicInteger nextIndex = new AtomicInteger();
		CountDownLatch completed = new CountDownLatch(3);
		for (int value = 1; value <= 3; value++) {
			int callbackValue = value;
			UiDispatch.invokeLater(() -> {
				observedOrder[nextIndex.getAndIncrement()] = callbackValue;
				completed.countDown();
			});
		}

		assertTrue(completed.await(2, TimeUnit.SECONDS));
		assertArrayEquals(new int[]{1, 2, 3}, observedOrder);
	}
}
