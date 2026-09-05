/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JButton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.graphic.frames.workspace.DefaultFrameManager;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.task.DefaultSubprojectHandler;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;

/** GUI-MSP-REFRESH-01: Escape cancels dirty-child refresh without discarding its in-memory state. */
class UnsavedSubprojectRefreshGuiAcceptanceTest {
	private JFrame window;
	private DefaultFrameManager frameManager;

	@AfterEach
	void closeWindow() throws Exception {
		if (frameManager != null)
			SwingUtilities.invokeAndWait(() -> frameManager.cleanUp());
		if (window != null)
			SwingUtilities.invokeAndWait(() -> window.dispose());
	}

	@Test
	void robotCancelsRefreshForDirtyLinkedChildAndKeepsItsProjection() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture();
		show(fixture);
		GuiAcceptanceSupport.await(() -> window.isShowing(), "master document window was not visible");
		fixture.manager.selectForTest(fixture.document);
		assertSame(fixture.document, fixture.manager.getCurrentFrame(), "master fixture must be the active document");
		assertTrue(fixture.child.needsSaving(), "fixture must represent a savable dirty child");

		AtomicBoolean completed = new AtomicBoolean();
		AtomicReference<Boolean> refreshResult = new AtomicReference<Boolean>();
		SwingUtilities.invokeLater(() -> {
			refreshResult.set(fixture.manager.refreshLinkedSubproject(fixture.reference));
			completed.set(true);
		});
		GuiAcceptanceSupport.await(() -> findRefreshDialog() != null || completed.get(),
				"refresh did not either show its decision dialog or return");
		assertFalse(completed.get(), "refresh returned before prompting; result=" + refreshResult.get()
				+ ", active=" + fixture.manager.getCurrentFrame() + ", child=" + fixture.reference.getSubproject());
		Dialog dialog = findRefreshDialog();
		assertTrue(dialog.isShowing());
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		capture(robot, dialog);
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(completed::get, "Escape did not complete the refresh decision");

