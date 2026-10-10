/* MIT License — Copyright (c) 2026 microProject */
package com.microproject.session;

import static org.junit.jupiter.api.Assertions.*;
import com.microproject.job.JobQueue;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class SessionFactoryDesktopTest {
	@Test void bothModelScopesShareOneDesktopSessionAndQueueUpdates() {
		SessionFactory factory = new SessionFactory();
		JobQueue first = new JobQueue("first", false);
		factory.setJobQueue(first);
		LocalSession session = factory.getLocalSession();
		assertTrue(session.isInitialized());
		assertSame(session, factory.getSession(true));
		assertSame(session, factory.getSession(false));
		assertSame(first, session.getJobQueue());
		JobQueue replacement = new JobQueue("replacement", false);
		factory.setJobQueue(replacement);
		assertSame(replacement, session.getJobQueue());
	}

	@Test void clearingRecreatesTheSessionWithTheCurrentQueue() {
		SessionFactory factory = new SessionFactory();
		JobQueue queue = new JobQueue("reset", false);
		factory.setJobQueue(queue);
		LocalSession previous = factory.getLocalSession();
		factory.clearSessions();
		LocalSession current = factory.getLocalSession();
		assertNotSame(previous, current);
		assertTrue(current.isInitialized());
		assertSame(queue, current.getJobQueue());
	}

	@Test void concurrentFirstAccessCannotCreateSeparateIdAllocators() throws Exception {
		SessionFactory factory = new SessionFactory();
		try (var executor = Executors.newFixedThreadPool(4)) {
			var sessions = new ArrayList<Future<LocalSession>>();
			for (int i = 0; i < 16; i++) sessions.add(executor.submit(factory::getLocalSession));
			LocalSession first = sessions.getFirst().get();
			for (var future : sessions) assertSame(first, future.get());
		}
	}
}
