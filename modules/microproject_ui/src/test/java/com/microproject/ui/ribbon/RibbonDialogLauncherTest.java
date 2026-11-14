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
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.ProjectMenuActionMap;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.ribbon.SwingRibbonModel;

/** The group dialog launchers declared by {@code <band>.dialogLauncher}: data, validation and wiring. */
class RibbonDialogLauncherTest {
	private static final String CALENDAR_VIEW_SETTINGS = "CalendarViewSettings";

	@Test
	void taskViewsResourcePropertiesAndScheduleGroupsDeclareTheirLaunchers() {
		SwingRibbonModel model = new SwingRibbonFactory(
			new MenuRibbonCommandSource(new ExtToolBarFactory(recordingActionMap(new ArrayList<>()),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT))),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT)).createModel(MenuManager.STANDARD_RIBBON);
		assertEquals("RibbonCalendarViewSettings", band(model, "ViewTaskRibbonBand").getDialogLauncherId());
		assertEquals("RibbonTaskInformation", band(model, "TaskTrackingRibbonBand").getDialogLauncherId());
		assertEquals("RibbonResourceInformation", band(model, "ResourcePropertiesRibbonBand").getDialogLauncherId());
		assertNull(band(model, "ViewDataRibbonBand").getDialogLauncherId());
	}

	@Test
	void aLauncherThatIsNotACommandWithAnActionFailsModelCreation() {
		assertThrows(IllegalStateException.class, () -> model(Map.of("ViewDataRibbonBand.dialogLauncher", "RibbonNoSuchCommand")));
	}

	@Test
	void theLauncherButtonOfTheViewTabRunsTheCalendarViewSettingsCommand() throws Exception {
		List<String> invoked = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			ExtToolBarFactory buttons = new ExtToolBarFactory(recordingActionMap(invoked),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			SwingRibbonFactory factory = new SwingRibbonFactory(new MenuRibbonCommandSource(buttons),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			JPanel host = factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
			findByText(host, "View").doClick();
			AbstractButton launcher = findByName(host, ModernRibbonPanel.DIALOG_LAUNCHER_NAME_PREFIX + "ViewTaskRibbonBand");
			assertNotNull(launcher.getAction(), "the launcher runs the command Action");
			assertEquals(MenuDefinitionSupport.ribbonBundles(Locale.ROOT).length > 0, launcher.getToolTipText() != null);
			assertNull(launcher.getText(), "the launcher is an icon only");
			invoked.clear();
			launcher.doClick(0);
		});
		assertEquals(List.of(CALENDAR_VIEW_SETTINGS), invoked);
	}

	private static AbstractButton findByName(JPanel root, String name) {
		return UiComponentWalker.flatten(root).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> name.equals(button.getName())).findFirst()
			.orElseThrow(() -> new AssertionError("No button named " + name));
	}

	private static AbstractButton findByText(JPanel root, String text) {
		return UiComponentWalker.flatten(root).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
	}

	/** An action map whose actions record the key they were created for when they run. */
	private static ProjectMenuActionMap recordingActionMap(List<String> invoked) {
		return new ProjectMenuActionMap() {
			@Override
			public Action getAction(String key) {
				if (key.startsWith("RibbonNoSuch")) {
					return null;
				}
				return new AbstractAction(key) {
					@Override
					public void actionPerformed(ActionEvent event) {
						invoked.add(key);
					}
				};
			}

			@Override
			public String getStringFromAction(Action action) {
				Object value = action.getValue(Action.NAME);
				return value == null ? "" : value.toString();
			}
		};
	}

	private static SwingRibbonModel model(Map<String, String> overrides) {
		ResourceBundle[] base = MenuDefinitionSupport.ribbonBundles(Locale.ROOT);
		ResourceBundle[] bundles = new ResourceBundle[base.length + 1];
		bundles[0] = new ListResourceBundle() {
			@Override
			protected Object[][] getContents() {
				return overrides.entrySet().stream()
					.map(entry -> new Object[] { entry.getKey(), entry.getValue() })
					.toArray(Object[][]::new);
			}
		};
		System.arraycopy(base, 0, bundles, 1, base.length);
		return new SwingRibbonFactory(new MenuRibbonCommandSource(
			new ExtToolBarFactory(recordingActionMap(new ArrayList<>()), bundles)), bundles)
			.createModel(MenuManager.STANDARD_RIBBON);
	}

	private static SwingRibbonModel.RibbonBand band(SwingRibbonModel model, String id) {
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			for (SwingRibbonModel.RibbonBand band : tab.getBands()) {
				if (band.getId().equals(id)) {
					return band;
				}
			}
		}
		throw new AssertionError("No ribbon band " + id);
	}
}
