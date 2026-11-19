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
package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.configuration.Dictionary;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.views.ResourceView;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.ui.theme.ThemeManager;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.FlatLafSupport;

/**
 * Issue #483: a row popup is created once and cached on its table, and it lives
 * outside every window tree. A theme switch therefore never reached a popup that
 * had already been created, and the next right-click showed the light colours in
 * a dark window. The cached instance must refresh itself instead of being rebuilt.
 */
class SpreadSheetPopupThemeTest {
	private boolean originalDark;

	@BeforeEach
	void setUp() {
		FlatLafSupport.initialize();
		originalDark = ThemeManager.isDark();
	}

	@AfterEach
	void restore() {
		ThemeManager.applyTheme(originalDark);
	}

	/** A popup created before the switch must be dark when the same cached instance is read after it. */
	@Test
	void popupCreatedBeforeTheSwitchIsDarkAfterIt() throws Exception {
		ThemeManager.applyTheme(false);
		ResourceTable table = ResourceTable.create();
		try {
			SpreadSheetPopupMenu[] created = new SpreadSheetPopupMenu[1];
			Color[] light = new Color[2];
			SwingUtilities.invokeAndWait(() -> {
				created[0] = table.sheet.getPopup();
				assertNotNull(created[0], "the resource table must have a row popup");
				light[0] = created[0].getBackground();
				light[1] = firstCommand(created[0]).getBackground();
				assertTrue(isLight(light[0]), () -> "precondition: the light popup must be light: " + light[0]);
			});

			ThemeManager.applyTheme(true);

			SwingUtilities.invokeAndWait(() -> {
				assertTrue(ThemeManager.isDark(), "precondition: the theme switch must have applied");
				SpreadSheetPopupMenu afterSwitch = table.sheet.getPopup();
				assertSame(created[0], afterSwitch,
						"the popup must stay cached; the cached instance must refresh itself");
				assertNotEquals(light[0], afterSwitch.getBackground(),
						"the cached popup kept its light background after the switch");
				assertNotEquals(light[1], firstCommand(afterSwitch).getBackground(),
						"the cached popup items kept their light background after the switch");
				assertTrue(isDark(afterSwitch.getBackground()),
						() -> "the cached popup must be dark: " + afterSwitch.getBackground());
			});
		} finally {
			table.release();
		}
	}

	private static JMenuItem firstCommand(SpreadSheetPopupMenu popup) {
		List<JMenuItem> commands = new ArrayList<>();
		for (Component component : popup.getComponents())
			if (component instanceof JMenuItem item && item.getName() != null
					&& item.getName().startsWith("popup."))
				commands.add(item);
		assertTrue(!commands.isEmpty(), "the resource table popup must offer at least one command");
		return commands.get(0);
	}

	private static boolean isLight(Color color) {
		return color != null && brightness(color) >= 200;
	}

	private static boolean isDark(Color color) {
		return color != null && brightness(color) < 100;
	}

	private static int brightness(Color color) {
		return (color.getRed() + color.getGreen() + color.getBlue()) / 3;
	}

	/** The resource sheet built the way ResourceView builds it, without a document frame. */
	private static final class ResourceTable {
		private final ReferenceNodeModelCache cache;
		private final SpreadSheet sheet;

		private ResourceTable(ReferenceNodeModelCache cache, SpreadSheet sheet) {
			this.cache = cache;
			this.sheet = sheet;
		}

		static ResourceTable create() throws Exception {
			Dictionary.getInstance();
			DataFactoryUndoController undo = new DataFactoryUndoController();
			ResourcePool pool = ResourcePool.createRourcePool("popup-theme", undo);
			Project project = Project.createProject(pool, undo);
			project.initialize(false, false);
			pool.addProject(project);
			pool.setLocal(true);
			Resource alpha = pool.createScriptedResource();
			alpha.setName("Alpha");
			ResourceTable[] built = new ResourceTable[1];
			SwingUtilities.invokeAndWait(() -> {
				ReferenceNodeModelCache cache = NodeModelCacheFactory.createResourceNodeModelCache(pool,
						pool.getResourceOutline());
				cache.update();
				ResourceView view = new ResourceView(cache, pool.getResourceOutline(), pool, false, false);
				built[0] = new ResourceTable(cache, view.getSpreadSheet());
			});
			return built[0];
		}

		void release() {
			cache.close();
		}
	}
}
