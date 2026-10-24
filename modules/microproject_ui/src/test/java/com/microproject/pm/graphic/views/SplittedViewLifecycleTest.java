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
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

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

	@Test
	void restoredDividerChoiceLocksBothMainViewChildrenAgainstResponsiveDefaults() throws Exception {
		TestView[] views = new TestView[2];
		int[] restoredLocation = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			MainView mainView = new MainView();
			mainView.setSize(1000, 600);
			views[0] = new TestView(new Synchronizer(), 180);
			views[1] = new TestView(new Synchronizer());
			views[0].init();
			views[1].init();
			views[0].setSize(1000, 260);
			views[1].setSize(1000, 260);
			views[0].setParentView(mainView);
			views[1].setParentView(mainView);
			mainView.setTopComponent(views[0]);
			mainView.setBottomComponent(views[1]);
			views[1].restoreForTest(360);
			restoredLocation[0] = views[0].getDividerLocation();
			assertTrue(views[0].isDividerLocationManuallyAdjusted());
			assertTrue(views[1].isDividerLocationManuallyAdjusted());
		});
		SwingUtilities.invokeAndWait(views[0]::requestResponsiveForTest);
		assertEquals(restoredLocation[0], views[0].getDividerLocation(),
			"the responsive sibling must preserve the divider restored through its peer");
	}

	@Test
	void responsiveClampAccountsForSplitInsetsAndRightPaneChrome() throws Exception {
		TestView[] view = new TestView[1];
		SwingUtilities.invokeAndWait(() -> {
			view[0] = new TestView(new Synchronizer(), 420, 180);
			view[0].setBorder(new EmptyBorder(3, 5, 4, 7));
			view[0].setSize(400, 260);
			view[0].init();
			view[0].getRightScrollPane().setBorder(new EmptyBorder(0, 2, 0, 3));
			view[0].doLayout();
			view[0].requestResponsiveForTest();
		});
		SwingUtilities.invokeAndWait(() -> view[0].doLayout());
		SwingUtilities.invokeAndWait(() -> {
			int rightPaneWidth = view[0].getRightScrollPane().getWidth();
			assertTrue(rightPaneWidth >= 180,
				"the responsive clamp must reserve the complete right-pane minimum including scrollpane chrome");
			assertTrue(view[0].getDividerLocation() <= 400 - 5 - 7 - view[0].getDividerSize() - 180 + 5,
				"the divider coordinate must include the split's left inset and reserve the right inset");
			view[0].cleanUp();
		});
	}

	@Test
	void reinitializeRequestsValidationAtOutermostAttachedSplitRoot() throws Exception {
		CountingRootPane[] rootPane = new CountingRootPane[1];
		TestView[] view = new TestView[1];
		SwingUtilities.invokeAndWait(() -> {
			rootPane[0] = new CountingRootPane();
			MainView mainView = new MainView();
			view[0] = new TestView(new Synchronizer());
			view[0].init();
			mainView.setTop(view[0]);
			rootPane[0].setContentPane(mainView);
			rootPane[0].revalidateCalls = 0;
			view[0].reinitialize();
		});
		assertTrue(rootPane[0].revalidateCalls > 0,
			"rebuilding an inner split must request validation from the outermost attached Swing validate root");
	}

	private static int synchronizerCount(Synchronizer synchronizer) throws ReflectiveOperationException {
		Field field = Synchronizer.class.getDeclaredField("scrollPaneSynchronizers");
		field.setAccessible(true);
		return ((List<?>) field.get(synchronizer)).size();
	}

	private static final class TestView extends SplittedView {
		private int leftCreated;
		private int rightCreated;
		private final int preferredLeftWidth;
		private final int minimumRightWidth;

		private TestView(Synchronizer synchronizer) {
			this(synchronizer, -1, 96);
		}

		private TestView(Synchronizer synchronizer, int preferredLeftWidth) {
			this(synchronizer, preferredLeftWidth, 96);
		}

		private TestView(Synchronizer synchronizer, int preferredLeftWidth, int minimumRightWidth) {
			super(synchronizer);
			this.preferredLeftWidth = preferredLeftWidth;
			this.minimumRightWidth = minimumRightWidth;
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

		@Override
		protected int preferredLeftPaneWidth() {
			return preferredLeftWidth;
		}

		@Override
		protected int minimumResponsiveRightPaneWidth() {
			return minimumRightWidth;
		}

		private void requestResponsiveForTest() {
			requestResponsiveDividerLayout();
		}
	}

	private static final class CountingRootPane extends JRootPane {
		private int revalidateCalls;

		@Override
		public void revalidate() {
			revalidateCalls++;
			super.revalidate();
		}
	}
}
