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
package com.microproject.pm.graphic.views;

import java.util.Iterator;
import java.util.SortedSet;

import com.microproject.configuration.Settings;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.pm.snapshot.DataSnapshot;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;

/** Gantt row sizing derived from the active project and view configuration. */
public final class GanttRowHeight {
	private GanttRowHeight() {
	}

	public static int calculate(Project project, SortedSet<Integer> baselines) {
		for (Iterator<Task> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Task task = tasks.next();
			int current = Snapshottable.CURRENT.intValue();
			for (int baseline = 0; baseline < Settings.numGanttBaselines(); baseline++) {
				if (baseline == current) {
					continue;
				}
				DataSnapshot snapshot = task.getSnapshot(Integer.valueOf(baseline));
				if (snapshot != null) {
					baselines.add(Integer.valueOf(baseline));
				}
			}
		}
		int baselineCount = baselines.isEmpty() ? 0 : baselines.last() + 1;
		GraphicConfiguration configuration = GraphicConfiguration.getInstance();
		return configuration.getRowHeight() + baselineCount * configuration.getBaselineHeight();
	}
}
