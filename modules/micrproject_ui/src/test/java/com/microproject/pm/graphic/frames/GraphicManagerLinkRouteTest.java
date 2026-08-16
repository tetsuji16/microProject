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
