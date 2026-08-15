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
