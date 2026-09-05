/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import java.io.File;
import java.util.Objects;

public record ExportTarget(File file, ExportFormat format) {
	public ExportTarget {
		Objects.requireNonNull(file, "file");
		Objects.requireNonNull(format, "format");
	}
}
