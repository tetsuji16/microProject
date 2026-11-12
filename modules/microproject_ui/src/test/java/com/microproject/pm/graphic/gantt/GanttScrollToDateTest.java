/*
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
 */
package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Scroll-to-date positioning for the "Scroll to Today" command (issue #1092). */
class GanttScrollToDateTest {

	@Test
	void centresTheDateInTheVisibleArea() {
		assertEquals(1600, Gantt.centredViewX(2000, 800, 10000));
	}

	@Test
	void clampsToTheStartWhenTheDateIsNearOrBeforeTheFirstInstant() {
		assertEquals(0, Gantt.centredViewX(100, 800, 10000));
		assertEquals(0, Gantt.centredViewX(-5000, 800, 10000));
	}

	@Test
	void clampsToTheEndWhenTheDateIsBeyondTheScrollableRange() {
		assertEquals(9200, Gantt.centredViewX(9900, 800, 10000));
		assertEquals(9200, Gantt.centredViewX(50000, 800, 10000));
	}

	@Test
	void staysAtZeroWhenTheChartIsNarrowerThanTheViewport() {
		assertEquals(0, Gantt.centredViewX(300, 800, 500));
	}

	@Test
	void scrollToTaskPutsTheStartAtTheLeftEdgeWithTheFixedMarginAndClampsToTheScrollableRange() {
		assertEquals(950, Gantt.scrollToTaskViewX(1000, 800, 10000));
		assertEquals(0, Gantt.scrollToTaskViewX(100, 800, 10000), "a task near the beginning sits flush left");
		assertEquals(9200, Gantt.scrollToTaskViewX(9990, 800, 10000), "never scroll past the last scrollable position");
		assertEquals(0, Gantt.scrollToTaskViewX(900, 800, 500), "a chart narrower than the viewport stays at 0");
	}
}