		assertFalse(Boolean.TRUE.equals(refreshResult.get()), "Cancel must reject refresh");
		assertSame(fixture.child, fixture.reference.getSubproject(), "Cancel must retain the in-memory child model");
		assertTrue(fixture.child.needsSaving(), "Cancel must retain unsaved child edits");
		assertTrue(fixture.childTask.getName().contains("unsaved"), "Cancel must retain the edited child task");
	}

	@Test
	void robotSavesDirtyChildDuringRefreshAndPersistsTheEdit() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture();
		show(fixture);
		GuiAcceptanceSupport.await(() -> window.isShowing(), "master document window was not visible");
		fixture.manager.selectForTest(fixture.document);
		fixture.child.setDirty(true);
		fixture.child.setGroupDirty(true);
		AtomicBoolean completed = new AtomicBoolean();
		AtomicReference<Boolean> result = new AtomicReference<>();
		SwingUtilities.invokeLater(() -> { result.set(fixture.manager.refreshLinkedSubproject(fixture.reference)); completed.set(true); });
		GuiAcceptanceSupport.await(() -> findRefreshDialog() != null || completed.get(),
				"refresh did not show its decision dialog or complete");
		assertTrue(!completed.get(), "Save refresh returned before prompting; child dirty=" + fixture.child.needsSaving());
		clickRefreshChoice("Save");
		awaitCompletion(completed, "Save did not complete the refresh decision");
		assertTrue(Boolean.TRUE.equals(result.get()), "Save refresh must succeed");
		assertTrue(!fixture.child.needsSaving(), "Save refresh must clear the dirty child state");
		Project persisted = loadProject(new File(fixture.child.getFileName()));
		assertTrue(hasTaskNamed(persisted, "unsaved child edit"),
				"Save refresh must persist the edited child task");
	}

	@Test
	void robotDiscardsDirtyChildDuringRefreshAndRestoresDiskProjection() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture();
		show(fixture);
		GuiAcceptanceSupport.await(() -> window.isShowing(), "master document window was not visible");
		fixture.manager.selectForTest(fixture.document);
		fixture.child.setDirty(true);
		fixture.child.setGroupDirty(true);
		AtomicBoolean completed = new AtomicBoolean();
		AtomicReference<Boolean> result = new AtomicReference<>();
		SwingUtilities.invokeLater(() -> { result.set(fixture.manager.refreshLinkedSubproject(fixture.reference)); completed.set(true); });
		GuiAcceptanceSupport.await(() -> findRefreshDialog() != null || completed.get(),
				"refresh did not show its decision dialog or complete");
		assertTrue(!completed.get(), "Discard refresh returned before prompting; child dirty=" + fixture.child.needsSaving());
		clickRefreshChoice("Discard");
		awaitCompletion(completed, "Discard did not complete the refresh decision");
		assertTrue(Boolean.TRUE.equals(result.get()), "Discard refresh must succeed; status="
				+ fixture.reference.getLoadStatus() + ", file=" + fixture.reference.getSubprojectFile());
		assertNotSame(fixture.child, fixture.reference.getSubproject(), "Discard must replace the in-memory child model");
		assertTrue(hasTaskNamed(fixture.reference.getSubproject(), "persisted child"),
				"Discard refresh must restore the task name from disk");
		assertTrue(!fixture.reference.getSubproject().needsSaving(), "Discard refresh must clear the replaced child dirty state");
	}

	private Fixture createFixture() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project master = Project.createProject(ResourcePool.createRourcePool("refresh-master-pool", undo), undo);
		master.initialize(false, false);
		master.setName("Refresh master");
		master.setMaster(true);
		Project child = Project.createProject(ResourcePool.createRourcePool("refresh-child-pool", undo), undo);
		child.initialize(false, false);
		child.setName("Dirty linked child");
		child.setLocal(false);
		NormalTask childTask = child.createScriptedTask();
		childTask.setName("persisted child");
		File childFile = File.createTempFile("msp-refresh-child-", ".mpo");
		childFile.deleteOnExit();
		child.setFileName(childFile.getAbsolutePath());
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(childFile.getAbsolutePath());
		writer.setProject(child);
		writer.exportFile();
		childTask.setName("unsaved child edit");
		DefaultSubProj reference = new DefaultSubProj(master, child.getUniqueId());
		reference.setName("Dirty linked child");
		reference.setSubprojectFile(child.getFileName());
		Node referenceNode = NodeFactory.getInstance().createNode(reference);
		master.addToDefaultOutline(null, referenceNode);
		new DefaultSubprojectHandler(master).addSubproject(child, referenceNode, true, false);
		ProjectFactory.getInstance().addProject(child, false, true);
		child.setDirty(true);
		child.setGroupDirty(true);
		return new Fixture(master, child, childTask, reference);
	}

	private static void clickRefreshChoice(String text) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Dialog dialog = findRefreshDialog();
			if (dialog == null) throw new AssertionError("Refresh decision dialog disappeared");
			JButton button = findButton(dialog, text);
			if (button == null) throw new AssertionError("Refresh decision has no " + text + " button");
			button.doClick();
		});
	}

	private static void awaitCompletion(AtomicBoolean completed, String message) throws Exception {
		long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
		while (!completed.get() && System.nanoTime() < deadline) Thread.sleep(25);
		assertTrue(completed.get(), message);
	}

	private static JButton findButton(java.awt.Container container, String text) {
		for (java.awt.Component component : container.getComponents()) {
			if (component instanceof JButton button && text.equals(button.getText())) return button;
			if (component instanceof java.awt.Container child) {
				JButton button = findButton(child, text);
				if (button != null) return button;
			}
		}
		return null;
	}

	private static Project loadProject(File file) throws Exception {
		MpoFileImporter importer = new MpoFileImporter();
		importer.setFileName(file.getAbsolutePath());
		importer.importFile();
		return importer.getProject();
	}

	private static String firstTaskName(Project project) {
		for (java.util.Iterator<?> iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Object value = iterator.next();
			if (value instanceof NormalTask task) return task.getName();
		}
		return "";
	}

	private static boolean hasTaskNamed(Project project, String name) {
		for (java.util.Iterator<?> iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Object value = iterator.next();
			if (value instanceof NormalTask task && name.equals(task.getName())) return true;
		}
		return false;
	}

	private void show(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			window = new JFrame("microProject — Unsaved linked-child refresh GUI acceptance");
			RefreshGraphicManager manager = new RefreshGraphicManager(window);
			fixture.manager = manager;
			frameManager = new DefaultFrameManager(window, new JPanel(), manager);
			manager.setFrameManager(frameManager);
			DocumentFrame document = new FixtureDocumentFrame(manager, fixture.master, "refresh-master");
			fixture.document = document;
			frameManager.addFrame(document);
			manager.selectForTest(document);
			window.setSize(800, 460);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});
	}

	private static Dialog findRefreshDialog() {
		for (Window candidate : Window.getWindows())
			if (candidate instanceof Dialog dialog && dialog.isShowing() && "Refresh Linked Project".equals(dialog.getTitle()))
				return dialog;
		return null;
	}

	private void capture(Robot robot, Dialog dialog) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(window.getBounds()).union(dialog.getBounds()));
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"),
				"unsaved-linked-subproject-refresh-cancel.png");
		Files.createDirectories(artifact.getParent());
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		ImageIO.write(image, "png", artifact.toFile());
	}

	private static final class RefreshGraphicManager extends GraphicManager {
		private static final long serialVersionUID = 1L;
		RefreshGraphicManager(JFrame window) { super(window); }
		void selectForTest(DocumentFrame document) {
			try {
				Field currentFrame = GraphicManager.class.getDeclaredField("currentFrame");
				currentFrame.setAccessible(true);
				currentFrame.set(this, document);
			} catch (ReflectiveOperationException e) {
				throw new AssertionError("could not install the active master fixture", e);
			}
		}
	}

	private static final class FixtureDocumentFrame extends DocumentFrame {
		private static final long serialVersionUID = 1L;
		FixtureDocumentFrame(GraphicManager manager, Project project, String id) {
			super(manager, project, id);
			removeAll();
			setLayout(new BorderLayout());
			add(new JLabel("Master is active; refresh dirty linked child", SwingConstants.CENTER), BorderLayout.CENTER);
		}
		@Override public void activateResourceView() { }
		@Override public void activateGanttView() { }
	}

	private static final class Fixture {
		private final Project master;
		private final Project child;
		private final NormalTask childTask;
		private final DefaultSubProj reference;
		private RefreshGraphicManager manager;
		private DocumentFrame document;
		Fixture(Project master, Project child, NormalTask childTask, DefaultSubProj reference) {
			this.master = master;
			this.child = child;
			this.childTask = childTask;
			this.reference = reference;
		}
	}
}
