/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.workspace.FrameHolder;
import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;

/** GUI-MSP-EXTERNAL-01: an unresolved cross-project endpoint remains visible and safe. */
class UnresolvedCrossProjectDependencyGuiAcceptanceTest {
	private TestFrame owner;
	private TaskInformationDialog dialog;

	@AfterEach
	void closeWindows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (dialog != null) dialog.dispose();
			if (owner != null) owner.dispose();
		});
	}

	@Test
	void unresolvedExternalPredecessorSurvivesRecalculationAndIsVisibleInTaskInformation() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = fixture();
		fixture.local.recalculate();
		assertTrue(fixture.externalTask.isExternal(), "fixture must model an unavailable external endpoint");
		assertTrue(fixture.localTask.getPredecessorList().size() == 1, "recalculation must retain unresolved link");

		SwingUtilities.invokeAndWait(() -> {
			owner = new TestFrame(fixture.local);
			owner.setSize(600, 300);
			owner.setLocationByPlatform(true);
			owner.setVisible(true);
			dialog = TaskInformationDialog.getInstance(owner, fixture.localTask, false);
			dialog.setModal(false);
			dialog.pack();
			dialog.setLocationByPlatform(true);
			dialog.setAlwaysOnTop(true);
			dialog.setVisible(true);
			dialog.toFront();
			dialog.updateAll();
		});
		GuiAcceptanceSupport.await(() -> dialog != null && dialog.isShowing(), "Task Information dialog did not open");
		boolean[] externalPredecessorShown = new boolean[1];
		SwingUtilities.invokeAndWait(() -> {
			selectPredecessorsTab(dialog);
			dialog.setSize(1_050, 600);
			dialog.predecessorsSpreadSheet.getColumnModel().getColumn(0).setPreferredWidth(330);
			dialog.predecessorsSpreadSheet.getColumnModel().getColumn(0).setWidth(330);
			externalPredecessorShown[0] = renderedPredecessorContains(dialog, "Offline project: Unavailable predecessor");
		});
		assertTrue(externalPredecessorShown[0], "unresolved external predecessor was not shown with its project identity");
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		capture(robot);
	}

	private static Fixture fixture() throws Exception {
		Project local = project("Local project");
		Project external = project("Offline project");
		NormalTask localTask = task(local, "Local successor");
		NormalTask externalTask = task(external, "Unavailable predecessor");
		DependencyService.getInstance().newDependency(externalTask, localTask, DependencyType.Kind.FS.code(), 0L,
				UnresolvedCrossProjectDependencyGuiAcceptanceTest.class);
		externalTask.setExternal(true);
		externalTask.setOwningProject(external);
		return new Fixture(local, localTask, externalTask);
	}

	private static Project project(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.initialize(false, false);
		project.setName(name);
		return project;
	}

	private static NormalTask task(Project project, String name) {
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName(name);
		return task;
	}

	private static void selectPredecessorsTab(Component component) {
		if (component instanceof JTabbedPane tabs) {
			for (int i = 0; i < tabs.getTabCount(); i++)
				if (tabs.getTitleAt(i).contains("Predecessor")) {
					tabs.setSelectedIndex(i);
					return;
				}
			// The Task Information tab order is a compatibility contract; the localized
			// predecessor title cannot be used as a Robot-test selector.
			if (tabs.getTabCount() > 2) tabs.setSelectedIndex(2);
		}
		if (component instanceof java.awt.Container container)
			for (Component child : container.getComponents()) selectPredecessorsTab(child);
	}

	private static boolean renderedPredecessorContains(TaskInformationDialog dialog, String expected) {
		if (dialog.predecessorsSpreadSheet == null) return false;
		for (int row = 0; row < dialog.predecessorsSpreadSheet.getRowCount(); row++)
			for (int column = 0; column < dialog.predecessorsSpreadSheet.getColumnCount(); column++) {
				TableCellRenderer renderer = dialog.predecessorsSpreadSheet.getCellRenderer(row, column);
				Component component = dialog.predecessorsSpreadSheet.prepareRenderer(renderer, row, column);
				if (component instanceof JLabel label && label.getText() != null && label.getText().contains(expected)) return true;
			}
		return false;
	}

	private void capture(Robot robot) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(dialog.getLocationOnScreen(), dialog.getSize()));
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"),
				"unresolved-cross-project-predecessor.png");
		Files.createDirectories(artifact.getParent());
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		ImageIO.write(image, "png", artifact.toFile());
	}

	private record Fixture(Project local, NormalTask localTask, NormalTask externalTask) { }

	private static final class TestFrame extends JFrame implements FrameHolder {
		private static final long serialVersionUID = 1L;
		private final TestGraphicManager manager;
		TestFrame(Project project) {
			super("Unresolved cross-project dependency GUI acceptance");
			manager = new TestGraphicManager(this);
			manager.setDocumentFrame(new DocumentFrame(manager, project, "unresolved-external-gui") {
				private static final long serialVersionUID = 1L;
				@Override public void activateGanttView() { }
				@Override public void activateResourceView() { }
			});
			manager.getMenuManager();
		}
		@Override public FrameManager getFrameManager() { return null; }
		@Override public GraphicManager getGraphicManager() { return manager; }
		@Override public void setGraphicManager(GraphicManager manager) { }
	}

	private static final class TestGraphicManager extends GraphicManager {
		private DocumentFrame documentFrame;
		TestGraphicManager(TestFrame frame) { super(frame); }
		void setDocumentFrame(DocumentFrame documentFrame) { this.documentFrame = documentFrame; }
		@Override public DocumentFrame getCurrentFrame() { return documentFrame; }
	}
}
