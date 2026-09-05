/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.nio.file.AccessDeniedException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.collaboration.ProjectMergeService;
import com.microproject.pm.graphic.frames.workspace.DefaultFrameManager;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.task.DefaultSubprojectHandler;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.SubProj;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/** GUI-MSP-RECOVERY-02: invalid children show their importer error and retain the master projection. */
class InvalidLinkedSubprojectRefreshGuiAcceptanceTest {
	private JFrame window;
	private DefaultFrameManager frameManager;
	private GraphicManager graphicManager;
	private GraphicManager previousGraphicManager;
	private Path invalidChildFile;
	private boolean previousClientSide;

	@AfterEach
	void closeWindow() throws Exception {
		if (graphicManager != null) {
			SwingUtilities.invokeAndWait(() -> graphicManager.cleanUp());
			restoreLastGraphicManager(previousGraphicManager);
		}
		if (window != null) SwingUtilities.invokeAndWait(() -> window.dispose());
		if (invalidChildFile != null) Files.deleteIfExists(invalidChildFile);
		Environment.setClientSide(previousClientSide);
	}

	@Test
	void robotShowsInvalidImportDetailsAndRetainsTheMasterProjection() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousClientSide = Environment.isClientSide();
		Environment.setClientSide(true);
		Fixture fixture = createFixture();
		show(fixture);
		GuiAcceptanceSupport.await(() -> window.isShowing(), "master document window was not visible");

		AtomicBoolean completed = new AtomicBoolean();
		AtomicReference<Boolean> refreshResult = new AtomicReference<Boolean>();
		SwingUtilities.invokeLater(() -> { refreshResult.set(fixture.manager.refreshLinkedSubproject(fixture.reference)); completed.set(true); });
		GuiAcceptanceSupport.await(() -> findWarningDialog() != null || completed.get(), "invalid linked child did not show a recovery warning or return");
		assertFalse(completed.get(), "refresh returned before displaying the invalid-file warning");
		Dialog dialog = findWarningDialog();
		assertTrue(dialog.isShowing());
		String warningText = dialogText(dialog);
		assertTrue(warningText.contains("INVALID_FILE"), () -> "warning did not identify the invalid file status: title="
			+ dialog.getTitle() + ", text=" + warningText + ", tree=" + dialogTree(dialog));
		assertTrue(warningText.contains("Details:"), () -> "warning did not include importer details: " + warningText);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		capture(robot, dialog);
		clickDismissButton(robot, dialog);
		GuiAcceptanceSupport.await(completed::get, "Escape did not dismiss the invalid-file warning");

