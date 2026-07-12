package test.com.projectlibre1.collaboration;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import junit.framework.TestCase;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Task;
import net.sf.mpxj.writer.ProjectWriter;
import net.sf.mpxj.writer.ProjectWriterUtility;

import com.projectlibre1.collaboration.CollaborationMetadataStore;
import com.projectlibre1.collaboration.CollaborationSession;
import com.projectlibre1.collaboration.ProjectMergeService;
import com.projectlibre1.collaboration.TaskLockManager;
import com.projectlibre1.exchange.LocalFileImporter;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.workspace.WorkspaceSetting;

public class CollaborationConflictTest extends TestCase {
	public void testOwnHeartbeatDoesNotTriggerExternalWarning() throws Exception {
		File projectFile = File.createTempFile("projectlibre-collaboration", ".xlsx");
		projectFile.deleteOnExit();

		CollaborationSession session = new CollaborationSession(null, projectFile.getAbsolutePath(), "alice");
		invokePrivate(session, "registerUser");
		invokePrivate(session, "poll");

		assertFalse(readBoolean(session, "externalChangePending"));
		assertFalse(readBoolean(session, "externalChangeWarned"));
	}

	public void testOtherUserMetadataChangeTriggersExternalWarning() throws Exception {
		File projectFile = File.createTempFile("projectlibre-collaboration", ".xlsx");
		projectFile.deleteOnExit();

		CollaborationSession session = new CollaborationSession(null, projectFile.getAbsolutePath(), "alice");
		invokePrivate(session, "registerUser");

		CollaborationMetadataStore store = new CollaborationMetadataStore(projectFile);
		store.mutate(metadata -> {
			CollaborationMetadataStore.UserRecord other = new CollaborationMetadataStore.UserRecord();
			other.setUserKey("bob");
			other.setDisplayName("bob");
			other.setClientInstanceId("other-client");
			other.setLastSeenAt(System.currentTimeMillis());
			metadata.getUsers().put("bob", other);
		});

		invokePrivate(session, "poll");

		assertTrue(readBoolean(session, "externalChangePending"));
		assertTrue(readBoolean(session, "externalChangeWarned"));
	}

	public void testSameUserStaleLockDoesNotTriggerExternalWarning() throws Exception {
		File projectFile = File.createTempFile("projectlibre-collaboration", ".xlsx");
		projectFile.deleteOnExit();

		CollaborationMetadataStore store = new CollaborationMetadataStore(projectFile);
		store.mutate(metadata -> {
			CollaborationMetadataStore.LockRecord lock = new CollaborationMetadataStore.LockRecord();
			lock.setTaskId(1L);
			lock.setOwnerKey("alice#old-client");
			lock.setUserKey("alice");
			lock.setDisplayName("alice");
			lock.setClientInstanceId("old-client");
			lock.setUpdatedAt(System.currentTimeMillis() - 60000L);
			lock.setLeaseUntil(System.currentTimeMillis() - 45000L);
			metadata.getLocks().put("1", lock);
		});

		CollaborationSession session = new CollaborationSession(null, projectFile.getAbsolutePath(), "alice");
		invokePrivate(session, "registerUser");
		invokePrivate(session, "poll");

		assertFalse(readBoolean(session, "externalChangePending"));
		assertFalse(readBoolean(session, "externalChangeWarned"));
	}

	public void testProjectFileChangeWithoutMetadataChangeDoesNotWarnImmediately() throws Exception {
		File projectFile = File.createTempFile("projectlibre-collaboration", ".xlsx");
		projectFile.deleteOnExit();

		CollaborationSession session = new CollaborationSession(null, projectFile.getAbsolutePath(), "alice");
		invokePrivate(session, "registerUser");
		assertTrue(projectFile.setLastModified(System.currentTimeMillis() + 2000L));
		invokePrivate(session, "poll");

		assertTrue(readBoolean(session, "externalChangePending"));
		assertFalse(readBoolean(session, "externalChangeWarned"));
	}

