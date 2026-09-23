/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import com.microproject.job.Job;
import com.microproject.pm.task.Project;

class AbstractSessionTest {
	@Test
	void singleProjectSaveDelegatesAsATypedSingletonList() {
		RecordingSession session = new RecordingSession();
		SaveOptions options = new SaveOptions();

		session.getSaveProjectJob((Project) null, options);

		assertEquals(1, session.projects.size());
		assertNull(session.projects.getFirst());
		assertSame(options, session.options);
	}

	private static final class RecordingSession extends AbstractSession {
		private List<Project> projects;
		private SaveOptions options;

		@Override
		public Job getSaveProjectJob(List<Project> projects, SaveOptions options) {
			this.projects = projects;
			this.options = options;
			return null;
		}

		@Override
		public boolean projectExists(long uniqueId) {
			return false;
		}

		@Override
		public long getId() {
			return 0L;
		}

		@Override
		public Job getLoadProjectJob(LoadOptions options) {
			return null;
		}

		@Override
		public Job getLoadProjectDescriptorsJob(boolean includeProjects, List descriptors, boolean allowOpenAs) {
			return null;
		}

		@Override
		public Job getCloseProjectsJob(Collection projects) {
			return null;
		}
	}
}
