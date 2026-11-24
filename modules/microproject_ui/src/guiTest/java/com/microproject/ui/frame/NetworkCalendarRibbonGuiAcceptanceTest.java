/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.frame;

import static com.microproject.ui.testsupport.NetworkDiagramGuiSupport.click;
import static com.microproject.ui.testsupport.NetworkDiagramGuiSupport.onEdt;
import static com.microproject.ui.testsupport.NetworkDiagramGuiSupport.screenBounds;

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

import com.microproject.ui.view.views.PertView;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.testsupport.GuiRobot;
import com.microproject.ui.testsupport.RibbonGuiSupport;
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
		ResourceBundle menu = ResourceBundle.getBundle("com.microproject.ui.menu.menu");

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

		clickCommand(robot, "RibbonNetworkLinkLabels");
		GuiAcceptanceSupport.await(() -> onEdt(() -> network.getDisplayOptions().isLinkLabels()), "Link Labels did not turn on");
		clickCommand(robot, "RibbonNetworkProgressMarks");
		GuiAcceptanceSupport.await(() -> onEdt(() -> network.getDisplayOptions().isProgressMarks()), "Progress Marks did not turn on");
		clickCommand(robot, "RibbonNetworkStraightLinks");
		GuiAcceptanceSupport.await(() -> onEdt(() -> network.getDisplayOptions().isStraightLinks()
			&& network.getLinkRouting() instanceof com.microproject.ui.view.link_routing.StraightNetworkLinkRouting), "Straight Links did not switch the routing");
		clickCommand(robot, "RibbonNetworkBoxFieldsCollapsed");
		GuiAcceptanceSupport.await(() -> onEdt(() -> network.getDisplayOptions().isCollapsed()), "Collapse Boxes did not collapse the boxes");
		clickCommand(robot, "RibbonNetworkBoxFieldsCollapsed");
		GuiAcceptanceSupport.await(() -> onEdt(() -> !network.getDisplayOptions().isCollapsed()), "second Collapse Boxes click did not expand the boxes");
		clickCommand(robot, "RibbonNetworkBoxStyles");
		confirmDialog(robot, dialog -> ((javax.swing.JComboBox<?>) find(dialog, javax.swing.JComboBox.class).get(0)).setSelectedIndex(1));
		GuiAcceptanceSupport.await(() -> onEdt(() -> network.getDisplayOptions().hasStyles()), "Box Styles did not store a border color");
		clickCommand(robot, "RibbonNetworkBoxFormat");
		confirmDialog(robot, dialog -> { });
		clickCommand(robot, "RibbonNetworkLayout");
		confirmDialog(robot, dialog -> ((javax.swing.JSpinner) find(dialog, javax.swing.JSpinner.class).get(0)).setValue(40));
		GuiAcceptanceSupport.await(() -> onEdt(() -> network.getDisplayOptions().getBoxSpacing() == 40), "the Layout dialog did not set the box spacing");

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
		confirmDialog(robot, dialog -> { });
		com.microproject.ui.dialog.CalendarViewDialogBox calendar = onEdt(() -> manager.getActiveCalendarDialog());
		clickCommand(robot, "RibbonCalendarTextStyles");
		confirmDialog(robot, dialog -> ((javax.swing.JSpinner) find(dialog, javax.swing.JSpinner.class).get(0)).setValue(14));
		GuiAcceptanceSupport.await(() -> onEdt(() -> calendar.getSettings().getTextStyle(com.microproject.ui.dialog.CalendarViewSettings.TextItem.BAR_TEXT).size() == 14),
			"Text Styles did not set the bar text size");
		clickCommand(robot, "RibbonCalendarBarStyles");
		confirmDialog(robot, dialog -> ((javax.swing.JComboBox<?>) find(dialog, javax.swing.JComboBox.class).get(0)).setSelectedIndex(1));
		GuiAcceptanceSupport.await(() -> onEdt(() -> calendar.getSettings().getBarColor(com.microproject.ui.dialog.CalendarViewSettings.BarType.NORMAL) != null),
			"Bar Styles did not set the bar color");
		onEdt(() -> { calendar.getSettings().setRowHeight(220); calendar.settingsChanged(); return null; });
		clickCommand(robot, "RibbonCalendarWeekHeightBestFit");
		GuiAcceptanceSupport.await(() -> onEdt(() -> calendar.getRowHeight() < 220), "Week Height Best Fit did not change the row height");
		clickCommand(robot, "RibbonCalendarLayout");
		confirmDialog(robot, dialog -> ((javax.swing.JCheckBox) find(dialog, javax.swing.JCheckBox.class).get(0)).setSelected(true));
		GuiAcceptanceSupport.await(() -> onEdt(() -> calendar.getSettings().isFitAsManyTasksAsPossible()), "the Calendar Layout dialog did not set the fit option");
		clickCommand(robot, "RibbonCalendarLayoutNow");
		clickCommand(robot, "RibbonCalendarTaskList");
		GuiAcceptanceSupport.await(() -> onEdt(() -> calendar.getSettings().isTaskListVisible() && !calendar.getTaskListEntries().isEmpty()),
			"Task List did not show the tasks of the month");
		onEdt(() -> {
			com.microproject.ui.view.views.DockableProjectToolView view = manager.getCurrentFrame().getCalendarView();
			javax.swing.JComponent embedded = (javax.swing.JComponent) view.getComponent(0);
			org.junit.jupiter.api.Assertions.assertSame(calendar.getSettings(),
				embedded.getClientProperty(com.microproject.ui.dialog.ViewSettingsState.PROPERTY),
				"the Calendar panel must expose its settings to the workspace");
			org.junit.jupiter.api.Assertions.assertNotNull(
				embedded.getClientProperty(com.microproject.ui.dialog.ViewSettingsState.REFRESH_PROPERTY),
				"restored Calendar settings must be re-applied to the view");
			return null;
		});
	}

	private static <T extends Component> java.util.List<T> find(Container container, Class<T> type) {
		java.util.List<T> result = new java.util.ArrayList<>();
		for (Component c : container.getComponents()) {
			if (type.isInstance(c)) result.add(type.cast(c));
			if (c instanceof Container child) result.addAll(find(child, type));
		}
		return result;
	}

	/** Waits for the option dialog, lets the test change it, and confirms it with OK. */
	private void confirmDialog(Robot robot, java.util.function.Consumer<Container> edit) throws Exception {
		GuiAcceptanceSupport.await(() -> onEdt(() -> {
			for (Window w : Window.getWindows())
				if (w instanceof JDialog d && d.isShowing() && d.isModal()) return true;
			return false;
		}), "the option dialog did not open");
		JDialog dialog = onEdt(() -> {
			for (Window w : Window.getWindows())
				if (w instanceof JDialog d && d.isShowing() && d.isModal()) return d;
			return null;
		});
		onEdt(() -> { edit.accept(dialog.getContentPane()); return null; });
		String okText = javax.swing.UIManager.getString("OptionPane.okButtonText");
		AbstractButton ok = onEdt(() -> {
			for (javax.swing.JButton b : find(dialog.getContentPane(), javax.swing.JButton.class))
				if (okText.equals(b.getText())) return b;
			return null;
		});
		click(robot, screenBounds(ok));
		GuiAcceptanceSupport.await(() -> onEdt(() -> !dialog.isShowing()), "the option dialog did not close after OK");
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
