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
package com.microproject.pm.assignment;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.microproject.pm.task.Task;

/** UI notification port for invalid task/resource calendar intersections. */
public final class AssignmentCalendarNotifications {
	private static final Consumer<Task> NO_OP = ignored -> { };
	private static final AtomicReference<Consumer<Task>> HANDLER = new AtomicReference<>(NO_OP);

	private AssignmentCalendarNotifications() {
	}

	/** Installs the application presentation handler; pass {@code null} to clear it. */
	public static void setHandler(Consumer<Task> handler) {
		HANDLER.set(handler == null ? NO_OP : handler);
	}

	/** Delivers the notification when a UI handler has been installed. */
	public static void notifyInvalidIntersection(Task task) {
		HANDLER.get().accept(Objects.requireNonNull(task));
	}
}
