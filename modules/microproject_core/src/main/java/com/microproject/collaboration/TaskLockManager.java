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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import com.microproject.collaboration.CollaborationMetadataStore.LockRecord;
import com.microproject.collaboration.CollaborationMetadataStore.Metadata;

public class TaskLockManager {
	public static final long DEFAULT_LEASE_MS = 15000L;

	private final CollaborationMetadataStore store;
	private final String userKey;
	private final String displayName;
	private final String clientInstanceId;
	private final String ownerKey;
	private final boolean sessionManaged;
	private final BooleanSupplier sessionActive;
	private final BooleanSupplier acquisitionAllowed;
	private final ConcurrentHashMap<Long, LockState> localLocks = new ConcurrentHashMap<Long, LockState>();
	private final AtomicLong nextGeneration = new AtomicLong();

	private static final class LockState {
		private final long generation;
		private final boolean held;
		private final boolean pending;
		private LockState(long generation, boolean held, boolean pending) {
			this.generation = generation;
			this.held = held;
			this.pending = pending;
		}
	}

	public TaskLockManager(CollaborationMetadataStore store, String userKey, String displayName, String clientInstanceId) {
		this(store, userKey, displayName, clientInstanceId, false, () -> true);
	}

	TaskLockManager(CollaborationMetadataStore store, String userKey, String displayName, String clientInstanceId, boolean sessionManaged) {
		this(store, userKey, displayName, clientInstanceId, sessionManaged, () -> true);
	}

	TaskLockManager(CollaborationMetadataStore store, String userKey, String displayName, String clientInstanceId,
			boolean sessionManaged, BooleanSupplier sessionActive) {
		this(store, userKey, displayName, clientInstanceId, sessionManaged, sessionActive, sessionActive);
	}

	TaskLockManager(CollaborationMetadataStore store, String userKey, String displayName, String clientInstanceId,
			boolean sessionManaged, BooleanSupplier sessionActive, BooleanSupplier acquisitionAllowed) {
		this.store = store;
		this.userKey = userKey;
		this.displayName = displayName;
		this.clientInstanceId = clientInstanceId;
		this.ownerKey = userKey + "#" + clientInstanceId;
		this.sessionManaged = sessionManaged;
		this.sessionActive = sessionActive;
		this.acquisitionAllowed = acquisitionAllowed;
	}

	public boolean acquire(long taskId) {
		return acquireWithStatus(taskId).acquired;
	}

	static final class AcquireOutcome {
		final boolean acquired;
		final boolean newlyAcquired;
		final boolean confirmedFailure;
		final long generation;
		AcquireOutcome(boolean acquired, boolean newlyAcquired, boolean confirmedFailure, long generation) {
			this.acquired = acquired;
			this.newlyAcquired = newlyAcquired;
			this.confirmedFailure = confirmedFailure;
			this.generation = generation;
		}
	}

