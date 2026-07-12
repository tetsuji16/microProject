package com.projectlibre1.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import sun.misc.Unsafe;

class GraphicManagerLinkRouteTest {
	@Test
	void linkAndUnlinkActionsUseActionRouteGuardsAfterTaskSelectionValidation() throws Exception {
		List<String> routedActions = new ArrayList<>();
		TestDocumentFrame documentFrame = allocateWithoutConstructor(TestDocumentFrame.class);
		documentFrame.setTaskSelectionCount(2);
		GraphicManager graphicManager = new GraphicManager(new JPanel()) {
			@Override
			protected boolean beforeActionRoute(String actionId) {
				routedActions.add(actionId);
				return true;
			}

			@Override
			public boolean isDocumentActive() {
				return true;
			}

			@Override
			public DocumentFrame getCurrentFrame() {
				return documentFrame;
			}
		};

		graphicManager.new LinkAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "link"));
		graphicManager.new UnlinkAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "unlink"));

		assertEquals(List.of("link", "unlink"), routedActions);
		assertFalse(routedActions.isEmpty());
		assertFalse(documentFrame.linkInvoked);
		assertFalse(documentFrame.unlinkInvoked);
	}

	private static <T> T allocateWithoutConstructor(Class<T> type) throws Exception {
		Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		Unsafe unsafe = (Unsafe) unsafeField.get(null);
		Object instance = unsafe.allocateInstance(type);
		assertNotNull(instance);
		return type.cast(instance);
	}

	private static final class TestDocumentFrame extends DocumentFrame {
		private int taskSelectionCount;
		private boolean linkInvoked;
		private boolean unlinkInvoked;

		private TestDocumentFrame() {
			super(null, null, "test");
			throw new UnsupportedOperationException("allocated reflectively in tests");
		}

		private void setTaskSelectionCount(int taskSelectionCount) {
			this.taskSelectionCount = taskSelectionCount;
		}

		@Override
		protected boolean hasTaskSelection(boolean excludeReadOnly, int minCount, boolean allowMixedSelection) {
			return taskSelectionCount >= minCount;
		}

		@Override
		public void doLinkTasks() {
			linkInvoked = true;
		}

		@Override
		public void doUnlinkTasks() {
			unlinkInvoked = true;
		}
	}
}
