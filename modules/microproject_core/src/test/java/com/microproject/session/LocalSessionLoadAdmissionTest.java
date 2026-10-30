/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.job.Job;
import com.microproject.job.JobQueue;

class LocalSessionLoadAdmissionTest {
	/** A relative alias needs the working drive, and hosted Windows keeps temp files on another one. */
	private static Path directoryOnWorkingDrive() throws java.io.IOException {
		return Files.createTempDirectory(Files.createDirectories(Path.of("build", "tmp")), "load-admission");
	}

	@Test
	void canonicalFileIdentityCoalescesRelativeAbsoluteAndPlatformCaseVariants() throws Exception {
		Path tempDirectory = directoryOnWorkingDrive();
		Path projectFile = Files.createFile(tempDirectory.resolve("MasterPlan.mpo"));
		Path relative = Path.of("").toAbsolutePath().normalize().relativize(projectFile.toAbsolutePath().normalize());
		LocalSession session = sessionWithQueue();

		String absoluteKey = loadJobKey(session, projectFile.toString());
		String relativeKey = loadJobKey(session, relative.toString());
		String caseVariantKey = loadJobKey(session, projectFile.toString().toUpperCase(Locale.ROOT));

		assertEquals(absoluteKey, relativeKey, "relative and absolute names for one file must coalesce");
		assertEquals(File.separatorChar == '\\', absoluteKey.equals(caseVariantKey),
			"case variants coalesce only on case-insensitive Windows paths");
		assertNotEquals(absoluteKey, loadJobKey(session, Files.createFile(tempDirectory.resolve("Other.mpo")).toString()));
	}

	@Test
	void missingFileLoadsUseProjectIdWhenAvailableAndLegacyNameOtherwise() {
		LocalSession session = sessionWithQueue();
		LoadOptions noIdentity = new LoadOptions();
		LoadOptions noIdentityAgain = new LoadOptions();
		LoadOptions missingProject = new LoadOptions();
		missingProject.setId(91001L);
		LoadOptions sameMissingProject = new LoadOptions();
		sameMissingProject.setId(91001L);
		LoadOptions otherMissingProject = new LoadOptions();
		otherMissingProject.setId(91002L);

		assertEquals("loadProject", session.getLoadProjectJob(noIdentity).getQueueAdmissionKey());
		assertEquals(session.getLoadProjectJob(noIdentity).getQueueAdmissionKey(),
			session.getLoadProjectJob(noIdentityAgain).getQueueAdmissionKey());
		assertEquals(session.getLoadProjectJob(missingProject).getQueueAdmissionKey(),
			session.getLoadProjectJob(sameMissingProject).getQueueAdmissionKey());
		assertNotEquals(session.getLoadProjectJob(missingProject).getQueueAdmissionKey(),
			session.getLoadProjectJob(otherMissingProject).getQueueAdmissionKey());
	}

	@Test
	void realPathAdmissionCoalescesRelativeAndSymbolicFileAliases() throws Exception {
		Path tempDirectory = directoryOnWorkingDrive();
		Path projectFile = Files.createFile(tempDirectory.resolve("real-project.mpo"));
		Path relative = Path.of("").toAbsolutePath().relativize(projectFile.toAbsolutePath());
		Path symbolicLink = tempDirectory.resolve("linked-project.mpo");
		try {
			Files.createSymbolicLink(symbolicLink, projectFile);
		} catch (UnsupportedOperationException | java.io.IOException | SecurityException unsupported) {
			assumeTrue(false, "symbolic links are unavailable in this environment: " + unsupported);
		}

		String canonicalKey = LocalSession.resolveProjectLoadAdmissionKey(projectFile.toString(), 0L);
		assertEquals(canonicalKey, LocalSession.resolveProjectLoadAdmissionKey(relative.toString(), 0L));
		assertEquals(canonicalKey, LocalSession.resolveProjectLoadAdmissionKey(symbolicLink.toString(), 0L));
		Path caseAlias = Path.of(projectFile.toString().toUpperCase(Locale.ROOT));
		boolean sameFile = Files.exists(caseAlias) && Files.isSameFile(caseAlias, projectFile);
		assertEquals(sameFile, canonicalKey.equals(
				LocalSession.resolveProjectLoadAdmissionKey(caseAlias.toString(), 0L)),
				"a case alias coalesces exactly when the file system resolves it to the same file");
	}

	@Test
	void realPathAdmissionCoalescesDirectoryJunctionAliasesOnWindows(@TempDir Path tempDirectory) throws Exception {
		assumeTrue(File.separatorChar == '\\', "Windows junctions are only available on Windows");
		Path projectFile = Files.createFile(tempDirectory.resolve("junction-project.mpo"));
		Path junction = tempDirectory.resolve("directory-junction");
		Process command = new ProcessBuilder("cmd.exe", "/c",
				"mklink /J \"" + junction + "\" \"" + tempDirectory + "\"")
			.redirectErrorStream(true).start();
		int result = command.waitFor();
		assumeTrue(result == 0 && Files.isDirectory(junction), "could not create a test junction");
		try {
			Path throughJunction = junction.resolve(projectFile.getFileName());
			assertEquals(LocalSession.resolveProjectLoadAdmissionKey(projectFile.toString(), 0L),
					LocalSession.resolveProjectLoadAdmissionKey(throughJunction.toString(), 0L));
		} finally {
			Files.deleteIfExists(junction);
		}
	}

	private static LocalSession sessionWithQueue() {
		LocalSession session = new LocalSession();
		session.setJobQueue(new JobQueue("load-admission-test", false));
		return session;
	}

	private static String loadJobKey(LocalSession session, String fileName) {
		LoadOptions options = new LoadOptions();
		options.setFileName(fileName);
		options.setImporter("missing-admission-test-importer");
		Job job = session.getLoadProjectJob(options);
		return job.getQueueAdmissionKey();
	}
}