	AcquireOutcome acquireWithStatus(long taskId) {
		Long taskKey = Long.valueOf(taskId);
		if (!acquisitionAllowed.getAsBoolean()) {
			Long heldGeneration = getHeldGeneration(taskId);
			return new AcquireOutcome(false, false, false, heldGeneration == null ? 0L : heldGeneration.longValue());
		}
		long generation = beginOperation(taskKey, true);
		if (!isCurrent(taskKey, generation)) return new AcquireOutcome(false, false, false, generation);
		if (!sessionActive.getAsBoolean() || !acquisitionAllowed.getAsBoolean()) {
			finishUnchanged(taskKey, generation);
			return new AcquireOutcome(false, false, false, generation);
		}
		AcquireOutcome result;
		try {
			result = store.withLockedMetadata(metadata -> {
				if (!isCurrent(taskKey, generation) || !sessionActive.getAsBoolean() || !acquisitionAllowed.getAsBoolean()) return new AcquireOutcome(false, false, false, generation);
				long now = System.currentTimeMillis();
				LockRecord beforeCleanup = metadata.getLocks().get(String.valueOf(taskId));
				boolean alreadyOwned = beforeCleanup != null && ownerKey.equals(beforeCleanup.getOwnerKey())
						&& beforeCleanup.getLeaseUntil() > now;
				cleanupExpiredLocks(metadata, now);
				LockRecord existing = metadata.getLocks().get(String.valueOf(taskId));
				if (existing != null && !ownerKey.equals(existing.getOwnerKey())) return new AcquireOutcome(false, false, true, generation);
				if (!isCurrent(taskKey, generation) || !sessionActive.getAsBoolean() || !acquisitionAllowed.getAsBoolean()) return new AcquireOutcome(false, false, false, generation);
				LockRecord record = existing == null ? new LockRecord() : existing;
				record.setTaskId(taskId);
				record.setOwnerKey(ownerKey);
				record.setUserKey(userKey);
				record.setDisplayName(displayName);
				record.setClientInstanceId(clientInstanceId);
				record.setUpdatedAt(now);
				record.setLeaseUntil(now + DEFAULT_LEASE_MS);
				if (!isCurrent(taskKey, generation) || !sessionActive.getAsBoolean() || !acquisitionAllowed.getAsBoolean()) return new AcquireOutcome(false, false, false, generation);
				if (sessionManaged) CollaborationMetadataStore.upsertSession(metadata, userKey, displayName, clientInstanceId, now);
				else CollaborationMetadataStore.refreshExistingSession(metadata, userKey, displayName, clientInstanceId, now);
				if (!isCurrent(taskKey, generation) || !sessionActive.getAsBoolean() || !acquisitionAllowed.getAsBoolean()) return new AcquireOutcome(false, false, false, generation);
				metadata.getLocks().put(String.valueOf(taskId), record);
				store.refreshProjectStats(metadata);
				return new AcquireOutcome(true, !alreadyOwned, false, generation);
			});
		} catch (RuntimeException failure) {
			// The transaction's outcome is unknown. Keep a prior lease cached for
			// renewal, but never use that cache as authorization.
			finishUnchanged(taskKey, generation);
			throw failure;
		}
		if (result.acquired && !sessionActive.getAsBoolean()) {
			release(taskId);
			return new AcquireOutcome(false, false, false, generation);
		}
		if (result.acquired) {
			if (!markHeld(taskKey, generation)) {
				Long retained = getHeldGeneration(taskId);
				if (retained != null) return new AcquireOutcome(true, result.newlyAcquired, false, retained.longValue());
				return new AcquireOutcome(false, false, false, generation);
			}
		} else if (result.confirmedFailure) {
			forgetIfCurrent(taskKey, generation);
		} else {
			finishUnchanged(taskKey, generation);
		}
		return result;
	}

	public void release(long taskId) {
		Long taskKey = Long.valueOf(taskId);
		long generation = beginOperation(taskKey, false);
		if (!isCurrent(taskKey, generation)) return;
		releaseLocked(taskId, taskKey, generation);
	}

	boolean releaseIfGeneration(long taskId, long expectedGeneration) {
		Long taskKey = Long.valueOf(taskId);
		long generation = nextGeneration.incrementAndGet();
		final boolean[] began = new boolean[1];
		localLocks.computeIfPresent(taskKey, (ignored, state) -> {
			if (state.generation != expectedGeneration || !state.held) return state;
			began[0] = true;
			return new LockState(generation, false, true);
		});
		if (!began[0]) return false;
		return releaseLocked(taskId, taskKey, generation);
	}

	private boolean releaseLocked(long taskId, Long taskKey, long generation) {
		try {
			Boolean released = store.withLockedMetadata(metadata -> {
				if (!isCurrent(taskKey, generation)) return Boolean.FALSE;
				cleanupExpiredLocks(metadata, System.currentTimeMillis());
				LockRecord existing = metadata.getLocks().get(String.valueOf(taskId));
				if (existing != null && ownerKey.equals(existing.getOwnerKey())) metadata.getLocks().remove(String.valueOf(taskId));
				store.refreshProjectStats(metadata);
				return Boolean.TRUE;
			});
			return Boolean.TRUE.equals(released);
		} finally {
			forgetIfCurrent(taskKey, generation);
		}
	}

	public void releaseAll() {
		List<Long> locks = new ArrayList<Long>(getLocalLocks());
		RuntimeException firstFailure = null;
		for (Long taskId : locks) {
			try {
				release(taskId.longValue());
			} catch (RuntimeException e) {
				if (firstFailure == null) firstFailure = e;
			}
		}
		if (firstFailure != null) {
			throw firstFailure;
		}
	}

