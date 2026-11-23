/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.dialog.calendar.ChangeWorkingTimeDialogBox;
import com.microproject.dialog.options.CalendarDialogBox;
import com.microproject.options.CalendarOption;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.configuration.FieldDictionary;
import com.microproject.testsupport.DialogLayoutAssertions;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.FlatLafSupport;

/**
 * Cross-window resize contract for #863: every audited top-level window must keep
 * its text and controls intact after growth and at its packed minimum, and a
 * resizable window must spend the growth on its work surface instead of leaving
 * an unexplained empty band.
 *
 * <p>This is the shared sweep; the per-route command journeys keep proving that
 * each window is reachable. A window that declares no work surface is expected to
 * be a fixed-size form, so the sweep fails instead of silently accepting a
 * resizable window whose content never grows.
 */
class WindowSizeContractSweepGuiAcceptanceTest {
	private final List<Window> opened = new ArrayList<>();
	private MainRibbonFrame owner;
	private GraphicManager manager;
	private Project project;
	private NormalTask task;
	private Resource resource;
	private List<com.microproject.grouping.core.Node> taskNodes;
	private DataFactoryUndoController undo;

	@AfterEach
	void disposeWindows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : opened) if (window.isDisplayable()) window.dispose();
			if (manager != null) manager.cleanUp();
			if (owner != null) owner.dispose();
		});
	}

	@Test
	void everyAuditedDialogHonoursItsDeclaredSizeContract() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for the cross-window resize contract.");
		FlatLafSupport.ensureInitialized();
		startApplicationWindow();
		Path artifacts = Path.of(System.getProperty("microproject.gui.artifacts.dir",
			"build/reports/guiTest-artifacts"));
		Files.createDirectories(artifacts);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(20);

		List<WindowCase> cases = List.of(
			new WindowCase("about", Contract.FIXED_FORM, () -> AboutDialog.getInstance(owner)),
			new WindowCase("help", Contract.FIXED_FORM, () -> HelpDialog.getInstance(owner)),
			new WindowCase("project-dialog", Contract.RESIZABLE, () -> ProjectDialog.getInstance(owner,
				new ProjectDialog.Form())),
			new WindowCase("project-information", Contract.RESIZABLE,
				() -> ProjectInformationDialog.getInstance(owner, project)),
			new WindowCase("status-date", Contract.FIXED_FORM, () -> StatusDateDialog.getInstance(owner, project)),
			new WindowCase("rename-project", Contract.FIXED_FORM,
				() -> RenameProjectDialog.getInstance(owner, renameProjectForm())),
			new WindowCase("baseline", Contract.FIXED_FORM,
				() -> BaselineDialog.getInstance(owner, new BaselineDialog.Form(), false, true)),
			new WindowCase("update-project", Contract.FIXED_FORM, () -> UpdateProjectDialogBox.getInstance(owner,
				new UpdateProjectDialogBox.Form(), true)),
			new WindowCase("recurring-task", Contract.FIXED_FORM,
				() -> RecurringTaskDialog.getInstance(owner, project)),
			new WindowCase("update-task", Contract.FIXED_FORM, () -> UpdateTaskDialog.getInstance(owner, taskNodes)),
			new WindowCase("delegate-task", Contract.FIXED_FORM,
				() -> DelegateTaskDialog.getInstance(owner, taskNodes)),
			new WindowCase("task-information", Contract.RESIZABLE,
				() -> TaskInformationDialog.getInstance(owner, task, false)),
			new WindowCase("resource-information", Contract.RESIZABLE,
				() -> ResourceInformationDialog.getInstance(owner, resource)),
			new WindowCase("resource-leveling", Contract.RESIZABLE,
				() -> ResourceLevelingDialogBox.getInstance(owner, project)),
			new WindowCase("field-alias", Contract.FIXED_FORM, () -> FieldAliasDialog.getInstance(owner,
				FieldDictionary.getInstance().getFieldFromId("Field.name"))),
			new WindowCase("calendar-options", Contract.FIXED_FORM,
				() -> CalendarDialogBox.getInstance(owner, CalendarOption.getInstance())),
			new WindowCase("change-working-time", Contract.RESIZABLE,
				() -> ChangeWorkingTimeDialogBox.getInstance(owner, project,
					(WorkingCalendar) project.getWorkCalendar(), null, false, undo)),
			new WindowCase("team-planner", Contract.RESIZABLE, () -> TeamPlannerDialogBox.getInstance(owner, project)),
			new WindowCase("calendar-view", Contract.RESIZABLE, () -> new CalendarViewDialogBox(owner, project)),
			new WindowCase("timeline", Contract.RESIZABLE, () -> new TimelineDialogBox(owner, project)),
			new WindowCase("custom-fields", Contract.RESIZABLE,
				() -> new CustomFieldsDialogBox(owner, project, List.of(task))),
			new WindowCase("custom-report", Contract.RESIZABLE, () -> new CustomReportDialogBox(owner, project)),
			new WindowCase("locale", Contract.RESIZABLE, () -> LocaleDialog.getInstance(manager)));

		List<String> report = new ArrayList<>();
		List<String> failures = new ArrayList<>();
		for (WindowCase windowCase : cases) {
			Window window = open(windowCase);
			try {
				verifySizeContract(window, windowCase, artifacts, robot, report, failures);
			} finally {
				SwingUtilities.invokeAndWait(window::dispose);
				opened.remove(window);
			}
		}
		assertTrue(report.size() == cases.size(), "every audited dialog must report its size contract");
		assertTrue(failures.isEmpty(), () -> "window size contract failures:"
			+ System.lineSeparator() + String.join(System.lineSeparator(), failures));
	}

	@Test
	void mainWindowKeepsItsWorkAreaWhenGrown() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for the cross-window resize contract.");
		FlatLafSupport.ensureInitialized();
		startApplicationWindow();
		Path artifacts = Path.of(System.getProperty("microproject.gui.artifacts.dir",
			"build/reports/guiTest-artifacts"));
		Files.createDirectories(artifacts);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(20);
		List<String> report = new ArrayList<>();
		List<String> failures = new ArrayList<>();
		verifySizeContract(owner, new WindowCase("main-window", Contract.RESIZABLE, () -> owner), artifacts, robot,
			report, failures);
		assertTrue(report.size() == 1, "the main window must report its size contract");
		assertTrue(failures.isEmpty(), () -> "main window size contract failures:"
			+ System.lineSeparator() + String.join(System.lineSeparator(), failures));
	}

	/**
	 * #1340: a large tool dialog is centered on its owner, so a fixed pixel size larger than
	 * the usable area hides its bottom-right action row. The dialog must fit the monitor it
	 * opens on, and this test leaves it where the product put it.
	 */
	@Test
	void resourceLevelingDialogOpensInsideTheUsableScreen() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for the dialog placement contract.");
		FlatLafSupport.ensureInitialized();
		startApplicationWindow();
		Window[] created = new Window[1];
		SwingUtilities.invokeAndWait(() -> {
			ResourceLevelingDialogBox dialog = ResourceLevelingDialogBox.getInstance(owner, project);
			dialog.setModal(false);
			dialog.setVisible(true);
			created[0] = dialog;
			opened.add(dialog);
		});
		Window dialog = created[0];
		GuiAcceptanceSupport.await(dialog::isShowing, "resource-leveling window did not become visible");
		// The dialog places itself on its owner; moving it first would hide the defect.
		DialogLayoutAssertions.assertWithinUsableScreen(dialog, "resource-leveling");
	}

	private void verifySizeContract(Window window, WindowCase windowCase, Path artifacts, Robot robot,
			List<String> report, List<String> failures) throws Exception {
		String name = windowCase.name();
		Dimension packed = window.getSize();
		boolean resizable = window instanceof Dialog dialog ? dialog.isResizable()
			: !(window instanceof Frame frame) || frame.isResizable();
		if (resizable != (windowCase.contract() == Contract.RESIZABLE))
			failures.add(name + ": declares " + windowCase.contract() + " but isResizable=" + resizable);
		java.awt.Container content = window instanceof javax.swing.RootPaneContainer rootPaneContainer
			? rootPaneContainer.getContentPane() : window;
		java.awt.Component surface = DialogLayoutAssertions.workSurface(content);
		try {
			DialogLayoutAssertions.assertWindowSizeContract(window, content, name);
		} catch (AssertionError failure) {
			failures.add(name + ": " + failure.getMessage());
		}
		window.toFront();
		robot.waitForIdle();
		Rectangle bounds = new Rectangle(window.getLocationOnScreen(), window.getSize());
		if (bounds.width > 0 && bounds.height > 0) {
			String environment = (System.getProperty("user.language", "unknown") + "-"
				+ System.getProperty("sun.java2d.uiScale", "default")).replaceAll("[^A-Za-z0-9_.-]", "_");
			ImageIO.write(robot.createScreenCapture(bounds), "png",
				artifacts.resolve("issue-863-resize-" + name + "-" + environment + ".png").toFile());
		}
		String line = "WINDOW_SIZE_CONTRACT name=" + name + " contract=" + windowCase.contract()
			+ " type=" + window.getClass().getSimpleName() + " resizable=" + resizable + " packed=" + packed.width + "x" + packed.height
			+ " minimum=" + window.getMinimumSize().width + "x" + window.getMinimumSize().height
			+ " workSurface=" + (surface == null ? "none" : surface.getClass().getSimpleName())
			+ " layout=" + DialogLayoutAssertions.layoutDiagnostic(content);
		System.out.println(line);
		report.add(line);
	}

	private Window open(WindowCase windowCase) throws Exception {
		Window[] created = new Window[1];
		SwingUtilities.invokeAndWait(() -> {
			Window window = windowCase.factory().get();
			if (window instanceof Dialog dialog) dialog.setModal(false);
			// AbstractDialog windows are packed; the FlatLaf dialogs declare their own
			// initial size through DialogSizing, and re-packing would replace it with
			// their (often much larger) content preferred size.
			if (window.getWidth() <= 0 || window.getHeight() <= 0) {
				if (window instanceof Dialog dialog) dialog.pack();
				else if (window instanceof Frame frame) frame.pack();
			}
			// Platform cascading offsets each window, so on a small screen a large dialog
			// would start past the usable right/bottom edge; anchor it at the usable origin.
			java.awt.GraphicsConfiguration configuration = owner.getGraphicsConfiguration();
			java.awt.Insets insets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(configuration);
			Rectangle screen = configuration.getBounds();
			window.setLocation(screen.x + insets.left, screen.y + insets.top);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
			window.toFront();
			window.requestFocus();
			created[0] = window;
			opened.add(window);
		});
		Window window = created[0];
		GuiAcceptanceSupport.await(window::isShowing, windowCase.name() + " window did not become visible");
		return window;
	}

	private void startApplicationWindow() throws Exception {
		undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("window-size-contract", undo);
		pool.setLocal(true);
		project = Project.createProject(pool, undo);
		project.initialize(false, false);
		project.setName("Window size contract");
		project.setForward(true);
		task = project.createScriptedTask();
		task.setName("Window size contract task");
		taskNodes = List.of(com.microproject.grouping.core.NodeFactory.getInstance().createNode(task));
		resource = pool.createScriptedResource();
		resource.setName("Window size contract resource");
		SwingUtilities.invokeAndWait(() -> {
			owner = new MainRibbonFrame("microProject — window size contract", null);
			manager = new GraphicManager(owner);
			owner.setGraphicManager(manager);
			manager.initView();
			owner.setAlwaysOnTop(true);
			// Start below the usable area (1024x720 on the CI runner) so the sweep has room to grow it.
			owner.setSize(900, 560);
			owner.setLocationByPlatform(true);
			owner.setVisible(true);
			manager.addProjectFrame(project);
		});
		GuiAcceptanceSupport.await(() -> owner.isShowing(), "application window did not become visible");
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
			&& manager.getCurrentFrame().getProject() == project, "fixture project was not activated");
	}

	/** A dialog the sweep opens, named after the window it represents in the #863 inventory. */
	private record WindowCase(String name, Contract contract, Supplier<Window> factory) {
	}

	/**
	 * The size contract the #863 inventory assigns to a window: a bounded form keeps
	 * its packed size, a window with a data view or canvas may be resized.
	 */
	private enum Contract {
		FIXED_FORM,
		RESIZABLE
	}

	/** The rename form refuses to bind until it knows the project name and the taken names. */
	private static RenameProjectDialog.Form renameProjectForm() {
		RenameProjectDialog.Form form = new RenameProjectDialog.Form();
		form.setName("Window size contract");
		form.setProjectNames(java.util.Set.of());
		return form;
	}
}
