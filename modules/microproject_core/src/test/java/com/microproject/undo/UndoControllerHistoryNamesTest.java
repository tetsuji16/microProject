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

import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.util.Environment;

class UndoControllerHistoryNamesTest {
	@AfterEach
	void resetBatchMode() {
		Environment.setBatchMode(false);
	}

	private static UndoController controllerWithEdits(String... names) {
		UndoController controller = new UndoController();
		for (String name : names) {
			controller.getEditSupport().postEdit(new AbstractUndoableEdit() {
				@Override
				public String getPresentationName() {
					return name;
				}
			});
		}
		return controller;
	}

	@Test
	void undoNamesListTheNextStepFirstAndRedoNamesFollowAnUndo() {
		UndoController controller = controllerWithEdits("A", "B", "C");
		assertEquals(List.of("C", "B", "A"), controller.getUndoNames(10));
		assertEquals(List.of("C", "B"), controller.getUndoNames(2));
		assertEquals(List.of(), controller.getRedoNames(10));

		controller.undo();
		controller.undo();
		assertEquals(List.of("A"), controller.getUndoNames(10));
		assertEquals(List.of("B", "C"), controller.getRedoNames(10));
		assertEquals("A", controller.getUndoName());
		assertEquals("B", controller.getRedoName());
	}
}
