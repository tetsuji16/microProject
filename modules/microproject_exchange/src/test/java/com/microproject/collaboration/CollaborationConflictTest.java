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
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Task;
import net.sf.mpxj.writer.ProjectWriter;
import com.microproject.exchange.mpxj.ProjectWriterFactory;

import com.microproject.exchange.LocalFileImporter;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourcePoolFactory;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.workspace.WorkspaceSetting;

class CollaborationConflictTest {
	@TempDir
	Path tempDir;
	private Project previousLastDeserialized;
	private List<ResourcePool> previousRegisteredResourcePools;
	private ResourcePool previousGlobalResourcePool;

	@BeforeEach
	void captureGlobalProjectState() {
		previousLastDeserialized = Project.lastDeserialized;
		previousRegisteredResourcePools = new ArrayList<>(ResourcePoolFactory.getInstance().getResourcePools());
		previousGlobalResourcePool = readGlobalResourcePool();
	}

	@AfterEach
	void restoreGlobalProjectState() {
		Project.lastDeserialized = previousLastDeserialized;
		List<ResourcePool> registeredPools = ResourcePoolFactory.getInstance().getResourcePools();
		registeredPools.clear();
		registeredPools.addAll(previousRegisteredResourcePools);
		writeGlobalResourcePool(previousGlobalResourcePool);
	}

	@Test
	void ownHeartbeatDoesNotTriggerExternalWarning() throws Exception {
		File projectFile = createTempFile("collaboration", ".xlsx");
		AtomicInteger notices = new AtomicInteger();
		CollaborationSession session = new CollaborationSession(null, projectFile.getAbsolutePath(), "alice");
		session.setExternalChangeNoticeHandler(message -> notices.incrementAndGet());
		long now = System.currentTimeMillis();

		session.pollAt(now);
		session.pollAt(now + 6000L);

		assertFalse(session.requiresSaveConfirmation());
		assertEquals(0, notices.get());
	}

	@Test
	void otherUserMetadataChangeTriggersExternalWarning() throws Exception {
		File projectFile = createTempFile("collaboration", ".xlsx");
		AtomicInteger notices = new AtomicInteger();
		CollaborationSession session = new CollaborationSession(null, projectFile.getAbsolutePath(), "alice");
		session.setExternalChangeNoticeHandler(message -> notices.incrementAndGet());
		long now = System.currentTimeMillis();
		session.pollAt(now);

		CollaborationMetadataStore store = new CollaborationMetadataStore(projectFile);
		store.mutate(metadata -> {
			CollaborationMetadataStore.UserRecord other = new CollaborationMetadataStore.UserRecord();
			other.setUserKey("bob");
			other.setDisplayName("bob");
			other.setClientInstanceId("other-client");
			other.setLastSeenAt(System.currentTimeMillis());
			metadata.getUsers().put("bob", other);
		});

		session.pollAt(now + 1L);

		assertTrue(session.requiresSaveConfirmation());
		assertEquals(1, notices.get());
	}

	@Test
	void sameUserStaleLockDoesNotTriggerExternalWarning() throws Exception {
		File projectFile = createTempFile("collaboration", ".xlsx");

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
		AtomicInteger notices = new AtomicInteger();
		session.setExternalChangeNoticeHandler(message -> notices.incrementAndGet());
		session.pollAt(System.currentTimeMillis());

		assertFalse(session.requiresSaveConfirmation());
		assertEquals(0, notices.get());
	}

	@Test
	void projectFileChangeWithoutMetadataChangeDoesNotWarnImmediately() throws Exception {
		File projectFile = createTempFile("collaboration", ".xlsx");

		CollaborationSession session = new CollaborationSession(null, projectFile.getAbsolutePath(), "alice");
		AtomicInteger notices = new AtomicInteger();
		session.setExternalChangeNoticeHandler(message -> notices.incrementAndGet());
		long now = System.currentTimeMillis();
		session.pollAt(now);
		assertTrue(projectFile.setLastModified(System.currentTimeMillis() + 2000L));
		session.pollAt(now + 1L);

		assertTrue(session.requiresSaveConfirmation());
		assertEquals(0, notices.get());
	}

