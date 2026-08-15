package com.microproject.collaboration;

import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Base64;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.microproject.collaboration.CollaborationMetadataStore.Metadata;
import com.microproject.collaboration.CollaborationMetadataStore.UserRecord;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.util.Alert;
import com.microproject.util.PopupDialogSupport;
import com.microproject.util.SafeObjectInput;
import com.microproject.workspace.WorkspaceSetting;

public class CollaborationSession {
	private static final Logger logger = Logger.getLogger(CollaborationSession.class.getName());
	private static final long POLL_INTERVAL_MS = 2000L;
	private static final long HEARTBEAT_INTERVAL_MS = 5000L;
	private static final long AUTO_RELOAD_SETTLE_MS = 1500L;

	public static final int SAVE_CANCEL = 0;
	public static final int SAVE_PROCEED = 1;
	public static final int SAVE_AS_COPY = 2;

	public interface ExternalProjectReloadHandler {
		void reload(Project project);
	}

	private final Project project;
	private final File projectFile;
	private final CollaborationMetadataStore store;
	private final TaskLockManager lockManager;
	private final ProjectMergeService mergeService;
	private final String userKey;
	private final String displayName;
	private final String clientInstanceId;
	private final Map<Long, ProjectMergeService.TaskState> lockBaselineStates = new LinkedHashMap<Long, ProjectMergeService.TaskState>();
	private volatile ExternalProjectReloadHandler externalReloadHandler;
	private volatile Timer timer;
	private final Object stateLock = new Object();
	private volatile long lastKnownProjectModified;
	private volatile long lastKnownProjectLength;
	private volatile KnownMetadataState lastKnownMetadataState;
	private volatile boolean externalChangePending;
	private volatile boolean externalChangeWarned;
	private volatile boolean externalReloadRequested;
	private volatile boolean pendingExternalReload;
	private volatile long pendingExternalReloadModified;
	private volatile long pendingExternalReloadLength;
	private volatile long pendingExternalReloadDetectedAt;
	private volatile long lastHeartbeatAt;

	public CollaborationSession(Project project, String fileName, String userKey) {
		this.project = project;
		this.projectFile = fileName == null ? null : new File(fileName);
		this.userKey = userKey == null || userKey.trim().length() == 0 ? "unknown" : userKey.trim();
		this.displayName = this.userKey;
		this.clientInstanceId = UUID.randomUUID().toString();
		this.store = new CollaborationMetadataStore(projectFile);
		this.lockManager = new TaskLockManager(store, this.userKey, this.displayName, this.clientInstanceId);
		this.mergeService = new ProjectMergeService();
		refreshKnownFileStats();
	}

