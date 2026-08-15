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
