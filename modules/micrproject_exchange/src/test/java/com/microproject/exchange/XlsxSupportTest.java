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

import java.io.BufferedInputStream;	import java.io.ByteArrayInputStream;
	import java.io.ByteArrayOutputStream;
	import java.io.File;
	import java.io.FileOutputStream;
	import java.io.InputStream;

	import org.apache.poi.ss.usermodel.Row;
	import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Task;
import net.sf.mpxj.writer.ProjectWriter;
import com.microproject.exchange.mpxj.ProjectWriterFactory;
import com.microproject.exchange.xlsx.ProjectLibreXlsxReader;

import com.microproject.collaboration.ProjectMergeService;
import com.microproject.exchange.MicrosoftImporter;
import com.microproject.exchange.LocalFileImporter;
import com.microproject.pm.task.NormalTask;
import com.microproject.core.pm.exchange.MspImporter;
import com.microproject.collaboration.CollaborationMetadataStore;
import com.microproject.session.FileHelper;

public class XlsxSupportTest extends TestCase {
	public void testFileHelperAcceptsMppForReadOnlyImport() {
		assertTrue(FileHelper.isFileNameAllowed("plan.mpp", false));
		assertFalse(FileHelper.isFileNameAllowed("plan.mpp", true));
		assertEquals(FileHelper.MSP_FILE_TYPE, FileHelper.getFileType("plan.mpp"));
	}

	public void testCollaborationDoesNotRecognizeMpp() {
		assertFalse(CollaborationMetadataStore.isCollaborationCandidate("plan.mpp"));
	}

	public void testMspImporterCanReadMpp() throws Exception {
		File sample = new File("modules/micrproject_exchange/testdata/New Product.mpp");
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
	}

	public void testFileHelperAcceptsXlsx() {
		assertTrue(FileHelper.isFileNameAllowed("plan.xlsx", true));
		assertTrue(FileHelper.isFileNameAllowed("plan.xlsx", false));
		assertEquals(FileHelper.MSP_FILE_TYPE, FileHelper.getFileType("plan.xlsx"));
	}

	public void testCollaborationRecognizesXlsx() {
		assertTrue(CollaborationMetadataStore.isCollaborationCandidate("plan.xlsx"));
	}

	public void testProjectWriterFactorySupportsXlsx() throws Exception {
		ProjectWriter writer = ProjectWriterFactory.forFile("plan.xlsx");
		assertNotNull(writer);
	}

	public void testXlsxDependencyLagRoundTrip() throws Exception {
		// Issue #162: the Dependencies sheet Lag column was written but never read,
		// silently dropping every dependency lag on the summary-sheet fallback path.
		// Build a workbook containing only the summary sheets (no _PL_DATA payload)
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

		ProjectFile reloaded = new ProjectLibreXlsxReader().read(new ByteArrayInputStream(out.toByteArray()));
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

	public void testMspImporterCanReadGeneratedXlsx() throws Exception {
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

	public void testCommercialConstructionPodExportsAndReloadsAsXlsx() throws Exception {
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
		assertNotNull(ProjectLibreXlsxReader.readProjectLibreProject(tempFile));
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
				assertEquals(original.work, imported.work);
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

	public void testMspImporterTreatsXmlContentWithXlsxExtensionAsXml() throws Exception {
		File tempFile = File.createTempFile("projectlibre-xlsx-xml-fallback", ".xlsx");
		tempFile.deleteOnExit();

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
			+ "<Project xmlns=\"http://schemas.microsoft.com/project\">"
			+ "<Name>Fallback</Name>"
			+ "<Tasks>"
			+ "<Task><UID>0</UID><ID>0</ID><Name>Project Summary</Name><Summary>1</Summary></Task>"
			+ "<Task><UID>1</UID><ID>1</ID><Name>XML Task</Name></Task>"
			+ "</Tasks>"
			+ "</Project>";
		FileOutputStream out = new FileOutputStream(tempFile);
		try {
			out.write(xml.getBytes(StandardCharsets.UTF_8));
		} finally {
			out.close();
		}

		MspImporter importer = new MspImporter();
		Method prepare = MspImporter.class.getDeclaredMethod("prepareProjectStream", InputStream.class);
		prepare.setAccessible(true);
		Method normalize = MspImporter.class.getDeclaredMethod("normalizeExtension", String.class, InputStream.class);
		normalize.setAccessible(true);

		InputStream in = null;
		try {
			in = (InputStream) prepare.invoke(importer, new java.io.FileInputStream(tempFile));
			String normalized = (String) normalize.invoke(importer, "xlsx", in);
			assertEquals("xml", normalized);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	public void testPrepareProjectStreamWrapsOnlyWhenNeeded() throws Exception {
		MspImporter importer = new MspImporter();
		Method prepare = MspImporter.class.getDeclaredMethod("prepareProjectStream", InputStream.class);
		prepare.setAccessible(true);

		BufferedInputStream buffered = new BufferedInputStream(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
		InputStream preparedBuffered = (InputStream) prepare.invoke(importer, buffered);
		assertSame(buffered, preparedBuffered);

		ByteArrayInputStream plain = new ByteArrayInputStream(new byte[] { 4, 5, 6 });
		InputStream preparedPlain = (InputStream) prepare.invoke(importer, plain);
		assertTrue(preparedPlain instanceof BufferedInputStream);
		assertNotSame(plain, preparedPlain);
	}

	public void testMspImporterSkipsRootSummaryTaskFromXml() throws Exception {
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
}
