/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import com.microproject.pm.task.Project;

/**
 * Process-local ownership bridge for extracted MPOF children.
 *
 * <p>The model module must not depend on exchange, so a transient extraction
 * session is kept here until the owning document frame closes.  Identity keys
 * are intentional: project equality is not a lifecycle identity.</p>
 */
public final class MpoExtractionOwnershipRegistry {
	private static final Object LOCK = new Object();
	private static final Map<Project, MpoExtractionSession> SESSIONS = new IdentityHashMap<>();

	private MpoExtractionOwnershipRegistry() {
	}

	/** Replaces a reload's previous session and closes it before returning. */
	public static void attach(Project project, MpoExtractionSession session) {
		Objects.requireNonNull(project, "project");
		Objects.requireNonNull(session, "session");
		MpoExtractionSession previous;
		synchronized (LOCK) {
			previous = SESSIONS.put(project, session);
		}
		if (previous != null && previous != session) previous.close();
	}

	/** Closes and forgets the session owned by one project, if present. */
	public static boolean close(Project project) {
		if (project == null) return false;
		MpoExtractionSession session;
		synchronized (LOCK) {
			session = SESSIONS.remove(project);
		}
		if (session == null) return false;
		session.close();
		return true;
	}

	/** Closes all still-owned sessions during application shutdown. */
	public static void closeAll() {
		MpoExtractionSession[] sessions;
		synchronized (LOCK) {
			sessions = SESSIONS.values().toArray(MpoExtractionSession[]::new);
			SESSIONS.clear();
		}
		for (MpoExtractionSession session : sessions) session.close();
	}

	/** Package-visible test/lifecycle probe; does not expose mutable registry state. */
	static int size() {
		synchronized (LOCK) {
			return SESSIONS.size();
		}
	}
}
