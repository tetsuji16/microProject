/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

/** Frozen identifiers used at the POD compatibility boundary. */
public final class PodFormatIdentifiers {
	/** Separator in existing POD files; its bytes are part of the frozen wire format. */
	public static final String EMBEDDED_XML_SEPARATOR = "@@@@@@@@@@ProjectLibreSeparator_MSXML@@@@@@@@@@";

	private static final String LEGACY_PACKAGE = "com.projectlibre1.";
	private static final String[] LEGACY_ALLOWED_PREFIXES = {
		LEGACY_PACKAGE, "org.projectlibre.", "org.projectlibre1."
	};
	/** Class names and obsolete keys accepted only when reading persisted importer options. */
	public static final java.util.Map<String, String> READ_IMPORTER_ALIASES = java.util.Map.of(
		"server-local", com.microproject.session.LocalSession.LOCAL_PROJECT_IMPORTER,
		"com.microproject.exchange.LocalFileImporter", com.microproject.session.LocalSession.LOCAL_PROJECT_IMPORTER,
		"com.microproject.exchange.MpoFileImporter", com.microproject.session.LocalSession.MPO_PROJECT_IMPORTER,
		"com.microproject.exchange.ServerLocalFileImporter", com.microproject.session.LocalSession.LOCAL_PROJECT_IMPORTER,
		"com.microproject.exchange.MicrosoftImporter", com.microproject.session.LocalSession.MICROSOFT_PROJECT_IMPORTER,
		"com.projectlibre1.exchange.LocalFileImporter", com.microproject.session.LocalSession.LOCAL_PROJECT_IMPORTER,
		"com.projectlibre.exchange.LocalFileImporter", com.microproject.session.LocalSession.LOCAL_PROJECT_IMPORTER);
	public static final String LEGACY_MISSING_PROJECT_DATA = "com.projity.server.data.ProjectData";

	public static boolean isAllowedLegacyClass(String name) {
		for (String prefix : LEGACY_ALLOWED_PREFIXES)
			if (name.startsWith(prefix)) return true;
		return false;
	}

	public static String remapClassName(String name) {
		return name.replace(LEGACY_PACKAGE, "com.microproject.");
	}

	private PodFormatIdentifiers() {
	}
}
