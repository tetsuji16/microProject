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
package com.microproject.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.EventQueue;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class SwingUiDispatcherTest {
	@Test
	void runsInlineWhenAlreadyOnTheEventQueue() throws Exception {
		AtomicReference<Object> result = new AtomicReference<>();
		EventQueue.invokeAndWait(() -> result.set(SwingUiDispatcher.callAndWait(EventQueue::isDispatchThread)));
		assertEquals(Boolean.TRUE, result.get());
	}

	@Test
	void runsOnTheEventQueueWhenCalledFromAnotherThread() {
		assertFalse(EventQueue.isDispatchThread());
		assertTrue(SwingUiDispatcher.callAndWait(EventQueue::isDispatchThread));
		AtomicReference<Boolean> ran = new AtomicReference<>(false);
		SwingUiDispatcher.runAndWait(() -> ran.set(EventQueue.isDispatchThread()));
		assertTrue(ran.get());
	}

	@Test
	void rethrowsRuntimeExceptionsUnchangedOnAndOffTheEventQueue() throws Exception {
		IllegalArgumentException failure = new IllegalArgumentException("boom");
		assertSame(failure, assertThrows(IllegalArgumentException.class, () -> SwingUiDispatcher.runAndWait(() -> {
			throw failure;
		})));
		AtomicReference<Throwable> onEdt = new AtomicReference<>();
		EventQueue.invokeAndWait(() -> onEdt.set(assertThrows(IllegalArgumentException.class,
			() -> SwingUiDispatcher.runAndWait(() -> {
				throw failure;
			}))));
		assertSame(failure, onEdt.get());
	}

	@Test
	void rethrowsErrorsUnchanged() {
		AssertionError failure = new AssertionError("fatal");
		assertSame(failure, assertThrows(AssertionError.class, () -> SwingUiDispatcher.callAndWait(() -> {
			throw failure;
		})));
	}

	@Test
	void wrapsCheckedExceptions() {
		IOException failure = new IOException("io");
		IllegalStateException wrapped = assertThrows(IllegalStateException.class,
			() -> SwingUiDispatcher.callAndWait(() -> {
				throw failure;
			}));
		assertSame(failure, wrapped.getCause());
	}

	@Test
	void restoresTheInterruptFlagWhenInterrupted() {
		CountDownLatch release = new CountDownLatch(1);
		// Keep the EDT busy so the queued task cannot finish before the wait starts.
		EventQueue.invokeLater(() -> {
			try {
				release.await(10, TimeUnit.SECONDS);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		});
		Thread.currentThread().interrupt();
		try {
			IllegalStateException wrapped = assertThrows(IllegalStateException.class,
				() -> SwingUiDispatcher.runAndWait(() -> { }));
			assertInstanceOf(InterruptedException.class, wrapped.getCause());
			assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
			release.countDown();
		}
	}
}
