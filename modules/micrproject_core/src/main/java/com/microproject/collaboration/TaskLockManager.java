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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.microproject.collaboration.CollaborationMetadataStore.LockRecord;
import com.microproject.collaboration.CollaborationMetadataStore.Metadata;
import com.microproject.collaboration.CollaborationMetadataStore.UserRecord;

public class TaskLockManager {
	public static final long DEFAULT_LEASE_MS = 15000L;

	private final CollaborationMetadataStore store;
	private final String userKey;
	private final String displayName;
	private final String clientInstanceId;
	private final String ownerKey;
	private final Set<Long> localLocks = java.util.Collections.synchronizedSet(new LinkedHashSet<Long>());

	public TaskLockManager(CollaborationMetadataStore store, String userKey, String displayName, String clientInstanceId) {
		this.store = store;
		this.userKey = userKey;
		this.displayName = displayName;
		this.clientInstanceId = clientInstanceId;
		this.ownerKey = userKey + "#" + clientInstanceId;
	}

	public boolean acquire(long taskId) {
		Boolean result = store.withLockedMetadata(new CollaborationMetadataStore.MetadataCallback<Boolean>() {
			public Boolean execute(Metadata metadata) {
				cleanupExpiredLocks(metadata, System.currentTimeMillis());
				LockRecord existing = metadata.getLocks().get(String.valueOf(taskId));
				if (existing != null && !ownerKey.equals(existing.getOwnerKey())) {
					return Boolean.FALSE;
				}
				long now = System.currentTimeMillis();
				LockRecord record = existing == null ? new LockRecord() : existing;
				record.setTaskId(taskId);
				record.setOwnerKey(ownerKey);
				record.setUserKey(userKey);
				record.setDisplayName(displayName);
				record.setClientInstanceId(clientInstanceId);
				record.setUpdatedAt(now);
				record.setLeaseUntil(now + DEFAULT_LEASE_MS);
				metadata.getLocks().put(String.valueOf(taskId), record);
				UserRecord userRecord = metadata.getUsers().get(userKey);
				if (userRecord == null) {
					userRecord = new UserRecord();
					userRecord.setUserKey(userKey);
					userRecord.setDisplayName(displayName);
					userRecord.setClientInstanceId(clientInstanceId);
					metadata.getUsers().put(userKey, userRecord);
				}
				userRecord.setDisplayName(displayName);
				userRecord.setClientInstanceId(clientInstanceId);
				userRecord.setLastSeenAt(now);
				store.refreshProjectStats(metadata);
				return Boolean.TRUE;
			}
		});
		if (Boolean.TRUE.equals(result)) {
			localLocks.add(Long.valueOf(taskId));
			return true;
		}
		return false;
	}

	public void release(long taskId) {
		store.mutate(new CollaborationMetadataStore.MetadataMutation() {
			public void mutate(Metadata metadata) {
				cleanupExpiredLocks(metadata, System.currentTimeMillis());
				LockRecord existing = metadata.getLocks().get(String.valueOf(taskId));
				if (existing != null && ownerKey.equals(existing.getOwnerKey())) {
					metadata.getLocks().remove(String.valueOf(taskId));
				}
				store.refreshProjectStats(metadata);
			}
		});
		localLocks.remove(Long.valueOf(taskId));
	}

	public void releaseAll() {
		List<Long> locks;
		synchronized (localLocks) {
			locks = new ArrayList<Long>(localLocks);
		}
		for (Long taskId : locks) {
			release(taskId.longValue());
		}
	}

	public void renewAll() {
		final List<Long> locksToRenew;
		synchronized (localLocks) {
			if (localLocks.isEmpty()) {
				return;
			}
			locksToRenew = new ArrayList<Long>(localLocks);
		}
		store.mutate(new CollaborationMetadataStore.MetadataMutation() {
			public void mutate(Metadata metadata) {
				long now = System.currentTimeMillis();
				cleanupExpiredLocks(metadata, now);
				for (Long taskId : locksToRenew) {
					LockRecord existing = metadata.getLocks().get(String.valueOf(taskId.longValue()));
					if (existing != null && ownerKey.equals(existing.getOwnerKey())) {
						existing.setUpdatedAt(now);
						existing.setLeaseUntil(now + DEFAULT_LEASE_MS);
					}
				}
				store.refreshProjectStats(metadata);
			}
		});
	}

	public Set<Long> getLocalLocks() {
		synchronized (localLocks) {
			return new LinkedHashSet<Long>(localLocks);
		}
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
		return localLocks.contains(Long.valueOf(taskId));
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
