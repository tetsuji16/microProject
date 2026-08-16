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
package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.model.NodeModel;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.pm.task.Project;

class SpreadSheetColumnUndoLifecycleTest {
	@Test
	void columnUndoEditDoesNotRetainTheSwingSpreadsheet() throws Exception {
		Class<?> editType = Class.forName(SpreadSheet.class.getName() + "$ColumnRemovalEdit");

		assertTrue(Modifier.isStatic(editType.getModifiers()));
		assertFalse(Arrays.stream(editType.getDeclaredFields())
				.anyMatch(field -> SpreadSheet.class.isAssignableFrom(field.getType())));
		assertFalse(Arrays.stream(editType.getDeclaredFields())
				.anyMatch(field -> field.getName().startsWith("this$")));
	}

	@Test
	void replacementSpreadsheetBecomesTheUndoTarget() throws Exception {
		NodeModel model = (NodeModel) Proxy.newProxyInstance(
				NodeModel.class.getClassLoader(), new Class<?>[] { NodeModel.class },
				(proxy, method, args) -> defaultValue(method.getReturnType()));
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet oldSheet = new SpreadSheet();
			SpreadSheet replacement = new SpreadSheet();

			SpreadSheet.registerLayoutTarget(model, "task", oldSheet);
			SpreadSheet.registerLayoutTarget(model, "task", replacement);
			SpreadSheet.unregisterLayoutTarget(model, "task", oldSheet);

			assertTrue(SpreadSheet.findLayoutTarget(model, "task") == replacement);

			SpreadSheet.unregisterLayoutTarget(model, "task", replacement);
			assertTrue(SpreadSheet.findLayoutTarget(model, "task") == null);
		});
	}

	@Test
	void unregisteringTheLatestSpreadsheetRestoresThePreviousLiveTarget() throws Exception {
		NodeModel model = proxyNodeModel(null);
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet original = new SpreadSheet();
			SpreadSheet replacement = new SpreadSheet();

			SpreadSheet.registerLayoutTarget(model, "task", original);
			SpreadSheet.registerLayoutTarget(model, "task", replacement);
			assertSame(replacement, SpreadSheet.findLayoutTarget(model, "task"));

			SpreadSheet.unregisterLayoutTarget(model, "task", replacement);
			assertSame(original, SpreadSheet.findLayoutTarget(model, "task"));

			SpreadSheet.unregisterLayoutTarget(model, "task", original);
			assertNull(SpreadSheet.findLayoutTarget(model, "task"));
		});
	}

	@Test
	void undoPrefersItsOriginatingSpreadsheetWhileItRemainsRegistered() throws Exception {
		NodeModel model = proxyNodeModel(null);
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet original = new SpreadSheet();
			SpreadSheet otherView = new SpreadSheet();

			SpreadSheet.registerLayoutTarget(model, "task", original);
			SpreadSheet.registerLayoutTarget(model, "task", otherView);

			assertSame(original, SpreadSheet.findLayoutTarget(model, "task", original));

			SpreadSheet.unregisterLayoutTarget(model, "task", otherView);
			SpreadSheet.unregisterLayoutTarget(model, "task", original);
		});
	}

	@Test
	void onlyTaskSpreadsheetLayoutIsPersistedOnTheProject() {
		Project project = Project.getDummy();
		NodeModel model = proxyNodeModel(project);

		assertSame(project, SpreadSheet.projectLayoutOwner(model, SpreadSheetCategories.taskSpreadsheetCategory));
		assertNull(SpreadSheet.projectLayoutOwner(model, SpreadSheetCategories.taskAssignmentSpreadsheetCategory));
		assertNull(SpreadSheet.projectLayoutOwner(model, SpreadSheetCategories.resourceSpreadsheetCategory));
	}

	private static NodeModel proxyNodeModel(Project project) {
		return (NodeModel) Proxy.newProxyInstance(
				NodeModel.class.getClassLoader(), new Class<?>[] { NodeModel.class },
				(proxy, method, args) -> "getDataFactory".equals(method.getName())
						? project
						: defaultValue(method.getReturnType()));
	}

	private static Object defaultValue(Class<?> type) {
		if (!type.isPrimitive())
			return null;
		if (type == boolean.class)
			return false;
		if (type == char.class)
			return '\0';
		return 0;
	}
}
