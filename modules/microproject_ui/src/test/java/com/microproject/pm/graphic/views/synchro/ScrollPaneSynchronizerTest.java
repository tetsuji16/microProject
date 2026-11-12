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
package com.microproject.pm.graphic.views.synchro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.Field;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;


class ScrollPaneSynchronizerTest {
	@Test
	void eachScrollPaneOwnsOneWheelListenerAndDetachIsBalanced() throws Exception {
		JPanel firstView = new JPanel();
		JPanel firstRowHeader = new JPanel();
		JPanel firstColumnHeader = new JPanel();
		JScrollPane first = new JScrollPane(firstView);
		first.setRowHeaderView(firstRowHeader);
		first.setColumnHeaderView(firstColumnHeader);

		JPanel secondView = new JPanel();
		JPanel secondRowHeader = new JPanel();
		JPanel secondColumnHeader = new JPanel();
		JScrollPane second = new JScrollPane(secondView);
		second.setRowHeaderView(secondRowHeader);
		second.setColumnHeaderView(secondColumnHeader);
		ScrollPaneSynchronizer synchronizer =
				new ScrollPaneSynchronizer(first, second, ScrollPaneSynchronizer.HORIZONTAL);
		List<Component> firstChildren = List.of(firstView, first.getRowHeader(), firstRowHeader,
				first.getColumnHeader(), firstColumnHeader);
		List<Component> secondChildren = List.of(secondView, second.getRowHeader(), secondRowHeader,
				second.getColumnHeader(), secondColumnHeader);

		SwingUtilities.invokeAndWait(() -> {
			synchronizer.activateSynchro();
			MouseWheelListener firstListener = synchronizer.scrollPane1WheelListener;
			MouseWheelListener secondListener = synchronizer.scrollPane2WheelListener;
			assertEquals(1, countListener(first, firstListener));
			assertEquals(1, countListener(second, secondListener));
			assertNoListener(firstChildren, firstListener);
			assertNoListener(secondChildren, secondListener);

			synchronizer.activateSynchro();
			assertEquals(1, countListener(first, firstListener));
			assertEquals(1, countListener(second, secondListener));

			synchronizer.deactivateSynchro();
			assertEquals(0, countListener(first, firstListener));
			assertEquals(0, countListener(second, secondListener));
			assertNoListener(firstChildren, firstListener);
			assertNoListener(secondChildren, secondListener);

			synchronizer.activateSynchro();
			MouseWheelListener reactivatedFirstListener = synchronizer.scrollPane1WheelListener;
			MouseWheelListener reactivatedSecondListener = synchronizer.scrollPane2WheelListener;
			assertEquals(1, countListener(first, reactivatedFirstListener));
			assertEquals(1, countListener(second, reactivatedSecondListener));
			synchronizer.deactivateSynchro();
			assertEquals(0, countListener(first, reactivatedFirstListener));
			assertEquals(0, countListener(second, reactivatedSecondListener));
		});
	}

	private static int countListener(Component component, MouseWheelListener listener) {
		return (int) java.util.Arrays.stream(component.getMouseWheelListeners()).filter(candidate -> candidate == listener).count();
	}

	private static void assertNoListener(List<Component> components, MouseWheelListener listener) {
		for (Component component : components) {
			assertFalse(List.of(component.getMouseWheelListeners()).contains(listener));
		}
	}

	@Test
	void synchronizerDoesNotRegisterTheSamePairTwice() throws ReflectiveOperationException {
		Synchronizer synchronizer = new Synchronizer();
		JScrollPane first = new JScrollPane(new JPanel());
		JScrollPane second = new JScrollPane(new JPanel());

		synchronizer.addSynchro(first, second, ScrollPaneSynchronizer.HORIZONTAL);
		synchronizer.addSynchro(first, second, ScrollPaneSynchronizer.HORIZONTAL);

		Field field = Synchronizer.class.getDeclaredField("scrollPaneSynchronizers");
		field.setAccessible(true);
		assertEquals(1, ((List<?>) field.get(synchronizer)).size());

		synchronizer.removeSynchro(first, second, ScrollPaneSynchronizer.HORIZONTAL);
		assertEquals(0, ((List<?>) field.get(synchronizer)).size());
	}
	@Test
	void resolveViewportLeftEdgeDateUsesCurrentViewportOffset() {
		double leftEdgeDate = ScrollPaneSynchronizer.resolveViewportLeftEdgeDate(x -> 1_000.0d + x * 2.0d, new Point(125, 0));

		assertEquals(1_250.0d, leftEdgeDate, 0.00001d);
	}

	@Test
	void resolveViewportLeftEdgeDateClampsNegativeViewportOffset() {
		double leftEdgeDate = ScrollPaneSynchronizer.resolveViewportLeftEdgeDate(x -> 500.0d + x, new Point(-25, 0));

		assertEquals(500.0d, leftEdgeDate, 0.00001d);
	}

	@Test
	void restoreViewportXRoundsConvertedCoordinate() {
		assertEquals(384, ScrollPaneSynchronizer.restoreViewportX(t -> t / 2.0d, 767.6d));
	}

	@Test
	void clampTargetScaleIndexKeepsInRangeValue() {
		assertEquals(3, ScrollPaneSynchronizer.clampTargetScaleIndex(3, 6));
	}

	@Test
	void clampTargetScaleIndexClampsNegativeValueToZero() {
		assertEquals(0, ScrollPaneSynchronizer.clampTargetScaleIndex(-4, 6));
	}

	@Test
	void clampTargetScaleIndexClampsLargeValueToLastScale() {
		assertEquals(5, ScrollPaneSynchronizer.clampTargetScaleIndex(12, 6));
	}

	@Test
	void horizontalWheelAccumulatesPreciseRotationInsteadOfFixedSteps() {
		// A full notch scrolls one unit step; fractional touchpad rotations keep their remainder.
		assertEquals(40.0d, ScrollPaneSynchronizer.splitWheelPixels(0.0d, 1.0d, 40)[0], 0.0d);
		double[] first = ScrollPaneSynchronizer.splitWheelPixels(0.0d, 0.3d, 10);
		assertEquals(3.0d, first[0], 0.0d);
		double[] slow = ScrollPaneSynchronizer.splitWheelPixels(0.0d, 0.04d, 10);
		assertEquals(0.0d, slow[0], 0.0d);
		double[] carried = ScrollPaneSynchronizer.splitWheelPixels(slow[1], 0.04d, 10);
		assertEquals(0.0d, carried[0], 0.0d);
		double[] reached = ScrollPaneSynchronizer.splitWheelPixels(carried[1] + 0.2d, 0.04d, 10);
		assertEquals(1.0d, reached[0], 0.0d);
		assertEquals(-25.0d, ScrollPaneSynchronizer.splitWheelPixels(0.0d, -2.5d, 10)[0], 0.0d);
	}
}
