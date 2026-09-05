/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.microproject.grouping.core.Node;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.workspace.FrameHolder;
import com.microproject.pm.graphic.frames.workspace.FrameManager;
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
			frame.manager.setDocumentFrame(new DocumentFrame(frame.manager, fixture.first, "cross-project-dependency-gui-test") {
				private static final long serialVersionUID = 1L;
				@Override public void activateGanttView() { }
				@Override public void activateResourceView() { }
			});
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

		JTabbedPane tabs = findTabbedPane(dialog.getContentPane());
		assertTrue(tabs != null, "Task Information tabs are missing");
		AbstractButton newPredecessor = null;
		for (int index = 0; index < tabs.getTabCount() && newPredecessor == null; index++) {
			final int selected = index;
			SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(selected));
			newPredecessor = findVisibleButton(dialog, "newPredecessorLink");
		}
		assertTrue(newPredecessor != null && newPredecessor.isShowing(), "predecessor link button must be visible");

		Robot robot = new Robot();
		robot.setAutoDelay(40);
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
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"),
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
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
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

	private Fixture fixture() {
		Project first = newProject("First project");
		Project second = newProject("Second project");
		NormalTask current = task(first, "Current task");
		NormalTask cross = task(second, "Cross project task");
		return new Fixture(first, second, current, cross);
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

	private record Fixture(Project first, Project second, NormalTask current, NormalTask crossProjectTask) { }

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
