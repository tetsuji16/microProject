/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static com.microproject.testsupport.NetworkDiagramGuiSupport.click;
import static com.microproject.testsupport.NetworkDiagramGuiSupport.onEdt;
import static com.microproject.testsupport.NetworkDiagramGuiSupport.screenBounds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.pm.graphic.views.UsageDetailView;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.GuiRobot;
import com.microproject.testsupport.RibbonGuiSupport;
import com.microproject.ui.ribbon.RibbonController;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/** Task Usage Format tab: detail-row toggles, the last-row guard and the Add Details dialog. */
class UsageDetailRibbonGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private RibbonDisplayMode previousRibbonDisplayMode;

	@AfterEach
	void closeWindow() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window owned : Window.getWindows())
				if (owned instanceof JDialog dialog && dialog.isShowing()) dialog.dispose();
			if (manager != null) manager.cleanUp();
			if (window != null) window.dispose();
		});
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		if (previousRibbonDisplayMode != null) RibbonDisplayPreferences.save(previousRibbonDisplayMode);
	}

	@Test
	void costToggleLastRowGuardAndAddDetailsDialogWorkOnTaskUsage() throws Exception {
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousRibbonDisplayMode = RibbonDisplayPreferences.load();
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for usage ribbon coverage.");
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		showProject(project());
		Robot robot = new GuiRobot();
		robot.setAutoDelay(45);
		GuiAcceptanceSupport.await(() -> onEdt(() -> window.isShowing() && manager.getCurrentFrame() != null),
			"project window did not become visible");

		AbstractButton viewTab = GuiAcceptanceSupport.findShowingButtonByText(window,
			ResourceBundle.getBundle("com.microproject.menu.menu").getString("ViewRibbonTask.title"));
		click(robot, screenBounds(viewTab));
		GuiAcceptanceSupport.await(() -> onEdt(viewTab::isSelected), "View tab click did not select the View ribbon");
		clickCommand(robot, "RibbonTaskUsageDetail");
		GuiAcceptanceSupport.await(() -> onEdt(() -> manager.getCurrentFrame().getActiveTopView() instanceof UsageDetailView),
			"Task Usage did not open");
		UsageDetailView view = onEdt(() -> (UsageDetailView) manager.getCurrentFrame().getActiveTopView());
		GuiAcceptanceSupport.await(() -> onEdt(() -> !visibleIds(view).isEmpty()), "usage detail rows did not initialize");

		AbstractButton formatTab = GuiAcceptanceSupport.findShowingButtonByText(window,
			ResourceBundle.getBundle("com.microproject.menu.menu").getString("TaskUsageFormat.contextualTitle"));
		click(robot, screenBounds(formatTab));
		GuiAcceptanceSupport.await(() -> onEdt(formatTab::isSelected), "Task Usage Format tab click did not select the tab");

		// Cost toggles on and off again.
		boolean initial = onEdt(() -> view.isDetailFieldVisible("Field.cost"));
		clickCommand(robot, "RibbonUsageDetailCost");
		GuiAcceptanceSupport.await(() -> onEdt(() -> view.isDetailFieldVisible("Field.cost") != initial),
			"Cost toggle did not change the detail row");
		clickCommand(robot, "RibbonUsageDetailCost");
		GuiAcceptanceSupport.await(() -> onEdt(() -> view.isDetailFieldVisible("Field.cost") == initial),
			"second Cost toggle did not restore the detail row");

		// Hide every row but one; the last one cannot be removed.
		List<String> visible = onEdt(() -> visibleIds(view));
		assertFalse(visible.isEmpty(), "usage grid must show at least one detail row");
		for (int i = 0; i < visible.size() - 1; i++) {
			String id = visible.get(i);
			onEdt(() -> view.toggleDetailField(id));
		}
		List<String> remaining = onEdt(() -> visibleIds(view));
		assertEquals(1, remaining.size());
		String last = remaining.get(0);
		onEdt(() -> view.toggleDetailField(last));
		assertEquals(List.of(last), onEdt(() -> visibleIds(view)), "the last detail row must stay visible");

		// Add Details opens its (modal) dialog; the click returns once the mouse is released.
		clickCommand(robot, "RibbonUsageDetailsAdd");
		GuiAcceptanceSupport.await(() -> onEdt(() -> {
			for (Window w : Window.getWindows())
				if (w instanceof JDialog d && d.isShowing() && containsList(d.getContentPane())) return true;
			return false;
		}), "Add Details did not open its dialog");
	}

	private static boolean containsList(Container container) {
		for (Component c : container.getComponents()) {
			if (c instanceof JList<?>) return true;
			if (c instanceof Container child && containsList(child)) return true;
		}
		return false;
	}

	private static List<String> visibleIds(UsageDetailView view) {
		List<String> ids = new ArrayList<>();
		if (view.getTimeSpreadSheet() == null) return ids;
		for (Object selected : view.getTimeSpreadSheet().getSelectedFieldArray())
			if (selected instanceof Field field) ids.add(field.getId());
		return ids;
	}

	private void showProject(Project project) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject - usage detail acceptance", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			RibbonController ribbon = findRibbonController(window);
			if (ribbon == null) throw new AssertionError("application ribbon controller was not installed");
			ribbon.setRibbonDisplayMode(RibbonDisplayMode.ALWAYS_SHOW);
			manager.addProjectFrame(project);
			window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			window.setSize(1600, 700);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(false);
			window.setVisible(true);
			window.toFront();
			window.requestFocus();
		});
	}

	private static Project project() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("usage-detail", undo), undo);
		project.initialize(false, false);
		for (int index = 0; index < 3; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName("Usage task " + index);
			project.connectTask(task);
			project.getTaskOutlines().addToAll(task, null);
		}
		return project;
	}

	private void clickCommand(Robot robot, String commandId) throws Exception {
		AbstractButton button = RibbonGuiSupport.findVisibleOrExpand(robot, window, commandId);
		click(robot, screenBounds(button));
	}

	private static RibbonController findRibbonController(Container container) {
		for (Component component : container.getComponents()) {
			if (component instanceof RibbonController ribbon) return ribbon;
			if (component instanceof Container child) {
				RibbonController found = findRibbonController(child);
				if (found != null) return found;
			}
		}
		return null;
	}
}