	public static CollaborationSession create(Project project, String fileName, String userKey) {
		if (project == null || fileName == null || !CollaborationMetadataStore.isCollaborationCandidate(fileName)) {
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
		registerUser();
		refreshKnownState(store.load());
		timer = new Timer("projectlibre-collaboration-" + clientInstanceId, true);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				poll();
			}
		}, POLL_INTERVAL_MS, POLL_INTERVAL_MS);
	}

	public void stop() {
		if (timer == null) {
			return;
		}
		try {
			saveWorkspace(project.getCollaborationWorkspace());
		} catch (Exception e) {
			logger.log(Level.FINE, "Failed to save workspace on session stop", e);
		}
		lockManager.releaseAll();
		lockBaselineStates.clear();
		timer.cancel();
		timer = null;
	}

	private void registerUser() {
		registerUser(true);
	}

	private void registerUser(boolean refreshKnownState) {
		store.mutate(metadata -> {
			UserRecord record = metadata.getUsers().get(userKey);
			if (record == null) {
				record = new UserRecord();
				record.setUserKey(userKey);
				metadata.getUsers().put(userKey, record);
			}
			record.setDisplayName(displayName);
			record.setClientInstanceId(clientInstanceId);
			record.setLastSeenAt(System.currentTimeMillis());
			store.refreshProjectStats(metadata);
		});
		if (refreshKnownState) {
			refreshKnownState(store.load());
		}
	}

	private void poll() {
		if (projectFile == null) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastHeartbeatAt >= HEARTBEAT_INTERVAL_MS) {
			lockManager.renewAll();
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
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								Alert.warn("This project was updated externally. Save will re-check for conflicts before writing.");
							}
						});
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
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ExternalProjectReloadHandler handler = externalReloadHandler;
				if (handler != null) {
					handler.reload(project);
				}
			}
		});
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

	private void rememberLockBaseline(Task task) {
		if (task == null) {
			return;
		}
		Long taskId = Long.valueOf(task.getUniqueId());
		if (lockBaselineStates.containsKey(taskId)) {
			return;
		}
		ProjectMergeService.TaskState state = ProjectMergeService.TaskState.capture(task);
		if (state != null) {
			lockBaselineStates.put(taskId, state);
		}
	}

	private void refreshLockBaselines() {
		for (Long taskId : lockManager.getLocalLocks()) {
			Task task = project == null ? null : project.findByUniqueId(taskId.longValue());
			ProjectMergeService.TaskState state = ProjectMergeService.TaskState.capture(task);
			if (state != null) {
				lockBaselineStates.put(taskId, state);
			} else {
				lockBaselineStates.remove(taskId);
			}
		}
	}

	public boolean tryLockTask(Task task, Component parent, String actionLabel) {
		if (task == null) {
			return true;
		}
		long taskId = task.getUniqueId();
		if (lockManager.isLockedByCurrentUser(taskId)) {
			return true;
		}
		if (!lockManager.acquire(taskId)) {
			String owner = lockManager.describeOwner(taskId);
			if (owner == null || owner.length() == 0) {
				owner = "another user";
			}
			Alert.warn("Cannot " + actionLabel + " task \"" + task.getName() + "\" because it is locked by " + owner + ".", parent);
			return false;
		}
		rememberLockBaseline(task);
		return true;
	}

	public boolean tryLockTasks(Iterable<Task> tasks, Component parent, String actionLabel) {
		if (tasks == null) {
			return true;
		}
		for (Task task : tasks) {
			if (!tryLockTask(task, parent, actionLabel)) {
				return false;
			}
		}
		return true;
	}

	public int checkBeforeSave(Component parent) {
		if (!hasSaveConflicts()) {
			return SAVE_PROCEED;
		}
		ProjectMergeService.ConflictResult conflicts = mergeService.findTaskConflicts(projectFile.getAbsolutePath(), lockBaselineStates);
		if (!conflicts.hasConflicts()) {
			return SAVE_PROCEED;
		}
		Object[] options = new Object[] {
			"Restore and Save",
			"Discard My Changes",
			"Save Copy"
		};
		int result = PopupDialogSupport.showOptionDialog(parent,
			"One or more tasks you are editing were changed or deleted externally.\nChoose how to resolve the conflict.",
			"ProjectLibre",
			JOptionPane.DEFAULT_OPTION,
			JOptionPane.WARNING_MESSAGE,
			null,
			options,
			options[0],
			JOptionPane.CANCEL_OPTION);
		if (result == 0) {
			return SAVE_PROCEED;
		}
		if (result == 2) {
			return SAVE_AS_COPY;
		}
		return SAVE_CANCEL;
	}

	public boolean hasSaveConflicts() {
		if (!externalChangePending) {
			return false;
		}
		ProjectMergeService.ConflictResult conflicts = mergeService.findTaskConflicts(projectFile.getAbsolutePath(), lockBaselineStates);
		return conflicts.hasConflicts();
	}

	public void afterSave() {
		store.mutate(metadata -> {
			store.refreshProjectStats(metadata);
		});
		refreshLockBaselines();
		refreshKnownFileStats();
		resetExternalChangeState(true);
	}

	public void afterExternalProjectRefresh() {
		refreshKnownFileStats();
		resetExternalChangeState(true);
	}

	private void resetExternalChangeState(boolean clearReloadRequest) {
		synchronized (stateLock) {
			externalChangePending = false;
			externalChangeWarned = false;
			if (clearReloadRequest) {
				externalReloadRequested = false;
			}
			clearPendingExternalReload();
		}
	}

	public Set<Long> getLocalLocks() {
		return lockManager.getLocalLocks();
	}
	public void saveWorkspace(WorkspaceSetting workspace) {
		if (workspace == null) {
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
		if (state == null || state.getWorkspacePayload() == null) {
			return null;
		}
		byte[] data = Base64.getDecoder().decode(state.getWorkspacePayload());
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
				if (Objects.equals(currentUserKey, entry.getKey())) {
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
