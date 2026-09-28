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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class ProjectListenerRegistryTest {
	@Test
	void projectListenersKeepRegistrationOrderAndTypedLookupAfterRemoval() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("listener-test", undo), undo);
		List<String> calls = new ArrayList<>();
		ProjectListener first = listener("first", calls);
		ProjectListener second = listener("second", calls);
		project.addProjectListener(first);
		project.addProjectListener(second);

		project.setName("first change");

		assertEquals(List.of("first", "second"), calls);
		assertArrayEquals(new ProjectListener[] { second, first }, project.getProjectListeners());
		EventListener[] projectListeners = project.getProjectListeners(ProjectListener.class);
		assertEquals(2, projectListeners.length);
		assertSame(second, projectListeners[0]);
		assertSame(first, projectListeners[1]);
		assertEquals(0, project.getProjectListeners(EventListener.class).length,
			"listener type lookup must retain EventListenerList's exact-type semantics");

		project.removeProjectListener(first);
		calls.clear();
		project.setName("second change");
		assertEquals(List.of("second"), calls);
	}

	private static ProjectListener listener(String name, List<String> calls) {
		return new ProjectListener() {
			@Override
			public void nameChanged(ProjectEvent event) {
				calls.add(name);
			}

			@Override
			public void groupDirtyChanged(ProjectEvent event) {
			}
		};
	}
}
