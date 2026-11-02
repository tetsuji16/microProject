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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Base64;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.collaboration.CollaborationMetadataStore.Metadata;
import com.microproject.collaboration.CollaborationMetadataStore.UserRecord;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.session.FileHelper;
import com.microproject.util.SafeObjectInput;
import com.microproject.workspace.WorkspaceSetting;
import com.microproject.util.RetirableFileLock;

public class CollaborationSession {
	private static final Logger logger = Logger.getLogger(CollaborationSession.class.getName());
	private static final long POLL_INTERVAL_MS = 2000L;
	private static final long HEARTBEAT_INTERVAL_MS = 5000L;
	private static final long AUTO_RELOAD_SETTLE_MS = 1500L;

	public static final int SAVE_CANCEL = 0;
	public static final int SAVE_PROCEED = 1;
	public static final int SAVE_AS_COPY = 2;
	public static final long CLOSE_OPERATION_WAIT_TIMEOUT_MS = 3000L;

	public interface ExternalProjectReloadHandler {
		void reload(Project project);
	}

	private final Project project;
	private final File projectFile;
	private final CollaborationMetadataStore store;
	private final TaskLockManager lockManager;
	private final String userKey;
	private final String displayName;
	private final String clientInstanceId;
	private final Map<Long, ProjectMergeService.TaskState> lockBaselineStates = new LinkedHashMap<Long, ProjectMergeService.TaskState>();
	private final Map<Long, Long> lockBaselineGenerations = new LinkedHashMap<Long, Long>();
	private volatile ExternalProjectReloadHandler externalReloadHandler;
	private volatile Consumer<String> externalChangeNoticeHandler;
	private volatile Timer timer;
	private final Object stateLock = new Object();
	private final Object lifecycleLock = new Object();
	private final AtomicReference<Object> operationGateToken = new AtomicReference<Object>();
	private final ThreadLocal<Object> acquisitionBatchToken = new ThreadLocal<Object>();
	private final AtomicBoolean stopFinalizationStarted = new AtomicBoolean();
	private final CompletableFuture<Void> stopFinalization = new CompletableFuture<Void>();
	private volatile boolean stopDeferredUntilOperationCompletes;
	private boolean closeWaitOwnsFinalization;
	private boolean stopFinalizationAbandoned;
	private volatile long lastKnownProjectModified;
	private volatile long lastKnownProjectLength;
	private volatile KnownMetadataState lastKnownMetadataState;
	private volatile boolean externalChangePending;
	private boolean externalRefreshFailureWarned;
	private volatile boolean externalChangeWarned;
	private volatile boolean externalReloadRequested;
	private volatile boolean pendingExternalReload;
	private volatile long pendingExternalReloadModified;
	private volatile long pendingExternalReloadLength;
	private volatile long pendingExternalReloadDetectedAt;
	private volatile long lastHeartbeatAt;
	private volatile boolean projectStatsRefreshPending;
	private volatile boolean stopped;

	public CollaborationSession(Project project, String fileName, String userKey) {
		this(project, fileName, userKey, new CollaborationMetadataStore(fileName == null ? null : new File(fileName)));
	}

	CollaborationSession(Project project, String fileName, String userKey, CollaborationMetadataStore store) {
		this.project = project;
		this.projectFile = fileName == null ? null : new File(fileName);
		this.userKey = CollaborationUserIdentity.normalize(userKey);
		this.displayName = this.userKey;
		this.clientInstanceId = UUID.randomUUID().toString();
		this.store = store;
		this.lockManager = new TaskLockManager(store, this.userKey, this.displayName, this.clientInstanceId, true,
				() -> !stopped, this::isTaskAcquisitionAllowed);
		refreshKnownFileStats();
	}

