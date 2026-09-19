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
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.microproject.pm.graphic.views.synchro.Synchronizer;
import com.microproject.strings.Messages;

class SplittedViewLifecycleTest {
	@Test
	void reinitializeReplacesBothPanesAndLeavesOneSynchronizer() throws ReflectiveOperationException {
		Synchronizer synchronizer = new Synchronizer();
		TestView view = new TestView(synchronizer);

		view.init();
		view.reinitialize();

		assertEquals(2, view.leftCreated);
		assertEquals(2, view.rightCreated);
		assertEquals(1, synchronizerCount(synchronizer));
		assertEquals(1, view.getPropertyChangeListeners(javax.swing.JSplitPane.DIVIDER_LOCATION_PROPERTY).length);

		view.cleanUp();
		assertEquals(0, synchronizerCount(synchronizer));
	}

	@Test
	void splitViewKeepsBothSidesUsableAndDescribed() {
		TestView view = new TestView(new Synchronizer());
		view.setSize(500, 300);
		view.init();

		view.restoreForTest(900);
		assertTrue(view.getDividerLocation() <= 404);
		view.restoreForTest(-20);
		assertTrue(view.getDividerLocation() >= 96);
		assertEquals(0.5, view.getResizeWeight());
		assertTrue(view.isContinuousLayout());
		assertEquals(Messages.getString("SplitView.accessibleName"),
			view.getAccessibleContext().getAccessibleName());
		assertEquals(Messages.getString("SplitView.leftPaneAccessibleName"),
			view.getLeftScrollPane().getAccessibleContext().getAccessibleName());
		assertEquals(Messages.getString("SplitView.rightPaneAccessibleName"),
			view.getRightScrollPane().getAccessibleContext().getAccessibleName());
	}

	private static int synchronizerCount(Synchronizer synchronizer) throws ReflectiveOperationException {
		Field field = Synchronizer.class.getDeclaredField("scrollPaneSynchronizers");
		field.setAccessible(true);
		return ((List<?>) field.get(synchronizer)).size();
	}

	private static final class TestView extends SplittedView {
		private int leftCreated;
		private int rightCreated;

		private TestView(Synchronizer synchronizer) {
			super(synchronizer);
		}

		@Override
		protected JScrollPane createLeftScrollPane() {
			leftCreated++;
			return new JScrollPane(new JPanel());
		}

		@Override
		protected JScrollPane createRightScrollPane() {
			rightCreated++;
			return new JScrollPane(new JPanel());
		}

		private void restoreForTest(int location) {
			restoreDividerLocation(location);
		}
	}
}
