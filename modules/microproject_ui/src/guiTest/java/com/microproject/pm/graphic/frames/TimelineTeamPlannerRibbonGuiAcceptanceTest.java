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

/** Timeline and Team Planner Format tabs: their toggles and option dialogs reach the view settings. */
class TimelineTeamPlannerRibbonGuiAcceptanceTest {
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
	void timelineAndTeamPlannerFormatTabsActOnTheirViews() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for timeline/team planner ribbon coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousRibbonDisplayMode = RibbonDisplayPreferences.load();
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Project project = project();
		showProject(project);
		Robot robot = new GuiRobot();
		robot.setAutoDelay(45);
		GuiAcceptanceSupport.await(() -> onEdt(() -> window.isShowing() && manager.getCurrentFrame() != null),
			"project window did not become visible");
		ResourceBundle menu = ResourceBundle.getBundle("com.microproject.menu.menu");

		AbstractButton viewTab = GuiAcceptanceSupport.findShowingButtonByText(window, menu.getString("ViewRibbonTask.title"));
		click(robot, screenBounds(viewTab));
		GuiAcceptanceSupport.await(() -> onEdt(viewTab::isSelected), "View tab click did not select the View ribbon");
		clickCommand(robot, "RibbonTimeline");
		GuiAcceptanceSupport.await(() -> onEdt(() -> manager.getActiveTimelineDialog() != null), "Timeline did not open");
		com.microproject.dialog.TimelineDialogBox timeline = onEdt(() -> manager.getActiveTimelineDialog());
		AbstractButton timelineTab = GuiAcceptanceSupport.findShowingButtonByText(window, menu.getString("TimelineFormat.contextualTitle"));
		click(robot, screenBounds(timelineTab));
		GuiAcceptanceSupport.await(() -> onEdt(timelineTab::isSelected), "Timeline Format tab click did not select the tab");

