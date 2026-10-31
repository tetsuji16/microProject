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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microproject.session.FileHelper;
import com.microproject.util.SafeFileReplace;

public class CollaborationMetadataStore {
	private static final Logger logger = Logger.getLogger(CollaborationMetadataStore.class.getName());
	private static final int MAX_METADATA_BYTES = 1024 * 1024; // 1 MB safety limit
	private static final long EDT_LOCK_TIMEOUT_MILLIS = 300L;
	public static final int SCHEMA_VERSION = 1;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Object JVM_LOCK_REGISTRY_GUARD = new Object();
	private static final ConcurrentHashMap<String, JvmLockEntry> JVM_LOCKS = new ConcurrentHashMap<String, JvmLockEntry>();

	private static final class JvmLockEntry {
		private final ReentrantLock monitor = new ReentrantLock();
		private final AtomicInteger references = new AtomicInteger();
	}

	private final File projectFile;
	private final File sidecarFile;
	private final File legacySidecarFile;
	private final File lockFile;

	public CollaborationMetadataStore(File projectFile) {
		this.projectFile = projectFile;
		this.sidecarFile = buildSidecarFile(projectFile);
		this.legacySidecarFile = buildLegacySidecarFile(projectFile);
		this.lockFile = buildLockFile(sidecarFile);
	}

	public static boolean isMpoCollaborationCandidate(String fileName) {
		return FileHelper.isMpoFile(fileName);
	}

	/**
	 * Compatibility alias for callers compiled against the previous API.  The
	 * collaboration policy itself is defined only by the MPO predicate above.
	 */
	@Deprecated
	public static boolean isCollaborationCandidate(String fileName) {
		return isMpoCollaborationCandidate(fileName);
	}

	public static File buildSidecarFile(File projectFile) {
		return buildMetadataFile(projectFile, ".microproject-sync.json");
	}

	private static File buildLegacySidecarFile(File projectFile) {
		return buildMetadataFile(projectFile, ".projectlibre-sync.json");
	}

	private static File buildMetadataFile(File projectFile, String suffix) {
		String name = projectFile.getName();
		int dot = name.lastIndexOf('.');
		String base = dot >= 0 ? name.substring(0, dot) : name;
		File parent = projectFile.getParentFile();
		return parent != null ? new File(parent, base + suffix) : new File(base + suffix);
	}

	public File getSidecarFile() {
		return sidecarFile;
	}

	/**
	 * Removes collaboration artifacts for an explicitly deleted/unshared
	 * project.  A malformed or partially synced sidecar is never treated as an
	 * empty lease table.  The sidecar and marker are removed only after the
	 * metadata has been read successfully, all leases are absent/expired, and
	 * the OS lock has been acquired and released.
	 *
	 * @return {@code true} when both artifacts were removed, otherwise
	 *         {@code false} and the files are left untouched
	 */
	public boolean removeArtifactsIfUnowned() {
		final long now = System.currentTimeMillis();
		Boolean unowned = withLockedMetadata(new MetadataCallback<Boolean>() {
			@Override
			public Boolean execute(Metadata metadata) {
				return Boolean.valueOf(metadata.getLocks().values().stream()
						.noneMatch(lock -> lock != null && lock.getLeaseUntil() > now
								&& ((lock.getOwnerKey() != null && !lock.getOwnerKey().isBlank())
										|| (lock.getUserKey() != null && !lock.getUserKey().isBlank()))));
			}

			@Override
			public Boolean onError(Exception error) {
				return Boolean.FALSE;
			}
		});
		if (!Boolean.TRUE.equals(unowned)) return false;

		// Keep the JVM monitor while acquiring/releasing the OS lock.  This
		// prevents another local caller from starting a new lease between the
		// ownership check and cleanup; other processes are serialized by the
		// marker lock itself.
		String lockKey;
		try {
			lockKey = lockFile.getCanonicalPath();
		} catch (IOException error) {
			lockKey = lockFile.getAbsolutePath();
		}
		JvmLockEntry entry;
		synchronized (JVM_LOCK_REGISTRY_GUARD) {
			entry = JVM_LOCKS.computeIfAbsent(lockKey, key -> new JvmLockEntry());
			entry.references.incrementAndGet();
		}
		try {
			long deadline = lockDeadline();
			acquireJvmLock(entry.monitor, deadline);
			try {
				try (FileChannel channel = FileChannel.open(lockFile.toPath(),
						StandardOpenOption.CREATE, StandardOpenOption.WRITE);
					 FileLock ignored = channel.tryLock()) {
					if (ignored == null) return false;
				}
				try {
					Files.deleteIfExists(sidecarFile.toPath());
					Files.deleteIfExists(lockFile.toPath());
					return true;
				} catch (IOException error) {
					logger.log(Level.WARNING, "Could not remove collaboration artifacts for " + projectFile, error);
					return false;
				}
			} finally {
				entry.monitor.unlock();
			}
		} catch (CollaborationLockUnavailableException error) {
			logger.log(Level.FINE, "Collaboration lock is busy during cleanup " + projectFile, error);
			return false;
		} catch (java.nio.channels.OverlappingFileLockException error) {
			logger.log(Level.FINE, "Collaboration lock is already held for cleanup " + projectFile, error);
			return false;
		} catch (IOException error) {
			logger.log(Level.WARNING, "Could not acquire collaboration lock for cleanup " + projectFile, error);
			return false;
		} finally {
			releaseJvmLock(lockKey, entry);
		}
	}

