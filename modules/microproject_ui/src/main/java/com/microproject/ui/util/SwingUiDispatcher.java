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

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import com.microproject.util.UiDispatcher;

/** Dispatches callbacks through Swing's event dispatch thread. */
public final class SwingUiDispatcher implements UiDispatcher {
	@Override
	public void invokeLater(Runnable task) {
		SwingUtilities.invokeLater(task);
	}

	@Override
	public boolean isDispatchThread() {
		return SwingUtilities.isEventDispatchThread();
	}

	/** Runs {@code task} on the EDT and waits; see {@link #callAndWait(Callable)}. */
	public static void runAndWait(Runnable task) {
		Objects.requireNonNull(task, "task");
		callAndWait(() -> {
			task.run();
			return null;
		});
	}

	/**
	 * Runs {@code task} on the EDT and returns its result. When already on the
	 * EDT the task runs inline. Failures are converted once: runtime exceptions
	 * and errors thrown by the task propagate unchanged, checked exceptions are
	 * wrapped in {@link IllegalStateException}, and interruption restores the
	 * interrupt flag before an {@link IllegalStateException} is thrown.
	 */
	public static <T> T callAndWait(Callable<T> task) {
		Objects.requireNonNull(task, "task");
		if (SwingUtilities.isEventDispatchThread()) {
			try {
				return task.call();
			} catch (Exception exception) {
				throw convert(exception);
			}
		}
		final AtomicReference<T> result = new AtomicReference<>();
		final AtomicReference<Throwable> failure = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				try {
					result.set(task.call());
				} catch (Throwable exception) {
					failure.set(exception);
				}
			});
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for the UI thread", interrupted);
		} catch (InvocationTargetException invocationFailure) {
			throw convert(invocationFailure.getCause());
		}
		if (failure.get() != null)
			throw convert(failure.get());
		return result.get();
	}

	private static RuntimeException convert(Throwable cause) {
		if (cause instanceof RuntimeException runtimeException)
			return runtimeException;
		if (cause instanceof Error error)
			throw error;
		return new IllegalStateException("UI task failed", cause);
	}
}
