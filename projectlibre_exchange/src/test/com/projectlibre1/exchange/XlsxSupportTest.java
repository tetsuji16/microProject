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
import net.sf.mpxj.writer.ProjectWriterUtility;

import com.projectlibre1.collaboration.ProjectMergeService;
import com.projectlibre1.exchange.MicrosoftImporter;
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
		File sample = new File("projectlibre_exchange/testdata/New Product.mpp");
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

	public void testProjectWriterUtilitySupportsXlsx() throws Exception {
		ProjectWriter writer = ProjectWriterUtility.getProjectWriter("plan.xlsx");
		assertNotNull(writer);
	}

	public void testMspImporterCanReadGeneratedXlsx() throws Exception {
		File tempFile = File.createTempFile("projectlibre-xlsx-import", ".xlsx");
		tempFile.deleteOnExit();

		ProjectFile file = new ProjectFile();
		file.addDefaultBaseCalendar();
		Task task = file.addTask();
		task.setName("Imported Task");

		ProjectWriter writer = ProjectWriterUtility.getProjectWriter(tempFile.getAbsolutePath());
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
		File sample = new File("sample data/Commercial construction project plan.pod");
		if (!sample.exists()) {
			sample = new File("../sample data/Commercial construction project plan.pod");
		}
		assertTrue(sample.exists());

		ProjectMergeService mergeService = new ProjectMergeService();
		com.projectlibre1.pm.task.Project project = mergeService.loadExternalProject(sample.getAbsolutePath());
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
		java.nio.file.Files.copy(tempFile.toPath(), new File("build/commercial-construction-debug.xlsx").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

		assertTrue(tempFile.length() > 0);
		com.projectlibre1.pm.task.Project reloaded = mergeService.loadExternalProject(tempFile.getAbsolutePath());
		assertNotNull(reloaded);
		assertEquals(taskCount, reloaded.getTasks().size());

		Map<Long, TaskSnapshot> reloadedTasks = snapshotsById(reloaded);
		assertEquals(originalTasks.size(), reloadedTasks.size());
		debugTaskState("original", project, 1L, 2L, 5L);
		debugTaskState("reloaded", reloaded, 1L, 2L, 5L);
		boolean checkedProgress = false;
		boolean checkedMultiDayDuration = false;
		for (Map.Entry<Long, TaskSnapshot> entry : originalTasks.entrySet()) {
			TaskSnapshot original = entry.getValue();
			TaskSnapshot imported = reloadedTasks.get(entry.getKey());
			assertNotNull(imported);
			assertEquals(original.name, imported.name);
			assertEquals(original.duration, imported.duration);
			assertEquals(original.percentComplete, imported.percentComplete, 0.0001);
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

	private Map<Long, TaskSnapshot> snapshotsById(com.projectlibre1.pm.task.Project project) {
		Map<Long, TaskSnapshot> tasks = new HashMap<Long, TaskSnapshot>();
		for (Object value : project.getTasks()) {
			if (!(value instanceof NormalTask)) {
				continue;
			}
			NormalTask task = (NormalTask) value;
			tasks.put(Long.valueOf(task.getId()), new TaskSnapshot(task.getName(), task.getDurationMillis(), task.getPercentComplete()));
		}
		return tasks;
	}

	private void debugTaskState(String label, com.projectlibre1.pm.task.Project project, long... ids) {
		java.util.Set<Long> idSet = new java.util.HashSet<Long>();
		for (long id : ids) {
			idSet.add(Long.valueOf(id));
		}
		System.out.println(label + " projectCalendar=" + (project.getWorkCalendar() == null ? "null" : project.getWorkCalendar().getName()));
		for (Object value : project.getTasks()) {
			if (!(value instanceof NormalTask)) {
				continue;
			}
			NormalTask task = (NormalTask) value;
			Long id = Long.valueOf(task.getId());
			if (!idSet.contains(id)) {
				continue;
			}
			System.out.println(label + " task " + id + " name=" + task.getName()
				+ " duration=" + task.getDurationMillis()
				+ " work=" + task.getWork()
				+ " actualWork=" + task.getActualWork(null)
				+ " remainingWork=" + task.getRemainingWork()
				+ " percent=" + task.getPercentComplete()
				+ " calendar=" + (task.getWorkCalendar() == null ? "null" : task.getWorkCalendar().getName())
				+ " assignments=" + task.getAssignments().size());
			for (Object assignmentValue : task.getAssignments()) {
				com.projectlibre1.pm.assignment.Assignment assignment = (com.projectlibre1.pm.assignment.Assignment) assignmentValue;
				com.projectlibre1.pm.resource.Resource resource = (com.projectlibre1.pm.resource.Resource) assignment.getResource();
				System.out.println(label + " task " + id + " assignment resource="
					+ (resource == null ? "null" : resource.getName())
					+ " resourceCal=" + (resource == null || resource.getWorkCalendar() == null ? "null" : resource.getWorkCalendar().getName())
					+ " units=" + assignment.getUnits());
			}
		}
	}

	private static class TaskSnapshot {
		private final String name;
		private final long duration;
		private final double percentComplete;

		private TaskSnapshot(String name, long duration, double percentComplete) {
			this.name = name;
			this.duration = duration;
			this.percentComplete = percentComplete;
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
