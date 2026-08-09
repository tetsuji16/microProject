package test.com.projectlibre1.exchange;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Task;
import net.sf.mpxj.writer.ProjectWriter;
import com.projectlibre1.exchange.mpxj.ProjectWriterFactory;
import com.projectlibre1.exchange.xlsx.ProjectLibreXlsxReader;

import com.projectlibre1.collaboration.ProjectMergeService;
import com.projectlibre1.exchange.MicrosoftImporter;
import com.projectlibre1.exchange.LocalFileImporter;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre.core.pm.exchange.MspImporter;
import com.projectlibre1.collaboration.CollaborationMetadataStore;
import com.projectlibre1.session.FileHelper;

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
		File sample = new File("modules/projectlibre_exchange/testdata/New Product.mpp");
		if (!sample.exists()) {
			sample = new File("testdata/New Product.mpp");
		}
		assertTrue(sample.exists());

		MspImporter importer = new MspImporter();
		com.projectlibre.pm.tasks.Project imported = importer.importProject(sample.getAbsolutePath(), new MspImporter.ProgressClosure() {
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
		com.projectlibre.pm.tasks.Project imported = importer.importProject(tempFile.getAbsolutePath(), new MspImporter.ProgressClosure() {
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
		sourceImporter.setProjectFactory(com.projectlibre1.pm.task.ProjectFactory.getInstance());
		sourceImporter.importFile();
		com.projectlibre1.pm.task.Project project = sourceImporter.getProject();
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
		com.projectlibre1.pm.task.Project reloaded = mergeService.loadExternalProject(tempFile.getAbsolutePath());
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

	private Map<Long, TaskSnapshot> snapshotsById(com.projectlibre1.pm.task.Project project) {
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
		com.projectlibre.pm.tasks.Project imported = importer.importProject(in, "xlsx", new MspImporter.ProgressClosure() {
			@Override
			public void updateProgress(float progress, String label) {
			}
		});

		assertNotNull(imported);
		assertEquals(1, imported.getTasks().size());
		assertEquals("Child Task", imported.getTasks().get(0).getPropertyValue("name"));
	}
}
