/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static com.microproject.testsupport.NetworkDiagramGuiSupport.click;
import static com.microproject.testsupport.NetworkDiagramGuiSupport.onEdt;
import static com.microproject.testsupport.NetworkDiagramGuiSupport.screenBounds;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Window;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.views.PertView;
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

/** Network Diagram and Calendar Format tabs: Summary Tasks, Layout Now and calendar Gridlines. */
class NetworkCalendarRibbonGuiAcceptanceTest {
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
	void networkAndCalendarFormatTabsActOnTheirViews() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for network/calendar ribbon coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousRibbonDisplayMode = RibbonDisplayPreferences.load();
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		showProject(project());
		Robot robot = new GuiRobot();
		robot.setAutoDelay(45);
		GuiAcceptanceSupport.await(() -> onEdt(() -> window.isShowing() && manager.getCurrentFrame() != null),
			"project window did not become visible");
		ResourceBundle menu = ResourceBundle.getBundle("com.microproject.menu.menu");

		AbstractButton viewTab = GuiAcceptanceSupport.findShowingButtonByText(window, menu.getString("ViewRibbonTask.title"));
		click(robot, screenBounds(viewTab));
		GuiAcceptanceSupport.await(() -> onEdt(viewTab::isSelected), "View tab click did not select the View ribbon");
		clickCommand(robot, "RibbonNetwork");
		GuiAcceptanceSupport.await(() -> onEdt(() -> manager.getActiveNetworkView() != null), "Network Diagram did not open");
		PertView network = onEdt(() -> manager.getActiveNetworkView());
		AbstractButton networkTab = GuiAcceptanceSupport.findShowingButtonByText(window, menu.getString("NetworkFormat.contextualTitle"));
		click(robot, screenBounds(networkTab));
		GuiAcceptanceSupport.await(() -> onEdt(networkTab::isSelected), "Network Format tab click did not select the tab");

		clickCommand(robot, "RibbonSummaryTasks");
		GuiAcceptanceSupport.await(() -> onEdt(() -> !network.isSummaryTasksVisible()), "Summary Tasks did not hide the summary boxes");
		clickCommand(robot, "RibbonSummaryTasks");
		GuiAcceptanceSupport.await(() -> onEdt(network::isSummaryTasksVisible), "second Summary Tasks click did not restore the boxes");
		clickCommand(robot, "RibbonNetworkLayoutNow");

		click(robot, screenBounds(viewTab));
		clickCommand(robot, "RibbonCalendarView");
		GuiAcceptanceSupport.await(() -> onEdt(() -> manager.getActiveCalendarDialog() != null), "Calendar view did not open");
		AbstractButton calendarTab = GuiAcceptanceSupport.findShowingButtonByText(window, menu.getString("CalendarFormat.contextualTitle"));
		click(robot, screenBounds(calendarTab));
		GuiAcceptanceSupport.await(() -> onEdt(calendarTab::isSelected), "Calendar Format tab click did not select the tab");
		clickCommand(robot, "RibbonSheetGridlines");
		GuiAcceptanceSupport.await(() -> onEdt(() -> {
			for (Window w : Window.getWindows())
				if (w instanceof JDialog d && d.isShowing() && containsCheckBox(d.getContentPane())) return true;
			return false;
		}), "Calendar Gridlines did not open its dialog");
	}

	private static boolean containsCheckBox(Container container) {
		for (Component c : container.getComponents()) {
			if (c instanceof javax.swing.JCheckBox) return true;
			if (c instanceof Container child && containsCheckBox(child)) return true;
		}
		return false;
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