	@Test
	void projectFileChangeRequestsReloadWhenProjectIsClean() throws Exception {
		File projectFile = createWorkbook("Baseline Task", "Unchanged Task");
		Project project = newProject();
		project.setGroupDirty(false);
		assertNotNull(project);
		assertFalse(project.needsSaving());

		AtomicInteger reloads = new AtomicInteger();
		CollaborationSession session = new CollaborationSession(project, projectFile.getAbsolutePath(), "alice");
		session.setExternalReloadHandler(new CollaborationSession.ExternalProjectReloadHandler() {
			public void reload(com.microproject.pm.task.Project changedProject) {
				reloads.incrementAndGet();
			}
		});
		long now = System.currentTimeMillis();
		session.pollAt(now);

		assertTrue(projectFile.setLastModified(System.currentTimeMillis() + 2000L));
		session.pollAt(now + 1L);
		assertEquals(0, reloads.get());
		session.pollAt(now + 1L + 1500L);

		assertEquals(1, reloads.get());
		assertFalse(session.requiresSaveConfirmation());
	}

	@Test
	void projectFileChangeRequestsReloadWhenOnlyLocalLockExists() throws Exception {
		File projectFile = createWorkbook("Baseline Task", "Unchanged Task");
		Project project = new ProjectMergeService().loadExternalProject(projectFile.getAbsolutePath());
		project.setGroupDirty(false);

		AtomicInteger reloads = new AtomicInteger();
		CollaborationSession session = new CollaborationSession(project, projectFile.getAbsolutePath(), "alice");
		session.setExternalReloadHandler(new CollaborationSession.ExternalProjectReloadHandler() {
			public void reload(com.microproject.pm.task.Project changedProject) {
				reloads.incrementAndGet();
			}
		});
		long now = System.currentTimeMillis();
		session.pollAt(now);
		com.microproject.pm.task.Task localTask =
			(com.microproject.pm.task.Task) project.getTasks().get(0);
		assertTrue(session.tryAcquireTaskLock(localTask));
		assertTrue(session.getLocalLocks().contains(Long.valueOf(localTask.getUniqueId())));

		assertTrue(projectFile.setLastModified(System.currentTimeMillis() + 2000L));
		session.pollAt(now + 1L);
		assertEquals(0, reloads.get());
		session.pollAt(now + 1L + 1500L);

		assertEquals(1, reloads.get());
		assertFalse(session.requiresSaveConfirmation());
	}

	@Test
	void projectFileChangeReloadsEvenWhenProjectIsLocallyDirty() throws Exception {
		File projectFile = createWorkbook("Baseline Task", "Unchanged Task");
		Project project = newProject();
		project.setGroupDirty(true);

		AtomicInteger reloads = new AtomicInteger();
		CollaborationSession session = new CollaborationSession(project, projectFile.getAbsolutePath(), "alice");
		session.setExternalReloadHandler(new CollaborationSession.ExternalProjectReloadHandler() {
			public void reload(com.microproject.pm.task.Project changedProject) {
				reloads.incrementAndGet();
			}
		});
		long now = System.currentTimeMillis();
		session.pollAt(now);

		assertTrue(projectFile.setLastModified(System.currentTimeMillis() + 2000L));
		session.pollAt(now + 1L);
		session.pollAt(now + 1L + 1500L);

		assertEquals(1, reloads.get());
		assertFalse(session.requiresSaveConfirmation());
	}

	@Test
	void xlsxConflictDetectionOnlyFlagsChangedLockedTasks() throws Exception {
		assertConflictDetectionOnlyFlagsChangedLockedTasks("xlsx");
	}

	@Test
	void podConflictDetectionOnlyFlagsChangedLockedTasks() throws Exception {
		assertConflictDetectionOnlyFlagsChangedLockedTasks("pod");
	}