		assertFalse(Boolean.TRUE.equals(refreshResult.get()));
		assertEquals(SubProj.LoadStatus.INVALID, fixture.reference.getLoadStatus());
		assertSame(fixture.child, fixture.reference.getSubproject());
		assertSame(fixture.master, fixture.reference.getProject());
	}

	@Test
	void robotShowsAccessDeniedAndRetainsTheMasterProjection() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousClientSide = Environment.isClientSide();
		Environment.setClientSide(true);
		Fixture fixture = createFixture();
		show(fixture);
		fixture.manager.forceLoadFailure(ProjectMergeService.LoadStatus.ACCESS_DENIED,
			new AccessDeniedException("locked-linked-child.mpo"));
		AtomicBoolean completed = new AtomicBoolean();
		AtomicReference<Boolean> refreshResult = new AtomicReference<Boolean>();
		SwingUtilities.invokeLater(() -> { refreshResult.set(fixture.manager.refreshLinkedSubproject(fixture.reference)); completed.set(true); });
		GuiAcceptanceSupport.await(() -> findWarningDialog() != null || completed.get(), "access-denied linked child did not show a recovery warning or return");
		assertFalse(completed.get(), "refresh returned before displaying the access-denied warning");
		Dialog dialog = findWarningDialog();
		String warningText = dialogText(dialog);
		assertTrue(warningText.contains("ACCESS_DENIED"), warningText);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		capture(robot, dialog, "access-denied-linked-subproject-refresh-warning.png");
		clickDismissButton(robot, dialog);
		GuiAcceptanceSupport.await(completed::get, "Escape did not dismiss the access-denied warning");
		assertFalse(Boolean.TRUE.equals(refreshResult.get()));
		assertEquals(SubProj.LoadStatus.ACCESS_DENIED, fixture.reference.getLoadStatus());
		assertSame(fixture.child, fixture.reference.getSubproject());
		assertSame(fixture.master, fixture.reference.getProject());
	}

	private Fixture createFixture() throws Exception {
		invalidChildFile = Files.createTempFile("invalid-linked-child-", ".mpo");
		Files.writeString(invalidChildFile, "not an MPOF ZIP archive");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project master = Project.createProject(ResourcePool.createRourcePool("invalid-refresh-master-pool", undo), undo);
		master.initialize(false, false);
		master.setName("Invalid refresh master");
		master.setMaster(true);
		Project child = Project.createProject(ResourcePool.createRourcePool("invalid-refresh-child-pool", undo), undo);
		child.initialize(false, false);
		child.setName("Broken child schedule");
		child.setFileName(invalidChildFile.toString());
		child.setLocal(false);
		DefaultSubProj reference = new FixtureSubProj(master, child.getUniqueId(), child);
		reference.setName(child.getName());
		reference.setSubprojectFile(invalidChildFile.toString());
		Node referenceNode = NodeFactory.getInstance().createNode(reference);
		master.addToDefaultOutline(null, referenceNode);
		new DefaultSubprojectHandler(master).addSubproject(child, referenceNode, true, false);
		child.setGroupDirty(false);
		return new Fixture(master, child, reference);
	}

	private void show(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			previousGraphicManager = lastGraphicManager();
			window = new JFrame("microProject — Invalid linked-child refresh GUI acceptance");
			InvalidRefreshGraphicManager manager = new InvalidRefreshGraphicManager(window);
			graphicManager = manager;
			fixture.manager = manager;
			frameManager = new DefaultFrameManager(window, new JPanel(), manager);
			manager.setFrameManager(frameManager);
			DocumentFrame document = new FixtureDocumentFrame(manager, fixture.master, "invalid-refresh-master");
			frameManager.addFrame(document);
			manager.selectForTest(document);
			window.setSize(800, 460);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});
	}

	private static GraphicManager lastGraphicManager() {
		try {
			Field field = GraphicManager.class.getDeclaredField("lastGraphicManager");
			field.setAccessible(true);
			return (GraphicManager) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("could not preserve the prior graphic manager", e);
		}
	}

	private static void restoreLastGraphicManager(GraphicManager manager) {
		try {
			Field field = GraphicManager.class.getDeclaredField("lastGraphicManager");
			field.setAccessible(true);
			field.set(null, manager);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("could not restore the prior graphic manager", e);
		}
	}

	private static Dialog findWarningDialog() {
		for (Window candidate : Window.getWindows())
			if (candidate instanceof Dialog dialog && dialog.isShowing()) return dialog;
		return null;
	}

	private static String dialogText(java.awt.Container container) {
		StringBuilder text = new StringBuilder();
		for (java.awt.Component component : container.getComponents()) {
			if (component instanceof javax.swing.JLabel label) text.append(label.getText());
			if (component instanceof JTextComponent textComponent) text.append(textComponent.getText());
			if (component instanceof java.awt.Container child) text.append(dialogText(child));
		}
		return text.toString();
	}

	private static String dialogTree(java.awt.Container container) {
		StringBuilder tree = new StringBuilder();
		for (java.awt.Component component : container.getComponents()) {
			tree.append(component.getClass().getSimpleName()).append(';');
			if (component instanceof java.awt.Container child) tree.append(dialogTree(child));
		}
		return tree.toString();
	}

	private static void clickDismissButton(Robot robot, Dialog dialog) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			javax.swing.JButton button = findDismissButton(dialog);
			if (button == null) throw new AssertionError("recovery warning has no dismissal button");
			java.awt.Point point = button.getLocationOnScreen();
			bounds[0] = new Rectangle(point.x, point.y, button.getWidth(), button.getHeight());
		});
		robot.mouseMove(bounds[0].x + bounds[0].width / 2, bounds[0].y + bounds[0].height / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
	}

	private static javax.swing.JButton findDismissButton(java.awt.Container container) {
		for (java.awt.Component component : container.getComponents()) {
			if (component instanceof javax.swing.JButton button) return button;
			if (component instanceof java.awt.Container child) {
				javax.swing.JButton button = findDismissButton(child);
				if (button != null) return button;
			}
		}
		return null;
	}

	private void capture(Robot robot, Dialog dialog) throws Exception {
		capture(robot, dialog, "invalid-linked-subproject-refresh-warning.png");
	}

	private void capture(Robot robot, Dialog dialog, String fileName) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(window.getBounds()).union(dialog.getBounds()));
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"), fileName);
		Files.createDirectories(artifact.getParent());
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		ImageIO.write(image, "png", artifact.toFile());
	}

	private static final class InvalidRefreshGraphicManager extends GraphicManager {
		private static final long serialVersionUID = 1L;
		private ProjectMergeService.ApplyResult forcedLoadFailure;
		InvalidRefreshGraphicManager(JFrame window) { super(window); }
		void selectForTest(DocumentFrame document) {
			try {
				Field currentFrame = GraphicManager.class.getDeclaredField("currentFrame");
				currentFrame.setAccessible(true);
				currentFrame.set(this, document);
			} catch (ReflectiveOperationException e) { throw new AssertionError("could not install the active master fixture", e); }
		}
		void forceLoadFailure(ProjectMergeService.LoadStatus status, Exception cause) {
			forcedLoadFailure = ProjectMergeService.failedLoad(status, cause);
		}
		@Override protected ProjectMergeService projectMergeService() {
			ProjectMergeService.ApplyResult forced = forcedLoadFailure;
			if (forced == null) return super.projectMergeService();
			return new ProjectMergeService() {
				@Override public ApplyResult applyExternalTaskUpdates(Project target, String fileName, java.util.Set<Long> lockedTaskIds) {
					return forced;
				}
			};
		}
	}

	private static final class FixtureDocumentFrame extends DocumentFrame {
		private static final long serialVersionUID = 1L;
		FixtureDocumentFrame(GraphicManager manager, Project project, String id) {
			super(manager, project, id);
			removeAll(); setLayout(new BorderLayout());
			add(new JLabel("Master projection remains available after invalid child refresh", SwingConstants.CENTER), BorderLayout.CENTER);
		}
		@Override public void activateResourceView() { }
		@Override public void activateGanttView() { }
	}

	/** Keeps the refresh fixture on the already projected child rather than the global registry. */
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
		private final DefaultSubProj reference;
		private InvalidRefreshGraphicManager manager;
		Fixture(Project master, Project child, DefaultSubProj reference) { this.master = master; this.child = child; this.reference = reference; }
	}
}
