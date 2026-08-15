package com.projectlibre1.graphic.configuration;

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