		boolean expanded = onEdt(() -> timeline.getSettings().isExpanded());
		clickCommand(robot, "RibbonTimelineExpand");
		GuiAcceptanceSupport.await(() -> onEdt(() -> timeline.getSettings().isExpanded() != expanded), "Expand Timeline did not toggle");
		boolean overlapped = onEdt(() -> timeline.getSettings().isShowOverlapped());
		clickCommand(robot, "RibbonTimelineOverlapped");
		GuiAcceptanceSupport.await(() -> onEdt(() -> timeline.getSettings().isShowOverlapped() != overlapped), "Overlapped Tasks did not toggle");
		boolean panZoom = onEdt(() -> timeline.getSettings().isShowPanZoom());
		clickCommand(robot, "RibbonTimelinePanZoom");
		GuiAcceptanceSupport.await(() -> onEdt(() -> timeline.getSettings().isShowPanZoom() != panZoom), "Pan and Zoom did not toggle");
		boolean labels = onEdt(() -> timeline.getSettings().isBarLabels());
		clickCommand(robot, "RibbonTimelineBarLabels");
		GuiAcceptanceSupport.await(() -> onEdt(() -> timeline.getSettings().isBarLabels() != labels), "Bar Labels did not toggle");
		clickCommand(robot, "RibbonTimelineTextLines");
		confirmDialog(robot, dialog -> ((javax.swing.JSpinner) find(dialog, javax.swing.JSpinner.class).get(0)).setValue(3));
		GuiAcceptanceSupport.await(() -> onEdt(() -> timeline.getSettings().getTextLines() == 3), "Text Lines did not set the line count");
		clickCommand(robot, "RibbonTimelineTextStyles");
		confirmDialog(robot, dialog -> ((javax.swing.JSpinner) find(dialog, javax.swing.JSpinner.class).get(0)).setValue(14));
		GuiAcceptanceSupport.await(() -> onEdt(() -> timeline.getSettings().getTextStyle(com.microproject.dialog.TimelineViewSettings.TextItem.LABEL).size() == 14), "Text Styles did not set the label size");
		clickCommand(robot, "RibbonTimelineDateRange");
		confirmDialog(robot, dialog -> ((javax.swing.JCheckBox) find(dialog, javax.swing.JCheckBox.class).get(0)).setSelected(true));
		int bars = onEdt(() -> timeline.getSettings().getBarCount());
		clickCommand(robot, "RibbonTimelineInsertBar");
		GuiAcceptanceSupport.await(() -> onEdt(() -> timeline.getSettings().getBarCount() == bars + 1), "Insert Bar did not add a timeline bar");
		clickCommand(robot, "RibbonTimelineAddExisting");
		confirmDialog(robot, dialog -> { });
		// Add to Timeline is enabled from the Timeline selection, so it must act on the Timeline's
		// selected task, not on the (empty) table selection.
		com.microproject.pm.task.Task target = project.getTasks().getFirst();
		boolean onTimeline = onEdt(target::isDisplayOnTimeline);
		onEdt(() -> { timeline.selectTasks(java.util.List.of(target)); return null; });
		onEdt(() -> { manager.getAction(com.microproject.ribbon.CommandId.TASK_ADD_TO_TIMELINE.actionId()).actionPerformed(
			new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, "AddToTimeline")); return null; });
		GuiAcceptanceSupport.await(() -> onEdt(() -> target.isDisplayOnTimeline() != onTimeline),
			"Add to Timeline did not act on the Timeline-selected task");
		org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(target), onEdt(timeline::getSelectedTasks),
			"refreshing the Timeline after Add to Timeline must keep its selection");

		click(robot, screenBounds(viewTab));
		// The View > Team Planner entry is a gallery; its command route is the Team Planner action.
		onEdt(() -> { manager.getAction(com.microproject.menu.MenuActionConstants.ACTION_TEAM_PLANNER).actionPerformed(new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, "TeamPlanner")); return null; });
		GuiAcceptanceSupport.await(() -> onEdt(() -> manager.getActiveTeamPlannerDialog() != null), "Team Planner did not open");
		com.microproject.dialog.TeamPlannerDialogBox planner = onEdt(() -> manager.getActiveTeamPlannerDialog());
		AbstractButton plannerTab = GuiAcceptanceSupport.findShowingButtonByText(window, menu.getString("TeamPlannerFormat.contextualTitle"));
		click(robot, screenBounds(plannerTab));
		GuiAcceptanceSupport.await(() -> onEdt(plannerTab::isSelected), "Team Planner Format tab click did not select the tab");

		boolean grid = onEdt(() -> planner.getSettings().isGridlines());
		clickCommand(robot, "RibbonTeamPlannerGridlines");
		GuiAcceptanceSupport.await(() -> onEdt(() -> planner.getSettings().isGridlines() != grid), "Gridlines did not toggle");
		boolean expandRows = onEdt(() -> planner.getSettings().isExpandRows());
		clickCommand(robot, "RibbonTeamPlannerExpandRows");
		GuiAcceptanceSupport.await(() -> onEdt(() -> planner.getSettings().isExpandRows() != expandRows), "Expand Resource Rows did not toggle");
		boolean unassigned = onEdt(() -> planner.getSettings().isShowUnassigned());
		clickCommand(robot, "RibbonTeamPlannerUnassigned");
		GuiAcceptanceSupport.await(() -> onEdt(() -> planner.getSettings().isShowUnassigned() != unassigned), "Unassigned Tasks did not toggle");
		boolean unscheduled = onEdt(() -> planner.getSettings().isShowUnscheduled());
		clickCommand(robot, "RibbonTeamPlannerUnscheduled");
		GuiAcceptanceSupport.await(() -> onEdt(() -> planner.getSettings().isShowUnscheduled() != unscheduled), "Unscheduled Tasks did not toggle");
		boolean prevent = onEdt(() -> planner.getSettings().isPreventOverallocation());
		clickCommand(robot, "RibbonTeamPlannerPreventOverallocation");
		GuiAcceptanceSupport.await(() -> onEdt(() -> planner.getSettings().isPreventOverallocation() != prevent), "Prevent Overallocation did not toggle");
		clickCommand(robot, "RibbonTeamPlannerTextLines");
		confirmDialog(robot, dialog -> ((javax.swing.JSpinner) find(dialog, javax.swing.JSpinner.class).get(0)).setValue(2));
		GuiAcceptanceSupport.await(() -> onEdt(() -> planner.getSettings().getTextLines() == 2), "Text Lines did not set the line count");
		clickCommand(robot, "RibbonTeamPlannerTextStyles");
		confirmDialog(robot, dialog -> ((javax.swing.JSpinner) find(dialog, javax.swing.JSpinner.class).get(0)).setValue(15));
		GuiAcceptanceSupport.await(() -> onEdt(() -> planner.getSettings().getTextStyle().size() == 15), "Text Styles did not set the text size");
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
			window = new MainRibbonFrame("microProject - timeline planner acceptance", null);
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
		Project project = Project.createProject(ResourcePool.createRourcePool("timeline-planner", undo), undo);
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
