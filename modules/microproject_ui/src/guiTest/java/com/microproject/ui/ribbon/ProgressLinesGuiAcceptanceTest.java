/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Component;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.pm.task.ProgressLineSettings;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.task.Project;
import com.microproject.util.DateTime;
import com.microproject.testsupport.GuiJourney;
import com.microproject.testsupport.GuiAcceptanceSupport;

/** Physical dialog journey for the Progress Lines ribbon command. */
class ProgressLinesGuiAcceptanceTest {
	@Test
	@GuiJourney(GuiJourney.InputMechanism.DIALOG)
	void progressLinesDialogAppliesCurrentAndRecurringSettings() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required");
		try (RibbonCommandGuiFixture fixture = RibbonCommandGuiFixture.open(3)) {
			SwingUtilities.invokeAndWait(() -> {
				var project = fixture.manager.getCurrentFrame().getProject();
				long start = DateTime.midnightToday();
				project.setStartDate(start);
				project.setStatusDate(start + 4L * 86_400_000L);
				project.setCurrentDate(start + 5L * 86_400_000L);
				int index = 0;
				for (var task : project.getTasks()) {
					task.setStart(start + index * 2L * 86_400_000L);
					task.setEnd(start + (index * 2L + 3L) * 86_400_000L);
					task.setPercentComplete(0.5d);
					index++;
				}
			});
			BufferedImage beforeRender = captureGantt(fixture);
			AbstractButton command = fixture.showCommand("RibbonToggleProgressLine");
			assertNotNull(command, "Progress Lines must be available on the contextual Gantt Format tab");
			Set<Window> before = fixture.showingWindows();
			fixture.click(command);
			GuiAcceptanceSupport.await(() -> {
				try { return fixture.newWindow(before) != null; }
				catch (Exception failure) { throw new AssertionError(failure); }
			}, "the physical Progress Lines command did not open its settings dialog");
			Window opened = fixture.newWindow(before);
			assertTrue(opened instanceof Dialog, "Progress Lines must open a dialog");
			Dialog dialog = (Dialog) opened;
			List<JCheckBox> checkboxes = UiComponentWalker.flatten(dialog).stream()
				.filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast).toList();
			List<JRadioButton> referenceDates = UiComponentWalker.flatten(dialog).stream()
				.filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).toList();
			JButton okButton = UiComponentWalker.flatten(dialog).stream()
				.filter(JButton.class::isInstance).map(JButton.class::cast)
				.filter(button -> "progress-lines-ok".equals(button.getName())).findFirst().orElse(null);
			assertEquals(2, checkboxes.size(), "dialog exposes current/reference and recurring-line choices");
			assertEquals(2, referenceDates.size(), "dialog exposes status/current date reference choices");
			assertNotNull(okButton, "dialog exposes an explicit OK button");
			fixture.click(checkboxes.get(0));
			fixture.click(referenceDates.get(1));
			fixture.click(checkboxes.get(1));
			fixture.click(okButton);
			GuiAcceptanceSupport.await(() -> {
				try { return !fixture.isShowing(dialog); }
				catch (Exception failure) { throw new AssertionError(failure); }
			}, "the Progress Lines dialog did not accept the settings");

			var expected = new ProgressLineSettings(true, ProgressLineSettings.ReferenceDate.CURRENT_DATE,
				true, 1, ProgressLineSettings.IntervalUnit.WEEK);
			var project = fixture.manager.getCurrentFrame().getProject();
			assertEquals(expected, project.getProgressLineSettings(), "physical choices update the project display model");
			assertTrue(fixture.manager.getCurrentFrame().getGanttView().getGantt().isProgressLineEnabled(),
				"the Gantt must enable the configured reference/recurring lines");
			BufferedImage afterRender = captureGantt(fixture);
			assertTrue(changedPixelCount(beforeRender, afterRender) > 0,
				"the configured progress lines must change the rendered Gantt image");
			SwingUtilities.invokeAndWait(() -> {
				project.getUndoController().undo();
				assertNullProgressSettings(project.getProgressLineSettings());
				project.getUndoController().redo();
				assertEquals(expected, project.getProgressLineSettings());
			});
			java.nio.file.Path savedMpo = fixture.reserveSavePath("progress-lines.mpo");
			fixture.routeSaveChooserTo(savedMpo);
			AbstractButton save = fixture.showCommand("RibbonSaveProject");
			assertNotNull(save, "save command must be available for an in-memory project");
			fixture.click(save);
			GuiAcceptanceSupport.await(() -> java.nio.file.Files.exists(savedMpo) && !project.needsSaving(),
				"the Ribbon Save command did not persist the MPO settings");

			DocumentFrame originalFrame = fixture.manager.getCurrentFrame();
			fixture.manager.closeDocumentWindow(originalFrame);
			GuiAcceptanceSupport.await(() -> !fixture.manager.getFrameManager().getDocumentFrames().contains(originalFrame),
				"the saved document frame did not close before reopening");
			Project reopened = fixture.loadProject(savedMpo);
			DocumentFrame reopenedFrame = fixture.manager.addProjectFrame(reopened);
			assertEquals(expected, reopened.getProgressLineSettings(), "normal MPO save/reload retains progress-line settings");
			assertTrue(reopenedFrame.getGanttView().getGantt().isProgressLineEnabled(),
				"a new DocumentFrame must derive visible progress lines from the persisted settings");
			assertTrue(changedPixelCount(beforeRender, captureGantt(fixture)) > 0,
				"reopened Gantt must render the persisted progress lines");
			assertFalse(fixture.isShowing(dialog));
		}
	}

	private static BufferedImage captureGantt(RibbonCommandGuiFixture fixture) throws Exception {
		BufferedImage[] result = new BufferedImage[1];
		SwingUtilities.invokeAndWait(() -> {
			Component gantt = fixture.manager.getCurrentFrame().getGanttView().getGantt();
			result[0] = new BufferedImage(gantt.getWidth(), gantt.getHeight(), BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D graphics = result[0].createGraphics();
			try { gantt.printAll(graphics); }
			finally { graphics.dispose(); }
		});
		return result[0];
	}

	private static long changedPixelCount(BufferedImage before, BufferedImage after) {
		long changed = 0L;
		for (int y = 0; y < Math.min(before.getHeight(), after.getHeight()); y++)
			for (int x = 0; x < Math.min(before.getWidth(), after.getWidth()); x++)
				if (before.getRGB(x, y) != after.getRGB(x, y)) changed++;
		return changed;
	}

	private static void assertNullProgressSettings(ProgressLineSettings settings) {
		assertTrue(settings == null, "Undo restores the document's legacy unset state");
	}
}