	static File buildLockFile(File sidecarFile) {
		String name = sidecarFile.getName();
		String suffix = ".json";
		String base = name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
		File parent = sidecarFile.getParentFile();
		return parent != null ? new File(parent, base + ".lock") : new File(base + ".lock");
	}

	public Metadata load() {
		return withLockedMetadata(new MetadataCallback<Metadata>() {
			public Metadata execute(Metadata metadata) {
				return metadata;
			}
		});
	}

	public void mutate(MetadataMutation mutation) {
		withLockedMetadata(new MetadataCallback<Object>() {
			public Object execute(Metadata metadata) {
				mutation.mutate(metadata);
				return null;
			}
		});
	}

	public <T> T withLockedMetadata(MetadataCallback<T> callback) {
		String lockKey;
		try {
			lockKey = lockFile.getCanonicalPath();
		} catch (IOException e) {
			lockKey = lockFile.getAbsolutePath();
		}
		JvmLockEntry entry;
		synchronized (JVM_LOCK_REGISTRY_GUARD) {
			entry = JVM_LOCKS.computeIfAbsent(lockKey, key -> new JvmLockEntry());
			entry.references.incrementAndGet();
		}
		long deadline = lockDeadline();
		try {
			acquireJvmLock(entry.monitor, deadline);
			try {
				return withLockedMetadataUnderJvmLock(callback, deadline);
			} finally {
				entry.monitor.unlock();
			}
		} finally {
			releaseJvmLock(lockKey, entry);
		}
	}

	private static void releaseJvmLock(String lockKey, JvmLockEntry entry) {
		synchronized (JVM_LOCK_REGISTRY_GUARD) {
			if (entry.references.decrementAndGet() == 0) {
				// Removal is guarded together with reference acquisition. A waiter
				// can therefore never observe a removed entry and create a second
				// monitor for the same canonical path.
				JVM_LOCKS.remove(lockKey, entry);
			}
		}
	}

	/** Package-private visibility keeps registry assertions out of production APIs. */
	static int jvmLockRegistrySizeForTests() {
		return JVM_LOCKS.size();
	}

	/** Package-private diagnostic used to prove monitor identity during contention. */
	static Object jvmLockMonitorForTests(File projectFile) {
		String key;
		try {
			key = buildLockFile(buildSidecarFile(projectFile)).getCanonicalPath();
		} catch (IOException e) {
			key = buildLockFile(buildSidecarFile(projectFile)).getAbsolutePath();
		}
		synchronized (JVM_LOCK_REGISTRY_GUARD) {
			JvmLockEntry entry = JVM_LOCKS.get(key);
			return entry == null ? null : entry.monitor;
		}
	}

	/** Package-private diagnostic used to ensure a waiter joined a lock entry. */
	static int jvmLockReferenceCountForTests(File projectFile) {
		String key;
		try {
			key = buildLockFile(buildSidecarFile(projectFile)).getCanonicalPath();
		} catch (IOException e) {
			key = buildLockFile(buildSidecarFile(projectFile)).getAbsolutePath();
		}
		synchronized (JVM_LOCK_REGISTRY_GUARD) {
			JvmLockEntry entry = JVM_LOCKS.get(key);
			return entry == null ? 0 : entry.references.get();
		}
	}

