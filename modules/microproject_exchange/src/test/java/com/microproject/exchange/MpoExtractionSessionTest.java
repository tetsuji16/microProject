/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MpoExtractionSessionTest {
	@TempDir Path temporaryDirectory;

	@Test
	void closeRemovesExtractedEntriesAndIsIdempotent() throws Exception {
		MpoExtractionSession session = MpoExtractionSession.open(temporaryDirectory);
		Path extracted = session.extract("child.mpo", new byte[] {1, 2, 3});
		Path directory = session.directory();
		assertTrue(Files.isRegularFile(extracted));
		session.close();
		session.close();
		assertFalse(Files.exists(directory));
	}

	@Test
	void manifestRecordsPurposeCreationAndProcessInstanceState() throws Exception {
		MpoExtractionSession session = MpoExtractionSession.open(temporaryDirectory);
		Path manifest = session.directory().resolveSibling(session.directory().getFileName() + ".manifest");
		String contents = Files.readString(manifest);

		assertTrue(contents.contains("purpose=mpof-extraction\n"));
		assertTrue(contents.matches("(?s).*createdAt=.+\\n.*"));
		assertTrue(contents.matches("(?s).*instanceId=[0-9a-f-]{36}\\n.*"));
		assertTrue(contents.matches("(?s).*processId=[0-9]+\\n.*"));
		assertTrue(contents.matches("(?s).*processStart=.+\\n.*"));
		assertTrue(contents.contains("state=open\n"));

		session.close();
	}

	@Test
	void rejectsDuplicateNamesAndDoesNotAllowUseAfterClose() throws Exception {
		MpoExtractionSession session = MpoExtractionSession.open(temporaryDirectory);
		session.extract("nested/child.mpo", new byte[] {1});
		assertThrows(java.io.IOException.class, () -> session.extract("../../outside.mpo", new byte[] {0}));
		assertThrows(java.nio.file.FileAlreadyExistsException.class,
				() -> session.extract("other/child.mpo", new byte[] {2}));
		session.close();
		assertThrows(IllegalStateException.class, () -> session.extract("later.mpo", new byte[] {3}));
	}
}
