/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocaleDialogTest {
	@Test
	void countryEqualityPreservesNullTypeAndCodeBehavior() {
		LocaleDialog.Country us = new LocaleDialog.Country("US", "United States");

		assertEquals(us, new LocaleDialog.Country("US", "United States"));
		assertFalse(us.equals(new LocaleDialog.Country("CA", "Canada")));
		assertFalse(us.equals(null));
		assertFalse(us.equals("US"));
	}

	@Test
	void copyResourceWritesAllBytesWhenStreamReturnsShortReads(@TempDir Path dir) throws IOException {
		byte[] data = new byte[10_000];
		for (int i = 0; i < data.length; i++)
			data[i] = (byte) i;
		InputStream shortReads = new ByteArrayInputStream(data) {
			@Override
			public synchronized int read(byte[] b, int off, int len) {
				return super.read(b, off, Math.min(len, 7));
			}

			@Override
			public synchronized int available() {
				return 7;
			}
		};
		File target = dir.resolve("client.properties").toFile();

		LocaleDialog.copyResource(shortReads, target);

		assertArrayEquals(data, Files.readAllBytes(target.toPath()));
	}

	@Test
	void copyResourceReportsFailureWhenTargetCannotBeCreated(@TempDir Path dir) {
		File target = dir.resolve("missing-dir").resolve("client.properties").toFile();

		assertThrows(IOException.class,
				() -> LocaleDialog.copyResource(new ByteArrayInputStream(new byte[] { 1 }), target));
		assertThrows(FileNotFoundException.class, () -> LocaleDialog.copyResource(null, dir.resolve("x").toFile()));
	}
}
