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
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.configuration.Dictionary;
import com.microproject.menu.MenuActionConstants;
import com.microproject.options.GeneralOption;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetPopupMenu;
import com.microproject.pm.graphic.views.ResourceView;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Issue #997: the row popup fix exposed the menus of every non-task table. Those
 * menus must offer the commands of their own table without task-only commands,
 * each item must be live and uniquely labelled, and the command must act on the
 * clicked row.
 */
class NonTaskRowPopupCommandsTest {
	private boolean confirmDeletes;

	@AfterEach
	void restoreOptions() {
		GeneralOption.getInstance().setConfirmDeletes(confirmDeletes);
	}

	/** The resource sheet popup names the table's commands once and adds no task-only command. */
	@Test
	void resourceSheetPopupOffersItsOwnCommandsOnce() throws Exception {
		confirmDeletes = GeneralOption.getInstance().isConfirmDeletes();
		GeneralOption.getInstance().setConfirmDeletes(false);
		ResourceFixture fixture = ResourceFixture.create();
		try {
			SwingUtilities.invokeAndWait(() -> {
				List<JMenuItem> commands = commands(fixture.sheet.getPopup());
				List<String> names = names(commands);
				assertTrue(names.contains("popup." + MenuActionConstants.ACTION_DELETE),
						() -> "Delete missing: " + names);
				assertTrue(names.contains("popup." + MenuActionConstants.ACTION_CUT), () -> "Cut missing: " + names);
				assertTrue(names.contains("popup." + MenuActionConstants.ACTION_COPY), () -> "Copy missing: " + names);
				assertTrue(names.contains("popup." + MenuActionConstants.ACTION_PASTE),
						() -> "Paste missing: " + names);
				assertTrue(names.contains("popup." + MenuActionConstants.ACTION_INSERT_RESOURCE),
						() -> "Insert Resource missing: " + names);
				assertFalse(names.contains("popup." + MenuActionConstants.ACTION_INSERT_TASK),
						() -> "Insert Task belongs to the task table: " + names);
				assertFalse(names.contains("popup." + MenuActionConstants.ACTION_PASTE_INSERT),
						() -> "Paste Insert is an MSP task command: " + names);

				Set<String> labels = new HashSet<>();
				for (JMenuItem command : commands) {
					assertNotNull(command.getAction(),
							() -> "popup item must have a live action: " + command.getName());
					assertFalse(command.getText() == null || command.getText().isBlank(),
							() -> "popup item must have a label: " + command.getName());
					assertTrue(labels.add(command.getText()),
							() -> "duplicate popup label: " + command.getText());
				}
			});
		} finally {
			fixture.release();
		}
	}

	/** Delete from the resource sheet popup removes the selected row and its resource. */
	@Test
	void resourceSheetPopupDeleteRemovesTheSelectedResource() throws Exception {
		confirmDeletes = GeneralOption.getInstance().isConfirmDeletes();
		GeneralOption.getInstance().setConfirmDeletes(false);
		ResourceFixture fixture = ResourceFixture.create();
		try {
			SwingUtilities.invokeAndWait(() -> {
				assertEquals(2, fixture.pool.getResourceList().size());
				fixture.sheet.setRowSelectionInterval(0, 0);
				JMenuItem delete = command(fixture.sheet.getPopup(), MenuActionConstants.ACTION_DELETE);
				assertNotNull(delete, "the resource popup must offer Delete");
				delete.doClick();
				assertEquals(1, fixture.pool.getResourceList().size(),
						"popup Delete must remove the selected resource");
			});
		} finally {
			fixture.release();
		}
	}

	/** The dependency table offers its Delete command only, and it removes the link. */
	@Test
	void dependencyTablePopupDeleteRemovesThePredecessor() throws Exception {
		Dictionary.getInstance();
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("dependency-popup", undo), undo);
		project.initialize(false, false);
		NormalTask predecessor = new NormalTask(project);
		project.connectTask(predecessor);
		NormalTask current = new NormalTask(project);
		project.connectTask(current);
		TaskInformationDialog.createDependency(current, predecessor, true, this);
		SwingUtilities.invokeAndWait(() -> {
			TaskDependencySpreadsheet sheet = TaskDependencySpreadsheet.create(null,
					TaskDependencySpreadsheet.Direction.PREDECESSORS, current);
			assertEquals(List.of("popup." + MenuActionConstants.ACTION_DELETE), names(commands(sheet.getPopup())),
					"the dependency table offers its Delete command only");
			sheet.setRowSelectionInterval(0, 0);
			command(sheet.getPopup(), MenuActionConstants.ACTION_DELETE).doClick();
			assertEquals(0, current.getPredecessorList().size(),
					"popup Delete must remove the predecessor link");
		});
	}

	private static final class ResourceFixture {
		private final ResourcePool pool;
		private final ReferenceNodeModelCache reference;
		private final SpreadSheet sheet;

		private ResourceFixture(ResourcePool pool, ReferenceNodeModelCache reference, SpreadSheet sheet) {
			this.pool = pool;
			this.reference = reference;
			this.sheet = sheet;
		}

		/** A local resource pool with two unassigned resources, shown the way ResourceView builds its sheet. */
		static ResourceFixture create() throws Exception {
			Dictionary.getInstance();
			DataFactoryUndoController undo = new DataFactoryUndoController();
			ResourcePool pool = ResourcePool.createRourcePool("non-task-popup", undo);
			Project project = Project.createProject(pool, undo);
			project.initialize(false, false);
			pool.addProject(project);
			pool.setLocal(true);
			Resource alpha = pool.createScriptedResource();
			alpha.setName("Alpha");
			Resource bravo = pool.createScriptedResource();
			bravo.setName("Bravo");
			ResourceFixture[] built = new ResourceFixture[1];
			SwingUtilities.invokeAndWait(() -> {
				ReferenceNodeModelCache cache = NodeModelCacheFactory.createResourceNodeModelCache(pool,
						pool.getResourceOutline());
				cache.update();
				ResourceView view = new ResourceView(cache, pool.getResourceOutline(), pool, false, false);
				built[0] = new ResourceFixture(pool, cache, view.getSpreadSheet());
			});
			return built[0];
		}

		void release() {
			reference.close();
		}
	}

	private static List<JMenuItem> commands(SpreadSheetPopupMenu popup) {
		assertNotNull(popup, "the table must have a row popup");
		List<JMenuItem> items = new ArrayList<>();
		for (Component component : popup.getComponents())
			if (component instanceof JMenuItem item && item.getName() != null
					&& item.getName().startsWith("popup."))
				items.add(item);
		return items;
	}

	private static List<String> names(List<JMenuItem> commands) {
		List<String> names = new ArrayList<>();
		for (JMenuItem command : commands)
			names.add(command.getName());
		return names;
	}

	private static JMenuItem command(SpreadSheetPopupMenu popup, String actionId) {
		for (JMenuItem item : commands(popup))
			if (("popup." + actionId).equals(item.getName()))
				return item;
		return null;
	}
}
