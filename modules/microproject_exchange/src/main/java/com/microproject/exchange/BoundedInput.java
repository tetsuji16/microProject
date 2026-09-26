/*
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */
package com.microproject.exchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Reads an input stream into memory while enforcing a configurable byte limit. */
public final class BoundedInput {
	public static final String XLSX_MAX_IMPORT_BYTES_PROPERTY = "microproject.xlsx.maxImportBytes";
	public static final long DEFAULT_XLSX_MAX_IMPORT_BYTES = 256L * 1024 * 1024;
	private static final int BUFFER_SIZE = 8192;

	private BoundedInput() {}

	public static byte[] readXlsxImport(InputStream input) throws IOException {
		return read(input, configuredXlsxLimit());
	}

	static byte[] read(InputStream input, long maxBytes) throws IOException {
		Objects.requireNonNull(input, "input");
		if (maxBytes < 0 || maxBytes >= Integer.MAX_VALUE) {
			throw new IllegalArgumentException("maxBytes must be between 0 and " + (Integer.MAX_VALUE - 1));
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.min(maxBytes, BUFFER_SIZE));
		byte[] buffer = new byte[BUFFER_SIZE];
		long total = 0;
		int count;
		while ((count = input.read(buffer)) != -1) {
			if (count == 0) {
				int single = input.read();
				if (single == -1) {
					break;
				}
				if (total == maxBytes) {
					throw tooLarge(maxBytes);
				}
				output.write(single);
				total++;
				continue;
			}
			if (count > maxBytes - total) {
				throw tooLarge(maxBytes);
			}
			output.write(buffer, 0, count);
			total += count;
		}
		return output.toByteArray();
	}

	private static long configuredXlsxLimit() {
		String configured = System.getProperty(XLSX_MAX_IMPORT_BYTES_PROPERTY);
		if (configured == null || configured.isBlank()) {
			return DEFAULT_XLSX_MAX_IMPORT_BYTES;
		}
		try {
			long limit = Long.parseLong(configured.trim());
			if (limit < 0 || limit >= Integer.MAX_VALUE) {
				throw new IllegalArgumentException("System property " + XLSX_MAX_IMPORT_BYTES_PROPERTY
					+ " must be between 0 and " + (Integer.MAX_VALUE - 1));
			}
			return limit;
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("System property " + XLSX_MAX_IMPORT_BYTES_PROPERTY
				+ " must be an integer byte count", exception);
		}
	}

	private static IOException tooLarge(long maxBytes) {
		return new IOException("XLSX import exceeds the configured limit of " + maxBytes + " bytes");
	}
}