	public void testProjectFileChangeRequestsReloadWhenProjectIsClean() throws Exception {
		File projectFile = createWorkbook("Baseline Task", "Unchanged Task");
		Project project = Project.getDummy();
		project.setGroupDirty(false);
		assertNotNull(project);
		assertFalse(project.needsSaving());

		AtomicInteger reloads = new AtomicInteger();
		CollaborationSession session = new CollaborationSession(project, projectFile.getAbsolutePath(), "alice");
		session.setExternalReloadHandler(new CollaborationSession.ExternalProjectReloadHandler() {
			public void reload(com.projectlibre1.pm.task.Project changedProject) {
				reloads.incrementAndGet();
			}
		});
		invokePrivate(session, "registerUser");

		assertTrue(projectFile.setLastModified(System.currentTimeMillis() + 2000L));
		invokePrivate(session, "poll");
		assertEquals(0, reloads.get());
		writeLong(session, "pendingExternalReloadDetectedAt", System.currentTimeMillis() - 2000L);
		invokePrivate(session, "poll");
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
			}
		});

		assertEquals(1, reloads.get());
		assertFalse(readBoolean(session, "externalChangePending"));
	}

	public void testProjectFileChangeRequestsReloadWhenOnlyLocalLockExists() throws Exception {
		File projectFile = createWorkbook("Baseline Task", "Unchanged Task");
		Project project = Project.getDummy();
		project.setGroupDirty(false);

		AtomicInteger reloads = new AtomicInteger();
		CollaborationSession session = new CollaborationSession(project, projectFile.getAbsolutePath(), "alice");
		session.setExternalReloadHandler(new CollaborationSession.ExternalProjectReloadHandler() {
			public void reload(com.projectlibre1.pm.task.Project changedProject) {
				reloads.incrementAndGet();
			}
		});
		invokePrivate(session, "registerUser");

		TaskLockManager lockManager = (TaskLockManager) readField(session, "lockManager");
		assertTrue(lockManager.acquire(1L));

		assertTrue(projectFile.setLastModified(System.currentTimeMillis() + 2000L));
		invokePrivate(session, "poll");
		assertEquals(0, reloads.get());
		writeLong(session, "pendingExternalReloadDetectedAt", System.currentTimeMillis() - 2000L);
		invokePrivate(session, "poll");
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
			}
		});

		assertEquals(1, reloads.get());
		assertFalse(readBoolean(session, "externalChangePending"));
	}

	public void testProjectFileChangeReloadsEvenWhenProjectIsLocallyDirty() throws Exception {
		File projectFile = createWorkbook("Baseline Task", "Unchanged Task");
		Project project = Project.getDummy();
		project.setGroupDirty(true);

		AtomicInteger reloads = new AtomicInteger();
		CollaborationSession session = new CollaborationSession(project, projectFile.getAbsolutePath(), "alice");
		session.setExternalReloadHandler(new CollaborationSession.ExternalProjectReloadHandler() {
			public void reload(com.projectlibre1.pm.task.Project changedProject) {
				reloads.incrementAndGet();
			}
		});
		invokePrivate(session, "registerUser");

		assertTrue(projectFile.setLastModified(System.currentTimeMillis() + 2000L));
		invokePrivate(session, "poll");
		writeLong(session, "pendingExternalReloadDetectedAt", System.currentTimeMillis() - 2000L);
		invokePrivate(session, "poll");
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
			}
		});

		assertEquals(1, reloads.get());
		assertFalse(readBoolean(session, "externalChangePending"));
	}

	public void testXlsxConflictDetectionOnlyFlagsChangedLockedTasks() throws Exception {
		assertConflictDetectionOnlyFlagsChangedLockedTasks("xlsx");
	}

	public void testPodConflictDetectionOnlyFlagsChangedLockedTasks() throws Exception {
		assertConflictDetectionOnlyFlagsChangedLockedTasks("pod");
	}

	private void assertConflictDetectionOnlyFlagsChangedLockedTasks(String extension) throws Exception {
		File original = createProjectFile(extension, "Baseline Task", "Unchanged Task");
		File changed = createProjectFile(extension, "Renamed Task", "Unchanged Task");

		ProjectMergeService mergeService = new ProjectMergeService();
		com.projectlibre1.pm.task.Project baselineProject = mergeService.loadExternalProject(original.getAbsolutePath());
		assertNotNull(baselineProject);
		assertTrue(baselineProject.getTasks().size() >= 2);

		Map<Long, ProjectMergeService.TaskState> lockedTaskStates = new LinkedHashMap<Long, ProjectMergeService.TaskState>();
		com.projectlibre1.pm.task.Task first = (com.projectlibre1.pm.task.Task) baselineProject.getTasks().get(0);
		com.projectlibre1.pm.task.Task second = (com.projectlibre1.pm.task.Task) baselineProject.getTasks().get(1);
		assertNotNull(first);
		assertNotNull(second);

		lockedTaskStates.put(Long.valueOf(first.getUniqueId()), ProjectMergeService.TaskState.capture(first));
		ProjectMergeService.ConflictResult changedConflict = mergeService.findTaskConflicts(changed.getAbsolutePath(), lockedTaskStates);
		assertTrue(changedConflict.hasConflicts());
		assertTrue(changedConflict.getChangedTaskIds().contains(Long.valueOf(first.getUniqueId())));

		lockedTaskStates.clear();
		lockedTaskStates.put(Long.valueOf(second.getUniqueId()), ProjectMergeService.TaskState.capture(second));
		ProjectMergeService.ConflictResult unchangedConflict = mergeService.findTaskConflicts(changed.getAbsolutePath(), lockedTaskStates);
		assertFalse(unchangedConflict.hasConflicts());
	}

	public void testXlsxBackgroundRefreshUpdatesOnlyUnlockedExistingTasks() throws Exception {
		assertBackgroundRefreshUpdatesOnlyUnlockedExistingTasks("xlsx");
	}

	public void testPodBackgroundRefreshUpdatesOnlyUnlockedExistingTasks() throws Exception {
		assertBackgroundRefreshUpdatesOnlyUnlockedExistingTasks("pod");
	}

	public void testPodCollaborationMetadataNeverChangesPodBytes() throws Exception {
		File podFile = createPodFile("Baseline Task", "Unchanged Task");
		byte[] before = Files.readAllBytes(podFile.toPath());
		long lastModified = podFile.lastModified();
		File sidecar = CollaborationMetadataStore.buildSidecarFile(podFile);
		if (sidecar.exists()) {
			assertTrue(sidecar.delete());
		}

		CollaborationSession session = new CollaborationSession(null, podFile.getAbsolutePath(), "alice");
		invokePrivate(session, "registerUser");
		TaskLockManager lockManager = (TaskLockManager) readField(session, "lockManager");
		assertTrue(lockManager.acquire(1L));
		lockManager.release(1L);
		session.saveWorkspace(new TestWorkspaceSetting("gantt"));

		byte[] after = Files.readAllBytes(podFile.toPath());
		assertTrue("Collaboration must store metadata in the sidecar, not mutate the POD file.", Arrays.equals(before, after));
		assertEquals(lastModified, podFile.lastModified());
		assertTrue(sidecar.exists());
		assertTrue(sidecar.length() > 0L);
	}

	private void assertBackgroundRefreshUpdatesOnlyUnlockedExistingTasks(String extension) throws Exception {
		File original = createProjectFile(extension, "Baseline Task", "Unchanged Task");
		File changed = createProjectFile(extension, "Renamed Task", "Externally Changed Task");

		ProjectMergeService mergeService = new ProjectMergeService();
		com.projectlibre1.pm.task.Project target = mergeService.loadExternalProject(original.getAbsolutePath());
		assertNotNull(target);

		com.projectlibre1.pm.task.Task first = (com.projectlibre1.pm.task.Task) target.getTasks().get(0);
		com.projectlibre1.pm.task.Task second = (com.projectlibre1.pm.task.Task) target.getTasks().get(1);
		Set<Long> locked = new LinkedHashSet<Long>();
		locked.add(Long.valueOf(first.getUniqueId()));

		ProjectMergeService.ApplyResult result = mergeService.applyExternalTaskUpdates(target, changed.getAbsolutePath(), locked);

		assertEquals("Baseline Task", first.getName());
		assertEquals("Externally Changed Task", second.getName());
		assertEquals(1, result.getUpdatedTaskCount());
		assertTrue(result.getSkippedLockedTaskIds().contains(Long.valueOf(first.getUniqueId())));
	}

	private static File createProjectFile(String extension, String firstTaskName, String secondTaskName) throws Exception {
		if ("pod".equals(extension)) {
			return createPodFile(firstTaskName, secondTaskName);
		}
		return createWorkbook(firstTaskName, secondTaskName);
	}

	private static File createPodFile(String firstTaskName, String secondTaskName) throws Exception {
		File seed = createWorkbook(firstTaskName, secondTaskName);
		ProjectMergeService mergeService = new ProjectMergeService();
		com.projectlibre1.pm.task.Project project = mergeService.loadExternalProject(seed.getAbsolutePath());
		assertNotNull(project);

		File file = File.createTempFile("projectlibre-conflict", ".pod");
		file.deleteOnExit();
		LocalFileImporter exporter = new LocalFileImporter();
		exporter.setFileName(file.getAbsolutePath());
		exporter.setProject(project);
		exporter.exportFile();
		return file;
	}

	private static File createWorkbook(String firstTaskName, String secondTaskName) throws Exception {
		File file = File.createTempFile("projectlibre-conflict", ".xlsx");
		file.deleteOnExit();

		ProjectFile project = new ProjectFile();
		project.addDefaultBaseCalendar();
		Task first = project.addTask();
		first.setName(firstTaskName);
		first.setUniqueID(Integer.valueOf(1));
		first.setNotes("Locked notes");

		Task second = project.addTask();
		second.setName(secondTaskName);
		second.setUniqueID(Integer.valueOf(2));

		ProjectWriter writer = ProjectWriterUtility.getProjectWriter(file.getAbsolutePath());
		writer.write(project, file);
		return file;
	}

	private static void invokePrivate(Object target, String methodName) throws Exception {
		Method method = target.getClass().getDeclaredMethod(methodName);
		method.setAccessible(true);
		method.invoke(target);
	}

	private static boolean readBoolean(Object target, String fieldName) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.getBoolean(target);
	}

	private static Object readField(Object target, String fieldName) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(target);
	}

	private static void writeLong(Object target, String fieldName, long value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.setLong(target, value);
	}

	private static final class TestWorkspaceSetting implements WorkspaceSetting {
		private static final long serialVersionUID = 1L;
		private final String viewName;

		private TestWorkspaceSetting(String viewName) {
			this.viewName = viewName;
		}
	}
}
