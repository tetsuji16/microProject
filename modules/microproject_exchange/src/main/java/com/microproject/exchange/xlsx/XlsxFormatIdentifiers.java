/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange.xlsx;

/** Stable write identifiers and read-only legacy aliases for embedded project data. */
final class XlsxFormatIdentifiers {
	static final String META_SHEET = "_MicroProject_META";
	static final String DATA_SHEET = "_MicroProject_MSPDI";
	static final String NATIVE_DATA_SHEET = "_MicroProject_NATIVE";
	static final String LEGACY_META_SHEET = "_PL_META";
	static final String LEGACY_DATA_SHEET = "_PL_DATA";
	static final String LEGACY_NATIVE_DATA_SHEET = "_PL_NATIVE";
	static final String LEGACY_EMBEDDED_MSPDI_SHEET = "_ProjectLibre_MSPDI";
	static final String CURRENT_KIND = "microproject";
	static final String LEGACY_KIND = "projectlibre";
	static final String CURRENT_NATIVE_MARKER = "MicroProjectNativeData";
	static final String LEGACY_NATIVE_MARKER = "ProjectLibreNativeData";

	static final String PROJECT_FILE_KIND = "projectfile";
	static final String PROJECT_FILE_VERSION = "2";
	static final String NATIVE_VERSION = "3";
	static final String WORKBOOK_KIND_LABEL = "WorkbookKind";
	static final String SCHEMA_VERSION_LABEL = "SchemaVersion";
	static final String PAYLOAD_KIND_LABEL = "PayloadKind";
	static final String PAYLOAD_LABEL = "Payload";

	private XlsxFormatIdentifiers() {
	}
}
