/*******************************************************************************
 * MIT License
 *
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
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Task;
import net.sf.mpxj.writer.ProjectWriter;
import com.microproject.exchange.mpxj.ProjectWriterFactory;

import com.microproject.exchange.LocalFileImporter;
import com.microproject.exchange.MpoConflictRecoveryException;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.exchange.MpoConflictRecoveryException;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourcePoolFactory;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.TaskSnapshot;
import com.microproject.temporary.TemporaryRoot;
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

	@Test
	void expiredTaskLeaseProtectsDirtyRefreshAndMpoSaveCreatesRecoveryCopy() throws Exception {
		ProjectMergeService mergeService = new ProjectMergeService();
		File workbook = createWorkbook("Original task", "Unchanged task");
		Project initial = mergeService.loadExternalProject(workbook.getAbsolutePath());
		assertNotNull(initial);
		File sharedMpo = Files.createTempFile(tempDir, "expired-lease-save-conflict", ".mpo").toFile();
		saveNativeMpo(initial, sharedMpo);

		Project aliceProject = mergeService.loadExternalProject(sharedMpo.getAbsolutePath());
		Project bobProject = mergeService.loadExternalProject(sharedMpo.getAbsolutePath());
		assertNotNull(aliceProject);
		assertNotNull(bobProject);
		com.microproject.pm.task.Task aliceTask = (com.microproject.pm.task.Task) aliceProject.getTasks().get(0);
		com.microproject.pm.task.Task bobTask = (com.microproject.pm.task.Task) bobProject.getTasks().get(0);
		long taskId = aliceTask.getUniqueId();
		CollaborationSession alice = new CollaborationSession(aliceProject, sharedMpo.getAbsolutePath(), "alice");
		CollaborationSession bob = new CollaborationSession(bobProject, sharedMpo.getAbsolutePath(), "bob");
		Path recoveryCopy = null;
		try {
			alice.start();
			assertTrue(alice.tryAcquireTaskLock(aliceTask));
			aliceTask.setName("Alice unsaved edit");
			assertTrue(aliceProject.needsSaving(), "Alice's change must remain unsaved in the open project");

			bob.start();
			CollaborationMetadataStore metadataStore = new CollaborationMetadataStore(sharedMpo);
			metadataStore.mutate(metadata -> {
				CollaborationMetadataStore.LockRecord lease = metadata.getLocks().get(Long.toString(taskId));
				assertNotNull(lease);
				lease.setLeaseUntil(System.currentTimeMillis() - 1L);
			});
			assertTrue(bob.tryAcquireTaskLock(bobTask), "Bob takes the task after Alice's lease expires");
			bobTask.setName("Bob committed edit");
			saveNativeMpo(bobProject, sharedMpo);
			byte[] bobCommittedArchive = Files.readAllBytes(sharedMpo.toPath());

			try (CollaborationSession.ExternalProjectRefreshLease refresh = alice.beginExternalProjectRefresh()) {
				assertTrue(refresh.getDirtyLostTaskIds().contains(Long.valueOf(taskId)),
						"refresh identifies Alice's locally changed task after confirming the lease expired");
				ProjectMergeService.ApplyResult refreshed = mergeService.applyExternalTaskUpdates(
						aliceProject, sharedMpo.getAbsolutePath(), refresh.getLockedTaskIds());
				assertFalse(refreshed.hasLoadFailure());
				assertTrue(refreshed.getSkippedLockedTaskIds().contains(Long.valueOf(taskId)),
						"the external refresh leaves Alice's unsaved task untouched");
				assertEquals("Alice unsaved edit", aliceTask.getName());
				alice.afterExternalProjectRefresh(refresh);
			}

			MpoFileImporter aliceWriter = new MpoFileImporter();
			aliceWriter.setFileName(sharedMpo.getAbsolutePath());
			aliceWriter.setProject(aliceProject);
			MpoConflictRecoveryException conflict = assertThrows(MpoConflictRecoveryException.class, aliceWriter::exportFile,
					"saving Alice's preserved stale-base edit must report the concurrent same-task change");
			recoveryCopy = conflict.recoveryCopy();
			assertFalse(conflict.conflicts().isEmpty());
			assertArrayEquals(bobCommittedArchive, Files.readAllBytes(sharedMpo.toPath()),
					"the shared MPO remains Bob's committed version instead of being silently overwritten");
			Project sharedAfterConflict = mergeService.loadExternalProject(sharedMpo.getAbsolutePath());
			assertEquals("Bob committed edit", ((com.microproject.pm.task.Task) sharedAfterConflict.getTasks().get(0)).getName());
			assertTrue(Files.isRegularFile(recoveryCopy));
			Project recoveredAlice = mergeService.loadExternalProject(recoveryCopy.toString());
			assertEquals("Alice unsaved edit", ((com.microproject.pm.task.Task) recoveredAlice.getTasks().get(0)).getName(),
					"the recovery copy contains Alice's protected unsaved value");
		} finally {
			if (recoveryCopy != null) Files.deleteIfExists(recoveryCopy);
			alice.stop();
			bob.stop();
		}
	}

	@Test
	void expiredDirtyTaskSurvivesExternalRefreshAndMpoConflictRecovery() throws Exception {
		File workbook = createWorkbook("Initial locked task", "Initial independent task");
		Project seedProject = new ProjectMergeService().loadExternalProject(workbook.getAbsolutePath());
		assertNotNull(seedProject);
		File shared = Files.createTempFile(tempDir, "lost-lease-mpo-", ".mpo").toFile();
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(seedProject);
		seed.exportFile();

		Project local = new ProjectMergeService().loadExternalProject(shared.getAbsolutePath());
		Project remote = new ProjectMergeService().loadExternalProject(shared.getAbsolutePath());
		assertNotNull(local);
		assertNotNull(remote);
		com.microproject.pm.task.Task localLocked = (com.microproject.pm.task.Task) local.getTasks().get(0);
		com.microproject.pm.task.Task localIndependent = (com.microproject.pm.task.Task) local.getTasks().get(1);
		com.microproject.pm.task.Task remoteLocked = (com.microproject.pm.task.Task) remote.findByUniqueId(localLocked.getUniqueId());
		com.microproject.pm.task.Task remoteIndependent = (com.microproject.pm.task.Task) remote.findByUniqueId(localIndependent.getUniqueId());
		assertNotNull(remoteLocked);
		assertNotNull(remoteIndependent);

		CollaborationMetadataStore store = new CollaborationMetadataStore(shared);
		CollaborationSession alice = new CollaborationSession(local, shared.getAbsolutePath(), "alice");
		CollaborationSession bob = new CollaborationSession(remote, shared.getAbsolutePath(), "bob");
		Path recoveryCopy = null;
		try {
			alice.start();
			assertTrue(alice.tryAcquireTaskLock(localLocked));
			localLocked.setName("Alice unsaved value");

			remoteLocked.setName("Bob committed value");
			remoteIndependent.setName("Bob independent update");
			MpoFileImporter remoteWriter = new MpoFileImporter();
			remoteWriter.setFileName(shared.getAbsolutePath());
			remoteWriter.setProject(remote);
			remoteWriter.exportFile();

			bob.start();
			store.mutate(metadata -> metadata.getLocks().get(Long.toString(localLocked.getUniqueId()))
				.setLeaseUntil(System.currentTimeMillis() - 1L));
			assertTrue(bob.tryAcquireTaskLock(remoteLocked));

			CollaborationSession.ExternalProjectRefreshLease refresh = alice.beginExternalProjectRefresh();
			try {
				assertTrue(refresh.getDirtyLostTaskIds().contains(localLocked.getUniqueId()));
				ProjectMergeService.ApplyResult result = new ProjectMergeService().applyExternalTaskUpdates(
					local, shared.getAbsolutePath(), refresh.getLockedTaskIds());
				assertFalse(result.hasLoadFailure());
				assertTrue(result.getSkippedLockedTaskIds().contains(localLocked.getUniqueId()));
				assertEquals("Alice unsaved value", localLocked.getName(), "lease loss must not overwrite the unsaved local edit");
				assertEquals("Bob independent update", localIndependent.getName(), "unmodified tasks still receive external updates");
				alice.afterExternalProjectRefresh(refresh);
			} finally {
				refresh.close();
			}

			MpoFileImporter localWriter = new MpoFileImporter();
			localWriter.setFileName(shared.getAbsolutePath());
			localWriter.setProject(local);
			MpoConflictRecoveryException conflict = assertThrows(MpoConflictRecoveryException.class, localWriter::exportFile,
				"the existing MPO journal must report the divergent same-field edits rather than replacing either branch");
			recoveryCopy = conflict.recoveryCopy();
			assertEquals("Alice unsaved value", ((com.microproject.pm.task.Task) new ProjectMergeService()
				.loadExternalProject(recoveryCopy.toString()).findByUniqueId(localLocked.getUniqueId())).getName(),
				"the MPO recovery copy must preserve the lease-lost local value");
		} finally {
			alice.stop();
			bob.stop();
			if (recoveryCopy != null) Files.deleteIfExists(recoveryCopy);
		}
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
	void backgroundRefreshPublishesOnlyAfterAllMatchedTasksAreUpdated() throws Exception {
		File original = createProjectFile("xlsx", "Baseline Task", "Unchanged Task");
		File changed = createProjectFile("xlsx", "Renamed Task", "Externally Changed Task");
		ProjectMergeService mergeService = new ProjectMergeService();
		Project target = mergeService.loadExternalProject(original.getAbsolutePath());
		com.microproject.pm.task.Task first = (com.microproject.pm.task.Task) target.getTasks().get(0);
		com.microproject.pm.task.Task second = (com.microproject.pm.task.Task) target.getTasks().get(1);
		AtomicInteger publications = new AtomicInteger();
		AtomicBoolean observerSawCompleteState = new AtomicBoolean(true);
		target.addScheduleListener(event -> {
			publications.incrementAndGet();
			observerSawCompleteState.compareAndSet(true,
				"Renamed Task".equals(first.getName()) && "Externally Changed Task".equals(second.getName()));
		});
		long revision = target.getMutationState().revision();

		ProjectMergeService.ApplyResult result = mergeService.applyExternalTaskUpdates(target, changed.getAbsolutePath(), Set.of());

		assertEquals(2, result.getUpdatedTaskCount());
		assertEquals(revision + 1, target.getMutationState().revision());
		assertTrue(publications.get() > 0);
		assertTrue(observerSawCompleteState.get(), "Observers must see all matched tasks after the single commit");
	}

	@Test
	void backgroundRefreshFailureRestoresTasksAndSuppressesPublication() throws Exception {
		File original = createProjectFile("xlsx", "Baseline Task", "Unchanged Task");
		File changed = createProjectFile("xlsx", "Renamed Task", "Externally Changed Task");
		ProjectMergeService mergeService = new ProjectMergeService() {
			@Override void afterExternalTaskUpdateApplied(int appliedCount) {
				if (appliedCount == 1) throw new IllegalStateException("injected external refresh failure");
			}
		};
		Project target = mergeService.loadExternalProject(original.getAbsolutePath());
		com.microproject.pm.task.Task first = (com.microproject.pm.task.Task) target.getTasks().get(0);
		com.microproject.pm.task.Task second = (com.microproject.pm.task.Task) target.getTasks().get(1);
		first.setName("Original first name");
		first.setNotes("Original first notes");
		first.setUniqueIdPredecessors(Long.toString(second.getUniqueId()));
		first.setDuration(first.getDuration() + 86400000L);
		first.setEnd(first.getEnd() + 86400000L);
		first.setPercentComplete(0.4D);
		TaskSnapshot baseline = new TaskSnapshot();
		baseline.setCurrentSchedule((com.microproject.pm.criticalpath.TaskSchedule) first.getCurrentSchedule().clone());
		first.setSnapshot(Integer.valueOf(0), baseline);
		String predecessor = first.getUniqueIdPredecessors();
		long start = first.getStart();
		long end = first.getEnd();
		long duration = first.getDuration();
		double percentComplete = first.getPercentComplete();
		long baselineStart = baseline.getCurrentSchedule().getStart();
		long baselineEnd = baseline.getCurrentSchedule().getEnd();
		String secondName = second.getName();
		first.setDirty(false);
		second.setDirty(false);
		target.setDirty(false);
		target.setGroupDirty(false);
		target.getUndoController().discardAllEdits();
		boolean dirty = target.isDirty();
		boolean groupDirty = target.isGroupDirty();
		boolean canUndo = target.getUndoController().canUndo();
		AtomicInteger publications = new AtomicInteger();
		AtomicInteger objectPublications = new AtomicInteger();
		target.addScheduleListener(event -> publications.incrementAndGet());
		target.addObjectListener(event -> objectPublications.incrementAndGet());
		long revision = target.getMutationState().revision();

		assertThrows(IllegalStateException.class,
			() -> mergeService.applyExternalTaskUpdates(target, changed.getAbsolutePath(), Set.of()));

		assertEquals("Original first name", first.getName());
		assertEquals("Original first notes", first.getNotes());
		assertEquals(predecessor, first.getUniqueIdPredecessors());
		assertEquals(start, first.getStart());
		assertEquals(end, first.getEnd());
		assertEquals(duration, first.getDuration());
		assertEquals(percentComplete, first.getPercentComplete());
		TaskSnapshot restoredBaseline = (TaskSnapshot) first.getSnapshot(Integer.valueOf(0));
		assertEquals(baselineStart, restoredBaseline.getCurrentSchedule().getStart());
		assertEquals(baselineEnd, restoredBaseline.getCurrentSchedule().getEnd());
		assertEquals(secondName, second.getName());
		assertFalse(first.isDirty());
		assertFalse(second.isDirty());
		assertEquals(dirty, target.isDirty());
		assertEquals(groupDirty, target.isGroupDirty());
		assertEquals(canUndo, target.getUndoController().canUndo());
		assertFalse(canUndo, "The rollback fixture starts without undo history");
		assertEquals(revision, target.getMutationState().revision());
		assertEquals(0, publications.get());
		assertEquals(0, objectPublications.get());
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
			assertTrue(sidecar.exists());
			assertTrue(sidecar.length() > 0L);
		} finally {
			session.stop();
		}

		byte[] after = Files.readAllBytes(podFile.toPath());
		assertTrue(Arrays.equals(before, after),
			"Collaboration must store metadata in the sidecar, not mutate the POD file.");
		assertEquals(lastModified, podFile.lastModified());
	}

	/** Issue #967: closing a project leaves nothing beside it unless someone else still needs it. */
	@Test
	void closingTheLastSessionRemovesItsFilesAndKeepsTheLayoutOnThisComputer() throws Exception {
		String previousRoot = System.getProperty(TemporaryRoot.ROOT_PROPERTY);
		System.setProperty(TemporaryRoot.ROOT_PROPERTY, tempDir.resolve("application-data/temp").toString());
		try {
			File project = createPodFile("First", "Second");
			File sidecar = CollaborationMetadataStore.buildSidecarFile(project);
			File marker = CollaborationMetadataStore.buildLockFile(sidecar);
			File transactionLock = new File(project.getPath() + ".lock");
			assertTrue(transactionLock.createNewFile(), "a marker left by an older release");

			CollaborationSession closing = new CollaborationSession(null, project.getPath(), "alice");
			closing.start();
			closing.saveWorkspace(new TestWorkspaceSetting("gantt"));
			assertTrue(sidecar.exists());
			closing.stop();
			assertFalse(sidecar.exists());
			assertFalse(marker.exists());
			assertFalse(transactionLock.exists());
			closing.saveWorkspace(new TestWorkspaceSetting("late"));
			closing.pollAt(System.currentTimeMillis());
			assertFalse(sidecar.exists(), "a stopped session must not write its files back");
			assertFalse(marker.exists(), "a stopped session must not write its files back");

			CollaborationSession reopened = new CollaborationSession(null, project.getPath(), "alice");
			reopened.start();
			assertEquals("gantt", ((TestWorkspaceSetting) reopened.loadWorkspace()).viewName);

			CollaborationSession secondCopy = new CollaborationSession(null, project.getPath(), "alice");
			secondCopy.start();
			reopened.stop();
			assertTrue(sidecar.exists(), "the same user's other running copy still uses the sidecar");
			secondCopy.stop();
			assertFalse(sidecar.exists());

			CollaborationSession alice = new CollaborationSession(null, project.getPath(), "alice");
			CollaborationSession bob = new CollaborationSession(null, project.getPath(), "bob");
			alice.start();
			bob.start();
			alice.stop();
			CollaborationMetadataStore.Metadata afterAliceStops = new CollaborationMetadataStore(project).load();
			assertTrue(afterAliceStops.getUsers().containsKey("alice"));
			assertTrue(afterAliceStops.getUsers().containsKey("bob"), "the other user's record survives the first close");
			bob.stop();
			assertTrue(sidecar.exists(), "the other user's record must survive either close");
			assertTrue(marker.exists());
			CollaborationMetadataStore.Metadata afterBothStop = new CollaborationMetadataStore(project).load();
			assertTrue(afterBothStop.getUsers().containsKey("alice"));
			assertTrue(afterBothStop.getUsers().containsKey("bob"), "offline user records keep the shared sidecar available");
		} finally {
			if (previousRoot == null) System.clearProperty(TemporaryRoot.ROOT_PROPERTY);
			else System.setProperty(TemporaryRoot.ROOT_PROPERTY, previousRoot);
		}
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

	private static void saveNativeMpo(Project project, File target) throws Exception {
		MpoFileImporter exporter = new MpoFileImporter();
		exporter.setFileName(target.getAbsolutePath());
		exporter.setProject(project);
		exporter.exportFile();
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
		// Pin a Monday start: an unpinned project starts "today", so weekday-sensitive schedule assertions
		// (for example end + 1 day) would depend on the calendar day the suite runs.
		java.util.Date monday = java.util.Date.from(java.time.LocalDateTime.of(2026, 10, 19, 8, 0)
			.atZone(java.time.ZoneId.systemDefault()).toInstant());
		project.getProjectProperties().setStartDate(monday);
		Task first = project.addTask();
		first.setName(firstTaskName);
		first.setUniqueID(Integer.valueOf(1));
		first.setStart(monday);
		first.setNotes("Locked notes");

		Task second = project.addTask();
		second.setName(secondTaskName);
		second.setUniqueID(Integer.valueOf(2));
		second.setStart(monday);

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
