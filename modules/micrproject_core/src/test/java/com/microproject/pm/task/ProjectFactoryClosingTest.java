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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ProjectFactoryClosingTest {
	@Test
	void closeCallbacksRunOnlyAfterTheProjectLeavesTheClosingState() {
		ProjectFactory factory = ProjectFactory.createInstance();
		long projectId = 4242L;
		List<String> events = new ArrayList<>();

		factory.addClosingProject(projectId);
		factory.runAfterProjectClosed(projectId, () -> events.add("first"));
		factory.runAfterProjectClosed(projectId, () -> events.add("second"));

		assertTrue(factory.isProjectClosing(projectId));
		assertTrue(events.isEmpty());

		factory.completeProjectClosing(projectId);

		assertFalse(factory.isProjectClosing(projectId));
		assertEquals(List.of("first", "second"), events);
	}

	@Test
	void closeCallbackRunsImmediatelyWhenNoCloseIsPending() {
		ProjectFactory factory = ProjectFactory.createInstance();
		List<String> events = new ArrayList<>();

		factory.runAfterProjectClosed(5252L, () -> events.add("now"));

		assertEquals(List.of("now"), events);
	}

	@Test
	void completingABranchClearsEveryClosingIdBeforeCallbacksRun() {
		ProjectFactory factory = ProjectFactory.createInstance();
		Set<Long> ids = Set.of(6101L, 6102L);
		List<Boolean> childStillClosing = new ArrayList<>();
		factory.addClosingProjects(ids);
		factory.runAfterProjectClosed(6101L,
				() -> childStillClosing.add(factory.isProjectClosing(6102L)));

		factory.completeProjectClosings(ids);

		assertFalse(factory.isProjectClosing(6101L));
		assertFalse(factory.isProjectClosing(6102L));
		assertEquals(List.of(false), childStillClosing);
	}
}
