/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

public enum ExportFormat {
	PDF("pdf"), PNG("png");
	private final String extension;
	ExportFormat(String extension) { this.extension = extension; }
	public String extension() { return extension; }
}