	public java.util.Map<Long, Long> renewAll() {
		final java.util.Map<Long, Long> snapshot = new java.util.LinkedHashMap<Long, Long>();
		for (java.util.Map.Entry<Long, LockState> entry : localLocks.entrySet()) {
			LockState state = entry.getValue();
			if (state.held && !state.pending) snapshot.put(entry.getKey(), Long.valueOf(state.generation));
		}
		if (snapshot.isEmpty()) return java.util.Collections.emptyMap();
		Set<Long> lost = store.withLockedMetadata(metadata -> {
			long now = System.currentTimeMillis();
			cleanupExpiredLocks(metadata, now);
			Set<Long> missing = new LinkedHashSet<Long>();
			for (Long taskId : snapshot.keySet()) {
				if (!isCurrent(taskId, snapshot.get(taskId).longValue())) continue;
				LockRecord existing = metadata.getLocks().get(String.valueOf(taskId.longValue()));
				if (existing != null && ownerKey.equals(existing.getOwnerKey()) && existing.getLeaseUntil() > now) {
					existing.setUpdatedAt(now);
					existing.setLeaseUntil(now + DEFAULT_LEASE_MS);
				} else {
					missing.add(taskId);
				}
			}
			store.refreshProjectStats(metadata);
			return missing;
		});
		java.util.Map<Long, Long> confirmedLost = new java.util.LinkedHashMap<Long, Long>();
		for (Long taskId : lost) {
			long generation = snapshot.get(taskId).longValue();
			if (forgetIfHeldCurrent(taskId, generation)) confirmedLost.put(taskId, Long.valueOf(generation));
		}
		return confirmedLost;
	}

	public Set<Long> getLocalLocks() {
		Set<Long> held = new LinkedHashSet<Long>();
		for (java.util.Map.Entry<Long, LockState> entry : localLocks.entrySet()) if (entry.getValue().held) held.add(entry.getKey());
		return held;
	}

	boolean hasPendingOperations() {
		for (LockState state : localLocks.values()) if (state.pending) return true;
		return false;
	}

	public String describeOwner(long taskId) {
		Metadata metadata = store.load();
		if (metadata == null) {
			return null;
		}
		cleanupExpiredLocks(metadata, System.currentTimeMillis());
		LockRecord existing = metadata.getLocks().get(String.valueOf(taskId));
		return existing == null ? null : existing.getDisplayName();
	}

	public boolean isLockedByCurrentUser(long taskId) {
		LockState state = localLocks.get(Long.valueOf(taskId));
		return state != null && state.held;
	}

	Long getHeldGeneration(long taskId) {
		LockState state = localLocks.get(Long.valueOf(taskId));
		return state != null && state.held ? Long.valueOf(state.generation) : null;
	}

	private boolean isCurrent(Long taskId, long generation) {
		LockState state = localLocks.get(taskId);
		return state != null && state.generation == generation;
	}

	private void forgetIfCurrent(Long taskId, long generation) {
		localLocks.computeIfPresent(taskId, (ignored, state) -> state.generation == generation ? null : state);
	}

	private long beginOperation(Long taskId, boolean preserveHeld) {
		long generation = nextGeneration.incrementAndGet();
		localLocks.compute(taskId, (ignored, state) -> {
			if (state != null && state.generation >= generation) return state;
			return new LockState(generation, preserveHeld && state != null && state.held, true);
		});
		return generation;
	}

	private boolean markHeld(Long taskId, long generation) {
		final boolean[] marked = new boolean[1];
		localLocks.computeIfPresent(taskId, (ignored, state) -> {
			if (state.generation != generation) return state;
			marked[0] = true;
			return new LockState(generation, true, false);
		});
		return marked[0];
	}

	private void finishUnchanged(Long taskId, long generation) {
		localLocks.computeIfPresent(taskId, (ignored, state) -> {
			if (state.generation != generation) return state;
			return state.held ? new LockState(generation, true, false) : null;
		});
	}

	private boolean forgetIfHeldCurrent(Long taskId, long generation) {
		final boolean[] removed = new boolean[1];
		localLocks.computeIfPresent(taskId, (ignored, state) -> {
			if (state.generation != generation || !state.held) return state;
			removed[0] = true;
			return null;
		});
		return removed[0];
	}

	private void cleanupExpiredLocks(Metadata metadata, long now) {
		Iterator<LockRecord> it = metadata.getLocks().values().iterator();
		while (it.hasNext()) {
			LockRecord record = it.next();
			if (record.getLeaseUntil() > 0L && record.getLeaseUntil() < now) {
				it.remove();
			}
		}
	}
}
