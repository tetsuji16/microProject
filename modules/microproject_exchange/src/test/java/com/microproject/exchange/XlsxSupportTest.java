/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package test.com.microproject.exchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.Task;
import net.sf.mpxj.mspdi.MSPDIWriter;
import net.sf.mpxj.writer.ProjectWriter;
import com.microproject.exchange.mpxj.ProjectWriterFactory;
import com.microproject.exchange.xlsx.MicroProjectXlsxReader;

import com.microproject.collaboration.ProjectMergeService;
import com.microproject.exchange.MicrosoftImporter;
import com.microproject.exchange.BoundedInput;
import com.microproject.exchange.LocalFileImporter;
import com.microproject.exchange.xlsx.MicroProjectXlsxWriter;
import com.microproject.pm.task.NormalTask;
import com.microproject.core.pm.exchange.MspImporter;
import com.microproject.collaboration.CollaborationMetadataStore;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.server.data.MSPDISerializer;
import com.microproject.session.FileHelper;
import com.microproject.undo.DataFactoryUndoController;

public class XlsxSupportTest {
	@Test
	void xlsxInputReaderAcceptsExactLimitAndRejectsTheNextByte() throws Exception {
		String property = BoundedInput.XLSX_MAX_IMPORT_BYTES_PROPERTY;
		String previous = System.getProperty(property);
		try {
			System.setProperty(property, "4");
			assertEquals(4, BoundedInput.readXlsxImport(new ByteArrayInputStream(new byte[] {1, 2, 3, 4})).length);
			try {
				BoundedInput.readXlsxImport(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5}));
				fail("Input over the configured limit must be rejected");
			} catch (java.io.IOException expected) {
				assertTrue(expected.getMessage().contains("4 bytes"));
			}
		} finally {
			if (previous == null) {
				System.clearProperty(property);
			} else {
				System.setProperty(property, previous);
			}
		}
	}

	@Test
	void bothXlsxImportersRejectOverLimitBeforeParsing() throws Exception {
		String property = BoundedInput.XLSX_MAX_IMPORT_BYTES_PROPERTY;
		String previous = System.getProperty(property);
		try {
			System.setProperty(property, "4");
			try {
				new MspImporter().importProject(new ByteArrayInputStream(new byte[5]), "xlsx",
					(progress, label) -> {});
				fail("Direct importer must reject over-limit XLSX input");
			} catch (java.io.IOException expected) {
				assertTrue(expected.getMessage().contains("configured limit"));
			}

			MicrosoftImporter importer = new MicrosoftImporter();
			importer.setFileName("oversized.xlsx");
			final boolean[] streamClosed = {false};
			try {
				importer.loadProject(new ByteArrayInputStream(new byte[5]) {
					@Override
					public void close() throws java.io.IOException {
						streamClosed[0] = true;
						super.close();
					}
				});
				fail("Microsoft importer must reject over-limit XLSX input");
			} catch (java.io.IOException expected) {
				assertTrue(expected.getMessage().contains("configured limit"));
			}
			assertFalse(com.microproject.util.Environment.isImporting());
			assertTrue("rejected input must release the caller's stream", streamClosed[0]);

			System.setProperty(property, "invalid");
			final boolean[] invalidConfigStreamClosed = {false};
			try {
				new MicrosoftImporter().parse(new ByteArrayInputStream(new byte[1]) {
					@Override
					public void close() throws java.io.IOException {
						invalidConfigStreamClosed[0] = true;
						super.close();
					}
				}, "xlsx");
				fail("Invalid byte-limit configuration must be reported");
			} catch (IllegalArgumentException expected) {
				assertTrue(expected.getMessage().contains("microproject.xlsx.maxImportBytes"));
			}
			assertFalse(com.microproject.util.Environment.isImporting());
			assertTrue("invalid configuration must release the caller's stream", invalidConfigStreamClosed[0]);
			System.setProperty(property, "4");

			File oversizedFile = File.createTempFile("oversized-xlsx", ".xlsx");
			oversizedFile.deleteOnExit();
			try (FileOutputStream output = new FileOutputStream(oversizedFile)) {
				output.write(new byte[5]);
			}
			MicrosoftImporter fileImporter = new MicrosoftImporter();
			fileImporter.setFileName(oversizedFile.getAbsolutePath());
			try {
				fileImporter.importFile();
				fail("File-based Microsoft importer must reject over-limit XLSX input");
			} catch (java.io.IOException expected) {
				assertTrue(expected.getMessage().contains("configured limit"));
			}
			assertFalse(com.microproject.util.Environment.isImporting());
		} finally {
			if (previous == null) {
				System.clearProperty(property);
			} else {
				System.setProperty(property, previous);
			}
		}
	}

	@Test
	void malformedXlsxResetsImportingStateAndClosesInput() throws Exception {
		String property = BoundedInput.XLSX_MAX_IMPORT_BYTES_PROPERTY;
		String previous = System.getProperty(property);
		try {
			System.setProperty(property, "1024");
			MicrosoftImporter importer = new MicrosoftImporter();
			importer.setFileName("malformed.xlsx");
			final boolean[] streamClosed = {false};
			try {
				importer.loadProject(new ByteArrayInputStream(new byte[] {1, 2, 3, 4}) {
					@Override
					public void close() throws java.io.IOException {
						streamClosed[0] = true;
						super.close();
					}
				});
				fail("Malformed XLSX must fail import");
			} catch (Exception expected) {
				assertFalse("failure must occur before domain conversion", importer.getProject() != null);
			}
			assertTrue("import must close the caller's stream", streamClosed[0]);
			assertFalse("failed parsing must not leave the global importing flag set",
				com.microproject.util.Environment.isImporting());
		} finally {
			if (previous == null) {
				System.clearProperty(property);
			} else {
				System.setProperty(property, previous);
			}
		}
	}

	@Test
	void fileHelperAcceptsMppForReadOnlyImport() {
		assertTrue(FileHelper.isFileNameAllowed("plan.mpp", false));
		assertFalse(FileHelper.isFileNameAllowed("plan.mpp", true));
		assertEquals(FileHelper.MSP_FILE_TYPE, FileHelper.getFileType("plan.mpp"));
	}

	@Test
	void collaborationDoesNotRecognizeMpp() {
		assertFalse(CollaborationMetadataStore.isMpoCollaborationCandidate("plan.mpp"));
	}

	@Test
	void mspImporterCanReadMpp() throws Exception {
		File sample = new File("modules/microproject_exchange/testdata/New Product.mpp");
		if (!sample.exists()) {
			sample = new File("testdata/New Product.mpp");
		}
		assertTrue(sample.exists());

		MspImporter importer = new MspImporter();
		com.microproject.pm.task.Project imported = importer.importProject(sample.getAbsolutePath(), new MspImporter.ProgressClosure() {
			@Override
			public void updateProgress(float progress, String label) {
			}
		});

		assertNotNull(imported);
		assertTrue(imported.getTasks().size() > 0);
		assertTrue(imported.getTasks().stream()
			.anyMatch(task -> task instanceof com.microproject.pm.task.NormalTask normalTask
				&& !normalTask.getAssignments().isEmpty()));
	}

	@Test
	void fileHelperAcceptsXlsx() {
		assertTrue(FileHelper.isFileNameAllowed("plan.xlsx", true));
		assertTrue(FileHelper.isFileNameAllowed("plan.xlsx", false));
		assertEquals(FileHelper.MSP_FILE_TYPE, FileHelper.getFileType("plan.xlsx"));
	}

	@Test
	void collaborationDoesNotRecognizeXlsx() {
		assertFalse(CollaborationMetadataStore.isMpoCollaborationCandidate("plan.xlsx"));
	}

	@Test
	void projectWriterFactorySupportsXlsx() throws Exception {
		ProjectWriter writer = ProjectWriterFactory.forFile("plan.xlsx");
		assertNotNull(writer);
	}

	@Test
	void xlsxDependencyLagRoundTrip() throws Exception {
		// Issue #162: the Dependencies sheet Lag column was written but never read,
		// silently dropping every dependency lag on the summary-sheet fallback path.
		// Build a workbook containing only the summary sheets (no MSPDI payload)
		// so the reader must use readDependencies().
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			org.apache.poi.ss.usermodel.Sheet tasks = workbook.createSheet("Tasks");
			writeRow(tasks, 0, "UID", "ID", "ParentUID", "Name", "Notes");
			writeRow(tasks, 1, 1.0, 1.0, null, "Pred", null);
			writeRow(tasks, 2, 2.0, 2.0, null, "Succ", null);
			writeRow(tasks, 3, 3.0, 3.0, null, "OtherSucc", null);
			writeRow(tasks, 4, 4.0, 4.0, null, "PercentSucc", null);
			writeRow(tasks, 5, 5.0, 5.0, null, "ElapsedSucc", null);
			org.apache.poi.ss.usermodel.Sheet resources = workbook.createSheet("Resources");
			writeRow(resources, 0, "UID", "ID", "Name", "Notes", "Group", "Email", "MaxUnits");
			org.apache.poi.ss.usermodel.Sheet assignments = workbook.createSheet("Assignments");
			writeRow(assignments, 0, "TaskUID", "ResourceUID", "Units", "Delay", "LevelingDelay", "WorkContour");
			org.apache.poi.ss.usermodel.Sheet deps = workbook.createSheet("Dependencies");
			writeRow(deps, 0, "SuccessorUniqueID", "PredecessorUniqueID", "Type", "Lag");
			writeRow(deps, 1, 2.0, 1.0, 0.0, "2.0d");            // MPXJ toString form
			writeRow(deps, 2, 3.0, 1.0, 0.0, "-1.0h");           // negative lead
			writeRow(deps, 3, 4.0, 1.0, 0.0, "50.0%");           // MPXJ percent form
			writeRow(deps, 4, 5.0, 1.0, 0.0, "2.0ed");           // elapsed
			workbook.write(out);
		}

		ProjectFile reloaded = new MicroProjectXlsxReader().read(new ByteArrayInputStream(out.toByteArray()));
		Map<String, Task> byName = new HashMap<String, Task>();
		for (Task task : reloaded.getTasks()) {
			if (task.getName() != null) {
				byName.put(task.getName(), task);
			}
		}

		assertEquals(2L * 24L * 60L * 60L * 1000L,
				com.microproject.core.pm.exchange.converters.mpx.MpxUtils.toMillis(byName.get("Succ").getPredecessors().get(0).getLag()));
		assertEquals(-1L * 60L * 60L * 1000L,
				com.microproject.core.pm.exchange.converters.mpx.MpxUtils.toMillis(byName.get("OtherSucc").getPredecessors().get(0).getLag()));
		net.sf.mpxj.Duration percentLag = byName.get("PercentSucc").getPredecessors().get(0).getLag();
		assertEquals(net.sf.mpxj.TimeUnit.PERCENT, percentLag.getUnits());
		assertEquals(50.0, percentLag.getDuration(), 0.0001);
		net.sf.mpxj.Duration elapsedLag = byName.get("ElapsedSucc").getPredecessors().get(0).getLag();
		assertEquals(2L * 24L * 60L * 60L * 1000L, com.microproject.core.pm.exchange.converters.mpx.MpxUtils.toMillis(elapsedLag));
	}

	@Test
	void externalXlsxSummaryFallbackPreservesHierarchyUnitsAndProgress() throws Exception {
		// Hand-authored summary sheets exercise the advertised external-workbook
		// fallback without the native payload or embedded MSPDI XML. The literal
		// rows define the expected parent, assignment percent, and task progress.
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			org.apache.poi.ss.usermodel.Sheet tasks = workbook.createSheet("Tasks");
			writeRow(tasks, 0, "UniqueID", "ID", "ParentUniqueID", "Name", "Notes", "Start", "Finish", "PercentComplete", "WBS");
			writeRow(tasks, 1, 10.0, 1.0, null, "Release", null, null, null, null, "1");
			writeRow(tasks, 2, 11.0, 2.0, 10.0, "Deploy", null, null, null, 40.0, "1.1");
			writeRow(tasks, 3, 12.0, 3.0, 10.0, "Audit", null, null, null, 0.0, "1.2");
			writeRow(tasks, 4, 13.0, 4.0, 10.0, "Review", null, null, null, 0.0, "1.3");
			org.apache.poi.ss.usermodel.Sheet resources = workbook.createSheet("Resources");
			writeRow(resources, 0, "UniqueID", "ID", "Name", "Notes", "Group", "Email", "MaxUnits");
			writeRow(resources, 1, 7.0, 1.0, "Engineer", null, null, null, 50.0);
			org.apache.poi.ss.usermodel.Sheet assignments = workbook.createSheet("Assignments");
			writeRow(assignments, 0, "TaskUniqueID", "ResourceUniqueID", "Units", "Delay", "LevelingDelay", "WorkContour");
			writeRow(assignments, 1, 11.0, 7.0, 50.0);
			writeRow(assignments, 2, 12.0, 7.0, 0.0);
			writeRow(assignments, 3, 13.0, 7.0);
			workbook.write(out);
		}

		com.microproject.pm.task.Project imported = new MspImporter().importProject(
			new ByteArrayInputStream(out.toByteArray()), "xlsx", (progress, label) -> {});
		com.microproject.pm.task.Task parent = imported.getTaskList().stream()
			.filter(task -> "Release".equals(task.getName())).findFirst().orElseThrow();
		NormalTask child = (NormalTask) imported.getTaskList().stream()
			.filter(task -> "Deploy".equals(task.getName())).findFirst().orElseThrow();
		NormalTask zeroUnitTask = (NormalTask) imported.getTaskList().stream()
			.filter(task -> "Audit".equals(task.getName())).findFirst().orElseThrow();
		NormalTask missingUnitsTask = (NormalTask) imported.getTaskList().stream()
			.filter(task -> "Review".equals(task.getName())).findFirst().orElseThrow();

		assertEquals("Release", child.getWbsParentTask().getName());
		assertEquals(0.4D, child.getPercentComplete(), 0.0001D);
		assertEquals(1, child.getAssignments().size());
		assertEquals(0.5D, ((com.microproject.pm.assignment.Assignment) child.getAssignments().get(0)).getUnits(), 0.0001D);
		assertEquals(0.0D, ((com.microproject.pm.assignment.Assignment) zeroUnitTask.getAssignments().get(0)).getUnits(), 0.0001D);
		assertEquals(1.0D, ((com.microproject.pm.assignment.Assignment) missingUnitsTask.getAssignments().get(0)).getUnits(), 0.0001D);
		assertEquals(0.5D, ((com.microproject.pm.assignment.Assignment) child.getAssignments().get(0))
			.getResource().getMaximumUnits(), 0.0001D);
		assertEquals(3, parent.getWbsChildrenNodes().size());

		ByteArrayOutputStream exported = new ByteArrayOutputStream();
		new MicroProjectXlsxWriter().writeMicroProjectProject(imported, exported);
		String resourceUid = Long.toString(((com.microproject.pm.assignment.Assignment) child.getAssignments().get(0))
			.getResource().getUniqueId());
		try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exported.toByteArray()))) {
			org.apache.poi.ss.usermodel.Sheet exportedResources = workbook.getSheet("Resources");
			boolean wroteExpectedResourceCapacity = false;
			for (int rowIndex = 1; rowIndex <= exportedResources.getLastRowNum(); rowIndex++) {
				Row row = exportedResources.getRow(rowIndex);
				if (row != null && row.getCell(0) != null && row.getCell(6) != null
						&& resourceUid.equals(row.getCell(0).getStringCellValue())
						&& Math.abs(Double.parseDouble(row.getCell(6).getStringCellValue()) - 50.0D) < 0.0001D) {
					wroteExpectedResourceCapacity = true;
					break;
				}
			}
			assertTrue("50% resource capacity must use the summary sheet percent scale", wroteExpectedResourceCapacity);

			org.apache.poi.ss.usermodel.Sheet exportedAssignments = workbook.getSheet("Assignments");
			String taskUid = Long.toString(child.getUniqueId());
			boolean wroteExpectedAssignmentAsPercent = false;
			for (int rowIndex = 1; rowIndex <= exportedAssignments.getLastRowNum(); rowIndex++) {
				Row row = exportedAssignments.getRow(rowIndex);
				if (row != null && row.getCell(0) != null && row.getCell(1) != null && row.getCell(2) != null
						&& taskUid.equals(row.getCell(0).getStringCellValue())
						&& resourceUid.equals(row.getCell(1).getStringCellValue())
						&& Math.abs(Double.parseDouble(row.getCell(2).getStringCellValue()) - 50.0D) < 0.0001D) {
					wroteExpectedAssignmentAsPercent = true;
					break;
				}
			}
			assertTrue("50% assignment must use the summary sheet percent scale", wroteExpectedAssignmentAsPercent);
		}

		ByteArrayOutputStream summaryOnly = new ByteArrayOutputStream();
		try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exported.toByteArray()))) {
			int payloadIndex = workbook.getSheetIndex("_MicroProject_MSPDI");
			assertTrue("writer output contains its higher-priority MSPDI payload", payloadIndex >= 0);
			workbook.removeSheetAt(payloadIndex);
			int nativePayloadIndex = workbook.getSheetIndex("_MicroProject_NATIVE");
			assertTrue("writer output contains its native project payload", nativePayloadIndex >= 0);
			workbook.removeSheetAt(nativePayloadIndex);
			workbook.write(summaryOnly);
		}
		com.microproject.pm.task.Project summaryImported = new MspImporter().importProject(
			new ByteArrayInputStream(summaryOnly.toByteArray()), "xlsx", (progress, label) -> {});
		NormalTask summaryChild = (NormalTask) summaryImported.getTaskList().stream()
			.filter(task -> "Deploy".equals(task.getName())).findFirst().orElseThrow();
		assertEquals(0.5D, ((com.microproject.pm.assignment.Assignment) summaryChild.getAssignments().get(0))
			.getResource().getMaximumUnits(), 0.0001D);
	}

	@Test
	void externalXlsxSummaryRejectsMalformedParentReferences() throws Exception {
		assertInvalidParentLinks(new Object[][] {
			{1.0, 1.0, 1.0, "Self parent"}
		}, "cannot be its own parent");
		assertInvalidParentLinks(new Object[][] {
			{1.0, 1.0, 9.0, "Dangling child"}
		}, "references no task");
		assertInvalidParentLinks(new Object[][] {
			{1.0, 1.0, 2.0, "Cycle A"},
			{2.0, 2.0, 1.0, "Cycle B"}
		}, "contain a cycle");
	}

	private static void assertInvalidParentLinks(Object[][] taskRows, String expectedMessage) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			org.apache.poi.ss.usermodel.Sheet tasks = workbook.createSheet("Tasks");
			writeRow(tasks, 0, "UniqueID", "ID", "ParentUniqueID", "Name", "Notes", "Start", "Finish", "PercentComplete", "WBS");
			for (int i = 0; i < taskRows.length; i++) {
				Object[] taskRow = taskRows[i];
				writeRow(tasks, i + 1, taskRow[0], taskRow[1], taskRow[2], taskRow[3], null, null, null, null, null);
			}
			workbook.write(out);
		}
		try {
			new MicroProjectXlsxReader().read(new ByteArrayInputStream(out.toByteArray()));
			fail("Malformed ParentUniqueID references must be rejected");
		} catch (net.sf.mpxj.MPXJException expected) {
			assertTrue(expected.getCause() instanceof IllegalArgumentException);
			assertTrue(expected.getCause().getMessage().contains(expectedMessage));
		}
	}

	private static void writeRow(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex, Object... values) {
		Row row = sheet.createRow(rowIndex);
		for (int i = 0; i < values.length; i++) {
			if (values[i] == null) {
				continue;
			}
			if (values[i] instanceof Number) {
				row.createCell(i).setCellValue(((Number) values[i]).doubleValue());
			} else {
				row.createCell(i).setCellValue(String.valueOf(values[i]));
			}
		}
	}

	private static byte[] writeNativeWorkbook(String taskName) throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		com.microproject.pm.task.Project project = com.microproject.pm.task.Project.createProject(
			ResourcePool.createRourcePool("native-xlsx", undo), undo);
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName(taskName);
		project.initialize(false, false);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		new MicroProjectXlsxWriter().writeMicroProjectProject(project, output);
		return output.toByteArray();
	}

	private static byte[] toLegacyNativeWorkbook(byte[] canonicalBytes) throws Exception {
		return rewriteWorkbook(canonicalBytes, workbook -> {
			workbook.setSheetName(workbook.getSheetIndex("_MicroProject_META"), "_PL_META");
			workbook.setSheetName(workbook.getSheetIndex("_MicroProject_MSPDI"), "_PL_DATA");
			workbook.setSheetName(workbook.getSheetIndex("_MicroProject_NATIVE"), "_PL_NATIVE");
			Row kindRow = workbook.getSheet("_PL_META").getRow(0);
			kindRow.removeCell(kindRow.getCell(0));
			kindRow.getCell(1).setCellValue("projectlibre");
			Row versionRow = workbook.getSheet("_PL_META").getRow(1);
			versionRow.removeCell(versionRow.getCell(0));
			workbook.getSheet("_PL_DATA").getRow(0).getCell(1).setCellValue("projectlibre");
			workbook.getSheet("_PL_NATIVE").getRow(0).getCell(0).setCellValue("ProjectLibreNativeData");
		});
	}

	private static byte[] addLegacyPayload(byte[] canonicalBytes, byte[] legacyBytes) throws Exception {
		return rewriteWorkbook(canonicalBytes, canonical -> {
			try (XSSFWorkbook legacy = new XSSFWorkbook(new ByteArrayInputStream(legacyBytes))) {
				copySheet(legacy.getSheet("_PL_META"), canonical, "_PL_META");
				copySheet(legacy.getSheet("_PL_DATA"), canonical, "_PL_DATA");
				copySheet(legacy.getSheet("_PL_NATIVE"), canonical, "_PL_NATIVE");
			} catch (Exception error) {
				throw new RuntimeException(error);
			}
		});
	}

	private static void copySheet(org.apache.poi.ss.usermodel.Sheet source,
			XSSFWorkbook destination, String name) {
		org.apache.poi.ss.usermodel.Sheet copy = destination.createSheet(name);
		for (int rowIndex = 0; rowIndex <= source.getLastRowNum(); rowIndex++) {
			Row sourceRow = source.getRow(rowIndex);
			if (sourceRow == null) continue;
			Row targetRow = copy.createRow(rowIndex);
			for (int column = 0; column < sourceRow.getLastCellNum(); column++) {
				org.apache.poi.ss.usermodel.Cell sourceCell = sourceRow.getCell(column);
				if (sourceCell == null) continue;
				org.apache.poi.ss.usermodel.Cell targetCell = targetRow.createCell(column);
				switch (sourceCell.getCellType()) {
				case STRING -> targetCell.setCellValue(sourceCell.getStringCellValue());
				case NUMERIC -> targetCell.setCellValue(sourceCell.getNumericCellValue());
				case BOOLEAN -> targetCell.setCellValue(sourceCell.getBooleanCellValue());
				default -> { }
				}
			}
		}
	}

	private static byte[] rewriteWorkbook(byte[] source, Consumer<XSSFWorkbook> edit) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(source))) {
			edit.accept(workbook);
			workbook.write(output);
		}
		return output.toByteArray();
	}

	private static void assertNativeReadFails(byte[] workbook) throws Exception {
		try {
			MicroProjectXlsxReader.readMicroProjectProject(new ByteArrayInputStream(workbook));
			fail("Corrupt canonical XLSX metadata or payload must not fall back to a legacy payload");
		} catch (net.sf.mpxj.MPXJException expected) {
			// The canonical workbook metadata or payload is corrupt.
		}
	}

	private static void assertProjectFileReadFails(byte[] workbook) throws Exception {
		try {
			new MicroProjectXlsxReader().read(new ByteArrayInputStream(workbook));
			fail("Corrupt canonical MSPDI payload must not fall back to a legacy payload");
		} catch (net.sf.mpxj.MPXJException expected) {
			// The canonical MSPDI payload is corrupt.
		}
	}

	@Test
	void mspImporterCanReadGeneratedXlsx() throws Exception {
		File tempFile = File.createTempFile("projectlibre-xlsx-import", ".xlsx");
		tempFile.deleteOnExit();

		ProjectFile file = new ProjectFile();
		file.addDefaultBaseCalendar();
		Task task = file.addTask();
		task.setName("Imported Task");

		ProjectWriter writer = ProjectWriterFactory.forFile(tempFile.getAbsolutePath());
		writer.write(file, tempFile);

		MspImporter importer = new MspImporter();
		com.microproject.pm.task.Project imported = importer.importProject(tempFile.getAbsolutePath(), new MspImporter.ProgressClosure() {
			@Override
			public void updateProgress(float progress, String label) {
			}
		});

		assertNotNull(imported);
	}

	@Test
	void microsoftAndDirectMspImportUseTheSameNativeXlsxPayload() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		com.microproject.pm.task.Project source = com.microproject.pm.task.Project.createProject(
			ResourcePool.createRourcePool("native-xlsx", undo), undo);
		NormalTask first = (NormalTask) source.createLocalTaskNode(null).getImpl();
		first.setName("Native first task");
		NormalTask second = (NormalTask) source.createLocalTaskNode(null).getImpl();
		second.setName("Native second task");
		source.initialize(false, false);

		File tempFile = File.createTempFile("projectlibre-native-xlsx", ".xlsx");
		tempFile.deleteOnExit();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName(tempFile.getAbsolutePath());
		try (FileOutputStream out = new FileOutputStream(tempFile)) {
			assertTrue(exporter.saveProject(source, out));
		}
		try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(
				java.nio.file.Files.readAllBytes(tempFile.toPath())))) {
			org.apache.poi.ss.usermodel.Sheet metadata = workbook.getSheet("_MicroProject_META");
			assertNotNull(metadata);
			assertEquals("WorkbookKind", metadata.getRow(0).getCell(0).getStringCellValue());
			assertEquals("microproject", metadata.getRow(0).getCell(1).getStringCellValue());
			assertEquals("SchemaVersion", metadata.getRow(1).getCell(0).getStringCellValue());
			assertEquals("3", metadata.getRow(1).getCell(1).getStringCellValue());
			org.apache.poi.ss.usermodel.Sheet mspdi = workbook.getSheet("_MicroProject_MSPDI");
			assertNotNull(mspdi);
			assertEquals("microproject", mspdi.getRow(0).getCell(1).getStringCellValue());
			org.apache.poi.ss.usermodel.Sheet nativeData = workbook.getSheet("_MicroProject_NATIVE");
			assertNotNull(nativeData);
			assertEquals("MicroProjectNativeData", nativeData.getRow(0).getCell(0).getStringCellValue());
		}

		MicrosoftImporter microsoftImporter = new MicrosoftImporter();
		microsoftImporter.setFileName(tempFile.getAbsolutePath());
		com.microproject.pm.task.Project microsoftProject;
		byte[] nativeWorkbook = java.nio.file.Files.readAllBytes(tempFile.toPath());
		final boolean[] streamClosed = {false};
		InputStream in = new ByteArrayInputStream(nativeWorkbook) {
			@Override
			public void close() throws java.io.IOException {
				streamClosed[0] = true;
				super.close();
			}
		};
		microsoftProject = microsoftImporter.loadProject(in);
		assertTrue("native XLSX import must release its source stream", streamClosed[0]);
		MspImporter directImporter = new MspImporter();
		com.microproject.pm.task.Project directProject = directImporter.importProject(tempFile.getAbsolutePath(),
			new MspImporter.ProgressClosure() {
				@Override
				public void updateProgress(float progress, String label) {
				}
			});

		assertEquals(microsoftProject.getDocumentId(), directProject.getDocumentId());
		assertEquals(microsoftProject.getTasks().size(), directProject.getTasks().size());
		com.microproject.pm.task.Project directNative = MicroProjectXlsxReader.readMicroProjectProject(
			new ByteArrayInputStream(nativeWorkbook));
		assertEquals(source.getDocumentId(), directNative.getDocumentId());
		assertEquals(java.util.Set.of("Native first task", "Native second task"), directNative.getTaskList().stream()
			.map(com.microproject.pm.task.Task::getName).collect(java.util.stream.Collectors.toSet()));
		for (int i = 0; i < microsoftProject.getTasks().size(); i++) {
			com.microproject.pm.task.Task microsoftTask =
				(com.microproject.pm.task.Task) microsoftProject.getTasks().get(i);
			com.microproject.pm.task.Task directTask =
				(com.microproject.pm.task.Task) directProject.getTasks().get(i);
			assertEquals(microsoftTask.getUniqueId(), directTask.getUniqueId());
			assertEquals(microsoftTask.getName(), directTask.getName());
		}
	}

	@Test
	void legacyNativeXlsxNamesReadAndCanonicalPayloadWinsOrFailsClosed() throws Exception {
		byte[] legacyBytes = toLegacyNativeWorkbook(writeNativeWorkbook("Legacy task"));
		com.microproject.pm.task.Project legacy = MicroProjectXlsxReader.readMicroProjectProject(
			new ByteArrayInputStream(legacyBytes));
		assertTrue(legacy.getTaskList().stream().anyMatch(task -> "Legacy task".equals(task.getName())));
		ProjectFile legacyMspdi = new MicroProjectXlsxReader().read(new ByteArrayInputStream(legacyBytes));
		assertEquals("Legacy task", legacyMspdi.getTasks().stream()
			.filter(task -> "Legacy task".equals(task.getName())).findFirst().orElseThrow().getName());

		byte[] canonicalBytes = writeNativeWorkbook("Canonical task");
		byte[] bothNames = addLegacyPayload(canonicalBytes, legacyBytes);
		com.microproject.pm.task.Project preferred = MicroProjectXlsxReader.readMicroProjectProject(
			new ByteArrayInputStream(bothNames));
		assertEquals("Canonical task", ((com.microproject.pm.task.Task) preferred.getTaskList().get(0)).getName());

		assertNativeReadFails(rewriteWorkbook(bothNames, workbook ->
			workbook.getSheet("_MicroProject_META").getRow(1).getCell(1).setCellValue("999")));
		assertNativeReadFails(rewriteWorkbook(bothNames, workbook ->
			workbook.getSheet("_MicroProject_NATIVE").getRow(1).getCell(0).setCellValue("!")));
		assertProjectFileReadFails(rewriteWorkbook(bothNames, workbook ->
			workbook.getSheet("_MicroProject_MSPDI").getRow(2).getCell(0).setCellValue("not XML")));
	}

	@Test
	void mspImporterUsesDeclaredBoundaryFromEmbeddedMspdiFallback() throws Exception {
		byte[] canonical = writeNativeWorkbook("Embedded MSPDI fallback task");
		byte[] embeddedOnly = rewriteWorkbook(canonical, workbook -> {
			int nativeIndex = workbook.getSheetIndex("_MicroProject_NATIVE");
			assertTrue(nativeIndex >= 0);
			workbook.removeSheetAt(nativeIndex);
		});
		byte[] summaryOnly = rewriteWorkbook(embeddedOnly, workbook -> {
			int mspdiIndex = workbook.getSheetIndex("_MicroProject_MSPDI");
			assertTrue(mspdiIndex >= 0);
			workbook.removeSheetAt(mspdiIndex);
		});

		MicroProjectXlsxReader reader = new MicroProjectXlsxReader();
		reader.read(new ByteArrayInputStream(embeddedOnly));
		assertNotNull(reader.consumeEmbeddedMspdiXml());
		assertNull("consuming the payload clears reader state", reader.consumeEmbeddedMspdiXml());
		reader.read(new ByteArrayInputStream(summaryOnly));
		assertNull("a subsequent summary-only read has no stale payload", reader.consumeEmbeddedMspdiXml());

		HeaderTrackingMspImporter importer = new HeaderTrackingMspImporter();
		com.microproject.pm.task.Project project = importer.importProject(new ByteArrayInputStream(embeddedOnly), "xlsx",
			(progress, label) -> {});
		assertTrue(project.getTaskList().stream().anyMatch(task -> "Embedded MSPDI fallback task".equals(task.getName())));
		assertEquals("embedded declared project boundary is applied before tasks", 1, importer.earlyHeaderPasses);
	}

	private static final class HeaderTrackingMspImporter extends MspImporter {
		private int earlyHeaderPasses;

		@Override protected void importProjectHeader(com.microproject.pm.task.Project project) {
			if (project.getTaskList().isEmpty()) earlyHeaderPasses++;
			super.importProjectHeader(project);
		}
	}

	@Test
	void mspImporterUsesMspdiProjectDefaultCalendar() throws Exception {
		ProjectFile file = new ProjectFile();
		file.addDefaultBaseCalendar();
		ProjectCalendar nightShift = file.addCalendar();
		nightShift.setName("Night Shift");
		file.getProjectProperties().setDefaultCalendar(nightShift);
		Task task = file.addTask();
		task.setName("Uses project calendar");
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		new MSPDIWriter().write(file, output);

		MspImporter importer = new MspImporter();
		com.microproject.pm.task.Project imported = importer.importProject(
				new ByteArrayInputStream(output.toByteArray()), "xml", new MspImporter.ProgressClosure() {
					@Override
					public void updateProgress(float progress, String label) {
					}
				});

		assertNotNull(imported);
		assertNotNull(imported.getBaseCalendar());
		assertEquals("Night Shift", imported.getBaseCalendar().getName());
	}

	@Test
	void mspdiRoundTripPreservesProjectDefaultCalendar() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		com.microproject.pm.task.Project source = com.microproject.pm.task.Project.createProject(
				ResourcePool.createRourcePool("calendar-round-trip", undo), undo);
		WorkingCalendar calendar = CalendarService.getInstance().getStandardBasedInstance();
		calendar.setName("MSP custom project calendar");
		source.setBaseCalendar(calendar);
		net.sf.mpxj.ProjectFile exported = new MSPDISerializer().serializeProject(source);
		assertNotNull(exported.getProjectProperties().getDefaultCalendar());
		assertEquals("MSP custom project calendar", exported.getProjectProperties().getDefaultCalendar().getName());
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		new MSPDIWriter().write(exported, output);

		MspImporter importer = new MspImporter();
		com.microproject.pm.task.Project imported = importer.importProject(
				new ByteArrayInputStream(output.toByteArray()), "xml", new MspImporter.ProgressClosure() {
					@Override
					public void updateProgress(float progress, String label) {
					}
				});

		assertEquals("MSP custom project calendar", imported.getBaseCalendar().getName());
	}

	@Test
	void commercialConstructionPodExportsAndReloadsAsXlsx() throws Exception {
		File sample = findSample("Commercial construction project plan.pod");

		LocalFileImporter sourceImporter = new LocalFileImporter();
		sourceImporter.setFileName(sample.getAbsolutePath());
		sourceImporter.setProjectFactory(com.microproject.pm.task.ProjectFactory.getInstance());
		sourceImporter.importFile();
		com.microproject.pm.task.Project project = sourceImporter.getProject();
		assertNotNull(project);
		int taskCount = project.getTasks().size();
		assertTrue(taskCount > 0);
		Map<Long, TaskSnapshot> originalTasks = snapshotsById(project);

		File tempFile = File.createTempFile("commercial-construction", ".xlsx");
		tempFile.deleteOnExit();

		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName(tempFile.getAbsolutePath());
		FileOutputStream out = new FileOutputStream(tempFile);
		try {
			assertTrue(exporter.saveProject(project, out));
		} finally {
			out.close();
		}
		assertTrue(tempFile.length() > 0);
		assertNotNull(MicroProjectXlsxReader.readMicroProjectProject(tempFile));
		ProjectMergeService mergeService = new ProjectMergeService();
		com.microproject.pm.task.Project reloaded = mergeService.loadExternalProject(tempFile.getAbsolutePath());
		assertNotNull(reloaded);
		assertEquals(taskCount, reloaded.getTasks().size());

		Map<Long, TaskSnapshot> reloadedTasks = snapshotsById(reloaded);
		assertEquals(originalTasks.size(), reloadedTasks.size());
		boolean checkedProgress = false;
		boolean checkedMultiDayDuration = false;
		for (Map.Entry<Long, TaskSnapshot> entry : originalTasks.entrySet()) {
			TaskSnapshot original = entry.getValue();
			TaskSnapshot imported = reloadedTasks.get(entry.getKey());
			assertNotNull(imported);
			assertEquals(original.name, imported.name);
			if (!original.summary) {
				assertEquals(original.duration, imported.duration);
				assertEquals(original.work, imported.work, 0.0001);
				assertEquals(original.actualWork, imported.actualWork);
				assertEquals(original.remainingWork, imported.remainingWork);
				assertEquals(original.percentComplete, imported.percentComplete, 0.0001);
			}
			if (original.percentComplete > 0.0D) {
				checkedProgress = true;
			}
			if (original.duration > 8L * 60L * 60L * 1000L) {
				checkedMultiDayDuration = true;
			}
		}
		assertTrue(checkedProgress);
		assertTrue(checkedMultiDayDuration);
	}

	@Test
	void xlsxSummaryImportSkipsNonNumericTextInNumericColumns() throws Exception {
		// Issue #186: a non-numeric text cell in a numeric column (e.g. "50%" typed
		// into % Complete, or a hand-edited UID) used to abort the entire import
		// with NumberFormatException instead of being skipped.
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			org.apache.poi.ss.usermodel.Sheet tasks = workbook.createSheet("Tasks");
			writeRow(tasks, 0, "UID", "ID", "ParentUID", "Name", "Notes", "Start", "Finish", "% Complete");
			writeRow(tasks, 1, "abc", 1.0, null, "Bad UID", null, null, null, "50%");
			writeRow(tasks, 2, 2.0, 2.0, null, "Good", null, null, null, "75%");
			workbook.write(out);
		}

		ProjectFile reloaded = new MicroProjectXlsxReader().read(new ByteArrayInputStream(out.toByteArray()));
		assertNotNull(reloaded);
	}

	private static File findSample(String name) {
		for (String prefix : new String[] { "samples/", "../samples/", "../../samples/" }) {
			File sample = new File(prefix + name);
			if (sample.isFile()) {
				return sample;
			}
		}
		throw new AssertionError("Missing sample: " + name);
	}

	private Map<Long, TaskSnapshot> snapshotsById(com.microproject.pm.task.Project project) {
		Map<Long, TaskSnapshot> tasks = new HashMap<Long, TaskSnapshot>();
		for (Object value : project.getTasks()) {
			if (!(value instanceof NormalTask)) {
				continue;
			}
			NormalTask task = (NormalTask) value;
			tasks.put(Long.valueOf(task.getId()), new TaskSnapshot(task.getName(), task.getDurationMillis(),
				task.getWork(), task.getActualWork(null), task.getRemainingWork(), task.getPercentComplete(),
				task.isSummary()));
		}
		return tasks;
	}

	private static class TaskSnapshot {
		private final String name;
		private final long duration;
		private final double work;
		private final long actualWork;
		private final long remainingWork;
		private final double percentComplete;
		private final boolean summary;

		private TaskSnapshot(String name, long duration, double work, long actualWork, long remainingWork,
				double percentComplete, boolean summary) {
			this.name = name;
			this.duration = duration;
			this.work = work;
			this.actualWork = actualWork;
			this.remainingWork = remainingWork;
			this.percentComplete = percentComplete;
			this.summary = summary;
		}
	}

	@Test
	void mspImporterSkipsRootSummaryTaskFromXml() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
			+ "<Project xmlns=\"http://schemas.microsoft.com/project\">"
			+ "<Name>Hierarchy</Name>"
			+ "<Tasks>"
			+ "<Task><UID>0</UID><ID>0</ID><Name>Project Summary</Name><OutlineLevel>0</OutlineLevel><OutlineNumber>0</OutlineNumber><Summary>1</Summary></Task>"
			+ "<Task><UID>1</UID><ID>1</ID><Name>Child Task</Name><OutlineLevel>1</OutlineLevel><OutlineNumber>1</OutlineNumber></Task>"
			+ "</Tasks>"
			+ "</Project>";

		MspImporter importer = new MspImporter();
		InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
		com.microproject.pm.task.Project imported = importer.importProject(in, "xlsx", new MspImporter.ProgressClosure() {
			@Override
			public void updateProgress(float progress, String label) {
			}
		});

		assertNotNull(imported);
		assertEquals(1, imported.getTasks().size());
		assertEquals("Child Task", imported.getTasks().get(0).getName());
	}

	@Test
	void mspImporterAcceptsInputStreamWithoutMarkSupport() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
			+ "<Project xmlns=\"http://schemas.microsoft.com/project\">"
			+ "<Name>Unmarked input</Name>"
			+ "<Tasks><Task><UID>1</UID><ID>1</ID><Name>Stream task</Name></Task></Tasks>"
			+ "</Project>";
		InputStream unmarked = new FilterInputStream(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
			@Override
			public boolean markSupported() {
				return false;
			}
		};

		com.microproject.pm.task.Project imported = new MspImporter().importProject(unmarked, "xml",
			(progress, label) -> {});

		assertNotNull(imported);
		assertEquals(1, imported.getTasks().size());
		assertEquals("Stream task", imported.getTasks().get(0).getName());
	}
}
