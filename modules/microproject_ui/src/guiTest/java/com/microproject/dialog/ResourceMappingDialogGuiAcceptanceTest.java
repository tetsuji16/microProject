/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.exchange.ResourceMappingForm;
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
			dialog.setLocationByPlatform(true);
		});
		SwingUtilities.invokeLater(() -> dialog.setVisible(true));
		GuiAcceptanceSupport.await(() -> dialog.isShowing(), "Resource Mapping dialog did not render");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog.getContentPane(),
			"Resource Mapping dialog (#724)");
		DialogLayoutAssertions.assertWithinUsableScreen(dialog, "Resource Mapping dialog (#724)");
		DialogLayoutAssertions.assertResizeKeepsTextControls(dialog, dialog.getContentPane(), 100, 40,
			"Resource Mapping dialog (#724)");

		Robot robot = new Robot();
		robot.waitForIdle();
		activateDialog(robot);
		Rectangle bounds = dialog.getBounds();
		BufferedImage screenshot = robot.createScreenCapture(bounds);
		Path artifactDirectory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(artifactDirectory);
		ImageIO.write(screenshot, "png", artifactDirectory.resolve("resource-mapping-dialog.png").toFile());

		Point cancelLocation = dialog.cancel.getLocationOnScreen();
		robot.mouseMove(cancelLocation.x + dialog.cancel.getWidth() / 2,
			cancelLocation.y + dialog.cancel.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "Cancel did not close Resource Mapping dialog");
		assertTrue(form.getSelectedResources().size() == 1,
			"Capturing and resizing the dialog must preserve its imported-resource mapping state");
	}

	private void activateDialog(Robot robot) throws Exception {
		Rectangle bounds = dialog.getBounds();
		robot.mouseMove(bounds.x + 60, bounds.y + 14);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(dialog::isActive, "Resource Mapping dialog did not receive the physical activation click");
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
