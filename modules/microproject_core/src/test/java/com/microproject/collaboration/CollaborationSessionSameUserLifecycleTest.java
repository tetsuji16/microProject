/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.workspace.WorkspaceSetting;

class CollaborationSessionSameUserLifecycleTest {
	@TempDir
	Path tempDir;

	@Test
	void sessionFreshnessComparisonDoesNotOverflowAtLongTimestampBoundary() {
		long now = Long.MAX_VALUE;
		long exactlyAtTtlBoundary = now - CollaborationMetadataStore.SESSION_TTL_MS;
		long oneMillisecondBeyondTtl = exactlyAtTtlBoundary - 1L;

		assertTrue(CollaborationMetadataStore.isFresh(exactlyAtTtlBoundary, now),
				"a session exactly at the TTL boundary remains fresh even when timestamp addition would overflow");
		assertFalse(CollaborationMetadataStore.isFresh(oneMillisecondBeyondTtl, now),
				"a session one millisecond older than the TTL is stale");
		assertTrue(CollaborationMetadataStore.isFresh(Long.MAX_VALUE, now - 1L),
				"future timestamps remain conservatively fresh");
	}

	@Test
	void stoppingOneSameUserSessionKeepsTheOtherSessionMetadataUsable() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("same-user.mpo"));
		Project project = project();
		NormalTask task = project.createScriptedTask();
		Path sidecar = CollaborationMetadataStore.buildSidecarFile(projectFile.toFile()).toPath();
		CollaborationSession first = new CollaborationSession(project, projectFile.toString(), "alice");
		CollaborationSession second = new CollaborationSession(project, projectFile.toString(), "alice");

		try {
			first.start();
			second.start();
			second.stop();

			assertTrue(Files.isRegularFile(sidecar), "the first open session still needs the shared sidecar");
			assertTrue(first.tryAcquireTaskLock(task), "the first session must still acquire a task after the second stops");
			assertTrue(new CollaborationMetadataStore(projectFile.toFile()).load().getLocks().containsKey(Long.toString(task.getUniqueId())));
			first.stop();
			assertFalse(Files.exists(sidecar), "the final session may retire the sidecar after releasing its lease");
		} finally {
			first.stop();
			second.stop();
		}
	}

	@Test
	void stoppingTheFirstSameUserSessionLeavesTheSecondSessionAbleToAcquireAndRetire() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("same-user-reverse.mpo"));
		Project project = project();
		NormalTask task = project.createScriptedTask();
		Path sidecar = CollaborationMetadataStore.buildSidecarFile(projectFile.toFile()).toPath();
		CollaborationSession first = new CollaborationSession(project, projectFile.toString(), "alice");
		CollaborationSession second = new CollaborationSession(project, projectFile.toString(), "alice");

		try {
			first.start();
			second.start();
			first.stop();

			assertTrue(Files.isRegularFile(sidecar), "the second open session still needs the shared sidecar");
			assertTrue(second.tryAcquireTaskLock(task), "the second session must still acquire a task after the first stops");
			second.stop();
			assertFalse(Files.exists(sidecar), "the final session may retire the sidecar after releasing its lease");
		} finally {
			first.stop();
			second.stop();
		}
	}

	@Test
	void sameTaskLeaseConflictSurvivesTheFirstSessionClosing() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("same-user-conflict.mpo"));
		Project project = project();
		NormalTask task = project.createScriptedTask();
		Path sidecar = CollaborationMetadataStore.buildSidecarFile(projectFile.toFile()).toPath();
		CollaborationSession first = new CollaborationSession(project, projectFile.toString(), "alice");
		CollaborationSession second = new CollaborationSession(project, projectFile.toString(), "alice");

		try {
			first.start();
			second.start();
			assertTrue(first.tryAcquireTaskLock(task));
			assertFalse(second.tryAcquireTaskLock(task), "a different session instance must not take a live task lease");

			first.stop();
			assertTrue(Files.isRegularFile(sidecar), "the second session remains open after the first releases its lease");
			assertTrue(second.tryAcquireTaskLock(task), "the surviving session can acquire the task after its lease is released");
			second.stop();
			assertFalse(Files.exists(sidecar), "the final session may retire the sidecar after releasing its lease");
		} finally {
			first.stop();
			second.stop();
		}
	}

	@Test
	void expiredLocalLeaseCannotBeMistakenForOwnershipAfterAnotherSessionAcquiresIt() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("same-user-expired-lease.mpo"));
		Project project = project();
		NormalTask task = project.createScriptedTask();
		long taskId = task.getUniqueId();
		CollaborationMetadataStore store = new CollaborationMetadataStore(projectFile.toFile());
		CollaborationSession first = new CollaborationSession(project, projectFile.toString(), "alice");
		CollaborationSession second = new CollaborationSession(project, projectFile.toString(), "bob");

		try {
			first.start();
			second.start();
			assertTrue(first.tryAcquireTaskLock(task));
			assertTrue(first.getLocalLocks().contains(taskId));

			store.mutate(metadata -> {
				CollaborationMetadataStore.LockRecord lease = metadata.getLocks().get(Long.toString(taskId));
				assertNotNull(lease);
				lease.setLeaseUntil(System.currentTimeMillis() - 1L);
			});
			assertTrue(second.tryAcquireTaskLock(task), "the second session takes the expired lease");

			assertFalse(first.tryAcquireTaskLock(task), "the first session's local cache must not override the current metadata owner");
			assertFalse(first.getLocalLocks().contains(taskId), "the stale local lock is forgotten");
			task.setName("Bob's saved update");
			CollaborationMetadataStore.LockRecord currentOwner = store.load().getLocks().get(Long.toString(taskId));
			assertNotNull(currentOwner);
			assertEquals("bob", currentOwner.getUserKey());
			assertTrue(second.getLocalLocks().contains(taskId));

			first.stop();
			currentOwner = store.load().getLocks().get(Long.toString(taskId));
			assertNotNull(currentOwner, "closing the stale owner must preserve the new owner's lease");
			assertEquals("bob", currentOwner.getUserKey());
			assertEquals("Bob's saved update", project.findByUniqueId(taskId).getName(), "the task update remains available to its current owner");
		} finally {
			first.stop();
			second.stop();
		}
	}

	@Test
	void legacyCanonicalUserRecordMigratesBeforeAnotherSessionRegisters() {
		CollaborationMetadataStore.Metadata metadata = new CollaborationMetadataStore.Metadata();
		CollaborationMetadataStore.UserRecord legacy = new CollaborationMetadataStore.UserRecord();
		legacy.setUserKey("alice");
		legacy.setDisplayName("Alice legacy");
		legacy.setClientInstanceId("legacy-instance");
		legacy.setLastSeenAt(10_000L);
		metadata.getUsers().put("alice", legacy);

		CollaborationMetadataStore.upsertSession(metadata, "alice", "Alice new", "new-instance", 10_100L);

		CollaborationMetadataStore.UserRecord migrated = metadata.getUsers().get("__microproject_session_v1__legacy-instance");
		assertNotNull(migrated, "the live legacy session is retained under its reserved session key");
		assertEquals("Alice legacy", migrated.getDisplayName());
		assertEquals("legacy-instance", migrated.getClientInstanceId());
		assertEquals("new-instance", metadata.getUsers().get("alice").getClientInstanceId());
		assertTrue(CollaborationMetadataStore.hasFreshSession(metadata, 10_100L));
	}

	@Test
	void staleSessionEntriesExpireUsingSuppliedTimeWithoutSleeping() {
		CollaborationMetadataStore.Metadata metadata = new CollaborationMetadataStore.Metadata();
		CollaborationMetadataStore.upsertSession(metadata, "alice", "Alice", "old-instance", 1_000L);
		assertTrue(metadata.getUsers().containsKey("__microproject_session_v1__old-instance"));

		long afterExpiry = 1_000L + CollaborationMetadataStore.SESSION_TTL_MS + 1L;
		CollaborationMetadataStore.upsertSession(metadata, "alice", "Alice", "new-instance", afterExpiry);

		assertFalse(metadata.getUsers().containsKey("__microproject_session_v1__old-instance"));
		assertTrue(metadata.getUsers().containsKey("__microproject_session_v1__new-instance"));
		assertTrue(CollaborationMetadataStore.hasFreshSession(metadata, afterExpiry));
	}

	@Test
	void expiredSessionCanReacquireAndBePromotedOverTheExpiredCanonicalRecord() {
		CollaborationMetadataStore.Metadata metadata = new CollaborationMetadataStore.Metadata();
		CollaborationMetadataStore.upsertSession(metadata, "alice", "Alice", "old-instance", 1_000L);
		long reacquiredAt = 1_000L + CollaborationMetadataStore.SESSION_TTL_MS + 1L;
		CollaborationMetadataStore.upsertSession(metadata, "alice", "Alice new", "new-instance", reacquiredAt);
		assertFalse(metadata.getUsers().containsKey("__microproject_session_v1__old-instance"));

		long oldSessionReacquiredAt = reacquiredAt + 1L;
		CollaborationMetadataStore.upsertSession(metadata, "alice", "Alice reopened", "old-instance", oldSessionReacquiredAt);
		assertTrue(metadata.getUsers().containsKey("__microproject_session_v1__old-instance"));
		assertFalse(CollaborationMetadataStore.removeSession(metadata, "alice", "new-instance", oldSessionReacquiredAt),
				"closing one instance must report that the reactivated sibling remains");
		assertEquals("old-instance", metadata.getUsers().get("alice").getClientInstanceId(),
				"the reactivated session must become canonical when the expired sibling closes");
		assertTrue(CollaborationMetadataStore.hasFreshSession(metadata, oldSessionReacquiredAt));
		assertTrue(CollaborationMetadataStore.removeSession(metadata, "alice", "old-instance", oldSessionReacquiredAt));
		assertFalse(CollaborationMetadataStore.hasFreshSession(metadata, oldSessionReacquiredAt));
	}

	@Test
	void zeroTimestampSessionRecordIsPreservedAsUnknownLiveness() {
		CollaborationMetadataStore.Metadata metadata = new CollaborationMetadataStore.Metadata();
		CollaborationMetadataStore.UserRecord unknown = new CollaborationMetadataStore.UserRecord();
		unknown.setUserKey("alice");
		unknown.setDisplayName("Alice");
		unknown.setClientInstanceId("unknown-time-instance");
		unknown.setLastSeenAt(0L);
		metadata.getUsers().put("__microproject_session_v1__unknown-time-instance", unknown);

		long now = CollaborationMetadataStore.SESSION_TTL_MS * 10L;
		CollaborationMetadataStore.upsertSession(metadata, "alice", "Alice current", "current-instance", now);

		assertEquals("unknown-time-instance", metadata.getUsers()
				.get("__microproject_session_v1__unknown-time-instance").getClientInstanceId());
		assertTrue(CollaborationMetadataStore.hasFreshSession(metadata, now),
				"a record without a timestamp is retained conservatively because it cannot be proven stale");
	}

	@Test
	void reservedSessionKeyCollisionPreservesExistingUserRecord() {
		CollaborationMetadataStore.Metadata metadata = new CollaborationMetadataStore.Metadata();
		CollaborationMetadataStore.UserRecord canonical = new CollaborationMetadataStore.UserRecord();
		canonical.setUserKey("alice");
		canonical.setDisplayName("Alice");
		canonical.setClientInstanceId("alice-instance");
		canonical.setLastSeenAt(10_000L);
		metadata.getUsers().put("alice", canonical);
		CollaborationMetadataStore.UserRecord occupant = new CollaborationMetadataStore.UserRecord();
		occupant.setUserKey("bob");
		occupant.setDisplayName("Bob's data");
		occupant.setClientInstanceId("other-instance");
		occupant.setLastSeenAt(10_000L);
		metadata.getUsers().put("__microproject_session_v1__collision", occupant);

		assertThrows(IllegalStateException.class,
				() -> CollaborationMetadataStore.upsertSession(metadata, "alice", "Alice changed", "collision", 10_100L));

		assertEquals("Bob's data", metadata.getUsers().get("__microproject_session_v1__collision").getDisplayName());
		assertEquals("other-instance", metadata.getUsers().get("__microproject_session_v1__collision").getClientInstanceId());
		assertEquals("alice-instance", metadata.getUsers().get("alice").getClientInstanceId());
	}

	@Test
	void logicalUserKeyCannotCollideWithAnActiveSessionRecordKey() throws Exception {
		CollaborationMetadataStore.Metadata metadata = new CollaborationMetadataStore.Metadata();
		CollaborationMetadataStore.upsertSession(metadata, "alice", "Alice", "alice-instance", 10_000L);
		String reservedKey = metadata.getUsers().keySet().stream()
				.filter(key -> key.startsWith("__microproject_session_v1__")).findFirst().orElseThrow();
		Set<String> originalKeys = Set.copyOf(metadata.getUsers().keySet());
		CollaborationMetadataStore.UserRecord original = metadata.getUsers().get(reservedKey);
		String originalUser = original.getUserKey();
		String originalClient = original.getClientInstanceId();
		String originalDisplay = original.getDisplayName();

		assertThrows(IllegalStateException.class,
				() -> CollaborationMetadataStore.upsertSession(metadata, reservedKey, "Impostor", "other-instance", 10_100L));

		assertEquals(originalKeys, metadata.getUsers().keySet());
		assertEquals(originalUser, metadata.getUsers().get(reservedKey).getUserKey());
		assertEquals(originalClient, metadata.getUsers().get(reservedKey).getClientInstanceId());
		assertEquals(originalDisplay, metadata.getUsers().get(reservedKey).getDisplayName());
	}

	@Test
	void concurrentSameUserStartsAndStopsRetireOnlyAfterBothClose() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("same-user-concurrent.mpo"));
		Path sidecar = CollaborationMetadataStore.buildSidecarFile(projectFile.toFile()).toPath();
		CollaborationSession first = new CollaborationSession(null, projectFile.toString(), "alice");
		CollaborationSession second = new CollaborationSession(null, projectFile.toString(), "alice");
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch startTogether = new CountDownLatch(1);
		try {
			Future<?> firstStarted = executor.submit(() -> { startTogether.await(); first.start(); return null; });
			Future<?> secondStarted = executor.submit(() -> { startTogether.await(); second.start(); return null; });
			startTogether.countDown();
			firstStarted.get();
			secondStarted.get();
			assertTrue(Files.isRegularFile(sidecar));

			CountDownLatch stopTogether = new CountDownLatch(1);
			Future<?> firstStopped = executor.submit(() -> { stopTogether.await(); first.stop(); return null; });
			Future<?> secondStopped = executor.submit(() -> { stopTogether.await(); second.stop(); return null; });
			stopTogether.countDown();
			firstStopped.get();
			secondStopped.get();
			assertFalse(Files.exists(sidecar), "concurrent final closes leave no stale sidecar");
		} finally {
			first.stop();
			second.stop();
			executor.shutdownNow();
		}
	}

	@Test
	void childProcessClosingSameUserSessionCannotRemoveParentSidecar() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("same-user-process.mpo"));
		Path sidecar = CollaborationMetadataStore.buildSidecarFile(projectFile.toFile()).toPath();
		CollaborationSession parent = new CollaborationSession(null, projectFile.toString(), "alice");
		Path childLog = tempDir.resolve("child-session.log");
		Process child = null;
		try {
			parent.start();
			child = startChildSession(projectFile, childLog);
			awaitFile(tempDir.resolve("child-session.ready"), child);
			assertTrue(Files.isRegularFile(sidecar), "both processes have registered their same-user sessions");

			Files.writeString(tempDir.resolve("child-session.stop"), "stop");
			awaitFile(tempDir.resolve("child-session.stopped"), child);
			assertTrue(child.waitFor(10, TimeUnit.SECONDS), "the child should exit after stopping its session");
			assertEquals(0, child.exitValue(), Files.exists(childLog) ? Files.readString(childLog) : "no child log");
			assertTrue(Files.isRegularFile(sidecar), "the parent process still has an open session");

			parent.stop();
			assertFalse(Files.exists(sidecar), "the final parent close may remove the sidecar");
		} finally {
			parent.stop();
			if (child != null && child.isAlive()) child.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
		}
	}

	@Test
	void pausedSameSessionAcquireKeepsExistingHeldLockVisible() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("same-user-paused-acquire.mpo"));
		Project project = project();
		NormalTask task = project.createScriptedTask();
		ControlledStore store = new ControlledStore(projectFile.toFile());
		CollaborationSession session = new CollaborationSession(project, projectFile.toString(), "alice", store);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			session.start();
			assertTrue(session.tryAcquireTaskLock(task));
			store.pauseNextTransaction();
			Future<Boolean> acquiring = executor.submit(() -> session.tryAcquireTaskLock(task));
			assertTrue(store.awaitTransaction(5, TimeUnit.SECONDS), "the second acquisition reaches the controlled transaction");
			assertTrue(session.getLocalLocks().contains(task.getUniqueId()), "a pending renewal/acquire must retain the held state");
			store.resumeTransaction();
			assertTrue(acquiring.get(5, TimeUnit.SECONDS));
			assertTrue(session.getLocalLocks().contains(task.getUniqueId()));
		} finally {
			store.resumeTransaction();
			session.stop();
			executor.shutdownNow();
		}
	}

	@Test
	void metadataIoFailureDuringAcquirePreservesPreviousBaselineAndHeldState() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("same-user-acquire-failure.mpo"));
		Project project = project();
		NormalTask task = project.createScriptedTask();
		ControlledStore store = new ControlledStore(projectFile.toFile());
		CollaborationSession session = new CollaborationSession(project, projectFile.toString(), "alice", store);
		try {
			session.start();
			assertTrue(session.tryAcquireTaskLock(task));
			Map<?, ?> baselineBefore = baselineMap(session);
			Object baselineStateBefore = baselineBefore.get(Long.valueOf(task.getUniqueId()));
			assertNotNull(baselineStateBefore);

			store.failNextTransaction();
			assertThrows(CollaborationLockUnavailableException.class, () -> session.tryAcquireTaskLock(task));

			assertTrue(session.getLocalLocks().contains(task.getUniqueId()), "an unknown transaction outcome keeps the existing held lease renewable");
			assertEquals(baselineStateBefore, baselineMap(session).get(Long.valueOf(task.getUniqueId())),
					"a transient metadata failure must preserve the prior comparison baseline");
		} finally {
			session.stop();
		}
	}

	@Test
	@ResourceLock(Resources.SYSTEM_PROPERTIES)
	void stoppingFinalSessionPersistsChangedWorkspaceForNextSession() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("workspace-stop.mpo"));
		Project project = project();
		TestWorkspace changedWorkspace = new TestWorkspace("latest-view-state");
		project.setCollaborationWorkspace(changedWorkspace);
		Path sidecar = CollaborationMetadataStore.buildSidecarFile(projectFile.toFile()).toPath();
		CollaborationSession closing = new CollaborationSession(project, projectFile.toString(), "alice");
		String previousRoot = isolatedWorkspaceRoot();
		try {
			closing.start();
			closing.stop();
			assertFalse(Files.exists(sidecar), "a final close retires the shared metadata after preserving the workspace locally");

			CollaborationSession reopening = new CollaborationSession(null, projectFile.toString(), "alice");
			WorkspaceSetting restored = reopening.loadWorkspace();
			assertEquals("latest-view-state", ((TestWorkspace) restored).marker);
			String localPayload = new LocalWorkspaceStore(projectFile.toFile(), "alice").load();
			assertNotNull(localPayload, "the workspace is stored outside the retired sidecar");
		} finally {
			closing.stop();
			restoreWorkspaceRoot(previousRoot);
		}
	}

	@Test
	@ResourceLock(Resources.SYSTEM_PROPERTIES)
	void stoppingOneSameUserSessionPreservesWorkspaceForOtherOpenSession() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("workspace-peer-session.mpo"));
		Project project = project();
		project.setCollaborationWorkspace(new TestWorkspace("shared-user-view"));
		Path sidecar = CollaborationMetadataStore.buildSidecarFile(projectFile.toFile()).toPath();
		CollaborationSession closing = new CollaborationSession(project, projectFile.toString(), "alice");
		CollaborationSession stillOpen = new CollaborationSession(null, projectFile.toString(), "alice");
		String previousRoot = isolatedWorkspaceRoot();
		try {
			closing.start();
			stillOpen.start();
			closing.stop();
			assertTrue(Files.isRegularFile(sidecar), "another same-user session keeps shared metadata available");
			UserWorkspaceState workspace = new CollaborationMetadataStore(projectFile.toFile())
					.load().getPerUserWorkspace().get("alice");
			assertNotNull(workspace);
			assertNotNull(workspace.getWorkspacePayload());
			assertEquals("shared-user-view", ((TestWorkspace) stillOpen.loadWorkspace()).marker);
		} finally {
			closing.stop();
			stillOpen.stop();
			restoreWorkspaceRoot(previousRoot);
		}
	}

	@Test
	void externalProjectRefreshDoesNotSeeExpiredLocalLockAsStillOwned() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("external-refresh-stale-lock.mpo"));
		Project project = project();
		NormalTask task = project.createScriptedTask();
		long taskId = task.getUniqueId();
		CollaborationMetadataStore store = new CollaborationMetadataStore(projectFile.toFile());
		CollaborationSession first = new CollaborationSession(project, projectFile.toString(), "alice");
		CollaborationSession second = new CollaborationSession(project, projectFile.toString(), "bob");
		AtomicBoolean reloadSawStaleOwnership = new AtomicBoolean(true);
		try {
			first.start();
			second.start();
			assertTrue(first.tryAcquireTaskLock(task));
			store.mutate(metadata -> metadata.getLocks().get(Long.toString(taskId))
					.setLeaseUntil(System.currentTimeMillis() - 1L));
			assertTrue(second.tryAcquireTaskLock(task));
			first.setExternalReloadHandler(reloaded -> reloadSawStaleOwnership.set(first.getLocalLocks().contains(taskId)));
			Files.writeString(projectFile, "updated by another process");

			long now = System.currentTimeMillis() + 10_000L;
			first.pollAt(now);
			first.pollAt(now + 2_000L);

			assertFalse(reloadSawStaleOwnership.get(), "external refresh must not treat a lease now owned by bob as locally held by alice");
			assertTrue(second.getLocalLocks().contains(taskId));
		} finally {
			first.stop();
			second.stop();
		}
	}

	@Test
	void stoppedSessionRejectsExternalRefreshLockReconciliation() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("stopped-refresh-reconciliation.mpo"));
		CollaborationSession session = new CollaborationSession(null, projectFile.toString(), "alice");
		session.stop();
		assertThrows(CollaborationLockUnavailableException.class, session::beginExternalProjectRefresh);
	}

	@Test
	void pendingTaskAcquireAbortsExternalRefreshAndCanRetryAfterAcquireFinishes() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("external-refresh-pending-acquire.mpo"));
		Project project = project();
		NormalTask task = project.createScriptedTask();
		ControlledStore store = new ControlledStore(projectFile.toFile());
		CollaborationSession session = new CollaborationSession(project, projectFile.toString(), "alice", store);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			session.start();
			assertTrue(session.tryAcquireTaskLock(task));
			store.pauseNextTransaction();
			Future<Boolean> acquiring = executor.submit(() -> session.tryAcquireTaskLock(task));
			assertTrue(store.awaitTransaction(5, TimeUnit.SECONDS), "the task acquisition is paused inside metadata transaction");
			assertTrue(session.getLocalLocks().contains(task.getUniqueId()), "the pending retry keeps the previously held lock visible");
			assertThrows(CollaborationLockUnavailableException.class, session::beginExternalProjectRefresh,
					"external refresh must abort while an acquisition has an unresolved outcome");

			store.resumeTransaction();
			assertTrue(acquiring.get(5, TimeUnit.SECONDS));
			assertTrue(session.getLocalLocks().contains(task.getUniqueId()), "the prior held lease survives the acquire-versus-refresh race");
			try (CollaborationSession.ExternalProjectRefreshLease refresh = session.beginExternalProjectRefresh()) {
				assertEquals(Set.of(task.getUniqueId()), refresh.getLockedTaskIds(),
						"refresh can retry after the pending acquire reaches a settled state");
			}
		} finally {
			store.resumeTransaction();
			session.stop();
			executor.shutdownNow();
		}
	}

	@Test
	void pendingMultiTaskBatchBlocksRefreshUntilConflictRollbackCompletes() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("external-refresh-batch-guard.mpo"));
		Project project = project();
		NormalTask firstTask = project.createScriptedTask();
		NormalTask conflictingTask = project.createScriptedTask();
		CollaborationSession batchSession = new CollaborationSession(project, projectFile.toString(), "alice");
		CollaborationSession ownerSession = new CollaborationSession(project, projectFile.toString(), "bob");
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch beforeSecondTask = new CountDownLatch(1);
		CountDownLatch continueBatch = new CountDownLatch(1);
		try {
			batchSession.start();
			ownerSession.start();
			assertTrue(ownerSession.tryAcquireTaskLock(conflictingTask));

			Iterable<Task> tasks = () -> new Iterator<>() {
				private int index;
				@Override public boolean hasNext() { return index < 2; }
				@Override public Task next() {
					if (index++ == 0) return firstTask;
					beforeSecondTask.countDown();
					try {
						if (!continueBatch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("batch barrier timed out");
					} catch (InterruptedException interrupted) {
						Thread.currentThread().interrupt();
						throw new IllegalStateException("batch barrier interrupted", interrupted);
					}
					return conflictingTask;
				}
			};
			Future<Task> batch = executor.submit(() -> batchSession.tryAcquireTasks(tasks));
			assertTrue(beforeSecondTask.await(5, TimeUnit.SECONDS), "the batch has acquired its first task and paused before the second");
			assertTrue(batchSession.getLocalLocks().contains(firstTask.getUniqueId()));
			assertThrows(CollaborationLockUnavailableException.class, batchSession::beginExternalProjectRefresh,
					"refresh cannot snapshot a partial batch while its later task and rollback remain pending");

			continueBatch.countDown();
			assertEquals(conflictingTask, batch.get(5, TimeUnit.SECONDS), "the conflicting second task aborts the batch");
			assertTrue(batchSession.getLocalLocks().isEmpty(), "the first task is rolled back before refresh retries");
			assertTrue(ownerSession.getLocalLocks().contains(conflictingTask.getUniqueId()));
			try (CollaborationSession.ExternalProjectRefreshLease refresh = batchSession.beginExternalProjectRefresh()) {
				assertTrue(refresh.getLockedTaskIds().isEmpty(), "retry sees the settled post-rollback lock set");
			}
		} finally {
			continueBatch.countDown();
			batchSession.stop();
			ownerSession.stop();
			executor.shutdownNow();
		}
	}

	@Test
	void duplicateExternalRefreshDoesNotReleaseFirstGateAndCloseReleasesIt() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("external-refresh-gated-acquire.mpo"));
		Project project = project();
		NormalTask task = project.createScriptedTask();
		CollaborationSession session = new CollaborationSession(project, projectFile.toString(), "alice");
		try {
			session.start();
			try (CollaborationSession.ExternalProjectRefreshLease firstRefresh = session.beginExternalProjectRefresh()) {
				assertTrue(firstRefresh.getLockedTaskIds().isEmpty());
				assertThrows(CollaborationLockUnavailableException.class, session::beginExternalProjectRefresh,
						"a concurrent refresh cannot acquire a second gate token");
				assertThrows(CollaborationLockUnavailableException.class, () -> session.tryAcquireTaskLock(task),
						"managed acquisition cannot start while the first refresh holds the gate");
				assertFalse(new CollaborationMetadataStore(projectFile.toFile()).load().getLocks()
						.containsKey(Long.toString(task.getUniqueId())), "a rejected acquisition does not write a lease");
			}
			assertTrue(session.tryAcquireTaskLock(task), "closing the first refresh lease releases the gate");
			assertThrows(IllegalStateException.class, () -> {
				try (CollaborationSession.ExternalProjectRefreshLease ignored = session.beginExternalProjectRefresh()) {
					throw new IllegalStateException("injected merge failure");
				}
			});
			try (CollaborationSession.ExternalProjectRefreshLease retry = session.beginExternalProjectRefresh()) {
				assertEquals(Set.of(task.getUniqueId()), retry.getLockedTaskIds(), "exception unwinding releases only its own gate token");
			}
		} finally {
			session.stop();
		}
	}

	@Test
	void stopDefersLockReleaseAndRetirementUntilActiveRefreshLeaseCloses() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("external-refresh-stop-deferred.mpo"));
		Project project = project();
		NormalTask task = project.createScriptedTask();
		NormalTask rejectedAfterStop = project.createScriptedTask();
		long taskId = task.getUniqueId();
		Path sidecar = CollaborationMetadataStore.buildSidecarFile(projectFile.toFile()).toPath();
		CollaborationMetadataStore store = new CollaborationMetadataStore(projectFile.toFile());
		CollaborationSession session = new CollaborationSession(project, projectFile.toString(), "alice");
		CollaborationSession.ExternalProjectRefreshLease refresh = null;
		try {
			session.start();
			assertTrue(session.tryAcquireTaskLock(task));
			refresh = session.beginExternalProjectRefresh();
			session.stop();
			assertTrue(Files.isRegularFile(sidecar), "stop defers retirement while an external refresh owns the token");
			assertTrue(session.getLocalLocks().contains(taskId), "stop defers lease release until the refresh hands off");
			assertThrows(CollaborationLockUnavailableException.class, () -> session.tryAcquireTaskLock(rejectedAfterStop),
					"stopped sessions reject new acquisition while finalization is deferred");
			assertTrue(store.load().getLocks().containsKey(Long.toString(taskId)));

			refresh.close();
			refresh = null;
			awaitFileAbsent(sidecar, 5, TimeUnit.SECONDS);
			assertFalse(Files.exists(sidecar), "closing the refresh lease completes deferred stop and cleanup");
		} finally {
			if (refresh != null) refresh.close();
			session.stop();
		}
	}

	@Test
	@ResourceLock(Resources.SYSTEM_PROPERTIES)
	void closeJobWaitsForRefreshFinalizationWorkspacePersistenceAndCleanup() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("external-refresh-close-job.mpo"));
		Project project = project();
		project.setCollaborationWorkspace(new TestWorkspace("close-job-final-view"));
		NormalTask task = project.createScriptedTask();
		Path sidecar = CollaborationMetadataStore.buildSidecarFile(projectFile.toFile()).toPath();
		CollaborationSession session = new CollaborationSession(project, projectFile.toString(), "alice");
		String previousRoot = isolatedWorkspaceRoot();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		CollaborationSession.ExternalProjectRefreshLease refresh = null;
		try {
			session.start();
			assertTrue(session.tryAcquireTaskLock(task));
			refresh = session.beginExternalProjectRefresh();
			Future<Boolean> closeJob = executor.submit(() -> session.stopAndAwaitClose(10_000L));
			awaitSessionStopped(session, 5, TimeUnit.SECONDS);
			assertFalse(closeJob.isDone(), "the close job must wait while the refresh still owns the finalization token");
			assertTrue(Files.isRegularFile(sidecar), "the sidecar remains until close finalization owns the refresh handoff");
			assertNull(new LocalWorkspaceStore(projectFile.toFile(), "alice").load(), "workspace persistence has not run before token release");

			refresh.close();
			refresh = null;
			assertTrue(closeJob.get(10, TimeUnit.SECONDS), "the close job returns only after finalization completes");
			assertFalse(Files.exists(sidecar), "the close job's completion implies sidecar cleanup completed");
			CollaborationSession reopening = new CollaborationSession(null, projectFile.toString(), "alice");
			assertEquals("close-job-final-view", ((TestWorkspace) reopening.loadWorkspace()).marker,
					"the workspace is durable before the close job completes");
		} finally {
			if (refresh != null) refresh.close();
			session.stop();
			executor.shutdownNow();
			restoreWorkspaceRoot(previousRoot);
		}
	}

	private Process startChildSession(Path projectFile, Path logFile) throws Exception {
		String java = Path.of(System.getProperty("java.home"), "bin",
				System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").toString();
		return new ProcessBuilder(java, "-cp", testRuntimeClasspath(), ChildSessionProcess.class.getName(),
				projectFile.toString(), tempDir.toString())
			.redirectErrorStream(true).redirectOutput(logFile.toFile()).start();
	}

	private static String testRuntimeClasspath() throws Exception {
		Set<Path> locations = new LinkedHashSet<>();
		for (Class<?> type : new Class<?>[] {ChildSessionProcess.class, CollaborationSession.class,
				com.fasterxml.jackson.databind.ObjectMapper.class, com.fasterxml.jackson.core.JsonFactory.class,
				com.fasterxml.jackson.annotation.JsonAnySetter.class}) {
			locations.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()));
		}
		String separator = System.getProperty("path.separator");
		return String.join(separator, locations.stream().map(Path::toString).toList());
	}

	private static void awaitFile(Path path, Process child) throws Exception {
		long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
		while (!Files.exists(path) && System.nanoTime() < deadline && child.isAlive()) Thread.sleep(25L);
		assertTrue(Files.exists(path), "timed out waiting for " + path.getFileName()
				+ (child.isAlive() ? "" : "; child exited with " + child.exitValue()));
	}

	private static void awaitFileAbsent(Path path, long timeout, TimeUnit unit) throws InterruptedException {
		long deadline = System.nanoTime() + unit.toNanos(timeout);
		while (Files.exists(path) && System.nanoTime() < deadline) Thread.sleep(20L);
	}

	public static final class ChildSessionProcess {
		public static void main(String[] args) throws Exception {
			Path projectFile = Path.of(args[0]);
			Path control = Path.of(args[1]);
			CollaborationSession session = new CollaborationSession(null, projectFile.toString(), "alice");
			session.start();
			try {
				Files.writeString(control.resolve("child-session.ready"), "ready");
				long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
				while (!Files.exists(control.resolve("child-session.stop")) && System.nanoTime() < deadline) Thread.sleep(25L);
				if (!Files.exists(control.resolve("child-session.stop"))) throw new IllegalStateException("timed out waiting for parent stop signal");
			} finally {
				session.stop();
				Files.writeString(control.resolve("child-session.stopped"), "stopped");
			}
		}
	}

	private static Project project() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("test", undoController), undoController);
		project.initialize(false, false);
		return project;
	}

	private static Map<?, ?> baselineMap(CollaborationSession session) throws Exception {
		Field field = CollaborationSession.class.getDeclaredField("lockBaselineStates");
		field.setAccessible(true);
		return (Map<?, ?>) field.get(session);
	}

	private static void awaitSessionStopped(CollaborationSession session, long timeout, TimeUnit unit) throws Exception {
		Field stopped = CollaborationSession.class.getDeclaredField("stopped");
		stopped.setAccessible(true);
		long deadline = System.nanoTime() + unit.toNanos(timeout);
		while (!stopped.getBoolean(session) && System.nanoTime() < deadline) Thread.sleep(20L);
		assertTrue(stopped.getBoolean(session), "the close job should mark the session closing before waiting for its refresh token");
	}

	private String isolatedWorkspaceRoot() {
		String previous = System.getProperty(com.microproject.temporary.TemporaryRoot.ROOT_PROPERTY);
		System.setProperty(com.microproject.temporary.TemporaryRoot.ROOT_PROPERTY, tempDir.resolve("temporary-root").toString());
		return previous;
	}

	private static void restoreWorkspaceRoot(String previous) {
		if (previous == null) System.clearProperty(com.microproject.temporary.TemporaryRoot.ROOT_PROPERTY);
		else System.setProperty(com.microproject.temporary.TemporaryRoot.ROOT_PROPERTY, previous);
	}

	private static final class ControlledStore extends CollaborationMetadataStore {
		private final AtomicBoolean pauseNext = new AtomicBoolean();
		private final AtomicBoolean failNext = new AtomicBoolean();
		private volatile CountDownLatch transactionEntered = new CountDownLatch(0);
		private volatile CountDownLatch resumeTransaction = new CountDownLatch(0);

		private ControlledStore(java.io.File projectFile) {
			super(projectFile);
		}

		void pauseNextTransaction() {
			transactionEntered = new CountDownLatch(1);
			resumeTransaction = new CountDownLatch(1);
			pauseNext.set(true);
		}

		boolean awaitTransaction(long timeout, TimeUnit unit) throws InterruptedException {
			return transactionEntered.await(timeout, unit);
		}

		void resumeTransaction() {
			resumeTransaction.countDown();
		}

		void failNextTransaction() {
			failNext.set(true);
		}

		@Override
		public <T> T withLockedMetadata(CollaborationMetadataStore.MetadataCallback<T> callback) {
			if (failNext.compareAndSet(true, false)) throw new CollaborationLockUnavailableException("injected metadata I/O failure");
			if (pauseNext.compareAndSet(true, false)) {
				transactionEntered.countDown();
				try {
					if (!resumeTransaction.await(5, TimeUnit.SECONDS)) throw new CollaborationLockUnavailableException("test transaction timed out");
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					throw new CollaborationLockUnavailableException("test transaction interrupted", interrupted);
				}
			}
			return super.withLockedMetadata(callback);
		}
	}

	private static final class TestWorkspace implements WorkspaceSetting {
		private static final long serialVersionUID = 1L;
		private final String marker;

		private TestWorkspace(String marker) {
			this.marker = marker;
		}
	}
}
