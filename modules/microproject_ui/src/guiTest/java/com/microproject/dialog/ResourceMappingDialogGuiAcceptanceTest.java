/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.microproject.testsupport.DialogLayoutAssertions.physicallyClick;
import static com.microproject.testsupport.DialogLayoutAssertions.physicallyScrollIntoView;

import java.awt.GraphicsEnvironment;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
		physicallyScrollIntoView(robot, dialog.localProject, "Read-only local project choice");
		assertReadOnlyChoiceVisible();
		Rectangle bounds = dialog.getBounds();
		BufferedImage screenshot = robot.createScreenCapture(bounds);
		Path artifactDirectory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(artifactDirectory);
		ImageIO.write(screenshot, "png", artifactDirectory.resolve("resource-mapping-dialog.png").toFile());

		physicallyClick(robot, dialog.localProject, "Read-only local project choice");
		GuiAcceptanceSupport.await(form::isLocal, "The read-only choice did not select the local project route");
		SwingUtilities.invokeAndWait(() -> {
			JTable associationTable = dialog.associationTable;
			assertTrue(form.isLocal(), "The read-only choice must stay on the local project route");
			assertFalse(dialog.field1.isEnabled(), "Read-only mode must disable resource merge-field editing");
			assertFalse(associationTable.isEnabled(), "Read-only mode must disable resource associations");
		});

		clickAtComponentCenter(robot, dialog.cancel);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "Cancel did not close Resource Mapping dialog");
		assertFalse(dialog.isShowing(), "Cancel did not close Resource Mapping dialog");
		assertTrue(form.getSelectedResources().size() == 1,
			"Capturing and resizing the dialog must preserve its imported-resource mapping state");
	}

	private void activateDialog(Robot robot) throws Exception {
		physicallyClick(robot, dialog.associationTable, "Resource Mapping association table activation");
		GuiAcceptanceSupport.await(dialog::isActive, "Resource Mapping dialog did not receive the physical activation click");
		assertTrue(dialog.isActive(), "Resource Mapping dialog did not receive the physical activation click");
	}

	private static void clickAtComponentCenter(Robot robot, Component component) throws Exception {
		java.awt.Point[] screenPoint = new java.awt.Point[1];
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Point location = component.getLocationOnScreen();
			screenPoint[0] = new java.awt.Point(location.x + component.getWidth() / 2,
				location.y + component.getHeight() / 2);
		});
		robot.mouseMove(screenPoint[0].x, screenPoint[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private void assertReadOnlyChoiceVisible() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Rectangle checkbox = new Rectangle(0, 0, dialog.localProject.getWidth(), dialog.localProject.getHeight());
			assertTrue(dialog.localProject.isShowing() && dialog.localProject.getVisibleRect().contains(checkbox),
				"Read-only local project choice must be visible before screenshot and activation; bounds="
					+ dialog.localProject.getBounds() + " visibleRect=" + dialog.localProject.getVisibleRect());
		});
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
