/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.graphic.frames.workspace.DefaultFrameManager;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.task.DefaultSubprojectHandler;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
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

	private Fixture createFixture() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project master = Project.createProject(ResourcePool.createRourcePool("refresh-master-pool", undo), undo);
		master.initialize(false, false);
		master.setName("Refresh master");
		master.setMaster(true);
		Project child = Project.createProject(ResourcePool.createRourcePool("refresh-child-pool", undo), undo);
		child.initialize(false, false);
		child.setName("Dirty linked child");
		child.setFileName("C:/plans/dirty-linked-child.mpo");
		child.setLocal(false);
		NormalTask childTask = child.createScriptedTask();
		childTask.setName("unsaved child edit");
		FixtureSubProj reference = new FixtureSubProj(master, child.getUniqueId(), child);
		reference.setName("Dirty linked child");
		reference.setSubprojectFile(child.getFileName());
		Node referenceNode = NodeFactory.getInstance().createNode(reference);
		master.addToDefaultOutline(null, referenceNode);
		new DefaultSubprojectHandler(master).addSubproject(child, referenceNode, true, false);
		child.setGroupDirty(true);
		return new Fixture(master, child, childTask, reference);
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

	/** Keeps this GUI fixture independent of the process-wide open-project registry. */
	private static final class FixtureSubProj extends DefaultSubProj {
		private static final long serialVersionUID = 1L;
		private final Project child;
		FixtureSubProj(Project master, long childId, Project child) {
			super(master, childId);
			this.child = child;
		}
		@Override public Project getSubproject() { return child; }
	}

	private static final class Fixture {
		private final Project master;
		private final Project child;
		private final NormalTask childTask;
		private final FixtureSubProj reference;
		private RefreshGraphicManager manager;
		private DocumentFrame document;
		Fixture(Project master, Project child, NormalTask childTask, FixtureSubProj reference) {
			this.master = master;
			this.child = child;
			this.childTask = childTask;
			this.reference = reference;
		}
	}
}
