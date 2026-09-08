/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/** Performs cheap, side-effect-free MPO container checks before model loading. */
public final class MpoArchiveValidator {
	private MpoArchiveValidator() {
	}

	public static MpoValidationResult validate(Path source) {
		if (source == null) return new MpoValidationResult(false, "source-null");
		if (!Files.isRegularFile(source)) return new MpoValidationResult(false, "source-missing");
		try (ZipFile zip = new ZipFile(source.toFile(), StandardCharsets.UTF_8)) {
			var mime = zip.getEntry(MpoFileImporter.MIMETYPE_ENTRY);
			if (mime == null) return new MpoValidationResult(false, "mimetype-missing");
			var manifest = zip.getEntry(MpoFileImporter.MANIFEST_ENTRY);
			if (manifest == null) return new MpoValidationResult(false, "manifest-missing");
			return new MpoValidationResult(true, "");
		} catch (IOException failure) {
			return new MpoValidationResult(false, failure.getClass().getSimpleName());
		}
	}
}