	private <T> T withLockedMetadataUnderJvmLock(MetadataCallback<T> callback, long deadline) {
		FileChannel channel = null;
		FileLock lock = null;
		FileChannel legacyChannel = null;
		FileLock legacyLock = null;
		try {
			File parent = sidecarFile.getParentFile();
			if (parent != null) {
				Files.createDirectories(parent.toPath());
			}
			channel = FileChannel.open(lockFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			lock = acquireFileLock(channel, deadline);
			// The canonical path is authoritative once present. During one-time
			// migration, hold the legacy marker too so older local writers cannot
			// change the source while it is copied; all new writers serialize on
			// the canonical marker.
			boolean useLegacy = !sidecarFile.exists() && legacySidecarFile.exists();
			if (useLegacy) {
				File legacyLockFile = buildLockFile(legacySidecarFile);
				legacyChannel = FileChannel.open(legacyLockFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
				legacyLock = acquireFileLock(legacyChannel, deadline);
			}
			byte[] originalBytes = useLegacy ? readAllBytes(legacySidecarFile) : readAllBytes(sidecarFile);
			Metadata metadata;
			try {
				metadata = originalBytes.length == 0 ? createDefaultMetadata() : parseMetadata(originalBytes);
			} catch (Exception parseEx) {
				// A cloud sync client can expose a partial sidecar transiently. Never
				// replace it with an empty lock table: that would discard live leases
				// and permit concurrent edits. The caller must retry after sync settles.
				logger.log(Level.WARNING, "Failed to parse collaboration metadata for " + projectFile + "; leaving it unchanged", parseEx);
				return callback.onError(parseEx);
			}
			normalize(metadata);
			T result = callback.execute(metadata);
			writeMetadataIfChanged(useLegacy ? null : originalBytes, metadata);
			return result;
		} catch (Exception e) {
			return callback.onError(e);
		} finally {
			try {
				if (legacyLock != null) legacyLock.release();
			} catch (IOException e) {
				logger.log(Level.FINE, "Failed to release legacy file lock on {0}", legacySidecarFile);
			}
			try {
				if (legacyChannel != null) legacyChannel.close();
			} catch (IOException e) {
				logger.log(Level.FINE, "Failed to close legacy file channel on {0}", legacySidecarFile);
			}
			try {
				if (lock != null) {
					lock.release();
				}
			} catch (IOException e) {
				logger.log(Level.FINE, "Failed to release file lock on {0}", sidecarFile);
			}
			try {
				if (channel != null) {
					channel.close();
				}
			} catch (IOException e) {
				logger.log(Level.FINE, "Failed to close file channel on {0}", sidecarFile);
			}
		}
	}

	private static long lockDeadline() {
		return com.microproject.util.UiDispatch.isDispatchThread()
				? System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(EDT_LOCK_TIMEOUT_MILLIS)
				: 0L;
	}

	private static void acquireJvmLock(ReentrantLock lock, long deadline) {
		try {
			if (deadline == 0L) {
				lock.lock();
			} else if (!lock.tryLock(Math.max(0L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)) {
				throw new CollaborationLockUnavailableException("Timed out waiting for the local collaboration lock");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CollaborationLockUnavailableException("Interrupted while waiting for the local collaboration lock", e);
		}
	}

	private static FileLock acquireFileLock(FileChannel channel, long deadline) throws IOException {
		if (deadline == 0L) {
			return channel.lock();
		}
		while (true) {
			try {
				FileLock lock = channel.tryLock();
				if (lock != null) return lock;
			} catch (java.nio.channels.OverlappingFileLockException ignored) {
				// A different local session holds this marker; retry only until the EDT deadline.
			}
			long remaining = deadline - System.nanoTime();
			if (remaining <= 0L) {
				throw new CollaborationLockUnavailableException("Timed out waiting for the collaboration file lock");
			}
			try {
				TimeUnit.NANOSECONDS.sleep(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(10L)));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new CollaborationLockUnavailableException("Interrupted while waiting for the collaboration file lock", e);
			}
		}
	}

	private byte[] readAllBytes(File source) throws IOException {
		if (!source.exists()) {
			return new byte[0];
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (java.io.InputStream input = Files.newInputStream(source.toPath())) {
			byte[] buf = new byte[4096];
			int read;
			int total = 0;
			while ((read = input.read(buf)) != -1) {
				total += read;
				if (total > MAX_METADATA_BYTES) {
					throw new IOException("Metadata file exceeds maximum allowed size of " + MAX_METADATA_BYTES + " bytes");
				}
				out.write(buf, 0, read);
			}
		}
		return out.toByteArray();
	}

	private void writeMetadataIfChanged(byte[] originalBytes, Metadata metadata) throws IOException {
		byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(metadata);
		if (Arrays.equals(originalBytes, bytes)) {
			return;
		}
		if (bytes.length > MAX_METADATA_BYTES) {
			throw new IOException("Metadata file exceeds maximum allowed size of " + MAX_METADATA_BYTES + " bytes");
		}
		Path parent = sidecarFile.toPath().toAbsolutePath().getParent();
		Path temporary = Files.createTempFile(parent, sidecarFile.getName() + ".", ".tmp");
		try {
			writeTempFile(temporary, bytes);
			SafeFileReplace.moveAtomically(temporary, sidecarFile.toPath(), true);
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	/** Hook for fault-injection tests; the sidecar is not touched until this succeeds. */
	protected void writeTempFile(Path temporary, byte[] bytes) throws IOException {
		try (FileChannel output = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
			java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
			while (buffer.hasRemaining()) {
				output.write(buffer);
			}
			output.force(true);
		}
	}

	private Metadata createDefaultMetadata() {
		Metadata metadata = new Metadata();
		metadata.setSchemaVersion(SCHEMA_VERSION);
		metadata.setProjectFileName(projectFile.getName());
		metadata.setProjectFingerprint(projectFile.getName());
		refreshProjectStats(metadata);
		return metadata;
	}

	public void refreshProjectStats(Metadata metadata) {
		metadata.setProjectFileLastModified(projectFile.exists() ? projectFile.lastModified() : 0L);
		metadata.setProjectFileLength(projectFile.exists() ? projectFile.length() : 0L);
	}

	private Metadata parseMetadata(byte[] bytes) {
		try {
			return OBJECT_MAPPER.readValue(bytes, Metadata.class);
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse collaboration metadata", e);
		}
	}

	private void normalize(Metadata metadata) {
		if (metadata.getSchemaVersion() <= 0) {
			metadata.setSchemaVersion(SCHEMA_VERSION);
		}
		if (metadata.getProjectFileName() == null) {
			metadata.setProjectFileName(projectFile.getName());
		}
		if (metadata.getProjectFingerprint() == null) {
			metadata.setProjectFingerprint(projectFile.getName());
		}
		if (metadata.getUsers() == null) {
			metadata.setUsers(new LinkedHashMap<String, UserRecord>());
		}
		if (metadata.getLocks() == null) {
			metadata.setLocks(new LinkedHashMap<String, LockRecord>());
		}
		if (metadata.getPerUserWorkspace() == null) {
			metadata.setPerUserWorkspace(new LinkedHashMap<String, UserWorkspaceState>());
		}
	}

	public interface MetadataMutation {
		void mutate(Metadata metadata);
	}

	public interface MetadataCallback<T> {
		T execute(Metadata metadata);

		default T onError(Exception e) {
			if (e instanceof CollaborationLockUnavailableException lockUnavailable) {
				throw lockUnavailable;
			}
			throw new RuntimeException(e);
		}
	}

	public static class Metadata implements java.io.Serializable {
		private static final long serialVersionUID = 1L;
		private int schemaVersion;
		private String projectFingerprint;
		private String projectFileName;
		private long projectFileLastModified;
		private long projectFileLength;
		private Map<String, UserRecord> users = new LinkedHashMap<String, UserRecord>();
		private Map<String, LockRecord> locks = new LinkedHashMap<String, LockRecord>();
		private Map<String, UserWorkspaceState> perUserWorkspace = new LinkedHashMap<String, UserWorkspaceState>();

		public static Metadata fromMap(Map<?, ?> map) {
			Metadata metadata = new Metadata();
			metadata.schemaVersion = asInt(map.get("schemaVersion"), SCHEMA_VERSION);
			metadata.projectFingerprint = asString(map.get("projectFingerprint"));
			metadata.projectFileName = asString(map.get("projectFileName"));
			metadata.projectFileLastModified = asLong(map.get("projectFileLastModified"));
			metadata.projectFileLength = asLong(map.get("projectFileLength"));
			Map<?, ?> usersMap = asMap(map.get("users"));
			if (usersMap != null) {
				for (Map.Entry<?, ?> entry : usersMap.entrySet()) {
					metadata.users.put(String.valueOf(entry.getKey()), UserRecord.fromMap(asMap(entry.getValue())));
				}
			}
			Map<?, ?> locksMap = asMap(map.get("locks"));
			if (locksMap != null) {
				for (Map.Entry<?, ?> entry : locksMap.entrySet()) {
					metadata.locks.put(String.valueOf(entry.getKey()), LockRecord.fromMap(asMap(entry.getValue())));
				}
			}
			Map<?, ?> workspaceMap = asMap(map.get("perUserWorkspace"));
			if (workspaceMap != null) {
				for (Map.Entry<?, ?> entry : workspaceMap.entrySet()) {
					metadata.perUserWorkspace.put(String.valueOf(entry.getKey()), fromWorkspaceMap(asMap(entry.getValue())));
				}
			}
			return metadata;
		}

		private static UserWorkspaceState fromWorkspaceMap(Map<?, ?> map) {
			UserWorkspaceState state = new UserWorkspaceState();
			if (map == null) {
				return state;
			}
			state.setUserKey(asString(map.get("userKey")));
			state.setDisplayName(asString(map.get("displayName")));
			state.setSavedAt(asLong(map.get("savedAt")));
			state.setWorkspacePayload(asString(map.get("workspacePayload")));
			return state;
		}

		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			map.put("schemaVersion", Integer.valueOf(schemaVersion));
			map.put("projectFingerprint", projectFingerprint);
			map.put("projectFileName", projectFileName);
			map.put("projectFileLastModified", Long.valueOf(projectFileLastModified));
			map.put("projectFileLength", Long.valueOf(projectFileLength));
			Map<String, Object> usersMap = new LinkedHashMap<String, Object>();
			for (Map.Entry<String, UserRecord> entry : users.entrySet()) {
				usersMap.put(entry.getKey(), entry.getValue().toMap());
			}
			map.put("users", usersMap);
			Map<String, Object> locksMap = new LinkedHashMap<String, Object>();
			for (Map.Entry<String, LockRecord> entry : locks.entrySet()) {
				locksMap.put(entry.getKey(), entry.getValue().toMap());
			}
			map.put("locks", locksMap);
			Map<String, Object> workspaceMap = new LinkedHashMap<String, Object>();
			for (Map.Entry<String, UserWorkspaceState> entry : perUserWorkspace.entrySet()) {
				Map<String, Object> stateMap = new LinkedHashMap<String, Object>();
				UserWorkspaceState state = entry.getValue();
				stateMap.put("userKey", state.getUserKey());
				stateMap.put("displayName", state.getDisplayName());
				stateMap.put("savedAt", Long.valueOf(state.getSavedAt()));
				stateMap.put("workspacePayload", state.getWorkspacePayload());
				workspaceMap.put(entry.getKey(), stateMap);
			}
			map.put("perUserWorkspace", workspaceMap);
			return map;
		}

		public int getSchemaVersion() {
			return schemaVersion;
		}

		public void setSchemaVersion(int schemaVersion) {
			this.schemaVersion = schemaVersion;
		}

		public String getProjectFingerprint() {
			return projectFingerprint;
		}

		public void setProjectFingerprint(String projectFingerprint) {
			this.projectFingerprint = projectFingerprint;
		}

		public String getProjectFileName() {
			return projectFileName;
		}

		public void setProjectFileName(String projectFileName) {
			this.projectFileName = projectFileName;
		}

		public long getProjectFileLastModified() {
			return projectFileLastModified;
		}

		public void setProjectFileLastModified(long projectFileLastModified) {
			this.projectFileLastModified = projectFileLastModified;
		}

		public long getProjectFileLength() {
			return projectFileLength;
		}

		public void setProjectFileLength(long projectFileLength) {
			this.projectFileLength = projectFileLength;
		}

		public Map<String, UserRecord> getUsers() {
			return users;
		}

		public void setUsers(Map<String, UserRecord> users) {
			this.users = users;
		}

		public Map<String, LockRecord> getLocks() {
			return locks;
		}

		public void setLocks(Map<String, LockRecord> locks) {
			this.locks = locks;
		}

		public Map<String, UserWorkspaceState> getPerUserWorkspace() {
			return perUserWorkspace;
		}

		public void setPerUserWorkspace(Map<String, UserWorkspaceState> perUserWorkspace) {
			this.perUserWorkspace = perUserWorkspace;
		}
	}

	public static class UserRecord implements java.io.Serializable {
		private static final long serialVersionUID = 1L;
		private String userKey;
		private String displayName;
		private String clientInstanceId;
		private long lastSeenAt;

		public static UserRecord fromMap(Map<?, ?> map) {
			UserRecord record = new UserRecord();
			if (map == null) {
				return record;
			}
			record.userKey = asString(map.get("userKey"));
			record.displayName = asString(map.get("displayName"));
			record.clientInstanceId = asString(map.get("clientInstanceId"));
			record.lastSeenAt = asLong(map.get("lastSeenAt"));
			return record;
		}

		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			map.put("userKey", userKey);
			map.put("displayName", displayName);
			map.put("clientInstanceId", clientInstanceId);
			map.put("lastSeenAt", Long.valueOf(lastSeenAt));
			return map;
		}

		public String getUserKey() {
			return userKey;
		}

		public void setUserKey(String userKey) {
			this.userKey = userKey;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public String getClientInstanceId() {
			return clientInstanceId;
		}

		public void setClientInstanceId(String clientInstanceId) {
			this.clientInstanceId = clientInstanceId;
		}

		public long getLastSeenAt() {
			return lastSeenAt;
		}

		public void setLastSeenAt(long lastSeenAt) {
			this.lastSeenAt = lastSeenAt;
		}
	}

	public static class LockRecord implements java.io.Serializable {
		private static final long serialVersionUID = 1L;
		private long taskId;
		private String ownerKey;
		private String userKey;
		private String displayName;
		private String clientInstanceId;
		private long leaseUntil;
		private long updatedAt;

		public static LockRecord fromMap(Map<?, ?> map) {
			LockRecord record = new LockRecord();
			if (map == null) {
				return record;
			}
			record.taskId = asLong(map.get("taskId"));
			record.ownerKey = asString(map.get("ownerKey"));
			record.userKey = asString(map.get("userKey"));
			record.displayName = asString(map.get("displayName"));
			record.clientInstanceId = asString(map.get("clientInstanceId"));
			record.leaseUntil = asLong(map.get("leaseUntil"));
			record.updatedAt = asLong(map.get("updatedAt"));
			return record;
		}

		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			map.put("taskId", Long.valueOf(taskId));
			map.put("ownerKey", ownerKey);
			map.put("userKey", userKey);
			map.put("displayName", displayName);
			map.put("clientInstanceId", clientInstanceId);
			map.put("leaseUntil", Long.valueOf(leaseUntil));
			map.put("updatedAt", Long.valueOf(updatedAt));
			return map;
		}

		public long getTaskId() {
			return taskId;
		}

		public void setTaskId(long taskId) {
			this.taskId = taskId;
		}

		public String getOwnerKey() {
			return ownerKey;
		}

		public void setOwnerKey(String ownerKey) {
			this.ownerKey = ownerKey;
		}

		public String getUserKey() {
			return userKey;
		}

		public void setUserKey(String userKey) {
			this.userKey = userKey;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public String getClientInstanceId() {
			return clientInstanceId;
		}

		public void setClientInstanceId(String clientInstanceId) {
			this.clientInstanceId = clientInstanceId;
		}

		public long getLeaseUntil() {
			return leaseUntil;
		}

		public void setLeaseUntil(long leaseUntil) {
			this.leaseUntil = leaseUntil;
		}

		public long getUpdatedAt() {
			return updatedAt;
		}

		public void setUpdatedAt(long updatedAt) {
			this.updatedAt = updatedAt;
		}
	}

	private static Map<?, ?> asMap(Object value) {
		return value instanceof Map<?, ?> map ? map : null;
	}

	private static String asString(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private static long asLong(Object value) {
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		if (value == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(value));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int asInt(Object value, int defaultValue) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

}
