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
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Issue #178: GraphicManager template creation must not throw an uncaught
 * NumberFormatException when a template definition carries a malformed
 * duration token; it degrades to a zero-day (milestone) task.
 */
class GraphicManagerTemplateParseTest {

	@Test
	void parseTemplateDurationDaysAcceptsNumericTokens() {
		assertEquals(2L, GraphicManager.parseTemplateDurationDays("2"));
		assertEquals(10L, GraphicManager.parseTemplateDurationDays("10"));
		assertEquals(0L, GraphicManager.parseTemplateDurationDays("0"));
	}

	@Test
	void parseTemplateDurationDaysDegradesMalformedTokensToZero() {
		assertEquals(0L, GraphicManager.parseTemplateDurationDays("abc"));
		assertEquals(0L, GraphicManager.parseTemplateDurationDays("2.5"));
		assertEquals(0L, GraphicManager.parseTemplateDurationDays(""));
		assertEquals(0L, GraphicManager.parseTemplateDurationDays(null));
	}

	@Test
	void templateTaskDurationDaysGuardsShortOrMissingRows() {
		assertEquals(5L, GraphicManager.templateTaskDurationDays(new String[] {"Task", "5"}));
		assertEquals(0L, GraphicManager.templateTaskDurationDays(new String[] {"Task"}));
		assertEquals(0L, GraphicManager.templateTaskDurationDays(new String[] {}));
		assertEquals(0L, GraphicManager.templateTaskDurationDays(null));
	}
}
