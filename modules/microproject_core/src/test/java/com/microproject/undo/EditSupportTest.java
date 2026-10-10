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
package com.microproject.undo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;

import org.junit.jupiter.api.Test;

class EditSupportTest {
	@Test
	void nestedUpdatesPublishOneCompoundWithUndoAndRedoInSwingOrder() {
		EditSupport support = new EditSupport();
		UndoController controller = new UndoController();
		List<String> changes = new ArrayList<>();
		support.addEditListener(controller::editPosted);
		support.addEditListener(edit -> changes.add("published"));
		support.beginUpdate();
		support.postEdit(edit("first", changes));
		support.beginUpdate();
		support.postEdit(edit("second", changes));
		support.endUpdate();
		assertTrue(changes.isEmpty());
		support.endUpdate();

		assertEquals(List.of("published"), changes);
		controller.undo();
		assertEquals(List.of("published", "undo:second", "undo:first"), changes);
		controller.redo();
		assertEquals(List.of("published", "undo:second", "undo:first", "redo:first", "redo:second"), changes);
	}

	@Test
	void listenersRunInRegistrationOrderAndUndoControllerReceivesBeforeObserver() {
		EditSupport support = new EditSupport();
		UndoController controller = new UndoController();
		List<String> notifications = new ArrayList<>();
		support.addEditListener(controller::editPosted);
		support.addEditListener(edit -> notifications.add(controller.canUndo() ? "history-ready" : "stale-history"));

		support.postEdit(new AbstractUndoableEdit());

		assertEquals(List.of("history-ready"), notifications);
	}

	private static UndoableEdit edit(String name, List<String> changes) {
		return new AbstractUndoableEdit() {
			@Override
			public void undo() {
				super.undo();
				changes.add("undo:" + name);
			}

			@Override
			public void redo() {
				super.redo();
				changes.add("redo:" + name);
			}
		};
	}
}