	private void assertConflictDetectionOnlyFlagsChangedLockedTasks(String extension) throws Exception {
		File original = createProjectFile(extension, "Baseline Task", "Unchanged Task");
		File changed = createProjectFile(extension, "Renamed Task", "Unchanged Task");

		ProjectMergeService mergeService = new ProjectMergeService();
		com.microproject.pm.task.Project baselineProject = mergeService.loadExternalProject(original.getAbsolutePath());
		assertNotNull(baselineProject);
		assertTrue(baselineProject.getTasks().size() >= 2);

		Map<Long, ProjectMergeService.TaskState> lockedTaskStates = new LinkedHashMap<Long, ProjectMergeService.TaskState>();
		com.microproject.pm.task.Task first = (com.microproject.pm.task.Task) baselineProject.getTasks().get(0);
		com.microproject.pm.task.Task second = (com.microproject.pm.task.Task) baselineProject.getTasks().get(1);
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

	@Test
	void xlsxBackgroundRefreshUpdatesOnlyUnlockedExistingTasks() throws Exception {
		assertBackgroundRefreshUpdatesOnlyUnlockedExistingTasks("xlsx");
	}

	@Test
	void podBackgroundRefreshUpdatesOnlyUnlockedExistingTasks() throws Exception {
		assertBackgroundRefreshUpdatesOnlyUnlockedExistingTasks("pod");
	}

	@Test
	void podCollaborationMetadataNeverChangesPodBytes() throws Exception {
		File podFile = createPodFile("Baseline Task", "Unchanged Task");
		byte[] before = Files.readAllBytes(podFile.toPath());
		long lastModified = podFile.lastModified();
		File sidecar = CollaborationMetadataStore.buildSidecarFile(podFile);
		if (sidecar.exists()) {
			assertTrue(sidecar.delete());
		}

		CollaborationSession session = new CollaborationSession(null, podFile.getAbsolutePath(), "alice");
		session.pollAt(System.currentTimeMillis());
		Project lockProject = new ProjectMergeService().loadExternalProject(podFile.getAbsolutePath());
		com.microproject.pm.task.Task podTask =
			(com.microproject.pm.task.Task) lockProject.getTasks().get(0);
		assertTrue(session.tryAcquireTaskLock(podTask));
		try {
			session.saveWorkspace(new TestWorkspaceSetting("gantt"));
		} finally {
			session.stop();
		}

		byte[] after = Files.readAllBytes(podFile.toPath());
		assertTrue(Arrays.equals(before, after),
			"Collaboration must store metadata in the sidecar, not mutate the POD file.");
		assertEquals(lastModified, podFile.lastModified());
		assertTrue(sidecar.exists());
		assertTrue(sidecar.length() > 0L);
	}

	@Test
	void podMergeUsesTheSameNativePayloadAsLocalLoad() throws Exception {
		File podFile = createPodFile("Native identity task", "Native second task");

		LocalFileImporter localImporter = new LocalFileImporter();
		localImporter.setFileName(podFile.getAbsolutePath());
		localImporter.importFile();
		Project localProject = localImporter.getProject();
		assertNotNull(localProject);

		Project mergeProject = new ProjectMergeService().loadExternalProject(podFile.getAbsolutePath());
		assertNotNull(mergeProject);
		assertEquals(localProject.getDocumentId(), mergeProject.getDocumentId(),
			"POD merge must use the native document identity");
		assertEquals(localProject.getTasks().size(), mergeProject.getTasks().size());
		for (int i = 0; i < localProject.getTasks().size(); i++) {
			com.microproject.pm.task.Task localTask =
				(com.microproject.pm.task.Task) localProject.getTasks().get(i);
			com.microproject.pm.task.Task mergeTask =
				(com.microproject.pm.task.Task) mergeProject.getTasks().get(i);
			assertEquals(localTask.getUniqueId(), mergeTask.getUniqueId());
			assertEquals(localTask.getName(), mergeTask.getName());
		}
	}

	private void assertBackgroundRefreshUpdatesOnlyUnlockedExistingTasks(String extension) throws Exception {
		File original = createProjectFile(extension, "Baseline Task", "Unchanged Task");
		File changed = createProjectFile(extension, "Renamed Task", "Externally Changed Task");

		ProjectMergeService mergeService = new ProjectMergeService();
		com.microproject.pm.task.Project target = mergeService.loadExternalProject(original.getAbsolutePath());
		assertNotNull(target);

		com.microproject.pm.task.Task first = (com.microproject.pm.task.Task) target.getTasks().get(0);
		com.microproject.pm.task.Task second = (com.microproject.pm.task.Task) target.getTasks().get(1);
		Set<Long> locked = new LinkedHashSet<Long>();
		locked.add(Long.valueOf(first.getUniqueId()));

		ProjectMergeService.ApplyResult result = mergeService.applyExternalTaskUpdates(target, changed.getAbsolutePath(), locked);

		assertEquals("Baseline Task", first.getName());
		assertEquals("Externally Changed Task", second.getName());
		assertEquals(1, result.getUpdatedTaskCount());
		assertTrue(result.getSkippedLockedTaskIds().contains(Long.valueOf(first.getUniqueId())));
	}

	private File createProjectFile(String extension, String firstTaskName, String secondTaskName) throws Exception {
		if ("pod".equals(extension)) {
			return createPodFile(firstTaskName, secondTaskName);
		}
		return createWorkbook(firstTaskName, secondTaskName);
	}

	private File createPodFile(String firstTaskName, String secondTaskName) throws Exception {
		File seed = createWorkbook(firstTaskName, secondTaskName);
		ProjectMergeService mergeService = new ProjectMergeService();
		com.microproject.pm.task.Project project = mergeService.loadExternalProject(seed.getAbsolutePath());
		assertNotNull(project);

		File file = Files.createTempFile(tempDir, "collaboration", ".pod").toFile();
		LocalFileImporter exporter = new LocalFileImporter();
		exporter.setFileName(file.getAbsolutePath());
		exporter.setProject(project);
		exporter.exportFile();
		return file;
	}

	private File createWorkbook(String firstTaskName, String secondTaskName) throws Exception {
		File file = Files.createTempFile(tempDir, "collaboration", ".xlsx").toFile();

		ProjectFile project = new ProjectFile();
		project.addDefaultBaseCalendar();
		Task first = project.addTask();
		first.setName(firstTaskName);
		first.setUniqueID(Integer.valueOf(1));
		first.setNotes("Locked notes");

		Task second = project.addTask();
		second.setName(secondTaskName);
		second.setUniqueID(Integer.valueOf(2));

		ProjectWriter writer = ProjectWriterFactory.forFile(file.getAbsolutePath());
		writer.write(project, file);
		return file;
	}

	private File createTempFile(String prefix, String suffix) throws Exception {
		return Files.createTempFile(tempDir, prefix, suffix).toFile();
	}

	private static Project newProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		return Project.createProject(ResourcePool.createRourcePool("collaboration-test", undo), undo);
	}

	private static ResourcePool readGlobalResourcePool() {
		return updateGlobalResourcePool(null, false);
	}

	private static void writeGlobalResourcePool(ResourcePool pool) {
		updateGlobalResourcePool(pool, true);
	}

	private static ResourcePool updateGlobalResourcePool(ResourcePool value, boolean write) {
		try {
			Field field = ResourcePool.class.getDeclaredField("globalPool");
			field.setAccessible(true);
			ResourcePool previous = (ResourcePool) field.get(null);
			if (write) field.set(null, value);
			return previous;
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError("Unable to isolate legacy global resource pool state", exception);
		}
	}

	private static final class TestWorkspaceSetting implements WorkspaceSetting {
		private static final long serialVersionUID = 1L;
		private final String viewName;

		private TestWorkspaceSetting(String viewName) {
			this.viewName = viewName;
		}
	}
}
