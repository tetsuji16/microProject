/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.workspace.FrameHolder;
import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;

/** Robot coverage for creating every supported link type to a task in another open project. */
class TaskInformationCrossProjectGuiAcceptanceTest {
	private TestFrame frame;
	private TaskInformationDialog dialog;

	@AfterEach
	void closeWindows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
				if (window instanceof TaskInformationDialog || window instanceof JDialog && window != frame)
					window.dispose();
			if (frame != null) frame.dispose();
			frame = null;
		});
	}

	@ParameterizedTest(name = "cross-project {0} link")
	@MethodSource("dependencyKinds")
	void robotCreatesLinkToAnotherOpenProject(DependencyType.Kind kind) throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = fixture();
		SwingUtilities.invokeAndWait(() -> {
			frame = new TestFrame(fixture);
			frame.manager.installDocumentShortcuts(frame);
			DocumentFrame documentFrame = new DocumentFrame(frame.manager, fixture.first, "cross-project-dependency-gui-test") {
				private static final long serialVersionUID = 1L;
				@Override public com.microproject.undo.UndoController getUndoController() {
					return fixture.first.getUndoController();
				}
				@Override public void activateGanttView() { }
				@Override public void activateResourceView() { }
			};
			documentFrame.setActive(true);
			frame.manager.setDocumentFrame(documentFrame);
			frame.setSize(720, 420);
			frame.setLocationByPlatform(true);
			frame.setVisible(true);
			dialog = TaskInformationDialog.getInstance(frame, fixture.current, false);
			dialog.setModal(false);
			dialog.pack();
			dialog.setLocationByPlatform(true);
			dialog.setAlwaysOnTop(true);
			dialog.setVisible(true);
			dialog.toFront();
			dialog.updateAll();
		});
		GuiAcceptanceSupport.await(() -> dialog != null && dialog.isVisible(), "Task Information dialog did not open");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		Rectangle dialogBounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> dialogBounds.setBounds(dialog.getBounds()));
		robot.mouseMove(dialogBounds.x + dialogBounds.width / 2, dialogBounds.y + 12);
		robot.waitForIdle();
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> dialog.isActive() && dialog.isFocused(),
			"Task Information dialog did not acquire foreground focus before the physical dependency command");

		JTabbedPane tabs = findTabbedPane(dialog.getContentPane());
		assertTrue(tabs != null, "Task Information tabs are missing");
		AbstractButton newPredecessor = null;
		for (int index = 0; index < tabs.getTabCount() && newPredecessor == null; index++) {
			final int selected = index;
			SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(selected));
			newPredecessor = findVisibleButton(dialog, "newPredecessorLink");
		}
		assertTrue(newPredecessor != null && newPredecessor.isShowing(), "predecessor link button must be visible");

		click(robot, newPredecessor);
		JComboBox<?> taskChoices = awaitComboContaining("Second project");
		selectComboItem(robot, taskChoices, "Second project");
		JComboBox<?> typeChoices = awaitComboContaining(kind.name());
		selectComboItem(robot, typeChoices, kind.name());
		JTextField lagInput = awaitVisibleTextField();
		enterLag(robot, lagInput, "1d");

		GuiAcceptanceSupport.await(() -> fixture.current.getPredecessorList().size() == 1,
			"cross-project dependency was not created through the visible controls");
		Dependency dependency = (Dependency) fixture.current.getPredecessorList().iterator().next();
		assertEquals(fixture.crossProjectTask, dependency.getPredecessor());
		assertEquals(kind.code(), dependency.getDependencyType());
		assertTrue(dependency.getLag() != 0L, "the visible lag prompt must persist the selected lead/lag");
		SwingUtilities.invokeAndWait(() -> dialog.updateAll());
		SpreadSheet predecessors = dialog.predecessorsSpreadSheet;
		SpreadSheet createdPredecessors = predecessors;
		GuiAcceptanceSupport.await(() -> createdPredecessors != null && createdPredecessors.getRowCount() == 1,
			"Task Information did not refresh the created predecessor link");
		assertTrue(fixture.first.getUndoController().canUndo(), "link creation must be recorded in the successor project's Undo history");
		assertEquals("DependencyCreation", fixture.first.getUndoController().getUndoName(),
			"successor project Undo history must identify cross-project link creation");
		assertTrue(!fixture.second.getUndoController().canUndo(),
			"link creation must not be recorded in the predecessor project's Undo history");
		closeWindowWithRobot(robot, dialog);
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "physical close did not return focus before creation Undo");
		awaitProjectFrameFocus();
		frame.manager.getMenuManager().setActionEnabled("Undo", true);
		KeyStroke undoKey = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
		Object undoBinding = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(undoKey);
		assertTrue(undoBinding != null, "physical Ctrl+Z must resolve through the active window root pane");
		Action undoAction = frame.getRootPane().getActionMap().get(undoBinding);
		assertTrue(undoAction != null && undoAction.isEnabled(), "root-pane Ctrl+Z must resolve to an enabled Undo action");
		press(robot, java.awt.event.KeyEvent.VK_CONTROL, java.awt.event.KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> !fixture.current.getPredecessorList().contains(dependency)
			&& !fixture.crossProjectTask.getSuccessorList().contains(dependency),
			"physical Ctrl+Z did not undo cross-project link creation from the successor project");
		assertEquals(1, fixture.current.getSuccessorList().size(), "creation Undo must preserve the unrelated dependency");
		dialog = showTaskInformation(robot, fixture);
		SpreadSheet undoneCreationPredecessors = dialog.predecessorsSpreadSheet;
		GuiAcceptanceSupport.await(() -> undoneCreationPredecessors != null && undoneCreationPredecessors.getRowCount() == 0,
			"creation Undo did not remove the rendered Task Information predecessor row");
		closeWindowWithRobot(robot, dialog);
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "physical close did not return focus before creation Redo");
		awaitProjectFrameFocus();
		frame.manager.getMenuManager().setActionEnabled("Redo", true);
		press(robot, java.awt.event.KeyEvent.VK_CONTROL, java.awt.event.KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> fixture.current.getPredecessorList().contains(dependency)
			&& fixture.crossProjectTask.getSuccessorList().contains(dependency),
			"physical Ctrl+Y did not redo cross-project link creation from the successor project");
		dialog = showTaskInformation(robot, fixture);
		predecessors = dialog.predecessorsSpreadSheet;
		SpreadSheet redoneCreationPredecessors = predecessors;
		GuiAcceptanceSupport.await(() -> redoneCreationPredecessors != null && redoneCreationPredecessors.getRowCount() == 1,
			"creation Redo did not restore the rendered Task Information predecessor row");
		assertTrue(predecessors.getColumnCount() > 1,
			"Task Information dependency grid must expose a selectable non-link column");
		// The first column is an endpoint hyperlink and intentionally navigates to
		// the linked task. Select a non-link column to operate on the dependency row.
		clickCell(robot, predecessors, 0, Math.max(0, predecessors.getColumnCount() - 1));
		AbstractButton removePredecessor = findVisibleButton(dialog, "removePredecessorLink");
		assertTrue(removePredecessor != null && removePredecessor.isEnabled(),
			"Task Information predecessor Remove button must enable for a selected link; selectedRows="
				+ predecessors.getSelectedRowCount());
		click(robot, removePredecessor);
		assertEquals(1, fixture.current.getSuccessorList().size(), "the unrelated outgoing link must remain present");
		GuiAcceptanceSupport.await(() -> fixture.current.getPredecessorList().isEmpty(),
			"Task Information predecessor Remove did not remove the selected dependency");
		assertTrue(fixture.crossProjectTask.getSuccessorList().isEmpty(), "Remove must clear the link at its external endpoint");
		assertEquals(1, fixture.current.getSuccessorList().size(), "Remove must preserve the unrelated dependency");
		assertEquals(0, predecessors.getRowCount(), "removed predecessor row must disappear from Task Information");
		assertSame(fixture.current, dependency.getSuccessor(), "Remove must preserve the successor task");
		assertSame(fixture.crossProjectTask, dependency.getPredecessor(), "Remove must preserve the predecessor task");
		closeWindowWithRobot(robot, dialog);
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "physical close did not return focus from Task Information to the project frame");
		awaitProjectFrameFocus();
		frame.manager.getMenuManager().setActionEnabled("Undo", true);
		press(robot, java.awt.event.KeyEvent.VK_CONTROL, java.awt.event.KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> fixture.current.getPredecessorList().contains(dependency)
			&& fixture.crossProjectTask.getSuccessorList().contains(dependency),
			"physical Ctrl+Z did not restore the selected cross-project dependency");
		dialog = showTaskInformation(robot, fixture);
		SpreadSheet restoredPredecessors = dialog.predecessorsSpreadSheet;
		GuiAcceptanceSupport.await(() -> restoredPredecessors != null && restoredPredecessors.getRowCount() == 1,
			"Ctrl+Z did not restore the rendered Task Information predecessor row");
		assertEquals(kind.code(), dependency.getDependencyType(), "Undo changed the selected dependency type");
		assertTrue(dependency.getLag() != 0L, "Undo lost the selected dependency lag");
		assertEquals(1, fixture.current.getSuccessorList().size(), "Undo must preserve the unrelated dependency");
		closeWindowWithRobot(robot, dialog);
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "physical close did not return focus before Redo");
		awaitProjectFrameFocus();
		frame.manager.getMenuManager().setActionEnabled("Redo", true);
		press(robot, java.awt.event.KeyEvent.VK_CONTROL, java.awt.event.KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> !fixture.current.getPredecessorList().contains(dependency)
			&& !fixture.crossProjectTask.getSuccessorList().contains(dependency),
			"physical Ctrl+Y did not remove the cross-project dependency again");
		dialog = showTaskInformation(robot, fixture);
		SpreadSheet redonePredecessors = dialog.predecessorsSpreadSheet;
		GuiAcceptanceSupport.await(() -> redonePredecessors != null && redonePredecessors.getRowCount() == 0,
			"Ctrl+Y did not remove the rendered Task Information predecessor row again");
		assertEquals(1, fixture.current.getSuccessorList().size(), "Redo must preserve the unrelated dependency");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(fixture.first, saved), "MPO save rejected the removed cross-project link state");
		Project reloaded = importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask reloadedCurrent = findTask(reloaded, fixture.current.getName());
		NormalTask reloadedUnrelated = findTask(reloaded, fixture.unrelatedTask.getName());
		assertTrue(reloadedCurrent.getPredecessorList().isEmpty(), "MPO reload restored the removed cross-project dependency");
		assertEquals(1, reloadedCurrent.getSuccessorList().size(), "MPO reload lost the unrelated dependency");
		assertEquals(reloadedUnrelated, ((Dependency) reloadedCurrent.getSuccessorList().iterator().next()).getSuccessor());
		Path artifact = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"),
				"cross-project-link-" + kind.name().toLowerCase(java.util.Locale.ROOT) + ".png");
		Files.createDirectories(artifact.getParent());
		ImageIO.write(robot.createScreenCapture(dialog.getBounds()), "png", artifact.toFile());
	}

	private static Stream<DependencyType.Kind> dependencyKinds() {
		return Stream.of(DependencyType.Kind.FS, DependencyType.Kind.SS,
				DependencyType.Kind.FF, DependencyType.Kind.SF);
	}

	private static void click(Robot robot, Component component) throws Exception {
		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Point location = component.getLocationOnScreen();
			bounds.setBounds(location.x, location.y, component.getWidth(), component.getHeight());
		});
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.waitForIdle();
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
	}

	private static void clickCell(Robot robot, SpreadSheet sheet, int row, int column) throws Exception {
		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			Rectangle cell = sheet.getCellRect(row, column, true);
			java.awt.Point location = sheet.getLocationOnScreen();
			bounds.setBounds(location.x + cell.x, location.y + cell.y, cell.width, cell.height);
		});
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static JComboBox<?> awaitComboContaining(String text) throws Exception {
		final JComboBox<?>[] result = new JComboBox<?>[1];
		GuiAcceptanceSupport.await(() -> {
			for (Window window : Window.getWindows()) {
				if (!window.isVisible() || window == null) continue;
				JComboBox<?> combo = findCombo(window);
				if (combo != null) {
					for (int index = 0; index < combo.getItemCount(); index++)
						if (String.valueOf(combo.getItemAt(index)).contains(text)) {
							result[0] = combo;
							return true;
						}
				}
			}
			return false;
		}, "input choice dialog did not show " + text);
		return result[0];
	}

	private static void selectComboItem(Robot robot, JComboBox<?> combo, String text) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (int index = 0; index < combo.getItemCount(); index++)
				if (String.valueOf(combo.getItemAt(index)).contains(text)) {
					combo.setSelectedIndex(index);
					return;
				}
		});
		robot.waitForIdle();
		robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
	}

	private static JComboBox<?> findCombo(java.awt.Container container) {
		for (Component child : container.getComponents()) {
			if (child instanceof JComboBox<?> combo && combo.isShowing()) return combo;
			if (child instanceof java.awt.Container nested) {
				JComboBox<?> combo = findCombo(nested);
				if (combo != null) return combo;
			}
		}
		return null;
	}

	private static JTextField awaitVisibleTextField() throws Exception {
		final JTextField[] result = new JTextField[1];
		GuiAcceptanceSupport.await(() -> {
			for (Window window : Window.getWindows()) {
				if (!window.isVisible()) continue;
				JTextField field = findTextField(window);
				if (field != null) {
					result[0] = field;
					return true;
				}
			}
			return false;
		}, "lag input dialog did not appear");
		return result[0];
	}

	private static JTextField findTextField(java.awt.Container container) {
		for (Component child : container.getComponents()) {
			if (child instanceof JTextField field && field.isShowing()) return field;
			if (child instanceof java.awt.Container nested) {
				JTextField field = findTextField(nested);
				if (field != null) return field;
			}
		}
		return null;
	}

	private static void enterLag(Robot robot, JTextField field, String value) throws Exception {
		click(robot, field);
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_A);
		robot.keyRelease(java.awt.event.KeyEvent.VK_A);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		for (char character : value.toCharArray()) {
			int key = java.awt.event.KeyEvent.getExtendedKeyCodeForChar(character);
			robot.keyPress(key);
			robot.keyRelease(key);
		}
		robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
	}

	private static AbstractButton findVisibleButton(java.awt.Container container, String name) {
		for (Component child : container.getComponents()) {
			if (child instanceof AbstractButton button && name.equals(button.getName()) && button.isShowing()) return button;
			if (child instanceof java.awt.Container nested) {
				AbstractButton button = findVisibleButton(nested, name);
				if (button != null) return button;
			}
		}
		return null;
	}

	private static JTabbedPane findTabbedPane(java.awt.Container container) {
		for (Component child : container.getComponents()) {
			if (child instanceof JTabbedPane tabs) return tabs;
			if (child instanceof java.awt.Container nested) {
				JTabbedPane tabs = findTabbedPane(nested);
				if (tabs != null) return tabs;
			}
		}
		return null;
	}

	private Fixture fixture() throws Exception {
		Project first = newProject("First project");
		Project second = newProject("Second project");
		NormalTask current = task(first, "Current task");
		NormalTask cross = task(second, "Cross project task");
		NormalTask unrelated = task(first, "Unrelated successor task");
		DependencyService.getInstance().newDependency(current, unrelated, DependencyType.SS, 0L, this);
		first.getUndoController().clear();
		second.getUndoController().clear();
		return new Fixture(first, second, current, cross, unrelated);
	}

	private static NormalTask findTask(Project project, String name) {
		for (Task task : project.getTasks())
			if (task instanceof NormalTask normalTask && name.equals(normalTask.getName())) return normalTask;
		throw new AssertionError("task not found after MPO reload: " + name);
	}

	private static void press(Robot robot, int... keys) {
		for (int key : keys) robot.keyPress(key);
		for (int index = keys.length - 1; index >= 0; index--) robot.keyRelease(keys[index]);
		robot.waitForIdle();
	}

	private TaskInformationDialog showTaskInformation(Robot robot, Fixture fixture) throws Exception {
		TaskInformationDialog[] result = new TaskInformationDialog[1];
		SwingUtilities.invokeAndWait(() -> {
			result[0] = TaskInformationDialog.getInstance(frame, fixture.current, false);
			result[0].setModal(false);
			result[0].pack();
			result[0].setLocationByPlatform(true);
			result[0].setAlwaysOnTop(true);
			result[0].setVisible(true);
			result[0].toFront();
			result[0].updateAll();
		});
		GuiAcceptanceSupport.await(() -> result[0].isShowing(), "Task Information did not reopen after the physical history command");
		JTabbedPane tabs = findTabbedPane(result[0].getContentPane());
		if (tabs != null && result[0].predecessorsSpreadSheet != null) {
			for (int index = 0; index < tabs.getTabCount() && !result[0].predecessorsSpreadSheet.isShowing(); index++) {
				final int selectedIndex = index;
				SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(selectedIndex));
			}
		}
		GuiAcceptanceSupport.await(() -> result[0].predecessorsSpreadSheet != null
			&& result[0].predecessorsSpreadSheet.isShowing(),
			"reopened Task Information did not show its predecessor dependency grid");
		return result[0];
	}

	private static void closeWindowWithRobot(Robot robot, Window target) throws Exception {
		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> bounds.setBounds(target.getBounds()));
		robot.mouseMove(bounds.x + bounds.width - 14, bounds.y + 12);
		robot.waitForIdle();
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private void awaitProjectFrameFocus() throws Exception {
		GuiAcceptanceSupport.await(() -> frame != null && frame.isActive() && frame.isFocused(),
			"the project frame did not regain native focus after closing Task Information");
	}

	private Project newProject(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.setName(name);
		project.initialize(false, false);
		return project;
	}

	private NormalTask task(Project project, String name) {
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName(name);
		return task;
	}

	private record Fixture(Project first, Project second, NormalTask current, NormalTask crossProjectTask,
			NormalTask unrelatedTask) { }

	private static final class TestFrame extends javax.swing.JFrame implements FrameHolder {
		private static final long serialVersionUID = 1L;
		private final TestGraphicManager manager;

		TestFrame(Fixture fixture) {
			super("Cross-project dependency GUI acceptance");
			manager = new TestGraphicManager(this, fixture);
			manager.getMenuManager();
		}

		@Override public FrameManager getFrameManager() { return null; }
		@Override public GraphicManager getGraphicManager() { return manager; }
		@Override public void setGraphicManager(GraphicManager manager) { }
	}

	private static final class TestGraphicManager extends GraphicManager {
		private final Fixture fixture;
		private DocumentFrame documentFrame;
		TestGraphicManager(TestFrame frame, Fixture fixture) { super(frame); this.fixture = fixture; }
		void setDocumentFrame(DocumentFrame documentFrame) { this.documentFrame = documentFrame; }
		@Override public DocumentFrame getCurrentFrame() { return documentFrame; }
		@Override public List<Project> getOpenProjects() { return List.of(fixture.first, fixture.second); }
	}
}
