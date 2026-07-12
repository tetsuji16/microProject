package com.projectlibre1.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.junit.jupiter.api.Test;

import com.projectlibre1.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.projectlibre1.pm.graphic.views.synchro.Synchronizer;

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

		view.cleanUp();
		assertEquals(0, synchronizerCount(synchronizer));
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
	}
}
