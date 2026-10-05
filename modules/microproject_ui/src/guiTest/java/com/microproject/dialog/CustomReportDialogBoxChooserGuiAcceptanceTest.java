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
import java.awt.Robot;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.util.UiServices;

/** Exercises Custom Report's physical export route through the shared file chooser (#759). */
class CustomReportDialogBoxChooserGuiAcceptanceTest {
	private CustomReportDialogBox dialog;
	private UiServices.FileChooserProvider previousChooser;
	private Path exportDirectory;

	@AfterEach
	void closeDialogAndRestoreChooser() throws Exception {
		if (dialog != null)
			SwingUtilities.invokeAndWait(dialog::dispose);
		UiServices.setFileChooserProvider(previousChooser);
		if (exportDirectory != null && Files.exists(exportDirectory)) {
			try (var files = Files.list(exportDirectory)) {
				for (Path file : files.toList())
					Files.deleteIfExists(file);
			}
			Files.deleteIfExists(exportDirectory);
		}
	}

	@Test
	void robotExportsCsvThroughSharedChooserAndCancelCreatesNoFile() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for report chooser acceptance coverage.");
		previousChooser = UiServices.getFileChooserProvider();
		exportDirectory = Files.createTempDirectory("custom-report-chooser-");
		Path csv = exportDirectory.resolve("custom-report.csv");
		Path cancelled = exportDirectory.resolve("cancelled-report.csv");
		AtomicInteger selection = new AtomicInteger();
		List<UiServices.FileChooserOptions> requestedOptions = new CopyOnWriteArrayList<>();
		Project project = CriticalChainStatusDialogGuiAcceptanceTest.newProjectWithTasks();
		SwingUtilities.invokeAndWait(() -> {
			dialog = new CustomReportDialogBox(null, project);
			dialog.setVisible(true);
		});
		// FlatLafDialog initializes the application theme in its constructor, which
		// installs the production provider. Override it only after that initialization.
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override
			public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				throw new AssertionError("Custom Report must use the specialized shared chooser contract");
			}

			@Override
			public String chooseFileName(UiServices.FileChooserOptions options, Object parent) {
				requestedOptions.add(options);
				return selection.getAndIncrement() == 0 ? csv.toString() : null;
			}
		});
		GuiAcceptanceSupport.await(() -> dialog.isShowing(), "Custom Report dialog did not open");
		GuiAcceptanceSupport.await(() -> dialog.isActive(), "Custom Report dialog did not become active");
		SwingUtilities.invokeAndWait(() -> { dialog.setAlwaysOnTop(true); dialog.toFront(); dialog.requestFocus(); });

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		clickExport(robot);
		GuiAcceptanceSupport.await(() -> Files.exists(csv), "Custom Report chooser approval did not write the CSV");
		assertEquals(new UiServices.FileChooserOptions(true, "project-report.csv", "Export custom report", "CSV (*.csv)", "csv"),
			requestedOptions.get(0), "Custom Report must request the shared CSV save contract");
		byte[] content = Files.readAllBytes(csv);
		assertTrue(content.length > 3, "approved report must have content");
		assertEquals(0xEF, content[0] & 0xff, "CSV must keep its UTF-8 BOM");
		assertTrue(new String(content, 3, content.length - 3, StandardCharsets.UTF_8).contains("Design"),
			"CSV must contain the fixture task produced by the existing report model");

		clickExport(robot);
		GuiAcceptanceSupport.await(() -> requestedOptions.size() == 2, "Custom Report did not route its second export through the shared chooser");
		assertEquals(requestedOptions.get(0), requestedOptions.get(1), "repeated export must preserve the shared CSV filter and defaults");
		assertFalse(Files.exists(cancelled), "a chooser cancellation must not create an output file");
	}

	private void clickExport(Robot robot) throws Exception {
		CriticalChainStatusDialogGuiAcceptanceTest.click(robot,
			CriticalChainStatusDialogGuiAcceptanceTest.findButton(dialog, UsabilityStrings.text("report.export")));
	}
}
