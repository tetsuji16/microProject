/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Point;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import javax.imageio.ImageIO;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.exchange.ResourceMappingForm;
import com.microproject.strings.Messages;
import com.microproject.testsupport.DialogLayoutAssertions;
import com.microproject.testsupport.GuiAcceptanceSupport;

/** Captures the Resource Mapping popup and checks its screen/resize contract (#724). */
class ResourceMappingDialogGuiAcceptanceTest {
	private ResourceMappingDialog dialog;

	@AfterEach
	void disposeDialog() throws Exception {
		if (dialog != null)
			SwingUtilities.invokeAndWait(dialog::dispose);
	}

	@Test
	void resourceMappingDialogFitsAndRemainsUsableWhenResized() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		ResourceMappingForm form = createForm();
		SwingUtilities.invokeAndWait(() -> {
			dialog = ResourceMappingDialog.getInstance(form);
			dialog.pack();
			dialog.bind(true);
			dialog.setLocationRelativeTo(null);
			JTable associationTable = dialog.associationTable;
			assertEquals(Messages.getString("ResourceMappingDialog.ProjectResources"),
				associationTable.getColumnModel().getColumn(1).getHeaderValue());
			assertEquals(Messages.getString("ResourceMappingDialog.OpenReadOnlyWithoutMerging"),
				dialog.localProject.getText());
		});
		SwingUtilities.invokeLater(() -> dialog.setVisible(true));
		GuiAcceptanceSupport.await(() -> dialog.isShowing(), "Resource Mapping dialog did not render");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog.getContentPane(),
			"Resource Mapping dialog (#724)");
		DialogLayoutAssertions.assertWithinUsableScreen(dialog, "Resource Mapping dialog (#724)");
		DialogLayoutAssertions.assertResizeKeepsTextControls(dialog, dialog.getContentPane(), 100, 40,
			"Resource Mapping dialog (#724)");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.waitForIdle();
		activateDialog(robot);
		Rectangle bounds = dialog.getBounds();
		BufferedImage screenshot = robot.createScreenCapture(bounds);
		Path artifactDirectory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(artifactDirectory);
		ImageIO.write(screenshot, "png", artifactDirectory.resolve("resource-mapping-dialog.png").toFile());

		ClickTarget readOnlyTarget = robotTargetFor(dialog.localProject);
		List<String> readOnlyAttempts = clickUntil(robot, readOnlyTarget, form::isLocal);
		SwingUtilities.invokeAndWait(() -> {
			JTable associationTable = dialog.associationTable;
			assertTrue(form.isLocal(), "The read-only choice must stay on the local project route; "
				+ readOnlyTarget.diagnostic(readOnlyAttempts));
			assertFalse(dialog.field1.isEnabled(), "Read-only mode must disable resource merge-field editing");
			assertFalse(associationTable.isEnabled(), "Read-only mode must disable resource associations");
		});

		ClickTarget cancelTarget = robotTargetFor(dialog.cancel);
		List<String> cancelAttempts = clickUntil(robot, cancelTarget, () -> !dialog.isShowing());
		assertFalse(dialog.isShowing(), "Cancel did not close Resource Mapping dialog; "
			+ cancelTarget.diagnostic(cancelAttempts));
		assertTrue(form.getSelectedResources().size() == 1,
			"Capturing and resizing the dialog must preserve its imported-resource mapping state");
	}

	private void activateDialog(Robot robot) throws Exception {
		ClickTarget target = robotTargetFor(dialog);
		List<String> attempts = clickUntil(robot, target, dialog::isActive);
		assertTrue(dialog.isActive(), "Resource Mapping dialog did not receive the physical activation click; "
			+ target.diagnostic(attempts));
	}

	private static ClickTarget robotTargetFor(Component component) throws Exception {
		Object[] values = new Object[2];
		SwingUtilities.invokeAndWait(() -> {
			Window window = component instanceof Window targetWindow
				? targetWindow : SwingUtilities.getWindowAncestor(component);
			Point componentLocation = component.getLocationOnScreen();
			Rectangle bounds = new Rectangle(componentLocation, component.getSize());
			Point logicalTarget = new Point((int) Math.round(bounds.getCenterX()),
				(int) Math.round(bounds.getCenterY()));
			var graphicsConfiguration = window.getGraphicsConfiguration();
			AffineTransform transform = graphicsConfiguration.getDefaultTransform();
			Point2D physical = transform.transform(logicalTarget, null);
			Point transformedTarget = new Point((int) Math.round(physical.getX()), (int) Math.round(physical.getY()));
			Rectangle physicalScreen = transform.createTransformedShape(graphicsConfiguration.getBounds())
				.getBounds();
			values[1] = new ClickTarget(bounds, logicalTarget, transform, transformedTarget, physicalScreen);
		});
		return (ClickTarget) values[1];
	}

	private static List<String> clickUntil(Robot robot, ClickTarget target, BooleanSupplier completed)
		throws Exception {
		List<String> attempts = new ArrayList<>();
		for (Point candidate : List.of(target.transformed(), target.logical())) {
			robot.mouseMove(candidate.x, candidate.y);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.waitForIdle();
			boolean done[] = new boolean[1];
			SwingUtilities.invokeAndWait(() -> done[0] = completed.getAsBoolean());
			Point pointer = MouseInfo.getPointerInfo() == null ? null : MouseInfo.getPointerInfo().getLocation();
			attempts.add("candidate=" + candidate + ", pointer=" + pointer + ", completed=" + done[0]);
			if (done[0]) break;
		}
		return attempts;
	}

	private record ClickTarget(Rectangle logicalBounds, Point logical, AffineTransform transform, Point transformed,
			Rectangle physicalScreen) {
		String diagnostic(List<String> attempts) {
			return "logicalBounds=" + logicalBounds + ", logicalTarget=" + logical
				+ ", transformedTarget=" + transformed
				+ ", defaultTransform=" + transform + ", physicalScreenBounds=" + physicalScreen
				+ ", attempts=" + attempts;
		}
	}

	private static ResourceMappingForm createForm() {
		Resource resource = new Resource("Shared engineer");
		Resource imported = new Resource("Shared engineer");
		ResourceMappingForm form = new ResourceMappingForm() {
			@Override
			public boolean execute() {
				return true;
			}
		};
		form.setResources(new ArrayList<>(List.of(resource)));
		form.setImportedResources(List.of(imported));
		form.setUnassignedResource(new Resource("Unassigned"));
		form.addMergeField(new ResourceMappingForm.MergeField("name", "name", "Name"));
		form.setMergeField(form.getMergeFields().get(1));
		return form;
	}

	public static final class Resource {
		private final String name;

		Resource(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