	public static CollaborationSession create(Project project, String fileName, String userKey) {
		if (project == null || fileName == null || !CollaborationMetadataStore.isMpoCollaborationCandidate(fileName)) {
			return null;
		}
		File sidecarFile = CollaborationMetadataStore.buildSidecarFile(new File(fileName));
		File sidecarParent = sidecarFile.getParentFile();
		if (sidecarParent != null && (!sidecarParent.exists() || !sidecarParent.canWrite())) {
			return null;
		}
		try {
			return new CollaborationSession(project, fileName, userKey);
		} catch (RuntimeException e) {
			logger.log(Level.FINE, "Collaboration metadata is unavailable for " + fileName, e);
			return null;
		}
	}

	public void start() {
		synchronized (lifecycleLock) {
			if (stopped || timer != null) return;
			registerUser();
			refreshKnownState(store.load());
			timer = new Timer("projectlibre-collaboration-" + clientInstanceId, true);
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					runGuardedPoll(CollaborationSession.this::poll);
				}
			}, POLL_INTERVAL_MS, POLL_INTERVAL_MS);
		}
	}

	/**
	 * Runs one scheduled poll without letting a failure escape to the
	 * {@link Timer}.  An uncaught exception cancels a {@code java.util.Timer}
	 * permanently, which would silently stop heartbeats and lock renewal for
	 * the rest of the session (issue #946).  JVM-level errors still propagate.
	 */
	static void runGuardedPoll(Runnable poll) {
		try {
			poll.run();
		} catch (VirtualMachineError fatal) {
			throw fatal;
		} catch (Throwable failure) {
			logger.log(Level.WARNING, "Collaboration poll failed; polling continues", failure);
		}
	}

	/** Ends the session, whether or not it got as far as starting.  Failures never block closing the project. */
	public void stop() {
		synchronized (lifecycleLock) {
			if (stopped) return;
			stopDeferredUntilOperationCompletes = true;
			stopped = true;
		}
		Timer runningTimer = timer;
		timer = null;
		if (runningTimer != null) runningTimer.cancel();
		if (operationGateToken.get() != null) {
			if (operationGateToken.get() == null) finishStop(true);
			return;
		}
		finishStop(false);
	}

	/** Stops collaboration and waits for any in-flight guarded operation off the EDT. */
	public boolean stopAndAwaitClose(long timeoutMillis) {
		if (com.microproject.util.UiDispatch.isDispatchThread()) {
			throw new IllegalStateException("Collaboration close must be coordinated by a background job");
		}
		if (timeoutMillis < 0L) throw new IllegalArgumentException("timeoutMillis must not be negative");
		Timer runningTimer;
		synchronized (lifecycleLock) {
			if (stopFinalizationAbandoned) return false;
			if (!stopped) {
				stopDeferredUntilOperationCompletes = true;
				stopped = true;
			}
			closeWaitOwnsFinalization = !stopFinalizationStarted.get();
			runningTimer = timer;
			timer = null;
		}
		if (runningTimer != null) runningTimer.cancel();

		long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
		while (operationGateToken.get() != null) {
			long remaining = deadline - System.nanoTime();
			if (remaining <= 0L) {
				synchronized (lifecycleLock) {
					if (operationGateToken.get() != null && !stopFinalizationStarted.get()) {
						stopFinalizationAbandoned = true;
						closeWaitOwnsFinalization = false;
						return false;
					}
				}
				break;
			}
			try {
				long sleepMillis = Math.max(1L, Math.min(20L,
						java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining)));
				Thread.sleep(sleepMillis);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				synchronized (lifecycleLock) {
					if (operationGateToken.get() != null && !stopFinalizationStarted.get()) {
						stopFinalizationAbandoned = true;
						closeWaitOwnsFinalization = false;
						return false;
					}
				}
				break;
			}
		}

		boolean runCloseFinalizer;
		synchronized (lifecycleLock) {
			if (stopFinalizationAbandoned) return false;
			if (operationGateToken.get() != null) return false;
			if (System.nanoTime() >= deadline && !stopFinalizationStarted.get()) {
				stopFinalizationAbandoned = true;
				closeWaitOwnsFinalization = false;
				return false;
			}
			closeWaitOwnsFinalization = false;
			runCloseFinalizer = stopFinalizationStarted.compareAndSet(false, true);
		}
		if (runCloseFinalizer) runStopFinalizer(false);
		if (runCloseFinalizer) return !stopFinalization.isCompletedExceptionally();
		long remaining = deadline - System.nanoTime();
		try {
			if (remaining <= 0L && !stopFinalization.isDone()) return false;
			stopFinalization.get(Math.max(0L, remaining), java.util.concurrent.TimeUnit.NANOSECONDS);
			return true;
		} catch (java.util.concurrent.TimeoutException timeout) {
			return false;
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			return false;
		} catch (java.util.concurrent.ExecutionException failure) {
			logger.log(Level.WARNING, "Collaboration session cleanup failed during project close", failure.getCause());
			return false;
		}
	}

	private void finishStop(boolean deferred) {
		synchronized (lifecycleLock) {
			if (stopFinalizationAbandoned || closeWaitOwnsFinalization || operationGateToken.get() != null
					|| !stopFinalizationStarted.compareAndSet(false, true)) return;
		}
		runStopFinalizer(deferred);
	}

	private void runStopFinalizer(boolean deferred) {
		Runnable finalizer = () -> {
			try {
				performStopFinalization();
				stopFinalization.complete(null);
			} catch (Throwable failure) {
				stopFinalization.completeExceptionally(failure);
				logger.log(Level.WARNING, "Collaboration session cleanup failed", failure);
			}
		};
		if (deferred) {
			Thread worker = new Thread(finalizer, "microproject-collaboration-close-" + clientInstanceId);
			worker.setDaemon(true);
			worker.start();
		} else {
			finalizer.run();
		}
	}

	private void performStopFinalization() {
		store.beginClosing();
		try {
			saveWorkspace(project == null ? null : project.getCollaborationWorkspace(), true);
		} catch (Exception e) {
			logger.log(Level.FINE, "Failed to save workspace on session stop", e);
		}
		try {
			lockManager.releaseAll();
		} catch (RuntimeException e) {
			logger.log(Level.WARNING, "Collaboration locks could not be released; leases will expire", e);
		} finally {
			synchronized (lockBaselineStates) { lockBaselineStates.clear(); lockBaselineGenerations.clear(); }
		}
		boolean unregistered = false;
		try {
			store.mutate(metadata -> CollaborationMetadataStore.removeSession(metadata, userKey, clientInstanceId, System.currentTimeMillis()));
			unregistered = true;
		} catch (RuntimeException e) {
			logger.log(Level.WARNING, "Collaboration session metadata could not be unregistered; artifacts stay in place", e);
		}
		if (unregistered) removeProjectArtifacts();
	}

	void awaitStopFinalizationForTests() {
		stopFinalization.join();
	}

	/** Leaves nothing beside the project once no other user needs it (issue #967). */
	private void removeProjectArtifacts() {
		if (projectFile == null) return;
		try {
			LocalWorkspaceStore workspaces = new LocalWorkspaceStore(projectFile, userKey);
			store.retireIfSoleUser(userKey, clientInstanceId, workspaces::save);
		} catch (RuntimeException e) {
			logger.log(Level.WARNING, "Collaboration files stay beside " + projectFile, e);
		}
		Path saveMarker = projectFile.toPath().resolveSibling(projectFile.getName() + ".lock");
		while (Files.isRegularFile(saveMarker) && !RetirableFileLock.retireIfUnheld(saveMarker)) {
			if (!store.pauseWhileClosing()) {
				logger.log(Level.WARNING, "The save marker stays beside {0}: it stayed in use while it closed", projectFile);
				return;
			}
		}
	}

	private void registerUser() {
		registerUser(true);
	}

	private void registerUser(boolean refreshKnownState) {
		if (stopped) return;
		store.mutate(metadata -> {
			if (stopped) return;
			CollaborationMetadataStore.upsertSession(metadata, userKey, displayName, clientInstanceId, System.currentTimeMillis());
			store.refreshProjectStats(metadata);
		});
		if (refreshKnownState) {
			refreshKnownState(store.load());
		}
	}

	private void poll() {
		pollAt(System.currentTimeMillis());
	}

	/** Runs one change-detection cycle at the supplied time for deterministic package tests. */
	void pollAt(long now) {
		if (projectFile == null || stopped) {
			return;
		}
		if (projectStatsRefreshPending) {
			refreshProjectStatsIfPending();
		}
		if (now - lastHeartbeatAt >= HEARTBEAT_INTERVAL_MS) {
			reconcileLostLockBaselines(lockManager.renewAll());
			lastHeartbeatAt = now;
			registerUser(false);
		}
		long projectModified = projectFile.exists() ? projectFile.lastModified() : 0L;
		long projectLength = projectFile.exists() ? projectFile.length() : 0L;
		Metadata metadata = store.load();
		KnownMetadataState currentMetadataState = KnownMetadataState.capture(metadata, userKey, clientInstanceId);

		boolean projectChanged;
		boolean metadataChanged;
		boolean shouldRequestReload = false;

		synchronized (stateLock) {
			projectChanged = projectModified != lastKnownProjectModified || projectLength != lastKnownProjectLength;
			metadataChanged = !currentMetadataState.equals(lastKnownMetadataState);
			if (pendingExternalReload) {
				if (!isPendingProjectStable(projectModified, projectLength)) {
					rememberPendingExternalReload(projectModified, projectLength, now);
					return;
				}
				if (canAutoReloadExternalProject() && now - pendingExternalReloadDetectedAt >= AUTO_RELOAD_SETTLE_MS) {
					shouldRequestReload = true;
				} else if (!canAutoReloadExternalProject()) {
					clearPendingExternalReload();
				}
			}
			if (!shouldRequestReload) {
				if (projectChanged) {
					markExternalProjectChanged(projectModified, projectLength, now);
					return;
				}
				if (metadataChanged) {
					markExternalMetadataChanged();
					if (metadataChanged && !externalChangeWarned) {
						externalChangeWarned = true;
						String message = "This project was updated externally. Save will re-check for conflicts before writing.";
						Consumer<String> noticeHandler = externalChangeNoticeHandler;
						if (noticeHandler == null) {
							logger.warning(message);
						} else {
							noticeHandler.accept(message);
						}
					}
				}
				refreshKnownState(projectModified, projectLength, currentMetadataState);
			}
		}

		if (shouldRequestReload) {
			requestExternalProjectReload(projectModified, projectLength, currentMetadataState);
		}
	}

	private void markExternalProjectChanged(long projectModified, long projectLength, long now) {
		synchronized (stateLock) {
			externalChangePending = true;
			rememberPendingExternalReload(projectModified, projectLength, now);
		}
	}

	private void markExternalMetadataChanged() {
		synchronized (stateLock) {
			externalChangePending = true;
		}
	}

	private boolean isPendingProjectStable(long projectModified, long projectLength) {
		synchronized (stateLock) {
			return projectModified == pendingExternalReloadModified && projectLength == pendingExternalReloadLength;
		}
	}

	private boolean canAutoReloadExternalProject() {
		return project != null
			&& externalReloadHandler != null
			&& !externalReloadRequested;
	}

	private void requestExternalProjectReload(long projectModified, long projectLength, KnownMetadataState currentMetadataState) {
		synchronized (stateLock) {
			externalReloadRequested = true;
			clearPendingExternalReload();
			refreshKnownState(projectModified, projectLength, currentMetadataState);
			resetExternalChangeState(false);
		}
		ExternalProjectReloadHandler handler = externalReloadHandler;
		if (handler != null) {
			handler.reload(project);
		}
	}

	private void refreshKnownFileStats() {
		if (projectFile == null) {
			return;
		}
		refreshKnownState(projectFile.exists() ? projectFile.lastModified() : 0L,
			projectFile.exists() ? projectFile.length() : 0L,
			KnownMetadataState.capture(store.load(), userKey, clientInstanceId));
	}

	private void refreshKnownState(Metadata metadata) {
		long projectModified = projectFile != null && projectFile.exists() ? projectFile.lastModified() : 0L;
		long projectLength = projectFile != null && projectFile.exists() ? projectFile.length() : 0L;
		refreshKnownState(projectModified, projectLength, KnownMetadataState.capture(metadata, userKey, clientInstanceId));
	}

	private void refreshKnownState(long projectModified, long projectLength, KnownMetadataState metadataState) {
		synchronized (stateLock) {
			lastKnownProjectModified = projectModified;
			lastKnownProjectLength = projectLength;
			lastKnownMetadataState = metadataState;
		}
	}

	private void rememberPendingExternalReload(long projectModified, long projectLength, long detectedAt) {
		synchronized (stateLock) {
			pendingExternalReload = true;
			pendingExternalReloadModified = projectModified;
			pendingExternalReloadLength = projectLength;
			pendingExternalReloadDetectedAt = detectedAt;
		}
	}

	private void clearPendingExternalReload() {
		synchronized (stateLock) {
			pendingExternalReload = false;
			pendingExternalReloadModified = 0L;
			pendingExternalReloadLength = 0L;
			pendingExternalReloadDetectedAt = 0L;
		}
	}

	private void rememberLockBaseline(Task task, boolean replace, long generation) {
		if (task == null) {
			return;
		}
		Long taskId = Long.valueOf(task.getUniqueId());
		synchronized (lockBaselineStates) {
			if (!replace && lockBaselineStates.containsKey(taskId)) {
				lockBaselineGenerations.put(taskId, Long.valueOf(generation));
				return;
			}
			ProjectMergeService.TaskState state = ProjectMergeService.TaskState.capture(task);
			if (state != null) {
				lockBaselineStates.put(taskId, state);
				lockBaselineGenerations.put(taskId, Long.valueOf(generation));
			} else {
				lockBaselineStates.remove(taskId);
				lockBaselineGenerations.remove(taskId);
			}
		}
	}

	private void refreshLockBaselines() {
		for (Long taskId : lockManager.getLocalLocks()) {
			Task task = project == null ? null : project.findByUniqueId(taskId.longValue());
			ProjectMergeService.TaskState state = ProjectMergeService.TaskState.capture(task);
			synchronized (lockBaselineStates) {
				if (state != null) lockBaselineStates.put(taskId, state);
				else {
					lockBaselineStates.remove(taskId);
					lockBaselineGenerations.remove(taskId);
				}
			}
		}
	}

	/** Acquires a task lock without showing UI; suitable for background sync. */
	public boolean tryAcquireTaskLock(Task task) {
		Object token = acquireOperationGate();
		acquisitionBatchToken.set(token);
		try {
			return acquireTaskLock(task).acquired;
		} finally {
			acquisitionBatchToken.remove();
			releaseOperationGate(token);
		}
	}

	private TaskLockManager.AcquireOutcome acquireTaskLock(Task task) {
		if (task == null) return new TaskLockManager.AcquireOutcome(true, false, false, 0L);
		try {
			TaskLockManager.AcquireOutcome outcome = lockManager.acquireWithStatus(task.getUniqueId());
			if (!outcome.acquired && outcome.confirmedFailure) {
				removeBaselineIfNotNewer(Long.valueOf(task.getUniqueId()), outcome.generation);
			} else if (outcome.acquired && outcome.newlyAcquired) {
				rememberLockBaseline(task, true, outcome.generation);
			} else if (outcome.acquired) {
				rememberLockBaseline(task, false, outcome.generation);
			}
			return outcome;
		} catch (RuntimeException failure) {
			Long retainedGeneration = lockManager.getHeldGeneration(task.getUniqueId());
			if (retainedGeneration != null) {
				synchronized (lockBaselineStates) {
					Long taskId = Long.valueOf(task.getUniqueId());
					if (lockBaselineStates.containsKey(taskId)) lockBaselineGenerations.put(taskId, retainedGeneration);
				}
			}
			throw failure;
		}
	}

	private void removeBaselineIfNotNewer(Long taskId, long generation) {
		synchronized (lockBaselineStates) {
			Long current = lockBaselineGenerations.get(taskId);
			if (current != null && current.longValue() <= generation) {
				lockBaselineStates.remove(taskId);
				lockBaselineGenerations.remove(taskId);
			}
		}
	}

	public String describeLockOwner(Task task) {
		String owner = task == null ? null : lockManager.describeOwner(task.getUniqueId());
		return owner == null || owner.length() == 0 ? "another user" : owner;
	}

	public Task tryAcquireTasks(Iterable<Task> tasks) {
		if (tasks == null) {
			return null;
		}
		Object batchToken = acquireOperationGate();
		acquisitionBatchToken.set(batchToken);
		try {
			return tryAcquireTasksWithinBatch(tasks);
		} finally {
			acquisitionBatchToken.remove();
			releaseOperationGate(batchToken);
		}
	}

	private Task tryAcquireTasksWithinBatch(Iterable<Task> tasks) {
		Map<Long, Long> newlyAcquired = new LinkedHashMap<Long, Long>();
		for (Task task : tasks) {
			if (task == null) continue;
			long taskId = task.getUniqueId();
			try {
				TaskLockManager.AcquireOutcome outcome = acquireTaskLock(task);
				if (!outcome.acquired) {
					releaseNewlyAcquiredLocks(newlyAcquired);
					return task;
				}
				if (outcome.newlyAcquired) newlyAcquired.put(Long.valueOf(taskId), Long.valueOf(outcome.generation));
			} catch (RuntimeException e) {
				releaseNewlyAcquiredLocks(newlyAcquired);
				throw e;
			}
		}
		return null;
	}

	private void releaseNewlyAcquiredLocks(Map<Long, Long> taskIds) {
		for (Map.Entry<Long, Long> entry : taskIds.entrySet()) {
			Long taskId = entry.getKey();
			boolean released = false;
			try {
				released = lockManager.releaseIfGeneration(taskId.longValue(), entry.getValue().longValue());
			} catch (RuntimeException e) {
				// release() forgets the local lease in a finally block; an unreleased lease expires.
				logger.log(Level.WARNING, "Could not roll back a task lock after a later lock request failed", e);
			}
			if (released) removeBaselineIfNotNewer(taskId, entry.getValue().longValue());
		}
	}

	public boolean requiresSaveConfirmation() {
		return externalChangePending && !isMpoProject();
	}

	private boolean isMpoProject() {
		return projectFile != null && FileHelper.isMpoFile(projectFile.getName());
	}

	public void afterSave() {
		projectStatsRefreshPending = true;
		refreshProjectStatsIfPending();
		refreshLockBaselines();
		refreshKnownFileStats();
		resetExternalChangeState(true);
	}

	private void refreshProjectStatsIfPending() {
		try {
			store.mutate(metadata -> store.refreshProjectStats(metadata));
			projectStatsRefreshPending = false;
		} catch (CollaborationLockUnavailableException e) {
			logger.log(Level.WARNING, "Could not update collaboration metadata after save; a later poll will retry", e);
		}
	}

	public void afterExternalProjectRefresh() {
		refreshKnownFileStats();
		resetExternalChangeState(true);
	}

	/** Returns true only for the first warning in one failed-refresh period. */
	public boolean shouldWarnExternalProjectRefreshFailure() {
		synchronized (stateLock) {
			if (externalRefreshFailureWarned) {
				return false;
			}
			externalRefreshFailureWarned = true;
			return true;
		}
	}

	/** Keeps the external-change guard armed so the next poll can retry the load. */
	public void afterExternalProjectRefreshFailure() {
		long modified = projectFile != null && projectFile.exists() ? projectFile.lastModified() : 0L;
		long length = projectFile != null && projectFile.exists() ? projectFile.length() : 0L;
		synchronized (stateLock) {
			externalChangePending = true;
			externalReloadRequested = false;
			pendingExternalReload = true;
			pendingExternalReloadModified = modified;
			pendingExternalReloadLength = length;
			pendingExternalReloadDetectedAt = System.currentTimeMillis();
		}
	}

	private void resetExternalChangeState(boolean clearReloadRequest) {
		synchronized (stateLock) {
			externalChangePending = false;
			externalChangeWarned = false;
			externalRefreshFailureWarned = false;
			if (clearReloadRequest) {
				externalReloadRequested = false;
			}
			clearPendingExternalReload();
		}
	}

	public Set<Long> getLocalLocks() {
		return lockManager.getLocalLocks();
	}

	/** Reconciles task leases and holds an acquisition gate until the refresh finishes. */
	public ExternalProjectRefreshLease beginExternalProjectRefresh() {
		Object token = acquireOperationGate();
		try {
			if (stopped || lockManager.hasPendingOperations()) {
				throw new CollaborationLockUnavailableException("A task lock operation is still in progress");
			}
			reconcileLostLockBaselines(lockManager.renewAll());
			if (stopped || lockManager.hasPendingOperations()) {
				throw new CollaborationLockUnavailableException("A task lock operation changed during external refresh");
			}
			return new ExternalProjectRefreshLease(token, lockManager.getLocalLocks());
		} catch (RuntimeException failure) {
			releaseOperationGate(token);
			throw failure;
		}
	}

	public final class ExternalProjectRefreshLease implements AutoCloseable {
		private final Object token;
		private final Set<Long> lockedTaskIds;

		private ExternalProjectRefreshLease(Object token, Set<Long> lockedTaskIds) {
			this.token = token;
			this.lockedTaskIds = java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<Long>(lockedTaskIds));
		}

		public Set<Long> getLockedTaskIds() { return lockedTaskIds; }

		@Override
		public void close() {
			releaseOperationGate(token);
		}
	}

	private Object acquireOperationGate() {
		Object token = new Object();
		if (stopped || !operationGateToken.compareAndSet(null, token))
			throw new CollaborationLockUnavailableException("Collaboration metadata operation is busy");
		if (stopped) {
			releaseOperationGate(token);
			throw new CollaborationLockUnavailableException("Collaboration session is closing");
		}
		return token;
	}

	private void releaseOperationGate(Object token) {
		if (operationGateToken.compareAndSet(token, null) && stopped) {
			boolean shouldFinalize;
			synchronized (lifecycleLock) {
				shouldFinalize = !closeWaitOwnsFinalization && !stopFinalizationAbandoned;
			}
			if (shouldFinalize) finishStop(stopDeferredUntilOperationCompletes);
		}
	}

	private boolean isTaskAcquisitionAllowed() {
		if (stopped) return false;
		Object gate = operationGateToken.get();
		Object batch = acquisitionBatchToken.get();
		return gate == null || (batch != null && gate == batch);
	}

	private void reconcileLostLockBaselines(Map<Long, Long> lostLocks) {
		if (lostLocks.isEmpty()) return;
		synchronized (lockBaselineStates) {
			for (Map.Entry<Long, Long> lost : lostLocks.entrySet()) {
				Long baselineGeneration = lockBaselineGenerations.get(lost.getKey());
				if (baselineGeneration != null && baselineGeneration.longValue() <= lost.getValue().longValue()) {
					lockBaselineStates.remove(lost.getKey());
					lockBaselineGenerations.remove(lost.getKey());
				}
			}
		}
	}
	public void saveWorkspace(WorkspaceSetting workspace) {
		saveWorkspace(workspace, false);
	}

	private void saveWorkspace(WorkspaceSetting workspace, boolean closingSession) {
		if (workspace == null || (stopped && !closingSession)) {
			return;
		}
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				ObjectOutputStream objectOut = new ObjectOutputStream(out)) {
			objectOut.writeObject(workspace);
			String payload = Base64.getEncoder().encodeToString(out.toByteArray());
			store.mutate(metadata -> {
				UserWorkspaceState state = new UserWorkspaceState();
				state.setUserKey(userKey);
				state.setDisplayName(displayName);
				state.setSavedAt(System.currentTimeMillis());
				state.setWorkspacePayload(payload);
				metadata.getPerUserWorkspace().put(userKey, state);
				store.refreshProjectStats(metadata);
			});
			refreshKnownState(store.load());
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to save workspace state for user " + userKey, e);
		}
	}

	public WorkspaceSetting loadWorkspace() {
		Metadata metadata = store.load();
		if (metadata == null) {
			return null;
		}
		UserWorkspaceState state = metadata.getPerUserWorkspace().get(userKey);
		String payload = state != null && state.getWorkspacePayload() != null
			? state.getWorkspacePayload()
			: projectFile == null ? null : new LocalWorkspaceStore(projectFile, userKey).load();
		if (payload == null) {
			return null;
		}
		byte[] data = Base64.getDecoder().decode(payload);
		try (var in = SafeObjectInput.create(new ByteArrayInputStream(data))) {
				return (WorkspaceSetting) in.readObject();
			} catch (Exception e) {
			return null;
		}
	}

	public String getSidecarFileName() {
		return store.getSidecarFile().getAbsolutePath();
	}

	public void setExternalReloadHandler(ExternalProjectReloadHandler externalReloadHandler) {
		this.externalReloadHandler = externalReloadHandler;
	}

	public void setExternalChangeNoticeHandler(Consumer<String> externalChangeNoticeHandler) {
		this.externalChangeNoticeHandler = externalChangeNoticeHandler;
	}

	private static final class KnownMetadataState {
		private final Map<String, Long> otherUsers = new LinkedHashMap<String, Long>();
		private final Map<String, String> otherLocks = new LinkedHashMap<String, String>();
		private final Map<String, Long> otherWorkspaces = new LinkedHashMap<String, Long>();

		static KnownMetadataState capture(Metadata metadata, String currentUserKey, String currentClientInstanceId) {
			KnownMetadataState state = new KnownMetadataState();
			if (metadata == null) {
				return state;
			}
			long now = System.currentTimeMillis();
			for (Map.Entry<String, UserRecord> entry : metadata.getUsers().entrySet()) {
				UserRecord record = entry.getValue();
				if (record == null) {
					continue;
				}
				if (Objects.equals(currentUserKey, entry.getKey()) || Objects.equals(currentUserKey, record.getUserKey())) {
					continue;
				}
				if (record.getLastSeenAt() > 0L && record.getLastSeenAt() + TaskLockManager.DEFAULT_LEASE_MS * 2 < now) {
					continue;
				}
				state.otherUsers.put(entry.getKey() + "#" + safe(record.getClientInstanceId()), Long.valueOf(record.getLastSeenAt()));
			}
			for (Map.Entry<String, CollaborationMetadataStore.LockRecord> entry : metadata.getLocks().entrySet()) {
				CollaborationMetadataStore.LockRecord lock = entry.getValue();
				if (lock == null) {
					continue;
				}
				if (Objects.equals(currentUserKey, lock.getUserKey())) {
					continue;
				}
				if (lock.getLeaseUntil() > 0L && lock.getLeaseUntil() < now) {
					continue;
				}
				state.otherLocks.put(entry.getKey(), safe(lock.getOwnerKey()) + "|" + lock.getLeaseUntil() + "|" + lock.getUpdatedAt());
			}
			for (Map.Entry<String, UserWorkspaceState> entry : metadata.getPerUserWorkspace().entrySet()) {
				if (Objects.equals(currentUserKey, entry.getKey())) {
					continue;
				}
				UserWorkspaceState workspace = entry.getValue();
				if (workspace != null) {
					state.otherWorkspaces.put(entry.getKey(), Long.valueOf(workspace.getSavedAt()));
				}
			}
			return state;
		}

		private static String safe(String value) {
			return value == null ? "" : value;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof KnownMetadataState)) {
				return false;
			}
			KnownMetadataState other = (KnownMetadataState) obj;
			return otherUsers.equals(other.otherUsers)
				&& otherLocks.equals(other.otherLocks)
				&& otherWorkspaces.equals(other.otherWorkspaces);
		}

		@Override
		public int hashCode() {
			return Objects.hash(otherUsers, otherLocks, otherWorkspaces);
		}
	}
}
