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
package com.microproject.graphic.configuration;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class BarStylesTest {
	@Test
	void evaluatesOnlyStylesInTheRequestedRenderingCategory() {
		BarStyles styles = new BarStyles();
		AtomicInteger normalEvaluations = new AtomicInteger();
		AtomicInteger annotationEvaluations = new AtomicInteger();
		for (int i = 0; i < 20; i++)
			styles.rows.add(countingStyle(normalEvaluations, false));
		for (int i = 0; i < 5; i++)
			styles.rows.add(countingStyle(annotationEvaluations, true));

		AtomicInteger matches = new AtomicInteger();
		styles.apply(new Object(), countingClosure(matches), false, true, false, false);

		assertEquals(0, normalEvaluations.get());
		assertEquals(5, annotationEvaluations.get());
		assertEquals(5, matches.get());
	}

	@Test
	void changingAStyleCategoryInvalidatesTheIndex() {
		BarStyles styles = new BarStyles();
		BarStyle style = countingStyle(new AtomicInteger(), false);
		style.setBelongsTo(styles);
		styles.rows.add(style);

		AtomicInteger matches = new AtomicInteger();
		styles.apply(new Object(), countingClosure(matches), false, false, false, false);
		style.setAnnotation(true);
		styles.apply(new Object(), countingClosure(matches), false, false, false, false);
		styles.apply(new Object(), countingClosure(matches), false, true, false, false);

		assertEquals(2, matches.get());
	}

	@Test
	void mutatingTheExposedStyleListInvalidatesTheIndex() {
		BarStyles styles = new BarStyles();
		BarStyle first = countingStyle(new AtomicInteger(), false);
		BarStyle second = countingStyle(new AtomicInteger(), false);
		styles.getRows().add(first);

		AtomicInteger matches = new AtomicInteger();
		styles.apply(new Object(), countingClosure(matches), false, false, false, false);
		styles.getRows().add(second);
		styles.apply(new Object(), countingClosure(matches), false, false, false, false);
		styles.getRows().remove(first);
		styles.apply(new Object(), countingClosure(matches), false, false, false, false);

		assertEquals(4, matches.get());
	}

	private static BarStyle countingStyle(AtomicInteger evaluations, boolean annotation) {
		BarStyle style = new BarStyle() {
			@Override
			public boolean evaluate(Object object) {
				evaluations.incrementAndGet();
				return true;
			}
		};
		style.setAnnotation(annotation);
		return style;
	}

	private static Consumer<Object> countingClosure(AtomicInteger matches) {
		return ignored -> matches.incrementAndGet();
	}
}
