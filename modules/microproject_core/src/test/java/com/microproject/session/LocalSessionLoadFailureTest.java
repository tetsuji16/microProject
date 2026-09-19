/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.session;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.microproject.job.Job;
import com.microproject.job.JobQueue;

class LocalSessionLoadFailureTest {
	@Test
	void unknownImporterProducesAnObservableFailedLoadJob() throws Exception {
		Path projectFile = Files.createTempFile("unknown-importer-", ".mpo");
		try {
			LocalSession session = new LocalSession();
			session.setJobQueue(new JobQueue("load-failure-test", false));
			LoadOptions options = new LoadOptions();
			options.setFileName(projectFile.toString());
			options.setImporter("missing-mpo-importer");
			options.setSync(true);

			Job job = session.getLoadProjectJob(options);
			job.addSync();
			session.schedule(job);

			Exception failure = assertThrows(Exception.class, job::waitResult);
			assertInstanceOf(IllegalStateException.class, failure);
			assertNotNull(job.getFailureException());
			assertInstanceOf(IllegalArgumentException.class, job.getFailureException().getCause());
		} finally {
			Files.deleteIfExists(projectFile);
		}
	}
}
