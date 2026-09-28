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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Routes callbacks to the application's UI thread when a UI is installed. */
public final class UiDispatch {
	private static final ExecutorService BACKGROUND_EXECUTOR = Executors.newSingleThreadExecutor(
		Thread.ofPlatform().daemon().name("microproject-callback-dispatch-", 0).factory());
	private static final UiDispatcher BACKGROUND_DISPATCHER = new UiDispatcher() {
		@Override
		public void invokeLater(Runnable task) {
			BACKGROUND_EXECUTOR.execute(task);
		}

		@Override
		public boolean isDispatchThread() {
			return false;
		}
	};

	private static volatile UiDispatcher dispatcher = BACKGROUND_DISPATCHER;

	private UiDispatch() {
	}

	public static void setDispatcher(UiDispatcher uiDispatcher) {
		dispatcher = uiDispatcher == null ? BACKGROUND_DISPATCHER : uiDispatcher;
	}

	public static void invokeLater(Runnable task) {
		dispatcher.invokeLater(task);
	}

	public static boolean isDispatchThread() {
		return dispatcher.isDispatchThread();
	}
}
