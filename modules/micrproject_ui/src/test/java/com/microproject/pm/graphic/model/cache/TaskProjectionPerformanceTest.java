/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.task.ProjectTaskKey;

class TaskProjectionPerformanceTest {
	private static final int TASKS = 10_000;
	private static final int DEPENDENCIES = 20_000;
	private static final int WARMUPS = 5;
	private static final int MEASUREMENTS = 20;
	private static final long ABSOLUTE_P95_CEILING_NANOS = 250_000_000L;

	@Test
	void fixedLargeFixtureHasConstantTimeLookupAndNoReadPathRebuild() {
		List<TaskProjectionSnapshot.Row> rows = new ArrayList<>(TASKS);
		for (int index = 0; index < TASKS; index++) {
			ProjectionRowKey key = new ProjectionRowKey(ProjectionRowKey.Kind.TASK,
					new ProjectTaskKey(1L, index + 1L), index + 1L, 0L);
			rows.add(new TaskProjectionSnapshot.Row(key, 1, false, false, false, false, false,
					true, false, false, false, true, index, index + 1L, index, 0.5d, "Task " + index,
					List.of(new TaskProjectionSnapshot.Interval(index, index + 1L))));
		}
		int[][] dependencies = new int[DEPENDENCIES][2];
		for (int index = 0; index < DEPENDENCIES; index++) {
			dependencies[index][0] = index % TASKS;
			dependencies[index][1] = (index * 31 + 7) % TASKS;
		}
		TaskProjectionSnapshot snapshot = new TaskProjectionSnapshot(1L, 1L, 0L, rows);
		long[] samples = new long[MEASUREMENTS];
		long checksum = 0L;
		for (int run = -WARMUPS; run < MEASUREMENTS; run++) {
			long started = System.nanoTime();
			for (TaskProjectionSnapshot.Row row : rows) checksum += snapshot.rowOf(row.key());
			for (int[] dependency : dependencies)
				checksum += snapshot.rowAt(dependency[0]).end() - snapshot.rowAt(dependency[1]).start();
			long elapsed = System.nanoTime() - started;
			if (run >= 0) samples[run] = elapsed;
		}
		Arrays.sort(samples);
		long p95 = samples[(int)Math.ceil(MEASUREMENTS * 0.95d) - 1];
		assertTrue(checksum != Long.MIN_VALUE);
		assertEquals(TASKS, snapshot.rows().size());
		assertTrue(p95 < ABSOLUTE_P95_CEILING_NANOS,
				"p95=" + p95 + "ns, JDK=" + System.getProperty("java.version")
						+ ", args=" + java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments());
	}
}
