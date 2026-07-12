package com.projectlibre1.collaboration;

import java.io.Serializable;

public class UserWorkspaceState implements Serializable {
	private static final long serialVersionUID = 1L;

	private String userKey;
	private String displayName;
	private long savedAt;
	private String workspacePayload;

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

	public long getSavedAt() {
		return savedAt;
	}

	public void setSavedAt(long savedAt) {
		this.savedAt = savedAt;
	}

	public String getWorkspacePayload() {
		return workspacePayload;
	}

	public void setWorkspacePayload(String workspacePayload) {
		this.workspacePayload = workspacePayload;
	}
}
