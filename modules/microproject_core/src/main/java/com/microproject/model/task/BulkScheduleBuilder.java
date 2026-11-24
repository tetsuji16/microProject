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
package com.microproject.model.task;

import java.util.Objects;

/** Initializes a fully populated project with a single scheduling transaction. */
public final class BulkScheduleBuilder {
	private BulkScheduleBuilder() { }

	/** Suspends schedule recalculation until the bulk update is closed. */
	public static int beginBulkUpdate(Project project) {
		return Objects.requireNonNull(project, "project").fireMultipleTransaction(0, true);
	}

	/** Completes a bulk update and publishes its single final schedule calculation. */
	public static void endBulkUpdate(Project project, int transactionId) {
		Objects.requireNonNull(project, "project").fireMultipleTransaction(transactionId, false);
	}

	/**
	 * Builds the scheduling state for an already populated project. The project
	 * initializer prepares the dependency ordered task list; its initial
	 * calculation is suspended and the outer transaction publishes one complete
	 * forward/backward schedule calculation when it closes.
	 */
	public static void initialize(Project project, boolean subproject, boolean updateDistribution) {
		Objects.requireNonNull(project, "project");
		int transactionId = beginBulkUpdate(project);
		Throwable failure = null;
		try {
			project.initialize(subproject, updateDistribution);
		} catch (RuntimeException | Error current) {
			failure = current;
			throw current;
		} finally {
			try {
				endBulkUpdate(project, transactionId);
			} catch (RuntimeException | Error completionFailure) {
				if (failure != null) failure.addSuppressed(completionFailure);
				else throw completionFailure;
			}
		}
	}
}
