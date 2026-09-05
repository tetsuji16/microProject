/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import java.io.File;
import java.util.Locale;

public enum ExportFormat {
	PDF("pdf"), PNG("png");
	private final String extension;
	ExportFormat(String extension) { this.extension = extension; }
	public String extension() { return extension; }

	File appendExtensionIfMissing(File file) {
		if (file == null || file.getName().toLowerCase(Locale.ROOT).endsWith("." + extension)) return file;
		File parent = file.getParentFile();
		return parent == null ? new File(file.getName() + "." + extension) : new File(parent, file.getName() + "." + extension);
	}

	static ExportFormat fromFileName(File file) {
		if (file == null) return null;
		String name = file.getName().toLowerCase(Locale.ROOT);
		for (ExportFormat format : values()) if (name.endsWith("." + format.extension)) return format;
		return null;
	}
}
